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
package at.usmile.gaitmodule;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import at.usmile.gaitmodule.accel.AccelRecorder;
import at.usmile.gaitmodule.extras.DataStorageLocation;
import at.usmile.gaitmodule.extras.LogMessage;
import at.usmile.gaitmodule.segmentation.GaitParameters;


public class RecordTrainingDataActivity extends Activity {

    public static final String PREFS_NAME = "Gait_Shared_PREF";
    private static final String TAG = "RecordTrainingDataActivity";
    SharedPreferences pref;
    private int appStatus;
    private Button startDataRecording;
    private Button stopDataRecording;
    private Button showInformationDialogue;
    private TextView userInfo;
    private String userName;
    private AccelRecorder myAccelRecorder;
    private String trainDataFilePath;
    private boolean existingUser;
    private boolean addUserNameToList = false;
    private boolean appendUserData;
    private boolean startTemplateCreationService = false;
    private boolean isScreenOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_training_data);

        //Get instance of AccelRecorder
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            userName = extras.getString("UserName");
            LogMessage.setStatus(TAG, "User Name is: " + userName);
            appStatus = extras.getInt("ApplicationStatus");
            LogMessage.setStatus(TAG, "Application Status is :" + appStatus);
            existingUser = extras.getBoolean("ExistingUser");
            addUserNameToList = extras.getBoolean("addUserNameToList");
            appendUserData = extras.getBoolean("AppendData");
        }

        trainDataFilePath = DataStorageLocation.TRAIN_RAW_DATA_PATH;
        //Buttons and textView setups
        startDataRecording = (Button) findViewById(R.id.startDataRecordingBtn);
        stopDataRecording = (Button) findViewById(R.id.stopDataRecordingBtn);
        showInformationDialogue = (Button) findViewById(R.id.showInformation);
        userInfo = (TextView) findViewById(R.id.userInfoRecording);
        myAccelRecorder = new AccelRecorder(this.getApplicationContext());
        checkApplicationStatus();
    }

    //This method is called when startButton is pressed on this activity
    public void startRecordingGaitDataPressed(View V) {

        //Change buttons status
        stopDataRecording.setEnabled(true);
        startDataRecording.setEnabled(false);
        userInfo.setText("Recorded data will be save in file:" + userName + "\nPress power button to turn screen off to start recording");

        //Here we call methods of AccelRecord to start data recording process
        // Code is moved to onKeyDown function so that we only start recording when user presses power down key
        myAccelRecorder.setmFilename(userName);
        myAccelRecorder.setExistingUser(existingUser);
        myAccelRecorder.onStartButtonIsClicked();

    }

    //This method is called when stop Button is pressed
    public void stopRecordingGaitDataPressed(View V) {

        //Call accel recorder method to stop data recording
        myAccelRecorder.onStopButtonIsClicked();
        satisfyWithDataRecording();

        //Change buttons status
        startDataRecording.setEnabled(true);
        stopDataRecording.setEnabled(false);
        appStatus = 1;
    }

    public void showInformationAlertPressed(View V) {
        infoDialogue(GaitParameters.dataRecordingMsgFirstTime);
    }

    //This method is called when this activity finishes its tasks and pass its results to caller activity(Main activity)
    private void backToCallingActivity() {
        LogMessage.setStatus(TAG, "Back to calling activity");
        Bundle dataReturn = new Bundle();
        dataReturn.putBoolean("TrainingStopped", true);
        dataReturn.putString("UserName", userName);
        dataReturn.putInt("ApplicationStatus", appStatus);
        dataReturn.putBoolean("AppendData", appendUserData);
        dataReturn.putBoolean("addUserNameToList", addUserNameToList);
        dataReturn.putBoolean("StartTemplateCreation", startTemplateCreationService);
        Intent returnIntent = new Intent();
        returnIntent.putExtras(dataReturn);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    //This method is responsible to display application specific messages to the user about recording instructions.
    public void checkApplicationStatus() {

        userInfo.setText("Press start button");
    }

    // This override method is used if user has started data recording and hits back button
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = false;

        switch (event.getKeyCode()) {

            case KeyEvent.KEYCODE_BACK:
                if (stopDataRecording.isEnabled()) {
                    Toast.makeText(getApplicationContext(), "First press Stop button before you go back!!!", Toast.LENGTH_SHORT).show();
                    result = false;
                } else {

                    backToCallingActivity();
                }
        }
        return result;

    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    /**
     * Just to ask the user if he/she is satisfied with the data recording
     */
    private void satisfyWithDataRecording() {
        //........Code starts to take user name as input via dialogue.......//
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Are you satisfy with data recording process");
        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                //addUserNameToList = true;
                new ExportTask().execute(new Boolean[]{true});
                startTemplateCreationService = true;
            }
        });

        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                startTemplateCreationService = false;
            }
        });
        alert.show();

        //...........Code Ends to take user name as an input via dialogue..........//
    }

    private void infoDialogue(String msg) {

        //........Code starts to take user name as input via dialogue.......//
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Information");
        alert.setIcon(R.drawable.ic_launcher);
        alert.setMessage(msg);
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (!startDataRecording.isEnabled()) {

                    startDataRecording.setEnabled(true);

                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (startDataRecording.isEnabled()) {
                    startDataRecording.setEnabled(false);
                }
            }
        });

        alert.show();

    }

    //This is inner class that starts progress dialogue
    @SuppressLint("NewApi")
    private class ExportTask extends AsyncTask<Boolean, Void, Void> {

        private ProgressDialog dialog;

        public ExportTask() {
            dialog = new ProgressDialog(RecordTrainingDataActivity.this);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onPreExecute() {
            this.dialog.setMessage("Please wait! Saving Data");
            this.dialog.show();
            Log.e("TAG", "PreExe");
        }

        //@SuppressWarnings("unused")
        public void onPostExecute() {
            Log.e("TAG", "PostExe");
        }

        @Override
        protected Void doInBackground(Boolean... arg0) {

            myAccelRecorder.stopTrackingAndSaveData(arg0[0].booleanValue());

            if (dialog.isShowing()) {
                dialog.dismiss();
                backToCallingActivity();

            }
            return null;
        }
    }
}
