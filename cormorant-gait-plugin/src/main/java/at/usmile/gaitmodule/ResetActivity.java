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
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import at.usmile.gaitmodule.database.DataBaseHelper;
import at.usmile.gaitmodule.extras.DataStorageLocation;
import at.usmile.gaitmodule.extras.LogMessage;

public class ResetActivity extends Activity {

    private static final String TAG = "ResetActivity";

    private DataBaseHelper gaitDataBase;
    private TextView userInfoTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset);
        LogMessage.setStatus(TAG, "Starting");
        userInfoTextView = (TextView) findViewById(R.id.textViewResetActivity);
        userInfoTextView.setText("To delete all Folders, data, templates please press delete button");
        gaitDataBase = new DataBaseHelper(this);
    }

    public void DeleteButtonPressed(View v) throws IOException {

        //Code for deleting gait data tmeplates form database
        if (gaitDataBase.numberOfRows() > 0) {
            Log.i(TAG, "Deleting gait Templates");
            gaitDataBase.emptyDataBaseTable2();
        } else {
            Log.i(TAG, "Database is already empty");
        }

        //Code to delete gait data templates from sdcard
        if (new File(Environment.getExternalStorageDirectory() + "/gaitDataRecording").isDirectory()) {

            DataStorageLocation.deleteDirectory(new File(Environment.getExternalStorageDirectory() + "/gaitDataRecording"));
            Toast.makeText(this, "Entire gait data is deleted", Toast.LENGTH_SHORT).show();
            LogMessage.setStatus(TAG, "Deletion is successful");
        } else {
            Toast.makeText(this, " There is no gait data to delete", Toast.LENGTH_SHORT).show();
        }
    }
}
