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

import android.app.IntentService;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.JMathStudio.Exceptions.IllegalArgumentException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import Jama.Matrix;
import at.usmile.gaitmodule.GaitAuthenticationModule;
import at.usmile.gaitmodule.R;
import at.usmile.gaitmodule.accel.SensorSample;
import at.usmile.gaitmodule.dataProcessing.GaitDataProcessingSteps;
import at.usmile.gaitmodule.dataProcessing.Template;
import at.usmile.gaitmodule.database.DataBaseHelper;
import at.usmile.gaitmodule.extras.DataStorageLocation;
import at.usmile.gaitmodule.extras.GaitDecisionModule;
import at.usmile.gaitmodule.extras.LogMessage;
import at.usmile.gaitmodule.segmentation.GaitParameters;
import at.usmile.gaitmodule.services.StepDetectorService.AuthenticationResponseReceiver;
import at.usmile.gaitmodule.GaitAuthenticationModule.AuthenticationResultReceiver;
import at.usmile.gaitmodule.utils.ArrayManupulation;
import at.usmile.gaitmodule.utils.SamplingRate;
import edu.umbc.cs.maple.utils.JamaUtils;

//import at.usmile.gait_authentication.GaitAuthenticationModule;

public class AuthenticationIntentService extends IntentService implements
        SensorEventListener {

    public static final String PARAM_OUT_MSG = "omsg";
    public static final String PARAM_IN_MSG = "imsg";
    private static final String TAG = "AuthIntentService";
    private static final int MAX_QUEUE_CAPACITY = 3001; // Maximum number of
    private static String DEVICEOWNER;
    private KeyguardManager keyGaurdManager;
    private SensorManager mSensorManager;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock lock;
    private double threshold;
    private double securityLevel;
    private boolean mIsTracking;
    // samples in the queue
    private int counter = 0;
    private Queue<SensorSample> mAccData; // Queue object to hold data.
    private Vibrator mVibrator;
    private boolean b;
    private double AuthenticationResult = 0.0;
    private MediaPlayer mp;
    public AuthenticationIntentService() {
        super(AuthenticationIntentService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mAccData = new LinkedList<>();
        getUserSharedPrefrences();
        initSensors();
        registerAccelSensor();

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometerEvents(event);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Get user entered device owner name
    public void getUserSharedPrefrences() {

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        DEVICEOWNER = sp.getString("OWNER", "NA").toUpperCase();
        String securityLevelVal = sp.getString("securityLevels", "1");
        // adjust securityValue
        securityLevel = adjustSecurityLevel(securityLevelVal);
        threshold = Double.parseDouble(sp.getString("threshold", ""));
    }

    private double adjustSecurityLevel(String secLevelVal) {
        double thresholdValue = 1000.0;
        switch (secLevelVal) {
            case "1":
                thresholdValue = 70.0;
                break;
            case "2":
                thresholdValue = 60.0;
                break;
            case "3":
                thresholdValue = 50.0;
                break;
        }
        return thresholdValue;
    }

    public void initSensors() {

        LogMessage.setStatus(TAG, "Started");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        LogMessage.setStatus(TAG, "Registering accelerometer Sensor");
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        LogMessage.setStatus(TAG, "Starting Sensor_Service");

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        lock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "SensorRead");

        LogMessage.setStatus(TAG, "Starting Power_Service");
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        LogMessage.setStatus(TAG, "Starting Vibrator_Service");
        keyGaurdManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        getUserSharedPrefrences();
        //Log.i(TAG, DEVICEOWNER);
    }

    private void getAccelerometerEvents(SensorEvent event) {
        mIsTracking = true;
        SensorSample entry = new SensorSample(
                TimeUnit.NANOSECONDS.toMillis(event.timestamp),
                Math.sqrt(event.values[0]*event.values[0]+event.values[1]*event.values[1]+event.values[2]*event.values[2]));
        counter++;
        mAccData.add(entry);

        // If queue is full
        if (counter == MAX_QUEUE_CAPACITY - 1) {
            try {
                // Save that data to the file
                try {
                    unRegisterAccelSensors();
                    stopTrackingAndSaveData(true);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                // new WriteDataToFileTask().execute(new Boolean[]{true});
            } catch (InterruptedException | IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // Reset counter back to its inital stage
            counter = 0;
        }
    }

    // When Stop button is clicked we have to stop data recording and save that
    // data to the files
    private void stopTrackingAndSaveData(boolean stopTracking)
            throws InterruptedException, IllegalArgumentException,
            IOException {
        SensorSample temp;

        mIsTracking = false;
        Matrix testDataMatrix = new Matrix(MAX_QUEUE_CAPACITY - 1, 2);
        Matrix testDataPreMat = new Matrix(110, 2);
        Matrix testDataPostMat = new Matrix(110, 2);
        int i = 0;
        while (mAccData.peek() != null) {
            temp = mAccData.poll();
            testDataMatrix.set(i, 0, temp.getTS());
            testDataMatrix.set(i, 1, temp.getMagnitude());
            i = i + 1;
        }

        LogMessage.setStatus(TAG, "Now working on data matrix");

        double[] ts = JamaUtils.getcol(testDataMatrix, 0).getColumnPackedCopy();
        double meanTS = Math.floor(ArrayManupulation.mean(ArrayManupulation.diff(ts)));

        GaitParameters.setSamplingFrequency(SamplingRate.computeSamplingRate(ts));
        LogMessage.setStatus(TAG, "Sampling Rate is:" +
                GaitParameters.getSamplingFrequency() + " meanTs:" + meanTS);
        // Reason to append mean values in the beginning and end of data
        // Walk extractor is looking for variance go above threshold value which
        // works abs fine
        // But algorithm did not manage to find value at the end when value
        // drops below the threshold value
        // So to cover up the case when user is still walking and accelerometer
        // has finished recording the data
        // The were two solution either intentionally add mean value or zeros in
        // the beaning and end of the data
        // So we add that mean value in the beginning and end now active walk
        // segmentation can detect any kind of segment.
        // Better solution could be to update algo.

        double tsFirst = ts[0];
        double tsLast = ts[ts.length - 1];
        Log.i(TAG, "genDATA");
        int j = 1;
        int k = testDataPreMat.getRowDimension() + 1;
        while (j < k) {
            testDataPreMat.set(j - 1, 0, tsFirst - (k - j) * meanTS);
            testDataPostMat.set(j - 1, 0, tsLast + j * meanTS);
            testDataPreMat.set(j - 1, 1, 9.81);
            testDataPostMat.set(j - 1, 1, 9.81);
            j = j + 1;
        }

        testDataMatrix = JamaUtils.rowAppend(testDataPreMat, testDataMatrix);
        testDataMatrix = JamaUtils.rowAppend(testDataMatrix, testDataPostMat);


        // This code was only used for testing to check if Test_RAW_DATA_PATH
        // matrix is fully written

        if (!new File(DataStorageLocation.TEST_RAW_DATA_PATH).exists()) {
            new File(DataStorageLocation.TEST_RAW_DATA_PATH).mkdirs();
        }
//        PrintWriter prt = new PrintWriter(new File(
//                DataStorageLocation.TEST_RAW_DATA_PATH
//                        .concat("gaitTestDataMatrix")));
//        testDataMatrix.print(prt, 6, 5);
//        prt.flush();
//        prt.close();
        // For Testing purpose if we want to load test data
//         Matrix testDataGait = DataStorageLocation.LoadDataMatrix(new
//         File(DataStorageLocation.TEST_RAW_DATA_PATH.concat("gaitTestDataA.txt")));
        // If we dont want to use thread
        // authenticationResults(testDataMatrix);

        // Approach to create new thread every time AuthenticationIntentService
        // records accelerometer data
        final Matrix testData = testDataMatrix.copy();
        //final Matrix testData = testDataGait;
        mAccData.clear();
        testDataMatrix = null;
        new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    authenticationResults(testData);
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                   e.printStackTrace();
               } catch (IOException e) {
                   // TODO Auto-generated catch block
                    e.printStackTrace();
               } catch (IllegalArgumentException e) {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
               }
            }

        }).start();

        LogMessage.setStatus(TAG, "Sending BC to StepDetector");
        Log.i(TAG, "Sending BC to StepDetector");
        broadcastIntent();
    }

    private void authenticationResults(Matrix _testDataMatrix)
            throws IOException, IllegalArgumentException {

        boolean b = false;

        Long tA = System.currentTimeMillis();
        Log.i(TAG, "startingTime:::" + tA);
        LogMessage.setStatus(TAG, "starting Data Processing");
        // Only for testing purpose to read data from file...
        //Matrix data = GaitDataProcessingSteps.loadRawData1(new File(DataStorageLocation.TEST_RAW_DATA_PATH.concat("MuaazTest.txt")));
        //b = GaitDataProcessingSteps.gaitDataLoading(_testDataMatrix);
       Template testTemplate = GaitDataProcessingSteps.gaitTestTemplateGeneration(_testDataMatrix);
        if(testTemplate != null && testTemplate.getAllRemainedGaitCycles().getColumnDimension() > 2){
            Log.i(TAG, "RemainedCyclesAre:"+testTemplate.getAllRemainedGaitCycles().getColumnDimension());
            b = true;
        }
        // boolean b =
        //GaitDataProcessingSteps.gaitDataLoading(DataStorageLocation.TEST_RAW_DATA_PATH);
        if (b == true) {
            LogMessage.setStatus(TAG, "data processing is done");
            Matrix remainedGaitTestCycles = testTemplate.getAllRemainedGaitCycles();

            // Write test template to the file....
            String fileName = new SimpleDateFormat("MMddhhmmss'testTemplate.txt'").format(new Date());
            PrintWriter prt = new PrintWriter(new File(
                    DataStorageLocation.TEST_RAW_DATA_PATH.concat(fileName)));
            remainedGaitTestCycles.print(prt, 6, 5);
            prt.flush();
            prt.close();

            DataBaseHelper myDb = new DataBaseHelper(AuthenticationIntentService.this);
            Matrix allRemainedGaitTemplates = myDb
                    .String2MatrixRepresentation(myDb.getUserData(DEVICEOWNER));

            LogMessage.setStatus(TAG, "SecLevel:" + securityLevel+ " Threshold:"+ threshold);
            double resultVerify = GaitDecisionModule.verifyUser(allRemainedGaitTemplates,remainedGaitTestCycles,securityLevel, threshold);
            if (resultVerify == 1) {
                AuthenticationResult = 1.0;
                LogMessage.setStatus(TAG, "Walk belongs to: "+ DEVICEOWNER  + "with Confidence:"
                        + AuthenticationResult);
                unlockDevice();
            } else {

                AuthenticationResult = -1.0;
                LogMessage.setStatus(TAG, "Failed to Recognize; Try again");
                //playSound();
                broadcastIntent1();
            }
        }

        if (b == false) {
            AuthenticationResult = -1.0;
            LogMessage.setStatus(TAG, "Failed to Recognize; Try again");
            //playSound();
            broadcastIntent1();
        }
        Long tB = System.currentTimeMillis();
        Log.i(TAG, "TimeTaken Complete :::" + (tB - tA));
    }

    private void unlockDevice() throws IOException {
        LogMessage.setStatus(TAG, "In unlock device Method");
        long[] pattern = {0, 100, 1000, 500, 2000, 100};
        mVibrator.vibrate(pattern, -1);
        playSound();
        broadcastIntent1();

    }

    public  void playSound() throws IOException {
        mp = MediaPlayer.create(this, R.raw.a);
        if(mp.isPlaying()){
            mp.stop();
            mp.release();
            mp.reset();
        }else{
            mp.start();
        }
    }

    private void unRegisterAccelSensors() {
        LogMessage.setStatus(TAG, "Unregistering accelerometer sensor");
        mSensorManager.unregisterListener(AuthenticationIntentService.this);
        if (lock.isHeld()) {
            lock.release();
        }
    }

    private void registerAccelSensor() {

        //
        mSensorManager.registerListener(AuthenticationIntentService.this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        if (!lock.isHeld()) {
            lock.acquire();
        }
    }

    private void broadcastIntent() {
        final Intent broadcastIntent = new Intent();
        broadcastIntent
                .setAction(AuthenticationResponseReceiver.ACTION_RESP_AUTHEVENT);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent
                .putExtra(PARAM_OUT_MSG, "Authentication event finished");
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

    }

    private void broadcastIntent1() {

        // From here we can send msg to the authentication framework
	Log.d(TAG, "sendConfidenceToAuthFramework");

        // create intent and add confidence from authentication process
        final Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(AuthenticationResultReceiver.ON_AUTHENTICATION);
        broadcastIntent.putExtra(AuthenticationResultReceiver.CONFIDENCE, AuthenticationResult);
		Log.i(TAG, "" + AuthenticationResult);
        this.sendBroadcast(broadcastIntent);

    }

}
