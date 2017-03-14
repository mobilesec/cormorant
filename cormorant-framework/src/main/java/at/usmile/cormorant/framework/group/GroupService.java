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
import android.os.IBinder;
import android.util.Log;

import org.jivesoftware.smack.chat2.Chat;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import at.usmile.cormorant.framework.messaging.CormorantMessage;
import at.usmile.cormorant.framework.messaging.CormorantMessageConsumer;
import at.usmile.cormorant.framework.messaging.MessagingService;

public class GroupService extends Service implements CormorantMessageConsumer {

    private final static int PIN_LENGTH = 5;
    private final static String LOG_TAG = GroupService.class.getSimpleName();
    private final IBinder mBinder = new GroupService.GroupServiceBinder();
    private final Random random = new Random();

    //TODO persistence + recover after destroy
    private List<TrustedDevice> group = new LinkedList<>();
    //TODO remove failed challenge after timeout
    private Map<String, GroupChallenge> challenges = new HashMap<>();

    public GroupService() {
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
        else Log.w(LOG_TAG, "MessageType unknown" + cormorantMessage.getClass());
    }

    //--> DEVICE A
    //TODO check duplicate
    //Called from Activity
    public void sendChallengeRequest(String targetJabberId){
        int pin = createPin();
        Log.d(LOG_TAG, "Sending ChallengeRequest to " + targetJabberId + " with pin: " + pin);

        GroupChallenge groupChallenge = new GroupChallenge(
                pin,
                targetJabberId);
        String challengeId = UUID.randomUUID().toString();
        challenges.put(challengeId, groupChallenge);
        GroupChallengeRequest groupChallengeRequest = new GroupChallengeRequest(challengeId, Build.MODEL);
        messagingService.sendMessage(targetJabberId, groupChallengeRequest);
        //TODO Show Pin on this device
    }

    private void checkChallengeResponse(GroupChallengeResponse groupChallengeResponse, Chat chat){
        Log.d(LOG_TAG, "Checking ChallengeResponse from " + chat.getXmppAddressOfChatPartner());
        GroupChallenge groupChallenge = challenges.get(groupChallengeResponse.getChallengeId());
        //TODO allow multiple retries?
        challenges.remove(groupChallengeResponse.getChallengeId());
        if(groupChallenge.getPin() == groupChallengeResponse.getPin()){
            addTrustedDevice(groupChallengeResponse.getTrustedDevice());
            //TODO sendFeedback to challenger (group + key) + removePinDialog + successToast
        }
        else {
            //TODO sendError to challenger
        }
    }
    //<-- DEVICE A

    //--> DEVICE B
    private void receiveChallengeRequest(GroupChallengeRequest groupChallengeRequest, Chat chat){
        Log.d(LOG_TAG, "Received ChallengeRequest from " + chat.getXmppAddressOfChatPartner());
        messagingService.sendMessage(chat, new GroupChallengeResponse(
                groupChallengeRequest.getChallengeId(),
                //TODO Get Display Size
                new TrustedDevice(Build.MANUFACTURER, Build.MODEL, 5, messagingService.getDeviceID()),
                11111));
        //TODO showUserInput for PIN and use it
    }
    //<-- DEVICE B

    //TODO Do real synchronisation
    private void synchronizeGroupInfo(List<TrustedDevice> trustedDevices){
        this.group = trustedDevices;
    }

    private void removeTrustedDevice(TrustedDevice trustedDevice){
        //TODO create new key + sync
        group.remove(trustedDevice);
    }

    private void addTrustedDevice(TrustedDevice trustedDevice){
        //TODO create new key + sync
        Log.d(LOG_TAG, "Adding new trusted device: " + trustedDevice);
        group.add(trustedDevice);
        Log.d(LOG_TAG, "Active Group: " + group);
    }

    private int createPin(){
        return 11111;
        /* TODO Uncomment after Debugging with fixed pin
        int pinPrefix = 10 * PIN_LENGTH;
        int pinRandomBorder = (pinPrefix * 10) - (pinPrefix + 1);
        return random.nextInt(pinRandomBorder) + pinPrefix;
        */
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
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            messagingServiceBound = false;
        }
    };

}
