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
package at.usmile.cormorant.framework.lock;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import org.jivesoftware.smack.chat2.Chat;

import at.usmile.cormorant.framework.R;
import at.usmile.cormorant.framework.common.TypedServiceBinder;
import at.usmile.cormorant.framework.common.TypedServiceConnection;
import at.usmile.cormorant.framework.messaging.CormorantMessage;
import at.usmile.cormorant.framework.messaging.CormorantMessageConsumer;
import at.usmile.cormorant.framework.messaging.MessagingService;

public class LockService extends Service implements CormorantMessageConsumer {

    private final static String LOG_TAG = LockService.class.getSimpleName();

    private KeyguardLock keyguardLock;

    private boolean locked = true;

    private TypedServiceConnection<MessagingService> messagingService = new TypedServiceConnection<MessagingService>() {
        @Override
        public void onServiceConnected(MessagingService service) {
            service.addMessageListener(CormorantMessage.TYPE.DEVICE, LockService.this);
        }

        @Override
        public void onServiceDisconnected(MessagingService service) {
            service.removeMessageListener(CormorantMessage.TYPE.DEVICE, LockService.this);
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();

        bindService(new Intent(this, MessagingService.class), messagingService, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        if (messagingService.isBound()) unbindService(messagingService);

        getKeyguard().reenableKeyguard();
        super.onDestroy();
    }

    // Reenable the keyguard. The keyguard will reappear if the previous call to disableKeyguard() caused it to be hidden.
    // Note: This call has no effect while any DevicePolicyManager is enabled that requires a password.
    public synchronized void lock() {
        Log.d(LOG_TAG, "locking");
        getKeyguard().reenableKeyguard();
        locked = true;
        Log.d(LOG_TAG, "locked");
    }

    // Might be this does in general not work with with Android >= 6...

    public synchronized void unlock() {
        Log.d(LOG_TAG, "unlocking");
        getKeyguard().disableKeyguard();
        locked = false;
        Log.d(LOG_TAG, "unlocked");
    }

    private synchronized KeyguardLock getKeyguard() {
        if (keyguardLock == null) {
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            keyguardLock = keyguardManager.newKeyguardLock(LOG_TAG);
        }
        return keyguardLock;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return TypedServiceBinder.from(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void handleMessage(CormorantMessage cormorantMessage, Chat chat) {
        Log.d(LOG_TAG, "handleMessage(" + cormorantMessage + ")");

        if (cormorantMessage instanceof DeviceLockCommand) {

            Handler handler = new Handler(Looper.getMainLooper());

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (locked) {
                        unlock();
                    } else {
                        lock();
                    }
                }
            });

        }

    }

}