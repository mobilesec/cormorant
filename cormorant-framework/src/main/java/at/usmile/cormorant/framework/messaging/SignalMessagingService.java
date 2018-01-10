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
import at.usmile.cormorant.framework.group.TrustedDevice;

public class SignalMessagingService extends Service {

    private final static String LOG_TAG = SignalMessagingService.class.getSimpleName();

    private static final String USER_AGENT = "[CORMORANT]";

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

        //new SendMessageTask().execute("86b08d6d-2c18-47c1-8c6f-0b24dc5da54e", "Hello world!");
        new ListenTask().execute();
    }

    public String getDeviceID() {
        return signalParameter.getUser();
    }

    public void sendMessage(TrustedDevice device, CormorantMessage cormorantMessage) {
        //Log.w(LOG_TAG, "sendMessage(" + cormorantMessage + ")");
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
                                        Log.w(LOG_TAG, "Decrypting" );
                                        SignalServiceContent message = cipher.decrypt(envelope);
                                        System.out.println("Received message: " + message.getDataMessage().get().getBody().get());
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
                String authorizedEntity = "457730743558"; // Project id from Google Developer Console
                String scope = "GCM"; // e.g. communicating using GCM, but you can use any
                // URL-safe characters up to a maximum of 1000, or
                // you can also leave it blank.
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

    private void updateDeviceId(String jabberId) {
        for (DeviceIdListener eachDeviceIdListener : this.deviceIdListeners) {
            eachDeviceIdListener.setJabberId(jabberId);
        }
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
