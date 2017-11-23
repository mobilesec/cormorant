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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.math.BigInteger;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import at.usmile.cormorant.framework.common.TypedServiceBinder;
import at.usmile.cormorant.framework.group.GroupChallengeRequest;
import at.usmile.cormorant.framework.group.GroupChallengeResponse;
import at.usmile.cormorant.framework.group.GroupUpdateMessage;
import at.usmile.cormorant.framework.group.TrustedDevice;
import at.usmile.cormorant.framework.lock.DeviceLockCommand;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;

public class MessagingService extends Service implements IncomingChatMessageListener {

    private final static String LOG_TAG = MessagingService.class.getSimpleName();
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

    public MessagingService() {
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
        Log.d(LOG_TAG, "Created message: " + message);
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
        Log.d(LOG_TAG, "New message from " + from + ": " + message.getBody());

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
                chatManager.addIncomingListener(MessagingService.this);

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
//            new ConnectTask().execute(); //FIXME MESSAGING WORKAROUND
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
    public IBinder onBind(Intent intent) {
        return TypedServiceBinder.from(this);
    }

}
