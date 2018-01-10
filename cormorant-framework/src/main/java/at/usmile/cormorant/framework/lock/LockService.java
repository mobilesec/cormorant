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

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import at.usmile.cormorant.framework.R;
import at.usmile.cormorant.framework.common.TypedServiceBinder;
import at.usmile.cormorant.framework.common.TypedServiceConnection;
import at.usmile.cormorant.framework.messaging.CormorantMessage;
import at.usmile.cormorant.framework.messaging.CormorantMessageConsumer;
import at.usmile.cormorant.framework.messaging.SignalMessagingService;

public class LockService extends Service implements CormorantMessageConsumer {

    private final static String LOG_TAG = LockService.class.getSimpleName();

    private boolean locked = true;
    private List<LockStateListener> lockStateListeners = new LinkedList<>();
    private NotificationManager notificationManager;

    private TypedServiceConnection<SignalMessagingService> messagingService = new TypedServiceConnection<SignalMessagingService>() {
        @Override
        public void onServiceConnected(SignalMessagingService service) {
            service.addMessageListener(CormorantMessage.TYPE.DEVICE, LockService.this);
        }

        @Override
        public void onServiceDisconnected(SignalMessagingService service) {
            service.removeMessageListener(CormorantMessage.TYPE.DEVICE, LockService.this);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        bindService(new Intent(this, SignalMessagingService.class), messagingService, Context.BIND_AUTO_CREATE);
        this.notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showLockNotification();
    }

    @Override
    public void onDestroy() {
        if (messagingService.isBound()) unbindService(messagingService);
        setLocked(true);
        super.onDestroy();
    }

    public boolean isLocked() {
        return locked;
    }

    private void setLocked(boolean locked) {
        this.locked = locked;
        showLockNotification();
    }

    public synchronized void lock() {
        setLocked(true);
        Log.d(LOG_TAG, "Device locked");
        notifyLockStateListeners();
    }

    public synchronized void unlock() {
        setLocked(false);
        Log.d(LOG_TAG, "Device unlocked");
        notifyLockStateListeners();
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
    public void handleMessage(CormorantMessage cormorantMessage, String source) {
        Log.d(LOG_TAG, "handleMessage(" + cormorantMessage + ")");

        if (cormorantMessage instanceof DeviceLockCommand) {
            if (isLocked()) {
                unlock();
            } else {
                lock();
            }
        }
    }

    private void showLockNotification() {
        int icon = isLocked() ? R.drawable.ic_lock_black_24dp : R.drawable.ic_lock_open_black_24dp;
        String title = isLocked() ? "LOCKED" : "UNLOCKED";
        String text = isLocked() ? "Device is locked" : "Device is unlocked";
        int id = 4711;

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, "default")
                        .setSmallIcon(icon)
                        .setOngoing(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentTitle(title)
                        .setContentText(text);
        notificationManager.notify(id, notificationBuilder.build());
    }

    private void notifyLockStateListeners() {
        lockStateListeners.forEach(eachListener -> eachListener.onLockStateChanged(isLocked()));
    }

    public void addLockStateListener(LockStateListener lockStateListener) {
        this.lockStateListeners.add(lockStateListener);
        notifyLockStateListeners();
    }

    public void removeLockStateListener(LockStateListener lockStateListener) {
        this.lockStateListeners.remove(lockStateListener);
    }

    public interface LockStateListener {
        void onLockStateChanged(boolean lockState);
    }

}