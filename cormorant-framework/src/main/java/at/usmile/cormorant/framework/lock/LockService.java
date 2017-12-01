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
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import org.jivesoftware.smack.chat2.Chat;

import at.usmile.cormorant.framework.AdminReceiver;
import at.usmile.cormorant.framework.common.TypedServiceBinder;
import at.usmile.cormorant.framework.common.TypedServiceConnection;
import at.usmile.cormorant.framework.messaging.CormorantMessage;
import at.usmile.cormorant.framework.messaging.CormorantMessageConsumer;
import at.usmile.cormorant.framework.messaging.MessagingService;

public class LockService extends Service implements CormorantMessageConsumer {

    private final static String LOG_TAG = LockService.class.getSimpleName();

    private DevicePolicyManager devicePolicyManager;
    private KeyguardManager keyguardManager;

    private ComponentName adminReceiverComponent;

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
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        adminReceiverComponent = new ComponentName(this, AdminReceiver.class);
    }

    @Override
    public void onDestroy() {
        if (messagingService.isBound()) unbindService(messagingService);

        enableLock();

        super.onDestroy();
    }

    public boolean isLocked() {
        boolean isLocked = keyguardManager.isDeviceSecure();
        Log.v(LOG_TAG, "Device locked: " + isLocked);
        return isLocked;
    }

    public synchronized void lock() {
        Log.d(LOG_TAG, "locking");
        enableLock();
        devicePolicyManager.lockNow();
        Log.d(LOG_TAG, "locked");
    }

    public synchronized void unlock() {
        Log.d(LOG_TAG, "unlocking");
        disableLock();
        Log.d(LOG_TAG, "unlocked");
    }

    public void disableLock() {
        // Set lockscreen to swipe-to-unlock
        devicePolicyManager.setPasswordQuality(adminReceiverComponent, DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
        devicePolicyManager.setPasswordMinimumLength(adminReceiverComponent, 0);
        boolean result = devicePolicyManager.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);

        if (result) {
            Log.d(LOG_TAG, "Successfully Disabled PIN lock");
        } else {
            Log.d(LOG_TAG, "Could not disable PIN lock");
        }
    }

    private void enableLock() {
        devicePolicyManager.setPasswordQuality(adminReceiverComponent, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        devicePolicyManager.setPasswordMinimumLength(adminReceiverComponent, 4);

        // FIXME: Hardcoded for now, have user configure the pin later
        boolean result = devicePolicyManager.resetPassword("1234", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        if (result) {
            Log.d(LOG_TAG, "Successfully enabled PIN lock");
        } else {
            Log.d(LOG_TAG, "Could not enabled PIN lock");
        }
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
            if (isLocked()) {
                unlock();
            } else {
                lock();
            }
        }

    }

}