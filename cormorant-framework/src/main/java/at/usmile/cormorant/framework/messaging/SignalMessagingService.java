/**
 * Copyright 2016 - 2017
 * <p>
 * Daniel Hintze <daniel.hintze@fhdw.de>
 * Sebastian Scholz <sebastian.scholz@fhdw.de>
 * Rainhard D. Findling <rainhard.findling@fh-hagenberg.at>
 * Muhammad Muaaz <muhammad.muaaz@usmile.at>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.usmile.cormorant.framework.messaging;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.iid.InstanceID;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.thoughtcrime.securesms.crypto.IdentityKeyUtil;
import org.thoughtcrime.securesms.crypto.PreKeyUtil;
import org.thoughtcrime.securesms.crypto.storage.SignalParameter;
import org.thoughtcrime.securesms.crypto.storage.SignalProtocolStoreImpl;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceMessagePipe;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.SignalServiceCipher;
import org.whispersystems.signalservice.api.messages.SignalServiceContent;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.websocket.ConnectivityListener;
import org.whispersystems.signalservice.internal.configuration.SignalServiceConfiguration;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import at.usmile.cormorant.framework.common.TypedServiceBinder;
import at.usmile.cormorant.framework.group.GroupChallengeRequest;
import at.usmile.cormorant.framework.group.GroupChallengeResponse;
import at.usmile.cormorant.framework.group.GroupUpdateMessage;
import at.usmile.cormorant.framework.group.TrustedDevice;
import at.usmile.cormorant.framework.lock.DeviceLockCommand;

public class SignalMessagingService extends Service {

    private final static String LOG_TAG = SignalMessagingService.class.getSimpleName();

    private static final String USER_AGENT = "[CORMORANT]";
    private final static String MESSAGE_SPLIT_CHAR = "!";

    private final Gson gson = new GsonBuilder().create();

    private SignalParameter signalParameter;

    private SignalServiceAccountManager accountManager;
    private SignalServiceMessageSender messageSender;
    private SignalServiceMessageReceiver messageReceiver;

    private SignalServiceMessagePipe messagePipe;

    private ListMultimap<CormorantMessage.TYPE, CormorantMessageConsumer> messageListeners = ArrayListMultimap.create();
    private List<DeviceIdListener> deviceIdListeners = new LinkedList<>();
    private SignalProtocolStoreImpl signalProtocolStore;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "MessagingService started");

        SharedPreferences prefs = getSharedPreferences(SignalParameter.PREFERENCE_NAME, Context.MODE_PRIVATE);
        if (!SignalParameter.isPresent(prefs)) {
            signalParameter = SignalParameter.init();
            IdentityKeyUtil.saveIdentityKeys(this, signalParameter.getIdentityKey());
        } else {
            signalParameter = SignalParameter.load(prefs);
        }

        Log.i(LOG_TAG, signalParameter.toString());

        SignalServiceConfiguration serviceConfiguration = SignalParameter.getServiceConfiguration(new CormorantTrustStore(this));

        accountManager = new SignalServiceAccountManager(serviceConfiguration, signalParameter.getUser(), signalParameter.getPassword(), USER_AGENT);

        if (signalParameter.isNew()) {
            new CreateAccountTask(prefs).execute();
        }

        signalProtocolStore = new SignalProtocolStoreImpl(this);


        messageSender = new SignalServiceMessageSender(serviceConfiguration, signalParameter.getUser(), signalParameter.getPassword(),
                signalProtocolStore, USER_AGENT, Optional.absent(), Optional.absent());

        messageReceiver = new SignalServiceMessageReceiver(serviceConfiguration, signalParameter.getUser(), signalParameter.getPassword(), signalParameter.getSignalingKey(), USER_AGENT,
                new ConnectivityListener() {

                    @Override
                    public void onDisconnected() {
                        System.out.println(
                                "SignaleMessagingService.SignaleMessagingService(...).new ConnectivityListener() {...}.onDisconnected()");
                    }

                    @Override
                    public void onConnecting() {
                        System.out
                                .println("SignaleMessagingService.SignaleMessagingService(...).new ConnectivityListener() {...}.onConnecting()");
                    }

                    @Override
                    public void onConnected() {
                        System.out.println("SignaleMessagingService.SignaleMessagingService(...).new ConnectivityListener() {...}.onConnected()");
                    }

                    @Override
                    public void onAuthenticationFailure() {
                        System.out.println(
                                "SignaleMessagingService.SignaleMessagingService(...).new ConnectivityListener() {...}.onAuthenticationFailure()");
                    }
                });

        Log.w(LOG_TAG, signalParameter.toString());

        new ListenTask().execute();
    }

    public String getDeviceID() {
        return signalParameter.getUser();
    }

    public void addMessageListener(CormorantMessage.TYPE messageType, CormorantMessageConsumer messageConsumer) {
        messageListeners.put(messageType, messageConsumer);
        Log.d(LOG_TAG, "New MessageConsumer added: " + messageConsumer);
    }

    public void removeMessageListener(CormorantMessage.TYPE messageType, CormorantMessageConsumer messageConsumer) {
        if (messageListeners.remove(messageType, messageConsumer))
            Log.d(LOG_TAG, "Removed MessageConsumer: " + messageConsumer);
        else
            Log.w(LOG_TAG, "MessageConsumer not found");
    }

    public void addDeviceIdListener(DeviceIdListener deviceIdListener) {
        this.deviceIdListeners.add(deviceIdListener);
    }

    public void removeDeviceIdListener(DeviceIdListener deviceIdListener) {
        this.deviceIdListeners.remove(deviceIdListener);
    }

    public void send(String recipient, String msg) throws Exception {
        Log.d(LOG_TAG, "send message to " + recipient);

        messageSender.sendMessage(new SignalServiceAddress(recipient),
                SignalServiceDataMessage.newBuilder().withBody(msg).build());
    }


    private void listen() throws Exception {
        while (true) {
            Log.w(LOG_TAG, "Waiting for websocket state change....");
            //waitForConnectionNecessary();

            Log.w(LOG_TAG, "Making websocket connection....");
            messagePipe = messageReceiver.createMessagePipe();

            SignalServiceMessagePipe localPipe = messagePipe;

            try {
                while (true) {
                    try {
                        Log.w(LOG_TAG, "Reading message...");
                        localPipe.read(1, TimeUnit.MINUTES,
                                envelope -> {
                                    Log.w(LOG_TAG, "Retrieved envelope! " + envelope.getSource());

                                    SignalServiceCipher cipher = new SignalServiceCipher(new SignalServiceAddress(signalParameter.getUser()), signalProtocolStore);
                                    try {
                                        Log.w(LOG_TAG, "Decrypting");
                                        SignalServiceContent message = cipher.decrypt(envelope);
                                        Log.w(LOG_TAG, "Received message: " + message.getDataMessage().get().getBody().get());

                                        final CormorantMessage cormorantMessage = parseMessage(message.getDataMessage().get().getBody().get());
                                        if (cormorantMessage == null) return;
                                        List<CormorantMessageConsumer> messageConsumers = messageListeners.get(cormorantMessage.getType());
                                        if (messageConsumers != null) {
                                            for (CormorantMessageConsumer eachConsumer : messageConsumers) {
                                                eachConsumer.handleMessage(cormorantMessage, envelope.getSource());
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.w(LOG_TAG, e);
                                        throw new RuntimeException(e);
                                    }
                                });
                    } catch (TimeoutException e) {
                        Log.w(LOG_TAG, "Application level read timeout...");
                    } catch (InvalidVersionException e) {
                        Log.w(LOG_TAG, e);
                    }
                }
            } catch (Throwable e) {
                Log.w(LOG_TAG, e);
            } finally {
                Log.w(LOG_TAG, "Shutting down pipe...");
                localPipe.shutdown();
            }

            Log.w(LOG_TAG, "Looping...");
        }

    }

    private class SendMessageTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            try {
                send(strings[0], strings[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private class ListenTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                listen();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private class CreateAccountTask extends AsyncTask<String, Void, Void> {

        private SharedPreferences prefs;

        private CreateAccountTask(SharedPreferences prefs) {
            this.prefs = prefs;
        }

        @Override
        protected Void doInBackground(String... strings) {
            try {
                String authorizedEntity = "457730743558";
                String scope = "GCM";
                String gcmToken = InstanceID.getInstance(SignalMessagingService.this).getToken(authorizedEntity, scope);


                SignedPreKeyRecord signedPreKey = PreKeyUtil.generateSignedPreKey(SignalMessagingService.this, signalParameter.getIdentityKey(), true);
                accountManager.createCormorantAccount(signalParameter.getSignalingKey(), signalParameter.getRegistrationId(), true);
                accountManager.setPreKeys(signalParameter.getIdentityKey().getPublicKey(), signedPreKey, signalParameter.getOneTimePreKeys());
                accountManager.setGcmId(Optional.of(gcmToken));
                signalParameter.setGcmId(gcmToken);
                signalParameter.save(prefs);

                signalProtocolStore.storeSignedPreKey(0, signedPreKey);
                for (int i = 0; i < signalParameter.getOneTimePreKeys().size(); i++) {
                    signalProtocolStore.storePreKey(i, signalParameter.getOneTimePreKeys().get(i));
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return null;
        }

    }

    private String createMessage(CormorantMessage cormorantMessage) {
        String message = cormorantMessage.getClazz() + MESSAGE_SPLIT_CHAR;
        message += gson.toJson(cormorantMessage);
        Log.v(LOG_TAG, "Created message: " + message);
        return message;
    }

    private CormorantMessage parseMessage(String message) {
        String[] parts = message.split(MESSAGE_SPLIT_CHAR);
        if (CormorantMessage.CLASS.GROUP_CHALLENGE_REQUEST.toString().equals(parts[0])) {
            return gson.fromJson(parts[1], GroupChallengeRequest.class);
        } else if (CormorantMessage.CLASS.GROUP_CHALLENGE_RESPONSE.toString().equals(parts[0])) {
            return gson.fromJson(parts[1], GroupChallengeResponse.class);
        } else if (CormorantMessage.CLASS.GROUP_UPDATE.toString().equals(parts[0])) {
            return gson.fromJson(parts[1], GroupUpdateMessage.class);
        } else if (CormorantMessage.CLASS.DEVICE_LOCK_COMMAND.toString().equals(parts[0])) {
            return gson.fromJson(parts[1], DeviceLockCommand.class);
        } else {
            Log.w(LOG_TAG, "ClassType unknown" + parts[0]);
            return null;
        }
    }

    public void sendMessage(TrustedDevice device, CormorantMessage cormorantMessage) {
        Log.w(LOG_TAG, "sendMessage(" + createMessage(cormorantMessage) + ")");

        new SendMessageTask().execute(device.getId(), createMessage(cormorantMessage));
    }

  /*  private void updateDeviceId(String jabberId) {
        for (DeviceIdListener eachDeviceIdListener : this.deviceIdListeners) {
            eachDeviceIdListener.setJabberId(jabberId);
        }
    }*/

    @Override
    public void onDestroy() {
        messagePipe.shutdown();
        Log.d(LOG_TAG, "MessagingService stopped");
    }


    @Override
    public IBinder onBind(Intent intent) {
        return TypedServiceBinder.from(this);
    }


}
