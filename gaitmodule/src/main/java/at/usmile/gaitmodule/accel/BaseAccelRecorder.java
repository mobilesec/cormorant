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
package at.usmile.gaitmodule.accel;

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

public abstract class BaseAccelRecorder implements SensorEventListener {

    private static final String TAG = "BaseAccelRec";
    protected int counter = 0;
    protected Vibrator mVibrator;
    protected KeyguardManager km;
    protected boolean mIsTracking; // Check if we are recording data
    protected boolean stopTracking; // Check to stop tracking of the data
    protected PowerManager mPowerManager;
    protected SensorManager mSensorManager;
    protected Queue<SensorSample> mAccData; // Queue object to hold data.
    // Member variables
    private Context mContext;
    private PowerManager.WakeLock lock;  // To acquire power lock so that accelerometer recods data when screen is off

    //Constructor
    public BaseAccelRecorder(Context context, boolean accel, boolean power, boolean lock, boolean keyguard, boolean vibrator) {

        this.mContext = context;

        if (accel) {
            this.mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        }

        if (power) {
            this.mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        }

        if (lock) {
            this.lock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorRead");
        }

        if (keyguard) {
            this.km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        }

        if (vibrator) {

            this.mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        //Initial state of checks when object is created
        this.mIsTracking = false;
        this.stopTracking = false;
        this.mAccData = new LinkedList<SensorSample>();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onStartButtonIsClicked() {

    }

    public void onStopButtonIsClicked() {

    }

    public void stopTrackingAndSaveData(boolean stopTracking) {

    }

    public void registerAccelSensor() {
        Log.i(TAG, "Registering Accelerometer Sensor");
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), mSensorManager.SENSOR_DELAY_FASTEST);
    }

    public void unRegisterAccelSensor() {
        Log.i(TAG, "unRegistering Accelerometer Sensor");
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

    }

    public void acquireLock() {
        if (!lock.isHeld()) {
            lock.acquire();
            Log.i(TAG, "Lock is held");
        }
    }

    public void releaseLock() {
        if (lock.isHeld()) {
            lock.release();
            Log.i(TAG, "Lock is released");

        }
    }

}
