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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import org.jivesoftware.smack.chat2.Chat;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import at.usmile.cormorant.framework.GroupListActivity;
import at.usmile.cormorant.framework.messaging.CormorantMessage;
import at.usmile.cormorant.framework.messaging.CormorantMessageConsumer;
import at.usmile.cormorant.framework.messaging.DeviceIdListener;
import at.usmile.cormorant.framework.messaging.MessagingService;

public class GroupService extends Service implements CormorantMessageConsumer, DeviceIdListener {

    private final static int PIN_LENGTH = 4;
    private final static String LOG_TAG = GroupService.class.getSimpleName();
    private final IBinder mBinder = new GroupService.GroupServiceBinder();
    private final Random random = new Random();

    //TODO persistence + recover after destroy (all fields below)
    private List<TrustedDevice> group = new LinkedList<>();
    //TODO remove challenge after timeout or just do not persist?
    //<challengeId, GC>
    private Map<String, GroupChallenge> challenges = new HashMap<>();
    private TrustedDevice self;

    public GroupService() {
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "GroupService started");
        Intent intent = new Intent(this, MessagingService.class);
        bindService(intent, messageServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        messagingService.removeMessageListener(CormorantMessage.TYPE.GROUP, this);
        messagingService.removeDeviceIdListener(this);
        if(messagingServiceBound) unbindService(messageServiceConnection);
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
        if(cormorantMessage instanceof GroupChallengeRequest){
            receiveChallengeRequest((GroupChallengeRequest) cormorantMessage, chat);
        }
        else if (cormorantMessage instanceof GroupChallengeResponse){
            checkChallengeResponse((GroupChallengeResponse) cormorantMessage, chat);
        }
        else if (cormorantMessage instanceof GroupUpdateMessage){
            receiveGroupUpdate((GroupUpdateMessage) cormorantMessage);
        }
        else Log.w(LOG_TAG, "MessageType unknown" + cormorantMessage.getClass());
    }

    @Override
    public void setJabberId(String jabberId) {
        self = new TrustedDevice(Build.MANUFACTURER, Build.MODEL, getScreenSize(), messagingService.getDeviceID());
        if(group.isEmpty()) addTrustedDevice(self);
    }

    //--> DEVICE A
    //TODO check duplicate
    public int sendChallengeRequest(String targetJabberId){
        int pin = createPin();
        Log.d(LOG_TAG, "Sending ChallengeRequest to " + targetJabberId + " with pin: " + pin);

        GroupChallenge groupChallenge = new GroupChallenge(
                pin,
                targetJabberId);
        String challengeId = UUID.randomUUID().toString();
        challenges.put(challengeId, groupChallenge);
        GroupChallengeRequest groupChallengeRequest = new GroupChallengeRequest(challengeId, self.getJabberId());
        messagingService.sendMessage(targetJabberId, groupChallengeRequest);
        return pin;
    }

    private void checkChallengeResponse(GroupChallengeResponse groupChallengeResponse, Chat chat){
        Log.d(LOG_TAG, "Checking ChallengeResponse from " + chat.getXmppAddressOfChatPartner());
        GroupChallenge groupChallenge = challenges.get(groupChallengeResponse.getChallengeId());
        challenges.remove(groupChallengeResponse.getChallengeId());
        if(groupChallenge.getPin() == groupChallengeResponse.getPin()){
            addTrustedDevice(groupChallengeResponse.getTrustedDevice());
            sendBroadcast(new Intent(DialogPinShowActivity.COMMAND_CLOSE));
            showToast("New device " + groupChallengeResponse.getTrustedDevice().getDevice() + " added successfully");
            Intent intent = new Intent(this, GroupListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        else {
            sendBroadcast(new Intent(DialogPinShowActivity.COMMAND_PIN_FAILED));
        }
    }
    //<-- DEVICE A

    //--> DEVICE B
    private void receiveChallengeRequest(GroupChallengeRequest groupChallengeRequest, Chat chat){
        Log.d(LOG_TAG, "Received ChallengeRequest from " + chat.getXmppAddressOfChatPartner());

        Intent intent = new Intent(this, DialogPinEnterActivity.class);
        intent.putExtra(DialogPinEnterActivity.KEY_CHALLENGE_ID, groupChallengeRequest.getChallengeId());
        intent.putExtra(DialogPinEnterActivity.KEY_SENDER_JABBER_ID, groupChallengeRequest.getSenderDeviceId());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void respondToChallengeRequest(String challengeId, int pin, String senderJabberId){
        Log.d(LOG_TAG, "Responding to Challenge " + challengeId);
        messagingService.sendMessage(senderJabberId, new GroupChallengeResponse(
            challengeId,
            self,
            pin));
    }
    //<-- DEVICE B

    //TODO Do real synchronisation + how to handle offline devices during sync?
    private void synchronizeGroupInfo(){
        for(TrustedDevice eachTrustedDevice : group){
            if(eachTrustedDevice.getJabberId().equals(messagingService.getDeviceID())) continue;
            messagingService.sendMessage(eachTrustedDevice.getJabberId(), new GroupUpdateMessage(group));
        }
    }

    private void receiveGroupUpdate(GroupUpdateMessage groupUpdateMessage){
        int oldGroupCount = this.group.size();
        this.group = groupUpdateMessage.getGroup();
        showToast("Group has been refreshed - 1 device "
                + ((oldGroupCount - this.group.size() < 0) ?  "added" : "removed"));
    }

    //TODO add function to groupListActivity
    private void removeTrustedDevice(TrustedDevice trustedDevice){
        //TODO create new key + edit group of removed device
        this.group.remove(trustedDevice);
        synchronizeGroupInfo();
    }

    private void addTrustedDevice(TrustedDevice trustedDevice){
        //TODO create new key
        Log.d(LOG_TAG, "Adding new trusted device: " + trustedDevice);
        group.add(trustedDevice);
        Log.d(LOG_TAG, "Active Group: " + group);
        synchronizeGroupInfo();
    }

    private int createPin(){
        int pinPrefix = (int) Math.pow(10, (PIN_LENGTH - 1));
        int pinRandomBorder = (pinPrefix * 10) - (pinPrefix + 1);
        return random.nextInt(pinRandomBorder) + pinPrefix;
    }

    //TODO Find more reliable implementation (navigation bar is not count = wrong results)
    private double getScreenSize() {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        double wi = (double) width / (double) dm.xdpi;
        double hi = (double) height / (double) dm.ydpi;
        double x = Math.pow(wi, 2);
        double y = Math.pow(hi, 2);
        return Math.sqrt(x + y);
    }

    private void showToast(final String message){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class GroupServiceBinder extends Binder {
        public GroupService getService() {
            return GroupService.this;
        }
    }

    private MessagingService messagingService;
    private boolean messagingServiceBound = false;

    private ServiceConnection messageServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MessagingService.MessagingServiceBinder binder = (MessagingService.MessagingServiceBinder) service;
            messagingService = binder.getService();
            messagingServiceBound = true;
            messagingService.addMessageListener(CormorantMessage.TYPE.GROUP, GroupService.this);
            messagingService.addDeviceIdListener(GroupService.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            messagingServiceBound = false;
        }
    };

}
