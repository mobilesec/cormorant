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
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import org.jivesoftware.smack.chat2.Chat;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import at.usmile.cormorant.framework.common.TypedServiceBinder;
import at.usmile.cormorant.framework.common.TypedServiceConnection;
import at.usmile.cormorant.framework.location.CoarseDeviceDistanceHelper;
import at.usmile.cormorant.framework.location.bluetooth.BeaconPublisher;
import at.usmile.cormorant.framework.location.bluetooth.BeaconScanner;
import at.usmile.cormorant.framework.location.bluetooth.DistanceHelper;
import at.usmile.cormorant.framework.messaging.CormorantMessage;
import at.usmile.cormorant.framework.messaging.CormorantMessageConsumer;
import at.usmile.cormorant.framework.messaging.DeviceIdListener;
import at.usmile.cormorant.framework.messaging.MessagingService;

public class GroupService extends Service implements
        CormorantMessageConsumer, DeviceIdListener, BeaconScanner.BeaconDistanceResultListener,
        CoarseDeviceDistanceHelper.CoarseDistanceListener {

    public final static int CHALLENGE_REQUEST_CANCELED = -1;
    private final static int PIN_LENGTH = 4;
    private final static String LOG_TAG = GroupService.class.getSimpleName();
    private final Random random = new Random();

    private static final String PREFERENCE_NAME = "cormorant";
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
        preferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        initSelf();
        initGroup();
        initLocationComponents();
        bindService(new Intent(this, MessagingService.class), messageService, Context.BIND_AUTO_CREATE);

        //FIXME MESSAGING WORKAROUND
        TrustedDevice selfDevice = new TrustedDevice("SelfId", "SelfDevice", 5, "selfId", UUID.fromString("a55e1589-d8c1-40b5-a399-5b07676a9c22"));
        Location location = new Location("Bla");
        location.setLatitude(51.731181);
        location.setLongitude(8.736454);
        selfDevice.setLocation(location);
        this.self = selfDevice;

        TrustedDevice otherDevice = new TrustedDevice("OtherId", "OtherDevice", 5, "jabberId", UUID.fromString("e55e1589-d8c1-40b5-a399-5b07676a9c22"));
        location = new Location("Bla");
        location.setLatitude(52.751181);
        location.setLongitude(8.746454);
        otherDevice.setLocation(location);

        TrustedDevice otherDevice2 = new TrustedDevice("OtherId2", "OtherDevice2", 5, "jabberId", UUID.fromString("f55e1589-d8c1-40b5-a399-5b07676a9c22"));
        location = new Location("Bla");
        location.setLatitude(52.251181);
        location.setLongitude(9.746454);
        otherDevice2.setLocation(location);

        group.clear();
        addTrustedDevice(selfDevice);
        addTrustedDevice(otherDevice);
        addTrustedDevice(otherDevice2);

        if(beaconScanner != null) beaconScanner.startScanner();

        coarseDeviceDistanceHelper.subscribeToLocationUpdates();
        //FIXME MESSAGING WORKAROUND
    }

    private void initLocationComponents(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null) {
            this.beaconPublisher = new BeaconPublisher(bluetoothAdapter.getBluetoothLeAdvertiser());
            this.beaconScanner = new BeaconScanner(bluetoothAdapter.getBluetoothLeScanner(),
                    getApplicationContext(), this);
        }
        else {
            Log.w(LOG_TAG, "Bluetooth for beacons not available");
            Toast.makeText(this, "Bluetooth for beacons not available", Toast.LENGTH_SHORT).show();
        }
        coarseDeviceDistanceHelper = new CoarseDeviceDistanceHelper(this, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        messageService.get().removeMessageListener(CormorantMessage.TYPE.GROUP, this);
        messageService.get().removeDeviceIdListener(this);
        if (messageService.isBound()) unbindService(messageService);

        if(beaconPublisher != null)beaconPublisher.stopBeacon();
        if(beaconScanner != null) beaconScanner.stopScanner();

        coarseDeviceDistanceHelper.unsubscribeFromLocationUpdates();
    }

    public List<TrustedDevice> getGroup() {
        return group;
    }

    public TrustedDevice getSelf() {
        return self;
    }

    @Override
    public void handleMessage(CormorantMessage cormorantMessage, Chat chat) {
        Log.d(LOG_TAG, "Handling Message:" + cormorantMessage);
        if (cormorantMessage instanceof GroupChallengeRequest) {
            receiveChallengeRequest((GroupChallengeRequest) cormorantMessage, chat);
        } else if (cormorantMessage instanceof GroupChallengeResponse) {
            checkChallengeResponse((GroupChallengeResponse) cormorantMessage, chat);
        } else if (cormorantMessage instanceof GroupUpdateMessage) {
            receiveGroupUpdate((GroupUpdateMessage) cormorantMessage);
        } else Log.w(LOG_TAG, "MessageType unknown" + cormorantMessage.getClass());
    }

    @Override
    public void setJabberId(String jabberId) {
        if (getSelf() == null) {
            this.self = new TrustedDevice(Build.MANUFACTURER, Build.MODEL, getScreenSize(),
                    messageService.get().getDeviceID(), UUID.randomUUID());
            saveSelf();
        }
        //TODO Raise expection if jabberId changed?
        else if (!getSelf().getJabberId().equals(jabberId)) Log.w(LOG_TAG, "JabberId changed!");
        if (group.isEmpty()) addTrustedDevice(self);

        //TODO Let the user set a custom txPower value
        //Start beacon here to ensure uuid is already set.
        if(beaconPublisher != null) beaconPublisher.startBeacon(self.getUuid());
        if(beaconScanner != null) beaconScanner.startScanner();

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
        Log.d(LOG_TAG, "Sending ChallengeRequest to " + deviceToTrust.getJabberId() + " with pin: " + pin);

        this.currentGroupChallenge = new GroupChallenge(pin, deviceToTrust.getJabberId());
        GroupChallengeRequest groupChallengeRequest = new GroupChallengeRequest(self.getJabberId());
        messageService.get().sendMessage(deviceToTrust, groupChallengeRequest);
        return pin;
    }

    private void checkChallengeResponse(GroupChallengeResponse groupChallengeResponse, Chat chat) {
        Log.d(LOG_TAG, "Checking ChallengeResponse from " + chat.getXmppAddressOfChatPartner());
        if (currentGroupChallenge.getPin() == groupChallengeResponse.getPin()) {
            addTrustedDevice(groupChallengeResponse.getTrustedDevice());
            sendBroadcast(new Intent(DialogPinShowActivity.COMMAND_CLOSE));
            showToast("New device " + groupChallengeResponse.getTrustedDevice().getDevice() + " added successfully");
            Intent intent = new Intent(this, GroupListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            sendBroadcast(new Intent(DialogPinShowActivity.COMMAND_PIN_FAILED));
        }
    }
    //<-- DEVICE A

    //--> DEVICE B
    private void receiveChallengeRequest(GroupChallengeRequest groupChallengeRequest, Chat chat) {
        Log.d(LOG_TAG, "Received ChallengeRequest from " + chat.getXmppAddressOfChatPartner());

        Intent intent = new Intent(this, DialogPinEnterActivity.class);
        intent.putExtra(DialogPinEnterActivity.KEY_SENDER_JABBER_ID, groupChallengeRequest.getSenderDeviceId());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void respondToChallengeRequest(int pin, TrustedDevice challengingDevice) {
        Log.d(LOG_TAG, "Responding to Challenge " + currentGroupChallenge);
        messageService.get().sendMessage(challengingDevice, new GroupChallengeResponse(
                self,
                pin));
    }
    //<-- DEVICE B

    //TODO Do real synchronisation + how to handle offline devices during sync?
    private void synchronizeGroupInfo() {
        Log.d(LOG_TAG, "Synching new group: " + group);
        for (TrustedDevice device : group) {
            if (!device.equals(self)) {
                messageService.get().sendMessage(device, new GroupUpdateMessage(group));
            }
        }
    }

    private void synchronizeGroupInfo(TrustedDevice removedDevice) {
        Log.d(LOG_TAG, "Synching device removal to: " + removedDevice);
        messageService.get().sendMessage(removedDevice, new GroupUpdateMessage(group));
        synchronizeGroupInfo();
    }

    private void receiveGroupUpdate(GroupUpdateMessage groupUpdateMessage) {
        Log.d(LOG_TAG, "Received GroupUpdateMessage: " + groupUpdateMessage);
        //device has been removed from group by other device
        if (!groupUpdateMessage.getGroup().contains(self)) {
            this.group.clear();
            this.group.add(self);
            showToast("Device has been removed from authentication group");
        } else {
            int oldGroupCount = this.group.size();
            this.group.clear();
            this.group.addAll(groupUpdateMessage.getGroup());

            showToast("Group has been refreshed - 1 device "
                    + ((oldGroupCount - this.group.size() < 0) ? "added" : "removed"));
        }
        notifyGroupChangeListeners();
    }

    public void removeTrustedDevice(TrustedDevice deviceToRemove) {
        Log.d(LOG_TAG, "Removing device: " + deviceToRemove);
        //TODO create new key
        if (deviceToRemove.equals(self)) {
            this.group.remove(self);
            synchronizeGroupInfo();
            this.group.clear();
            this.group.add(self);
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
//        synchronizeGroupInfo(); //FIXME MESSAGING WORKAROUND
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
        }
    }

    private void saveSelf() {
        preferences.edit().putString(PREF_GROUP_SELF, gson.toJson(getSelf())).apply();
    }

    private void initSelf() {
        String selfJson = preferences.getString(PREF_GROUP_SELF, "");
        if (selfJson.isEmpty()) this.group = new LinkedList<>();
        else {
            this.self = gson.fromJson(selfJson, TrustedDevice.class);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return TypedServiceBinder.from(this);
    }

    private TypedServiceConnection<MessagingService> messageService = new TypedServiceConnection<MessagingService>() {

        @Override
        public void onServiceConnected(MessagingService service) {
            service.addMessageListener(CormorantMessage.TYPE.GROUP, GroupService.this);
            service.addDeviceIdListener(GroupService.this);
        }
    };

    @Override
    public void onResult(ScanResult result, DistanceHelper.DISTANCE distance, UUID uuid) {
        Log.d(LOG_TAG, String.format("Device is %s with uuid: %s", distance, uuid));
        group.stream().filter(device -> device.getUuid().equals(uuid))
                .findFirst()
                .get()
                .setDistanceToOtherDeviceBluetooth(distance);
        notifyGroupChangeListeners();
    }

    @Override
    public void onLocationChanged(Location location) {
        self.setLocation(location);
        coarseDeviceDistanceHelper.calculateDistances(group, self);
        notifyGroupChangeListeners();
//        synchronizeGroupInfo(); //FIXME MESSAGING WORKAROUND
    }
}
