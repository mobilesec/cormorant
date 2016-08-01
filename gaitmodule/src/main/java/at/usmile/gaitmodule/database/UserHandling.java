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
package at.usmile.gaitmodule.database;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import Jama.Matrix;
import at.usmile.gaitmodule.ExistingUserActivity;
import at.usmile.gaitmodule.R;
import at.usmile.gaitmodule.RecordTrainingDataActivity;
import at.usmile.gaitmodule.extras.DataStorageLocation;
import at.usmile.gaitmodule.extras.LogMessage;

// This class is used to get away some of the functionality of main class
public class UserHandling {
    static final int REQUEST_CODE_TRAIN_STARTED = 2; // This is responsible for gettting RecordTrainingData Activity results
    static final int REQUEST_CODE_EXISTING_USER = 1; // This is responsible for getting existing user activity results
    private static final String TAG = "UserHandling";
    public static ArrayList<String> userNameList;
    private static String userName;
    private static Activity activity;
    public final String PREFS_NAME = "Gait_Shared_PREF";
    final CharSequence[] userTypes = {"New User", "Existing User"};
    public DataBaseHelper gaitDatabase;
    public String databaseName = DataBaseHelper.DATABASE_NAME;
    //static private final Logger LOG = LoggerFactory.getLogger(UserHandling.class);
    private int appStatus;
    private Context context;
    private int userID;
    private SharedPreferences pref;
    private int MODE_PRIVATE = 0;
    public UserHandling() {

    }

    public UserHandling(Context _context) {

        this.context = _context;

    }

    public UserHandling(Activity _mActivity) {
        this.activity = _mActivity;
    }

    public UserHandling(Activity _mActivity, Context _mContext) {

        this.activity = _mActivity;
        this.context = _mContext;
    }

    //Prompt user to enter data using alert dialogue box
    public static void takeUserDataInput() {
        //........Code starts to take user name as input via dialogue.......//
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setTitle("User Information Dialogue");
        alert.setMessage("Please enter your name");
        alert.setIcon(R.drawable.ic_launcher);
        Log.i(TAG, "here");
        // Set an EditText view to get user input
        final EditText input = new EditText(activity);
        alert.setView(input);


        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                userName = input.getText().toString().toUpperCase();
                Log.i("Entered User Name", userName);


                if (userNameList.contains(userName)) {

                    Toast.makeText(activity, "Training data of enterd user name already exist please continue as existing user", Toast.LENGTH_SHORT).show();
                }
                if (userName.isEmpty() | userName.equals(null) | userNameList.contains(userName)) {

                    Toast.makeText(activity, "Please provide a valid user name", Toast.LENGTH_SHORT).show();


                } else {

                    startRecordingForNewUser(userName);

                }

            }


        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        alert.show();

        //...........Code Ends to take user name as an input via dialogue..........//
    }

    // This function check if user already exists, If user already exists we don't
    // allow him to continue with same name and allow him to continue with different user name.
    static void startRecordingForNewUser(String userName) {


        boolean addUserNameToList = true;
        Intent RecordTrainingDataActivityIntent = new Intent(activity, RecordTrainingDataActivity.class);
        RecordTrainingDataActivityIntent.putExtra("UserName", userName);
        RecordTrainingDataActivityIntent.putExtra("ExistingUser", false);
        RecordTrainingDataActivityIntent.putExtra("AppendData", false);
        RecordTrainingDataActivityIntent.putExtra("ApplicationStatus", 0);
        RecordTrainingDataActivityIntent.putExtra("addUserNameToList", true);
        activity.startActivityForResult(RecordTrainingDataActivityIntent, REQUEST_CODE_TRAIN_STARTED);

    }

    // This function check if user already exists, If user already exists we don't
    // allow him to continue with same name and allow him to continue with different user name.
    public static void startRecordingForExistingUser(String userName) {


        boolean addUserNameToList = true;
        Intent RecordTrainingDataActivityIntent = new Intent(activity, RecordTrainingDataActivity.class);
        RecordTrainingDataActivityIntent.putExtra("UserName", userName);
        RecordTrainingDataActivityIntent.putExtra("ExistingUser", true);
        RecordTrainingDataActivityIntent.putExtra("AppendData", true);
        RecordTrainingDataActivityIntent.putExtra("ApplicationStatus", 1);
        RecordTrainingDataActivityIntent.putExtra("addUserNameToList", false);
        activity.startActivityForResult(RecordTrainingDataActivityIntent, REQUEST_CODE_TRAIN_STARTED);
    }

    /**
     * @return the userNameList
     */
    public ArrayList<String> getUserNameList() {
        return userNameList;
    }

    /**
     * @param userNameList the userNameList to set
     */
    public void setUserNameList(ArrayList<String> userNameList) {
        this.userNameList = userNameList;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the userID
     */
    public int getUserID() {
        return userID;
    }

    /**
     * @param userID the userID to set
     */
    public void setUserID(int userID) {
        this.userID = userID;
    }

    // This function is used to display user selection dialogue
    public void userTypeSelectionAlertDialogue(final Context _context) {
        userNameList = new ArrayList<String>();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select user type");
        builder.setIcon(R.drawable.ic_launcher);
        builder.setSingleChoiceItems(userTypes, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                Toast.makeText(activity, userTypes[item], Toast.LENGTH_SHORT).show();
                if ("New User".equals(userTypes[item])) {
                    dialog.dismiss();
                    takeUserDataInput();
                }

                if ("Existing User".equals(userTypes[item])) {
                    dialog.dismiss();

                    if (doesDatabaseExist()) {

                        Intent existingUserActivityIntent = new Intent(activity.getApplicationContext(), ExistingUserActivity.class);
                        LogMessage.setStatus(TAG, "Starting Existinguser Activity");
                        activity.startActivityForResult(existingUserActivityIntent, REQUEST_CODE_EXISTING_USER);

                    } else {
                        Toast.makeText(_context, "Data base is not yet created", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        })

                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        AlertDialog alert = builder.create();
        alert.show();


    }

    public boolean doesDatabaseExist() {
        File dbFile = context.getDatabasePath(databaseName);
        return dbFile.exists();
    }

    @SuppressWarnings("unchecked")
    public void getUserList() {
        gaitDatabase = new DataBaseHelper(context);
        userNameList = gaitDatabase.getAllUsers();
    }

    public void removeUserFromDataSet(String _userName, int _userID) throws IOException {
        //Empty the previous userList
        if (doesDatabaseExist()) {

            gaitDatabase = new DataBaseHelper(context);
            gaitDatabase.deleteUser(_userName);
        }

        //Remove his training data files from the list
        File filePath1 = new File(DataStorageLocation.TRAIN_RAW_DATA_PATH.concat("/").concat(_userName));
        DataStorageLocation.deleteDirectory(filePath1);
        LogMessage.setStatus(TAG, "Train DataOFuser is removed:" + _userName);

        DataStorageLocation.deleteDirectory(new File(DataStorageLocation.TRAIN_PROCESSED_DATA_PATH.concat("/").concat(_userName)));

        File allBestGaitCycles = new File(DataStorageLocation.ALL_TEMPLATES_PATH.concat("/BestGaitCycles/").concat(_userName).concat(".txt"));
        if (allBestGaitCycles.exists()) {

            allBestGaitCycles.delete();
        }

        File allRemainedGaiCycles = new File(DataStorageLocation.ALL_TEMPLATES_PATH.concat("/RemainedGaitCycles/").concat(_userName).concat(".txt"));
        if (allRemainedGaiCycles.exists()) {

            allRemainedGaiCycles.delete();
        }
    }

    // Shared preferences... is an easy way of sharing data between activities.
    public void createSharedPrefrences() {

        pref = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putString("UserName", userName);
        editor.putInt("ApplicationStatus", appStatus);
        editor.putString("TrainDataFilePath", DataStorageLocation.TRAIN_RAW_DATA_PATH);
        editor.putString("TemplateDataFilePath", DataStorageLocation.TEMPLATE_PATH);
        editor.putString("MetaDataFilePath", DataStorageLocation.META_DATA_FILE);
        editor.putString("TestDataFilePath", DataStorageLocation.TEST_RAW_DATA_PATH);
        // here you can add more preferences
        editor.commit();
    }


    public void removeUserFromDataSet(String _userName) throws IOException {

        File filePath1 = new File(DataStorageLocation.TRAIN_RAW_DATA_PATH.concat("/").concat(_userName));
        DataStorageLocation.deleteDirectory(filePath1);

        LogMessage.setStatus(TAG, "Train DataOFuser is removed:" + _userName);

        DataStorageLocation.deleteDirectory(new File(DataStorageLocation.TRAIN_PROCESSED_DATA_PATH.concat("/").concat(_userName)));

        File allBestGaitCycles = new File(DataStorageLocation.ALL_TEMPLATES_PATH.concat("/BestGaitCycles/").concat(_userName).concat(".txt"));
        if (allBestGaitCycles.exists()) {

            allBestGaitCycles.delete();
        }

        File allRemainedGaiCycles = new File(DataStorageLocation.ALL_TEMPLATES_PATH.concat("/RemainedGaitCycles/").concat(_userName).concat(".txt"));
        if (allRemainedGaiCycles.exists()) {

            allRemainedGaiCycles.delete();
        }
    }

    public void displayUsersData() {
        if (doesDatabaseExist()) {
            gaitDatabase = new DataBaseHelper(context);
            Log.i(TAG, "Number of Users in Database" + gaitDatabase.numberOfRows());
            ArrayList array_list = gaitDatabase.getAllUsers();

            for (int i = 0; i < array_list.size(); i++) {

                String str = (String) array_list.get(i);
                Log.i(TAG, "User at " + i + " " + str);
                String userData = gaitDatabase.getUserData(str);
                Matrix mtr = gaitDatabase.String2MatrixRepresentation(userData);
                mtr.print(5, 5);
            }
        } else {
            Log.i(TAG, "gait database does not exist at the moment");
        }
    }


}
