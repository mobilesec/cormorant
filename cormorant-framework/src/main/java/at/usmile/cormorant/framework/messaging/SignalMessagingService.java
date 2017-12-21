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
import android.os.IBinder;
import android.util.Log;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.impl.InMemorySignalProtocolStore;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceMessagePipe;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.SignalServiceCipher;
import org.whispersystems.signalservice.api.messages.SignalServiceContent;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.websocket.ConnectivityListener;
import org.whispersystems.signalservice.internal.configuration.SignalServiceConfiguration;

import java.util.List;
import java.util.concurrent.TimeUnit;

import at.usmile.cormorant.framework.common.TypedServiceBinder;

public class SignalMessagingService extends Service {

    private final static String LOG_TAG = SignalMessagingService.class.getSimpleName();

    private static final String PREFERENCE_NAME = "cormorant";

    private static final String USER_AGENT = "[CORMORANT]";

    private SignalParameter signalParameter;

    private int registrationId;
    private SignalServiceAccountManager accountManager;
    private SignalServiceMessageSender messageSender;
    private SignalServiceMessageReceiver messageReceiver;

    private SignalServiceMessagePipe messagePipe;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "MessagingService started");

        SharedPreferences prefs = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        if (!SignalParameter.isPresent(prefs)) {
            signalParameter = SignalParameter.init();
            signalParameter.save(prefs);
        } else {
            signalParameter = SignalParameter.load(prefs);
        }

        SignalServiceConfiguration serviceConfiguration = SignalParameter.getServiceConfiguration();


        accountManager = new SignalServiceAccountManager(serviceConfiguration, signalParameter.getUser(), signalParameter.getPassword(), USER_AGENT);

        if (signalParameter.isNew()) {
            try {
                IdentityKeyPair identityKey = KeyHelper.generateIdentityKeyPair();
                List<PreKeyRecord> oneTimePreKeys = KeyHelper.generatePreKeys(0, 100);
                SignedPreKeyRecord signedPreKeyRecord = KeyHelper.generateSignedPreKey(signalParameter.getIdentityKey(), 0);

                accountManager.createCormorantAccount(signalParameter.getSignalingKey(), signalParameter.getRegistrationId(), true);
                accountManager.setPreKeys(identityKey.getPublicKey(), signedPreKeyRecord, oneTimePreKeys);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        messageSender = new SignalServiceMessageSender(serviceConfiguration, signalParameter.getUser(), signalParameter.getPassword(),
                new InMemorySignalProtocolStore(signalParameter.getIdentityKey(), 0), USER_AGENT, Optional.absent(), Optional.absent());

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

    }

    public void send(String recipient, String msg) throws Exception {
        messageSender.sendMessage(new SignalServiceAddress(recipient),
                SignalServiceDataMessage.newBuilder().withBody(msg).build());
    }

    public void listen() throws Exception {
        try {
            messagePipe = messageReceiver.createMessagePipe();

            while (true) {
                SignalServiceEnvelope envelope = messagePipe.read(10, TimeUnit.SECONDS);
                SignalServiceCipher cipher = new SignalServiceCipher(new SignalServiceAddress(signalParameter.getUser()),
                        new InMemorySignalProtocolStore(signalParameter.getIdentityKey(), registrationId));
                SignalServiceContent message = cipher.decrypt(envelope);

                System.out.println("Received message: " + message.getDataMessage().get().getBody().get());
            }

        } finally {
            if (messagePipe != null)
                messagePipe.shutdown();
        }
    }

    /*
    private final static String LOG_TAG = SignalMessagingService.class.getSimpleName();
    private final static String MESSAGE_SPLIT_CHAR = "!";

    private static final String PREFERENCE_NAME = "cormorant";
    private static final String PREF_XMPP_USER = "xmppUser";
    private static final String PREF_XMPP_PASSWORD = "xmppPassword";

    private final Gson gson = new GsonBuilder().create();

    private SharedPreferences prefs;
    private AbstractXMPPConnection connection;

    // See https://github.com/ChatSecure/xmpp-server-list/blob/master/servers.json for alternative
    // servers. Those should all feature In-Band registration (XEP-0077.
    private String host = "home.zom.im";
    private String user;
    private String password;

    private Map<CormorantMessage.TYPE, List<CormorantMessageConsumer>> messageListeners = new HashMap<>();
    private List<DeviceIdListener> deviceIdListeners = new LinkedList<>();
    private boolean initialConnectionSuccess = true;

    public SignalMessagingService() {
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "MessagingService started");

        try {
            // Initialize key store
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        prefs = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        registerReceiver(deviceConnectionReceiver, new IntentFilter(CONNECTIVITY_ACTION));
        initialConnectionSuccess = connectToXMPP();
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

    @Override
    public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
        Log.v(LOG_TAG, "New message from " + from + ": " + message.getBody());

        final CormorantMessage cormorantMessage = parseMessage(message.getBody());
        if (cormorantMessage == null) return;
        List<CormorantMessageConsumer> messageConsumers = messageListeners.get(cormorantMessage.getType());
        if (messageConsumers != null) {
            for (CormorantMessageConsumer eachConsumer : messageConsumers) {
                eachConsumer.handleMessage(cormorantMessage, chat);
            }
        }
    }

    public void sendMessage(TrustedDevice device, CormorantMessage cormorantMessage) {
        if(connection == null){
            Log.w(LOG_TAG, "Connection is null - message not sent: " + cormorantMessage);
            return;
        }
        try {
            ChatManager chatManager = ChatManager.getInstanceFor(connection);
            EntityBareJid receiver = JidCreate.entityBareFrom(device.getJabberId());
            Chat chat = chatManager.chatWith(receiver);
            chat.send(createMessage(cormorantMessage));
        } catch (XmppStringprepException | SmackException.NotConnectedException | InterruptedException e) {
            //TODO handle exception
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    public void addMessageListener(CormorantMessage.TYPE messageType, CormorantMessageConsumer messageConsumer) {
        List<CormorantMessageConsumer> messageConsumers = messageListeners.get(messageType);
        if (messageConsumers == null) {
            messageConsumers = new LinkedList<>();
            messageListeners.put(messageType, messageConsumers);
        }
        messageConsumers.add(messageConsumer);
        Log.d(LOG_TAG, "New MessageConsumer added: " + messageConsumer);
    }

    public void removeMessageListener(CormorantMessage.TYPE messageType, CormorantMessageConsumer messageConsumer) {
        List<CormorantMessageConsumer> messageConsumers = messageListeners.get(messageType);
        if (messageConsumers != null) {
            messageConsumers.remove(messageConsumer);
            Log.d(LOG_TAG, "Removed MessageConsumer: " + messageConsumer);
        } else Log.w(LOG_TAG, "MessageConsumer not found");
    }

    private void updateDeviceId(String jabberId) {
        for (DeviceIdListener eachDeviceIdListener : this.deviceIdListeners) {
            eachDeviceIdListener.setJabberId(jabberId);
        }
    }

    public void addDeviceIdListener(DeviceIdListener deviceIdListener) {
        this.deviceIdListeners.add(deviceIdListener);
    }

    public void removeDeviceIdListener(DeviceIdListener deviceIdListener) {
        this.deviceIdListeners.remove(deviceIdListener);
    }

    private void loadAccount() {
        user = prefs.getString(PREF_XMPP_USER, null);
        password = prefs.getString(PREF_XMPP_PASSWORD, null);
    }

    private class ConnectTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.d(LOG_TAG, "Connecting to " + host);

                XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration
                        .builder()
                        .setHost(host)
                        .setXmppDomain(host)
                        .setCompressionEnabled(false)
                        //.setCustomSSLContext(MessagingService.this.createSSLContext())
                        //.setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                        .setKeystoreType(null)
                        .build();

                connection = new XMPPTCPConnection(conf);
                connection.connect();

                Log.d(LOG_TAG, "Connected to " + host);

                // load username and password from preferences
                loadAccount();

                // No account yet - create one
                if (user == null) {
                    createAccount();
                }
                updateDeviceId(getDeviceID());

                Log.d(LOG_TAG, "Logging in with user " + user);
                connection.login(user, password);
                Log.d(LOG_TAG, "Login successful");

                ChatManager chatManager = ChatManager.getInstanceFor(connection);
                chatManager.addIncomingListener(SignalMessagingService.this);

                // FIXME Just debugging stuff
                EntityBareJid debugJid = JidCreate.entityBareFrom("cormorant-debug@0nl1ne.cc");
                Chat chat = chatManager.chatWith(debugJid);
                chat.send("Howdy!");

            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }

            return null;
        }

    }

    private void createAccount() {
        try {
            String uuid = UUID.randomUUID().toString();

            String userName = "cormorant-" + uuid;
            String password = new BigInteger(130, new SecureRandom()).toString(32);

            Log.i(LOG_TAG, "Creating new account: " + userName + "@" + host);

            AccountManager accountManager = AccountManager.getInstance(connection);
            accountManager.createAccount(Localpart.from(userName), password);

            SharedPreferences.Editor editor = prefs.edit();

            editor.putString(PREF_XMPP_USER, userName);
            editor.putString(PREF_XMPP_PASSWORD, password);

            editor.apply();

            Log.i(LOG_TAG, "Account creation successful");

            this.user = userName;
            this.password = password;
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    private SSLContext createSSLContext() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("AndroidCAStore");

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

        return sslContext;
    }

    private boolean connectToXMPP() {
        if (isDeviceConnected()) {
            new ConnectTask().execute();
            return true;
        } else {
            Log.w(LOG_TAG, "Could not connect to XMPP Server, device is not connected to a network");
            return false;
        }
    }

    //TODO Do we have / want a Util class for stuff like this?
    private boolean isDeviceConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] activeNetworks = connectivityManager.getAllNetworks();
        for (Network network : activeNetworks) {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
            if (networkInfo != null && networkInfo.isConnected()) return true;
        }
        return false;
    }

    private final BroadcastReceiver deviceConnectionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ((connection != null && !connection.isConnected())
                    || (connection == null && !initialConnectionSuccess)) {
                connectToXMPP();
            }
        }
    };

    @Override
    public void onDestroy() {
        connection.disconnect();
        unregisterReceiver(deviceConnectionReceiver);
        Log.d(LOG_TAG, "MessagingService stopped");
    }

    public String getDeviceID() {
        return user + "@" + host;
    }

    @Override
    */
    public IBinder onBind(Intent intent) {
        return TypedServiceBinder.from(this);
    }


}
