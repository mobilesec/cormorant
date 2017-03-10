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
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import at.usmile.cormorant.framework.MainActivity;

public class LockService extends Service {

    private final static String LOG_TAG = MainActivity.class.getSimpleName();

    private final IBinder mBinder = new LockServiceBinder();
    private KeyguardManager keyguardManager;

    private KeyguardLock keyguardLock;

    @Override
    public void onCreate() {
        super.onCreate();

        keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        keyguardLock = keyguardManager.newKeyguardLock(LOG_TAG);
    }

    @Override
    public void onDestroy() {
        keyguardLock.reenableKeyguard();
        super.onDestroy();
    }

    // Reenable the keyguard. The keyguard will reappear if the previous call to disableKeyguard() caused it to be hidden.
    // Note: This call has no effect while any DevicePolicyManager is enabled that requires a password.
    public void lock() {
        Log.d(LOG_TAG, "locking");

        keyguardLock.reenableKeyguard();
    }

    public void unlock() {
        Log.d(LOG_TAG, "unlocking");

        keyguardLock.disableKeyguard();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LockServiceBinder extends Binder {
        public LockService getService() {
            return LockService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

}