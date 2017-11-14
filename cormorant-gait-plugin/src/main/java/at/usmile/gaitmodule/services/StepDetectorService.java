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
package at.usmile.gaitmodule.services;

/**
 * This service must be used in combination with Authentication Intent service. This is Started service it continuously listens to step detector
 * Main theme behind separating step detector and AuthenticationIntentService is to keep the size of running service small on the storage so that
 * Android OS does not not kill our service. This service listen to step detector sensor and counts the steps if step count reaches to a limit
 * it starts Authentication Intent service. Authentication Intent service after finishing the task broadcast its results and our
 * step detector service listens to that broad cast and register step detector again.
 */

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import at.usmile.gaitmodule.MainGaitActivity;
import at.usmile.gaitmodule.R;
import at.usmile.gaitmodule.extras.LogMessage;


@TargetApi(Build.VERSION_CODES.KITKAT)
public class StepDetectorService extends Service implements SensorEventListener {

    private static final String TAG = "StepDetectorService";
    private SensorManager mSensorManager;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock lock;
    private float counts;
    private long lastUpdate;
    private AuthenticationResponseReceiver receiver;
    private IntentFilter filter;
    private Thread t;
    private NotificationManager mNM;
    private int NOTIFICATION = 10002; //Any unique number for this notification
    @Override
    public void onCreate() {
        //Register response reciever
        Log.i(TAG, "Starting StepDetector Service");
        filter = new IntentFilter(AuthenticationResponseReceiver.ACTION_RESP_AUTHEVENT);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new AuthenticationResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

        //Starting power and sensor services
        Log.i(TAG, "Starting stepDetector");
        LogMessage.setStatus(TAG, "Started");
        mSensorManager = (SensorManager) getSystemService(StepDetectorService.this.SENSOR_SERVICE);
        LogMessage.setStatus(TAG, "Starting Sensor_Service");

        mPowerManager = (PowerManager) getSystemService(StepDetectorService.this.POWER_SERVICE);
        mNM = (NotificationManager) getSystemService(StepDetectorService.this.NOTIFICATION_SERVICE);
        LogMessage.setStatus(TAG, "Starting Power_Service");
        lock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorRead");

        if (!lock.isHeld()) {
            lock.acquire();
            Log.i(TAG, "Lock is held now");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerStepDetector();
        showNotification();
        return StepDetectorService.START_STICKY;
    }

    @Override
    public void onDestroy() {

        unRegisterStepDetector();
        Log.i(TAG, "Step Detector Destroyed");
        LogMessage.setStatus(TAG, "Step Detector Service Destroyed");
        LocalBroadcastManager.getInstance(StepDetectorService.this).unregisterReceiver(receiver);
        mNM.cancel(NOTIFICATION);
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "onSensorChanged");
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            getStepCounterEvents(event);
        }
    }

    private void registerStepDetector() {
        //lock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorRead");
//        if (!lock.isHeld()) {
//            lock.acquire();
//            Log.i(TAG, "Lock is held");
//        }
        LogMessage.setStatus(TAG, "Registering StepDetector Sensor");
        mSensorManager.registerListener(StepDetectorService.this, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void getStepCounterEvents(SensorEvent event) {

        long actualTime = TimeUnit.NANOSECONDS.toMillis(event.timestamp);
        Log.i(TAG, "Sensor is working" + "::" + (actualTime-lastUpdate));
        if (actualTime - lastUpdate < 1000) {

            if (counts < 3 || counts > 3) {
                counts++;
                Log.i(TAG, "" + counts);
                lastUpdate = actualTime;
                return;
            }
            if (counts == 3.0) {
                counts++;
                lastUpdate = actualTime;
                unRegisterStepDetector();
                startAuthenticationIntentService();
                return;
            }

        }
        if (counts >= 30 || actualTime - lastUpdate > 2000) {
            counts = 0;
        }
        lastUpdate = actualTime;
        Log.i(TAG, "called");
    }

    private void unRegisterStepDetector() {
        Log.i(TAG, "called");
        counts = 0;
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR));
        if(lock.isHeld()){
            lock.release();
        }
    }

    private void startAuthenticationIntentService() {
        Intent AuthenticationServiceIntent = new Intent(StepDetectorService.this, AuthenticationIntentService.class);
        AuthenticationServiceIntent.putExtra(AuthenticationIntentService.PARAM_IN_MSG, "Start Authentication Event");
        startService(AuthenticationServiceIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class AuthenticationResponseReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP_AUTHEVENT =
                "AuthenticationIntentProcessed";

        @Override
        public void onReceive(Context context, Intent intent) {

            LogMessage.setStatus(TAG, intent.getStringExtra(AuthenticationIntentService.PARAM_OUT_MSG));
            registerStepDetector();
        }
    }

    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Step detector service is running";

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainGaitActivity.class), 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Gait Authentication")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(contentIntent)
                .build();

        // Set the info for the views that show in the notification panel.
        //Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
        //notification.setLatestEventInfo(this, "Gait Authentication", text, contentIntent);
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        // Send the notification.
        mNM.notify(NOTIFICATION, notification);

    }

}
