package at.usmile.cormorant.framework.lock;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

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
        keyguardLock.reenableKeyguard();
    }

    public void unlock() {
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