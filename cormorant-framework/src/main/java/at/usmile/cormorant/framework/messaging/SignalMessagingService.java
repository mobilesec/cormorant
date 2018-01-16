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
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.internal.configuration.SignalServiceConfiguration;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import at.usmile.cormorant.framework.common.TypedServiceBinder;
import at.usmile.cormorant.framework.group.TrustedDevice;

public class SignalMessagingService extends Service {

    private final static String LOG_TAG = SignalMessagingService.class.getSimpleName();

    private static final String USER_AGENT = "[CORMORANT]";
    private final static String MESSAGE_SPLIT_CHAR = "!";

    private final Gson gson = new GsonBuilder().create();

    private SignalParameter param;

    private SignalServiceAccountManager accountManager;
    private SignalServiceMessageReceiver messageReceiver;
    private SignalServiceConfiguration config;

    private Set<IConnectionListener> connectionListeners = new HashSet<>();
    private ListMultimap<CormorantMessage.TYPE, CormorantMessageConsumer> messageListeners = ArrayListMultimap.create();
    private SignalProtocolStoreImpl signalProtocolStore;

    private SignalListenTask listenTask;

    private ExecutorService listenPool = Executors.newFixedThreadPool(1);

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "MessagingService started");

        SharedPreferences prefs = getSharedPreferences(SignalParameter.PREFERENCE_NAME, Context.MODE_PRIVATE);
        if (!SignalParameter.isPresent(prefs)) {
            param = SignalParameter.init();
            param.save(prefs);
            IdentityKeyUtil.saveIdentityKeys(this, param.getIdentityKey());
        } else {
            param = SignalParameter.load(prefs);
        }

        config = SignalParameter.getServiceConfiguration(new CormorantTrustStore(this));
        accountManager = new SignalServiceAccountManager(config, param.getUser().toString(), param.getPassword(), USER_AGENT);
        messageReceiver = new SignalServiceMessageReceiver(config, param.getUser().toString(), param.getPassword(), param.getSignalingKey(), USER_AGENT, new SignalConnectivityListener());
        signalProtocolStore = new SignalProtocolStoreImpl(this);
        listenTask = new SignalListenTask(param, messageReceiver, signalProtocolStore, messageListeners, connectionListeners);

        if (param.isNew()) {
            // create account and start listening laster
            new CreateAccountTask(prefs).execute();
        } else {
            // immediately start listening
            listenPool.execute(listenTask);
        }



        Log.w(LOG_TAG, param.toString());
    }

    public UUID getDeviceID() {
        return param.getUser();
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

    private class MessageSendTask extends AsyncTask<String, String, Void> {

        SignalServiceMessageSender messageSender = new SignalServiceMessageSender(config, param.getUser().toString(), param.getPassword(),
                signalProtocolStore, USER_AGENT, Optional.absent(), Optional.absent());


        @Override
        protected Void doInBackground(String... strings) {
            try {
                SignalServiceAddress recipient = new SignalServiceAddress(strings[0]);
                SignalServiceDataMessage message = SignalServiceDataMessage.newBuilder().withBody(strings[1]).build();

                Log.w(LOG_TAG, "Sending message to " + recipient + "(" + message + ")");

                messageSender.sendMessage(recipient, message);
            } catch (UntrustedIdentityException | IOException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
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

                SignedPreKeyRecord signedPreKey = PreKeyUtil.generateSignedPreKey(SignalMessagingService.this, param.getIdentityKey(), true);
                accountManager.createCormorantAccount(param.getSignalingKey(), param.getRegistrationId(), true);
                accountManager.setPreKeys(param.getIdentityKey().getPublicKey(), signedPreKey, param.getOneTimePreKeys());
                accountManager.setGcmId(Optional.of(gcmToken));
                param.setGcmId(gcmToken);
                param.setAccountCreated();
                param.save(prefs);

                signalProtocolStore.storeSignedPreKey(0, signedPreKey);
                for (int i = 0; i < param.getOneTimePreKeys().size(); i++) {
                    signalProtocolStore.storePreKey(i, param.getOneTimePreKeys().get(i));
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            listenPool.execute(listenTask);

            return null;
        }

    }

    private String createMessage(CormorantMessage cormorantMessage) {
        String message = cormorantMessage.getClazz() + MESSAGE_SPLIT_CHAR;
        message += gson.toJson(cormorantMessage);
        Log.v(LOG_TAG, "Created message: " + message);
        return message;
    }


    public void sendMessage(UUID recipient, String message) {
        Log.w(LOG_TAG, "sending message to " + recipient + ": " + message);

        new MessageSendTask().execute(recipient.toString(), message);
    }

    public void sendMessage(TrustedDevice device, CormorantMessage cormorantMessage) {
        sendMessage(device.getId(), createMessage(cormorantMessage));
    }

    @Override
    public void onDestroy() {
        listenPool.shutdown();
        listenTask.shutdown();

        Log.d(LOG_TAG, "MessagingService stopped");
    }

    public void addConnectionListener(IConnectionListener listener) {
        connectionListeners.add(listener);
    }

    public void removeConnectionListener(IConnectionListener listener) {
        connectionListeners.remove(listener);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return TypedServiceBinder.from(this);
    }


}
