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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
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

import at.usmile.cormorant.framework.group.GroupChallengeRequest;
import at.usmile.cormorant.framework.group.GroupChallengeResponse;
import at.usmile.cormorant.framework.lock.LockService;

public class MessagingService extends Service implements IncomingChatMessageListener {

    private final static String LOG_TAG = MessagingService.class.getSimpleName();
    private final static String MESSAGE_SPLIT_CHAR = "!";

    private static final String PREFERENCE_NAME = "cormorant";
    private static final String PREF_XMPP_USER = "xmppUser";
    private static final String PREF_XMPP_PASSWORD = "xmppPassword";

    private final IBinder mBinder = new MessagingServiceBinder();
    private final Gson gson = new GsonBuilder().create();

    private LockService lockService;

    private SharedPreferences prefs;
    private AbstractXMPPConnection connection;

    private String host = "0nl1ne.cc";
    private String user;
    private String password;

    private Map<CormorantMessage.TYPE, List<CormorantMessageConsumer>> messageListener = new HashMap<>();

    public MessagingService() {
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "MessagingService started");

        prefs = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);

        Intent lockServiceIntent = new Intent(this, LockService.class);
        startService(lockServiceIntent);
        bindService(lockServiceIntent, lockServiceConnection, Context.BIND_AUTO_CREATE);

        new ConnectTask().execute();
    }

    private String createMessage(CormorantMessage cormorantMessage){
        String message = cormorantMessage.getClazz() + MESSAGE_SPLIT_CHAR;
        message += gson.toJson(cormorantMessage);
        Log.d(LOG_TAG, "Created message: " + message);
        return message;
    }

    private CormorantMessage parseMessage(String message){
        String[] parts = message.split(MESSAGE_SPLIT_CHAR);
        if(CormorantMessage.CLASS.GROUP_CHALLENGE_REQUEST.toString().equals(parts[0])){
            return gson.fromJson(parts[1], GroupChallengeRequest.class);
        }
        else if(CormorantMessage.CLASS.GROUP_CHALLENGE_RESPONSE.toString().equals(parts[0])){
            return gson.fromJson(parts[1], GroupChallengeResponse.class);
        }
        else {
            Log.w(LOG_TAG, "ClassType unknown" + parts[0]);
            return null;
        }
    }

    @Override
    public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
        Log.d(LOG_TAG, "New message from " + from + ": " + message.getBody());

        if (message.getBody().equals("lock")) {
            lockService.lock();
            return;
        } else if (message.getBody().equals("unlock")) {
            lockService.unlock();
            return;
        }

        //TODO Is everything a CormorantMessage or only certain messages?
        final CormorantMessage cormorantMessage = parseMessage(message.getBody());
        if(cormorantMessage == null) return;
        List<CormorantMessageConsumer> messageConsumers = messageListener.get(cormorantMessage.getType());
        if(messageConsumers != null) {
            for(CormorantMessageConsumer eachConsumer : messageConsumers){
                eachConsumer.handleMessage(cormorantMessage, chat);
            }
        }
    }

    public void sendMessage(String jabberId, CormorantMessage cormorantMessage){
        try {
            ChatManager chatManager = ChatManager.getInstanceFor(connection);
            chatManager.addIncomingListener(MessagingService.this);

            EntityBareJid receiver = JidCreate.entityBareFrom(jabberId);
            Chat chat = chatManager.chatWith(receiver);
            chat.send(createMessage(cormorantMessage));
        } catch (SmackException.NotConnectedException  | XmppStringprepException | InterruptedException e) {
            //TODO handle exception
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    public void sendMessage(Chat chat, CormorantMessage cormorantMessage){
        try {
            chat.send(createMessage(cormorantMessage));
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            //TODO handle exception
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    public void addMessageListener(CormorantMessage.TYPE messageType, CormorantMessageConsumer messageConsumer){
        List<CormorantMessageConsumer> messageConsumers = messageListener.get(messageType);
        if(messageConsumers == null) {
            messageConsumers = new LinkedList<>();
            messageListener.put(messageType, messageConsumers);
        }
        messageConsumers.add(messageConsumer);
        Log.d(LOG_TAG, "New MessageConsumer added: " + messageConsumer);
    }

    public void removeMessageListener(CormorantMessage.TYPE messageType, CormorantMessageConsumer messageConsumer){
        List<CormorantMessageConsumer> messageConsumers = messageListener.get(messageType);
        if(messageConsumers != null) {
            messageConsumers.remove(messageConsumer);
            Log.d(LOG_TAG, "Removed MessageConsumer: " + messageConsumer);
        }
        else Log.w(LOG_TAG, "MessageConsumer not found");
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
                        .setCustomSSLContext(MessagingService.this.createSSLContext())
                        .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
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

                Log.d(LOG_TAG, "Logging in with user " + user);
                connection.login(user, password);
                Log.d(LOG_TAG, "Login successful");

                ChatManager chatManager = ChatManager.getInstanceFor(connection);
                chatManager.addIncomingListener(MessagingService.this);

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

            editor.commit();

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

    @Override
    public void onDestroy() {
        connection.disconnect();

        Log.d(LOG_TAG, "MessagingService stopped");
    }

    public String getDeviceID() {
        return user + "@" + host;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class MessagingServiceBinder extends Binder {
        public MessagingService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MessagingService.this;
        }
    }

    private ServiceConnection lockServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            LockService.LockServiceBinder binder = (LockService.LockServiceBinder) service;
            lockService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
}