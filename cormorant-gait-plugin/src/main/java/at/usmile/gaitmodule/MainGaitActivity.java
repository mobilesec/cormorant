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
package at.usmile.gaitmodule;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.JMathStudio.Exceptions.IllegalArgumentException;

import java.io.FileNotFoundException;
import java.io.IOException;

import at.usmile.gaitmodule.database.DataBaseHelper;
import at.usmile.gaitmodule.database.UserHandling;
import at.usmile.gaitmodule.extras.LogMessage;
import at.usmile.gaitmodule.services.StepDetectorService;
import at.usmile.gaitmodule.services.TemplateCreationService;

public class MainGaitActivity extends Activity {

    // Setup Logger
    //static private final Logger LOG = LoggerFactory.getLogger(MainGaitActivity.class);

    public static final String PREFS_NAME = "Gait_Shared_PREF";
    static final int REQUEST_CODE_EXISTING_USER = 1; // This is responsible for getting existing user// activity results
    static final int REQUEST_CODE_TRAIN_STARTED = 2; // This is responsible for getting RecordTrainingData// Activity results
    static final int REQUEST_CODE_AUTHENTICATION_SERVICE = 3;
    private static final String TAG = "MainGaitActivity";
    public boolean startAuthenticationService;
    SharedPreferences pref;
    IntentFilter filter;
    UserHandling myUserHandling;
    private int appStatus;
    private Button train;
    private Button test;
    private TextView infoText;
    private String userName;
    private String deviceOwner;
    private String securityLevel;
    private boolean appendUserData;
    private DataBaseHelper gaitDataBase;
    private int userID;
    private boolean addUserNameToList;
    private TemplateCreationResponseReceiver receiver;
    private ProgressDialog ringProgressDialog;
    private boolean templateExists;
    private boolean authenticationServiceRunning;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_gait);

        LogMessage.setStatus(TAG, "Starting MainGaitActivity");
        // setting up training and testing button
        train = (Button) findViewById(R.id.trainingButton);
        test = (Button) findViewById(R.id.testingButton);
        checkIfSensorPresent();
        infoText = (TextView) findViewById(R.id.userInfoText);
        filter = new IntentFilter(TemplateCreationResponseReceiver.ACTION_RESP);

        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new TemplateCreationResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        gaitDataBase = new DataBaseHelper(this);

        authenticationServiceRunning = isMyServiceRunning(StepDetectorService.class);
        Log.d(TAG, "" + authenticationServiceRunning);
        templateExists = templatePresent();
        LogMessage.setStatus(TAG, "AppStatus\t templateExists:"
                + templateExists + "\t" + "StepDetector running:" + authenticationServiceRunning);
        myUserHandling = new UserHandling(MainGaitActivity.this,
                getApplicationContext());

        try {
            checkApplicationStatus();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // This function checks if template is present in the database
    public boolean templatePresent() {
        boolean templateStatus = false;
        // Here each row represents one user.
        int userPresent = gaitDataBase.numberOfRows();

        if (userPresent > 0) {
            templateStatus = true;
        } else {
            templateStatus = false;
        }
        return templateStatus;
    }

    // To perform action when train button is pressed
    public void TrainButtonPressed(View V) {

        // myUserHandling = new
        // UserHandling2(MainGaitActivity.this,getApplicationContext());
        if (authenticationServiceRunning) {
            Toast.makeText(
                    this,
                    "Train button is pressed, First stop Authentication service before recording more/new data",
                    Toast.LENGTH_SHORT).show();
            // First stop authentication service
            Intent startGaitAuthenticationIntent = new Intent(this,
                    GaitAuthenticationTest.class);
            startGaitAuthenticationIntent.putExtra("infoText",
                    "Press stop button to start authentication service");
            startActivityForResult(startGaitAuthenticationIntent,
                    REQUEST_CODE_AUTHENTICATION_SERVICE);
        }
        if (!authenticationServiceRunning) {
            myUserHandling
                    .userTypeSelectionAlertDialogue(getApplicationContext());
        }
    }

    public void TestButtonPressed(View V) {

        if (templateExists && !authenticationServiceRunning) {

            Intent startGaitAuthenticationIntent = new Intent(this,
                    GaitAuthenticationTest.class);
            startGaitAuthenticationIntent.putExtra("infoText",
                    "Press start button to start authentication service");
            startActivityForResult(startGaitAuthenticationIntent,
                    REQUEST_CODE_AUTHENTICATION_SERVICE);
        }
    }

    public void ResetButtonPressed(View v) {

        myUserHandling.displayUsersData();
        // To check if button is working
        Log.i(TAG, "Reset Button is pressed");
        Intent startResetSettingActivity = new Intent(this, ResetActivity.class);
        startActivity(startResetSettingActivity);
    }

    public void settingButtonPressed(View v) {
        Log.i(TAG, "Setting button Clicked");

        Intent intent = new Intent(MainGaitActivity.this, Prefrences.class);
        startActivity(intent);
    }

    // This method is used to check status of the application and deliver
    // respective msgs to the uses
    private void checkApplicationStatus() throws IllegalArgumentException {
        // Toast.makeText(this,""+appStatus,Toast.LENGTH_SHORT).show();

        getUserSharedPrefrences();

        if (deviceOwner.equals("NA") | deviceOwner.isEmpty()
                | deviceOwner == null) {
            Log.i(TAG, "please enter DEVICE OWNER Name");
            infoText.setText("Please set device owner name in settings");
            train.setEnabled(false);
            test.setEnabled(false);
            return;
        } else {
            train.setEnabled(true);
            test.setEnabled(true);

            if (!templatePresent()) {
                test.setEnabled(false);
                infoText.setText("No training data is available at the moment. Press Train button to record your gait training data or add additional gait data to your training dataset");

            }

            if (templatePresent()) {
                startAuthenticationService = true;
                test.setEnabled(true);
                infoText.setText("Training data is already available. If you like to launch testing service press Test button. If you like to record more training data press Train button");

            }
        }

    }

    // This method is responsible for saving the application state
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        try {
            checkApplicationStatus();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        savedInstanceState.putInt("applicationStatus", appStatus);
        super.onSaveInstanceState(savedInstanceState);
    }

    // This method is responsible for recalling the save application state
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            checkApplicationStatus();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        appStatus = savedInstanceState.getInt("applicationStatus");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this portion of code is now moved to onResume()
        //myUserHandling = new UserHandling(MainGaitActivity.this,
        //		getApplicationContext());

        if (requestCode == REQUEST_CODE_EXISTING_USER) {
            if (resultCode == RESULT_OK) {
                Bundle results = data.getExtras();
                userName = results.getString("UserName");
                userID = results.getInt("UserID");
                boolean action = results.getBoolean("RemoveUser");
                LogMessage.setStatus(TAG, "User Name is:" + userName
                        + "\t UserID is:" + userID);
                if (action == true) {
                    // Delete user data from data base
                    gaitDataBase.deleteUser(userName);

                    // Delete user data from files
                    try {
                        myUserHandling.removeUserFromDataSet(userName);
                    } catch (IOException e) {
                        LogMessage.setStatus(TAG, "User Data Deletion Failed");
                        e.printStackTrace();
                    }
                }

                if (action == false) {
                    LogMessage.setStatus(TAG, "Append user data" + action);
                    myUserHandling.startRecordingForExistingUser(userName);

                }
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(getApplicationContext(),
                            "You have not selected any user name",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }

        if (requestCode == REQUEST_CODE_TRAIN_STARTED) {
            if (resultCode == RESULT_OK) {
                Bundle results = data.getExtras();
                boolean action = results.getBoolean("TrainingStopped");
                if (action == true) {

                    // We can move this code to add user to the dataset if and
                    // only if active segments are detected
                    userName = results.getString("UserName", "");
                    appStatus = results.getInt("ApplicationStatus", -1);
                    addUserNameToList = results.getBoolean("addUserNameToList");
                    appendUserData = results.getBoolean("AppendData");
                    boolean startTemplateCreaion = results
                            .getBoolean("StartTemplateCreation");
                    if (startTemplateCreaion) {
                        try {

                            gaitDataTemplateGeneration2();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Template Creation is unsuccessful",
                                Toast.LENGTH_SHORT).show();
                    }
                }

            }
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(),
                        "You have not selected any user name",
                        Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_CODE_AUTHENTICATION_SERVICE) {
            if (resultCode == RESULT_OK) {
                Bundle results = data.getExtras();
                boolean action = results
                        .getBoolean("authenticationServiceStatus");
                if (action == false) { // not running
                    authenticationServiceRunning = false;
                    LogMessage.setStatus(TAG,
                            "Authentication Service is not running");
                }

                if (action == true) {
                    authenticationServiceRunning = true;
                    LogMessage.setStatus(TAG,
                            "Authentication service is running");
                }
            }
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(),
                        "You have not performed any action", Toast.LENGTH_SHORT)
                        .show();
            }
        }

    }// onActivityResult

    // Shared preferences... is an easy way of sharing data between activities.
    public void createSharedPrefrences() {

        pref = getApplicationContext().getSharedPreferences(PREFS_NAME,
                MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putString("UserName", userName);
        editor.putInt("ApplicationStatus", appStatus);
        editor.putBoolean("TemplateExist", templateExists);
        editor.putBoolean("AuthenticationServiceState",
                authenticationServiceRunning);
        editor.putString("OwnerExist", "deviceOnwerExist");
        // here you can add more preferences
        editor.commit();
    }

    public void gaitDataTemplateGeneration2() throws FileNotFoundException,
            IOException, IllegalArgumentException {
        if (appStatus == 1) {
            Intent templateCreationIntent = new Intent(getApplicationContext(),
                    TemplateCreationService.class);
            templateCreationIntent.putExtra(
                    TemplateCreationService.PARAM_IN_MSG, userName);
            templateCreationIntent.putExtra("AppendData", appendUserData);
            appStatus = 0;
            startService(templateCreationIntent);
            launchRingDialog();
        }
    }

    //
    public void launchRingDialog() {
        ringProgressDialog = ProgressDialog.show(MainGaitActivity.this,
                "Please wait ...", " Generating your Gait Template ...", true);
        ringProgressDialog.setCancelable(false);
        ringProgressDialog.show();
    }

    public void stopRingDialog() {

        ringProgressDialog.dismiss();

        if (templatePresent() ) {
            Intent startGaitAuthenticationIntent = new Intent(this,
                    GaitAuthenticationTest.class);
            startGaitAuthenticationIntent.putExtra("infoText",
                    "Press start button to start authentication service");
            startActivityForResult(startGaitAuthenticationIntent,
                    REQUEST_CODE_AUTHENTICATION_SERVICE);
        } else {
            LogMessage.setStatus(TAG, "First record gait template");
            Log.i(TAG, "Record Template before starting authentication service");
        }
    }

    // Function to check if specific service is running or not
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);
        myUserHandling = new UserHandling(MainGaitActivity.this,
                getApplicationContext());
        try {
            checkApplicationStatus();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);
        try {
            checkApplicationStatus();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
    }

    public void getUserSharedPrefrences() {

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        deviceOwner = sp.getString("OWNER", "NA");

        // We dont need to call this here since we only need
        // it for user verification process
        String securityLevelVal = sp.getString("securityLevels", "1");
        String[] securityLevels = getResources().getStringArray(
                R.array.securityLevlesSettings);
        int ss = Integer.parseInt(securityLevelVal);
        securityLevel = securityLevels[ss - 1];
        double threshold = Double.parseDouble(sp.getString("threshold","0.25"));
        Log.i(TAG, deviceOwner + "::" + securityLevel + "::" + threshold);
    }

    //Function to check the presence of required sensors for this application
    public void checkIfSensorPresent() {

        SensorManager sensorManager = (SensorManager) this
                .getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor stepDetectorSensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        // If stepDetector is missing
        if (accelerometerSensor != null && stepDetectorSensor == null) {
            Log.i(TAG, "Your device is missing Step Detector sensor");
            train.setEnabled(false);
            test.setEnabled(false);
            Toast.makeText(this,
                    "Your device is missing stepDetector sensor, Exist",
                    Toast.LENGTH_LONG).show();
            this.finish();
        }
        // if Accelerometer sensor is missing, We know if device has step detector
        // then there must be accelerometer also but just to cover the case of
        // broken accelerometer
        if (accelerometerSensor == null && stepDetectorSensor != null) {
            train.setEnabled(false);
            test.setEnabled(false);
            Log.i(TAG, "Your device is missing accelerometer sensor");
            Toast.makeText(this,
                    "Your device is missing accelerometer sensor, Exist",
                    Toast.LENGTH_LONG).show();
            this.finish();
        }

        // if accelerometer and step detector both are missing
        if (accelerometerSensor == null && stepDetectorSensor == null) {
            train.setEnabled(false);
            test.setEnabled(false);
            Log.i(TAG, "Your device is missing accelerometer sensor");
            Toast.makeText(this,
                    "Your device is missing accelerometer sensor, Exit",
                    Toast.LENGTH_LONG).show();
            Log.i(TAG,
                    "Your device doesn't have Step Detector and accelerometer sensor");
            this.finish();
        }
    }

    public class TemplateCreationResponseReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP = "at.usmile.gait_authentication.intent.action.MESSAGE_PROCESSED";

        @Override
        public void onReceive(Context context, Intent intent) {

            Toast.makeText(
                    getApplicationContext(),
                    intent.getStringExtra(TemplateCreationService.PARAM_OUT_MSG),
                    Toast.LENGTH_SHORT).show();

            boolean templateCreationExitCode = intent.getBooleanExtra(
                    "ExitCode", false);
            if (templateCreationExitCode) {
                LogMessage.setStatus(TAG, "Template creation code is "
                        + templateCreationExitCode);
                if (addUserNameToList) {
                    LogMessage.setStatus(TAG, "User:" + userName
                            + "is added to the data set");
                }
                if (appendUserData) {
                    LogMessage.setStatus(TAG, "Data if user:" + userName
                            + "is appended to the data set");
                }

                if (ringProgressDialog.isShowing()) {
                    stopRingDialog();
                }
                try {
                    checkApplicationStatus();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            } else {
                LogMessage.setStatus(TAG, "Template creation code is"
                        + templateCreationExitCode);
                Toast.makeText(
                        getApplicationContext(),
                        "Template Generation Finished! could not find any template Record data again!!!",
                        Toast.LENGTH_SHORT).show();
                if (ringProgressDialog.isShowing()) {

                    stopRingDialog();
                    try {
                        checkApplicationStatus();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }

                if (templatePresent()) {
                    startAuthenticationService = true;
                }

                LogMessage
                        .setStatus(TAG,
                                "Template generation has failed so no new user data will be added to the data set");

                try {
                    checkApplicationStatus();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_gait, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Log.i(TAG, "Setting button Clicked");
            Intent intent = new Intent(MainGaitActivity.this, Prefrences.class);
            startActivity(intent);
        }
        if(id == R.id.action_reset){

            myUserHandling.displayUsersData();
            // To check if button is working
            Log.i(TAG, "Reset Button is pressed");
            Intent startResetSettingActivity = new Intent(this, ResetActivity.class);
            startActivity(startResetSettingActivity);
        }

        if(id == R.id.action_exit){
            finish();
        }

        return super.onOptionsItemSelected(item);
    }


}
