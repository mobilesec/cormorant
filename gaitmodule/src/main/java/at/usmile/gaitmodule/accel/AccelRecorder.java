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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import Jama.Matrix;
import at.usmile.gaitmodule.extras.DataStorageLocation;
import at.usmile.gaitmodule.extras.LogMessage;
import at.usmile.gaitmodule.segmentation.GaitParameters;
import at.usmile.gaitmodule.utils.SamplingRate;
import edu.umbc.cs.maple.utils.JamaUtils;

public class AccelRecorder extends BaseAccelRecorder {
    private static final String TAG = "AccelRec";
    private static boolean accel = true;
    private static boolean power = true;
    private static boolean lock = true;
    private static boolean keyguard = true;
    private static boolean vibrator = false;
    private final String trainDataFileName = DataStorageLocation.TRAIN_RAW_DATA_PATH;
    private int MAX_QUEUE_CAPACITY = GaitParameters.maxQueueCapacityTrainingData;
    private String mFilename; // name of the file where user data will be stored
    private boolean existingUser; // in case if data is being recorded for already existing user

    public AccelRecorder(Context context) {
        super(context, accel, power, lock, keyguard, vibrator);
    }

    // Returns filename which contains accelerometer data
    public String getmFilename() {
        return mFilename;
    }

    // File which will contain accelerometer data
    public void setmFilename(String _mFilename) {
        this.mFilename = _mFilename;
    }

    public void setExistingUser(boolean _existingUser) {
        this.existingUser = _existingUser;
    }


    @Override
    public void onStartButtonIsClicked() {

        Log.e("StartButton is Clicked:", "YES");
        if (mIsTracking) {
            return;
        }
        registerAccelSensor();
        acquireLock();
        mIsTracking = true;

    }

    @Override
    public void onStopButtonIsClicked() {

        if (mIsTracking) {
            stopTracking = true;
            //stopTrackingAndSaveData(stopTracking);
            mIsTracking = false;
            unRegisterAccelSensor();
        } else {
            return;
        }

        counter = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                SensorSample entry = new SensorSample(event.timestamp, event.values[0], event.values[1], event.values[2]);
                Log.i(TAG, "" + event.timestamp + event.values[0] + event.values[1] + event.values[2]);
                if (mIsTracking) {
                    mAccData.add(entry);
                    counter++;
                }

                break;
        }

        if (counter == MAX_QUEUE_CAPACITY - 1) {
            stopTrackingAndSaveData(true);
            counter = 0;
        }

    }

    //This method is responsible for creating subfolders such as folder for storing acceleration and linear acceleration data.
    public void createSubFiles() {

        File accFilePath = new File(trainDataFileName);
        // create folders for file path
        if (!accFilePath.exists()) {
            accFilePath.mkdirs();
        }
    }

    // When Stop button is clicked we have to stop data recording and save that data to the files
    public void stopTrackingAndSaveData(boolean stopTracking) {
        OutputStream fo;
        File tempFile;
        SensorSample temp;
        String tempString;
        Matrix TimeStampsMat = new Matrix(mAccData.size(), 1);
        try {

            if (mAccData.size() > MAX_QUEUE_CAPACITY || stopTracking) {

                File parentDir = new File(trainDataFileName.concat(mFilename));
                parentDir.mkdirs();
                tempFile = new File(parentDir.getPath().concat("/").concat(mFilename + ".txt"));

                if (existingUser) {
                    tempFile.delete();
                    existingUser = false;
                }

                tempFile.createNewFile();
                // write the bytes in file
                if (tempFile.exists()) {

                    fo = new FileOutputStream(tempFile, true);
                    int i = 0;
                    while (mAccData.peek() != null) {
                        temp = mAccData.poll();
                        tempString = TimeUnit.NANOSECONDS.toMillis(temp.getTS()) + "\t" + Math.sqrt(temp.getX() * temp.getX() + temp.getY() * temp.getY() + temp.getZ() * temp.getZ());
                        TimeStampsMat.set(i, 0, TimeUnit.NANOSECONDS.toMillis(temp.getTS()));
                        fo.write(tempString.getBytes());
                        //
                        fo.write("\n".getBytes());
                        i = i + 1;
                    }
                    fo.flush();
                    fo.close();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        double samplingRate = SamplingRate.computeSamplingRate(JamaUtils.getcol(TimeStampsMat, 0).getColumnPackedCopy());
        LogMessage.setStatus(TAG, "sampling rate is:" + samplingRate);
        GaitParameters.setSamplingFrequency(SamplingRate.computeSamplingRate(JamaUtils.getcol(TimeStampsMat, 0).getColumnPackedCopy()));

    }
}
