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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import at.usmile.gaitmodule.database.DataBaseHelper;
import at.usmile.gaitmodule.extras.LogMessage;

public class ExistingUserActivity extends Activity {
    private static final String TAG = "ExistingUserActivity";

    //private TextView userInforText_ExistingUser;
    private ListView listViewExistingUser;
    private ArrayList<String> userNameList2;
    private String selectedUserName;
    private int selectedUserID;
    private boolean deleteConfirmation = false;
    private DataBaseHelper myDb;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_existing_user);

        LogMessage.setStatus(TAG, "Starting ExistingUserActivity");

        listViewExistingUser = (ListView) findViewById(R.id.listViewExistingUserActivity);
        myDb = new DataBaseHelper(this);
        userNameList2 = myDb.getAllUsers();

        if (!userNameList2.isEmpty()) {

            // Call method to show list to the user.
            displayUserList();

            // What to do when user clicks on a listItem.
            listViewExistingUser.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                    selectedUserName = (String) listViewExistingUser.getItemAtPosition(position);
                    selectedUserID = position;
                    Toast.makeText(getApplicationContext(), selectedUserName + "is present at Position:" + position, Toast.LENGTH_SHORT).show();
                    recordMoreTrainingDataConfirmationDialogue();
                }

            });

            //What to do when user Longpresses on a listItem
            listViewExistingUser.setOnItemLongClickListener(new OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                    selectedUserName = (String) listViewExistingUser.getItemAtPosition(position);
                    selectedUserID = position;
                    Toast.makeText(getApplicationContext(), selectedUserName + "is present at Position pressed Long:" + position, Toast.LENGTH_SHORT).show();
                    deleteConfirmation = true;
                    deleteConfirmationDialogue(deleteConfirmation);
                    return true;
                }
            });
        } else {

            Toast.makeText(this, "No user is available to view", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to setup listAdapter and ListView
    private void displayUserList() {

        //Enable these lines for SQLiteDataBase
        final ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this, R.layout.activity_existing_usersnames, userNameList2);
        listViewExistingUser.setAdapter(adapter1);

    }

    // Method to delete a username from the list
    public void deleteConfirmationDialogue(boolean _deletConfimation) {

        if (_deletConfimation) {

            //........Code starts to take user name as input via dialogue.......//
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Do you want to delete training data of " + selectedUserName);
            alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int whichButton) {

                    Bundle dataReturn = new Bundle();
                    dataReturn.putString("UserName", selectedUserName);
                    dataReturn.putInt("UserID", selectedUserID);
                    dataReturn.putBoolean("RemoveUser", true);
                    Intent returnIntent = new Intent();
                    returnIntent.putExtras(dataReturn);
                    setResult(RESULT_OK, returnIntent);
                    finish();

                }

            });

            alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });

            alert.show();

            //...........Code Ends to take user name as an input via dialogue..........//
        }
    }

    //Method to add more data recorded by the user
    public void recordMoreTrainingDataConfirmationDialogue() {

        //........Code starts to take user name as input via dialogue.......//
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Do you want to record more training data for " + selectedUserName);
        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {

                // Return back your results to the parent activity
                // Think about how to record more training data for the user

                Bundle dataReturn = new Bundle();
                dataReturn.putString("UserName", selectedUserName);
                dataReturn.putInt("UserID", selectedUserID);
                dataReturn.putBoolean("RemoveUser", false);
                Intent returnIntent = new Intent();
                returnIntent.putExtras(dataReturn);

                setResult(RESULT_OK, returnIntent);
                finish();
            }

        });

        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        alert.show();

        //...........Code Ends to take user name as an input via dialogue..........//
    }


}




