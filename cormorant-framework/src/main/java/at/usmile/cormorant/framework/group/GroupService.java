/**
 * Copyright 2016 - 2017
 *
 * Daniel Hintze <daniel.hintze@fhdw.de>
 * Sebastian Scholz <sebastian.scholz@fhdw.de>
 * Rainhard D. Findling <rainhard.findling@fh-hagenberg.at>
 * Muhammad Muaaz <muhammad.muaaz@usmile.at>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.usmile.cormorant.framework.group;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.thoughtcrime.securesms.crypto.storage.SignalParameter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import at.usmile.cormorant.framework.common.TypedServiceBinder;
import at.usmile.cormorant.framework.common.TypedServiceConnection;
import at.usmile.cormorant.framework.location.CoarseDeviceDistanceHelper;
import at.usmile.cormorant.framework.location.bluetooth.BeaconPublisher;
import at.usmile.cormorant.framework.location.bluetooth.BeaconScanner;
import at.usmile.cormorant.framework.location.bluetooth.DistanceHelper;
import at.usmile.cormorant.framework.lock.LockService;
import at.usmile.cormorant.framework.messaging.CormorantMessage;
import at.usmile.cormorant.framework.messaging.CormorantMessageConsumer;
import at.usmile.cormorant.framework.messaging.IConnectionListener;
import at.usmile.cormorant.framework.messaging.SignalMessagingService;
import at.usmile.cormorant.framework.plugin.PluginInfo;
import at.usmile.cormorant.framework.plugin.PluginManager;

public class GroupService extends Service implements
        CormorantMessageConsumer,  BeaconScanner.BeaconDistanceResultListener,
        CoarseDeviceDistanceHelper.CoarseDistanceListener,
        PluginManager.PluginChangeListener,
        LockService.LockStateListener, IConnectionListener {

    public final static int CHALLENGE_REQUEST_CANCELED = -1;
    private final static int PIN_LENGTH = 4;
    private final static String LOG_TAG = GroupService.class.getSimpleName();
    private final Random random = new Random();

    private static final String PREF_GROUP_LIST = "groupList";
    private static final String PREF_GROUP_SELF = "groupSelf";

    private final Gson gson = new GsonBuilder().create();

    private SharedPreferences preferences;
    private TrustedDevice self;
    private GroupChallenge currentGroupChallenge;
    private List<TrustedDevice> group;
    private List<GroupChangeListener> groupChangeListeners = new LinkedList<>();
    private BeaconScanner beaconScanner;
    private BeaconPublisher beaconPublisher;
    private CoarseDeviceDistanceHelper coarseDeviceDistanceHelper;

    public GroupService() {
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "GroupService started");
        preferences = getSharedPreferences(SignalParameter.PREFERENCE_NAME, Context.MODE_PRIVATE);
        initData();
        initLocationComponents();

        bindService(new Intent(this, SignalMessagingService.class), messageService, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, LockService.class), lockService, Context.BIND_AUTO_CREATE);

        PluginManager.getInstance().addPluginChangeListener(this);
    }

    private void initLocationComponents() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Log.w(LOG_TAG, "Bluetooth for beacons not available");
            Toast.makeText(this, "Bluetooth for beacons not available", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if (bluetoothAdapter != null && bluetoothLeAdvertiser != null && bluetoothLeScanner != null) {
            this.beaconPublisher = new BeaconPublisher(bluetoothLeAdvertiser);
            this.beaconScanner = new BeaconScanner(bluetoothLeScanner,
                    getApplicationContext(), this);
        } else {
            Log.w(LOG_TAG, "Bluetooth for beacons not available");
            Toast.makeText(this, "Bluetooth for beacons not available", Toast.LENGTH_SHORT).show();
        }
        coarseDeviceDistanceHelper = new CoarseDeviceDistanceHelper(this, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (messageService != null) {
            messageService.get().removeMessageListener(CormorantMessage.TYPE.GROUP, this);
            messageService.get().removeConnectionListener(this);
            if (messageService.isBound()) unbindService(messageService);
        }

        if (lockService.isBound()) unbindService(lockService);

        if (beaconPublisher != null) beaconPublisher.stopBeacon();
        if (beaconScanner != null) beaconScanner.stopScanner();

        if (coarseDeviceDistanceHelper != null)
            coarseDeviceDistanceHelper.unsubscribeFromLocationUpdates();

        PluginManager.getInstance().removePluginChangeListener(this);
    }

    public List<TrustedDevice> getGroup() {
        return group;
    }

    public TrustedDevice getSelf() {
        return this.self;
    }

    @Override
    public void handleMessage(CormorantMessage cormorantMessage, String source) {
        Log.v(LOG_TAG, "Handling Message:" + cormorantMessage);
        if (cormorantMessage instanceof GroupChallengeRequest) {
            receiveChallengeRequest((GroupChallengeRequest) cormorantMessage, source);
        } else if (cormorantMessage instanceof GroupChallengeResponse) {
            checkChallengeResponse((GroupChallengeResponse) cormorantMessage, source);
        } else if (cormorantMessage instanceof GroupUpdateMessage) {
            receiveGroupUpdate((GroupUpdateMessage) cormorantMessage);
        } else Log.w(LOG_TAG, "MessageType unknown" + cormorantMessage.getClass());
    }

    @Override
    public void connected(UUID id) {
        if (getSelf() == null) {
            this.self = new TrustedDevice(id, Build.MANUFACTURER, Build.MODEL, getScreenSize());
            Log.w(LOG_TAG, "self = " + id);
            saveSelf();
        }
        //TODO Raise expection if jabberId changed?
        else if (!getSelf().getId().equals(id)) Log.w(LOG_TAG, "Id changed!");
        if (group.isEmpty()) addTrustedDevice(getSelf());

        //TODO Let the user set a custom txPower value
        //Start beacon here to ensure uuid is already set.
        UUID uuid = getSelf().getId();

        if (beaconPublisher != null) beaconPublisher.startBeacon(uuid);
        if (beaconScanner != null) beaconScanner.startScanner();

        if (coarseDeviceDistanceHelper != null)
            coarseDeviceDistanceHelper.subscribeToLocationUpdates();
    }

    public void addGroupChangeListener(GroupChangeListener groupChangeListener) {
        this.groupChangeListeners.add(groupChangeListener);
    }

    public void removeGroupChangeListener(GroupChangeListener groupChangeListener) {
        this.groupChangeListeners.remove(groupChangeListener);
    }

    private void notifyGroupChangeListeners() {
        saveGroup();
        for (GroupChangeListener eachGroupChangeListener : groupChangeListeners) {
            eachGroupChangeListener.groupChanged();
        }
    }

    //--> DEVICE A
    public int sendChallengeRequest(TrustedDevice deviceToTrust) {
        if (group.contains(deviceToTrust)) {
            Log.d(LOG_TAG, "ChallengeRequest canceled - device is already in group: " + deviceToTrust);
            showToast("Device is already in group");
            return CHALLENGE_REQUEST_CANCELED;
        }

        int pin = createPin();
        Log.d(LOG_TAG, "Sending ChallengeRequest to " + deviceToTrust.getId() + " with pin: " + pin);

        this.currentGroupChallenge = new GroupChallenge(pin, deviceToTrust.getId());
        GroupChallengeRequest groupChallengeRequest = new GroupChallengeRequest(getSelf().getId());
        messageService.get().sendMessage(deviceToTrust, groupChallengeRequest);
        return pin;
    }

    private void checkChallengeResponse(GroupChallengeResponse groupChallengeResponse, String source) {
        Log.d(LOG_TAG, "Checking ChallengeResponse from " + source);
        if (currentGroupChallenge.getPin() == groupChallengeResponse.getPin()) {
            addTrustedDevice(groupChallengeResponse.getTrustedDevice());
            sendBroadcast(new Intent(DialogPinShowActivity.COMMAND_CLOSE));
            showToast("New device " + groupChallengeResponse.getTrustedDevice().getModel() + " added successfully");
        } else {
            sendBroadcast(new Intent(DialogPinShowActivity.COMMAND_PIN_FAILED));
        }
    }
    //<-- DEVICE A

    //--> DEVICE B
    private void receiveChallengeRequest(GroupChallengeRequest groupChallengeRequest, String source) {
        Log.d(LOG_TAG, "Received ChallengeRequest from " + source);

        Intent intent = new Intent(this, DialogPinEnterActivity.class);
        intent.putExtra(DialogPinEnterActivity.KEY_SENDER_ID, groupChallengeRequest.getSenderDeviceId());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void respondToChallengeRequest(int pin, TrustedDevice challengingDevice) {
        Log.d(LOG_TAG, "Responding to Challenge " + currentGroupChallenge);
        messageService.get().sendMessage(challengingDevice, new GroupChallengeResponse(
                getSelf(),
                pin));
    }
    //<-- DEVICE B

    //TODO Do real synchronisation + how to handle offline devices during sync?
    private void synchronizeGroupInfo() {
        Log.v(LOG_TAG, "Syncing new group: " + group);
        for (TrustedDevice device : group) {
            if (!device.equals(getSelf())) {
                messageService.get().sendMessage(device, new GroupUpdateMessage(group));
            }
        }
    }

    private void synchronizeGroupInfo(TrustedDevice removedDevice) {
        Log.d(LOG_TAG, "Syncing device removal to: " + removedDevice);
        messageService.get().sendMessage(removedDevice, new GroupUpdateMessage(group));
        synchronizeGroupInfo();
    }

    private void receiveGroupUpdate(GroupUpdateMessage groupUpdateMessage) {
        Log.v(LOG_TAG, "Received GroupUpdateMessage: " + groupUpdateMessage);
        //device has been removed from group by other device
        if (!groupUpdateMessage.getGroup().contains(getSelf())) {
            this.group.clear();
            this.group.add(getSelf());
            showToast("Device has been removed from authentication group");
        } else {
            this.group.clear();
            this.group.addAll(groupUpdateMessage.getGroup());
            rebindSelfToGroup();
            showToast("Group has been refreshed");
        }
        notifyGroupChangeListeners();
    }

    public void removeTrustedDevice(TrustedDevice deviceToRemove) {
        Log.d(LOG_TAG, "Removing device: " + deviceToRemove);
        //TODO create new key
        if (deviceToRemove.equals(getSelf())) {
            this.group.remove(getSelf());
            synchronizeGroupInfo();
            this.group.clear();
            this.group.add(getSelf());
        } else {
            this.group.remove(deviceToRemove);
            synchronizeGroupInfo(deviceToRemove);
        }
        notifyGroupChangeListeners();
    }

    private void addTrustedDevice(TrustedDevice trustedDevice) {
        //TODO create new key
        Log.d(LOG_TAG, "Adding new trusted device: " + trustedDevice);
        group.add(trustedDevice);
        Log.d(LOG_TAG, "Active Group: " + group);
        synchronizeGroupInfo();
        notifyGroupChangeListeners();
    }

    private int createPin() {
        int pinPrefix = (int) Math.pow(10, (PIN_LENGTH - 1));
        int pinRandomBorder = (pinPrefix * 10) - (pinPrefix + 1);
        return random.nextInt(pinRandomBorder) + pinPrefix;
    }

    //TODO Find more reliable implementation (navigation bar is not count = wrong results)
    private double getScreenSize() {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        double wi = (double) width / (double) dm.xdpi;
        double hi = (double) height / (double) dm.ydpi;
        double x = Math.pow(wi, 2);
        double y = Math.pow(hi, 2);
        return Math.sqrt(x + y);
    }

    private void showToast(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        });
    }

    private void initData() {
        //fixed order
        initSelf();
        initGroup();
    }

    private void saveGroup() {
        preferences.edit().putString(PREF_GROUP_LIST, gson.toJson(getGroup())).apply();
    }

    private void initGroup() {
        String groupJson = preferences.getString(PREF_GROUP_LIST, "");
        if (groupJson.isEmpty()) this.group = new LinkedList<>();
        else {
            Type groupListType = new TypeToken<LinkedList<TrustedDevice>>() {
            }.getType();
            this.group = gson.fromJson(groupJson, groupListType);
            rebindSelfToGroup();
        }
    }

    private void rebindSelfToGroup() {
        Collections.replaceAll(this.group, getSelf(), getSelf());
    }

    private void saveSelf() {
        preferences.edit().putString(PREF_GROUP_SELF, gson.toJson(getSelf())).apply();
    }

    private void initSelf() {
        String selfJson = preferences.getString(PREF_GROUP_SELF, "");

        Log.w(LOG_TAG, selfJson);

        if (selfJson.isEmpty()) this.group = new LinkedList<>();
        else {
            this.self = gson.fromJson(selfJson, TrustedDevice.class);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return TypedServiceBinder.from(this);
    }

    private TypedServiceConnection<SignalMessagingService> messageService = new TypedServiceConnection<SignalMessagingService>() {

        @Override
        public void onServiceConnected(SignalMessagingService service) {
            service.addMessageListener(CormorantMessage.TYPE.GROUP, GroupService.this);
            service.addConnectionListener(GroupService.this);
        }

        @Override
        public void onServiceDisconnected(SignalMessagingService service) {
            service.removeMessageListener(CormorantMessage.TYPE.GROUP, GroupService.this);
            service.removeConnectionListener(GroupService.this);
        }
    };

    private TypedServiceConnection<LockService> lockService = new TypedServiceConnection<LockService>() {

        @Override
        public void onServiceConnected(LockService service) {
            service.addLockStateListener(GroupService.this);
        }

        @Override
        public void onServiceDisconnected(LockService service) {
            service.removeLockStateListener(GroupService.this);
        }
    };

    @Override
    public void onResult(ScanResult result, DistanceHelper.DISTANCE distance, UUID uuid) {
        Log.d(LOG_TAG, String.format("Device is %s with uuid: %s", distance, uuid));
        group.stream().filter(device -> device.getId().equals(uuid.toString()))
                .findFirst()
                .ifPresent(device -> device.setDistanceToOtherDeviceBluetooth(distance));
        notifyGroupChangeListeners();
    }

    @Override
    public void onLocationChanged(Location location) {
        getSelf().setLocation(location);
        coarseDeviceDistanceHelper.calculateDistances(group, getSelf());

        try {
            //TODO Might have a bad performance impact
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                getSelf().getLocation().setAddress(address.getAddressLine(0));
            } else {
                getSelf().getLocation().setAddress("Address: UNKNOWN");
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Couldn't get address for location.", e);
        }

        notifyGroupChangeListeners();
        synchronizeGroupInfo();
    }

    @Override
    public void onPluginsChanged() {
        List<PluginData> pluginDataList = new LinkedList<>();
        List<PluginInfo> pluginListReadOnly = PluginManager.getInstance().getPluginListReadOnly();
        pluginListReadOnly.forEach(eachPlugin -> pluginDataList.add(new PluginData(eachPlugin)));
        getSelf().setActivePlugins(pluginDataList);
        synchronizeGroupInfo();
    }

    @Override
    public void onLockStateChanged(boolean lockState) {
        if (self == null) return;

        self.setLocked(lockState);
        notifyGroupChangeListeners();
        synchronizeGroupInfo();
    }

}
