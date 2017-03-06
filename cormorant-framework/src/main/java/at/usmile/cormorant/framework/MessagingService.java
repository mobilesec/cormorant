/**
 * Copyright 2016 - Daniel Hintze <daniel.hintze@fhdw.de>
 * 				 Sebastian Scholz <sebastian.scholz@fhdw.de>
 * 				 Rainhard D. Findling <rainhard.findling@fh-hagenberg.at>
 * 				 Muhammad Muaaz <muhammad.muaaz@usmile.at>
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
package at.usmile.cormorant.framework;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
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

import java.math.BigInteger;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class MessagingService extends Service implements IncomingChatMessageListener {

    private final static String LOG_TAG = MessagingService.class.getSimpleName();

    private static final String PREFERENCE_NAME = "cormorant";
    private static final String PREF_XMPP_USER = "xmppUser";
    private static final String PREF_XMPP_PASSWORD = "xmppPassword";

    private final IBinder mBinder = new MessagingServiceBinder();

    private SharedPreferences prefs;
    private AbstractXMPPConnection connection;

    private String host = "0nl1ne.cc";
    private String user;
    private String password;

    public MessagingService() {
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "MessagingService started");

        prefs = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);

        new ConnectTask().execute();
    }

    @Override
    public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
        Log.d(LOG_TAG, "New message from " + from + ": " + message.getBody());
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

                EntityBareJid debugJid = JidCreate.entityBareFrom("cormorant-debug2@0nl1ne.cc");
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

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class MessagingServiceBinder extends Binder {
        MessagingService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MessagingService.this;
        }
    }
}
