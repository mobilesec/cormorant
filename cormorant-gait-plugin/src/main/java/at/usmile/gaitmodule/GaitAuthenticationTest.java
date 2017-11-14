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
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import at.usmile.gaitmodule.extras.LogMessage;
import at.usmile.gaitmodule.services.StepDetectorService;


public class GaitAuthenticationTest extends Activity {
    private static final String TAG = "GaitAuthenticationTest";

    private Button startButtonAuthenticationService;
    private Button stopButtonAuthenticationService;
    private boolean serviceStatus;
    private Intent serviceIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gait_authentication_test);

        // Initalize start and stop buttons...
        LogMessage.setStatus(TAG, "Starting");
        startButtonAuthenticationService = (Button) findViewById(R.id.startService);
        stopButtonAuthenticationService = (Button) findViewById(R.id.stopService);

        serviceIntent = new Intent(this, StepDetectorService.class);
        // We need to set buttons accordingly if service is running or not.
        // This also allows us to permanently save the state of the activity
        // When service is not running we only show start button
        serviceStatus = isMyServiceRunning(StepDetectorService.class);
        if (!serviceStatus) {
            Log.i(TAG, "service is not running");
            startButtonAuthenticationService.setEnabled(true);
            stopButtonAuthenticationService.setEnabled(false);
        }

        //When service is running we only show stop button
        if (serviceStatus) {
            Log.i(TAG, "service is running");
            startButtonAuthenticationService.setEnabled(false);
            stopButtonAuthenticationService.setEnabled(true);
        }

    }

    public void startServicePressed(View v) {

        startButtonAuthenticationService.setEnabled(false);
        stopButtonAuthenticationService.setEnabled(true);
        startService(serviceIntent);


        Bundle dataReturn = new Bundle();
        dataReturn.putBoolean("authenticationServiceStatus", true);
        Intent returnIntent = new Intent();
        returnIntent.putExtras(dataReturn);
        setResult(RESULT_OK, returnIntent);
        Log.d("authenticationServiceStatus", "true");
        finish();
    }

    public void stopServicePressed(View v) {

        startButtonAuthenticationService.setEnabled(true);
        stopButtonAuthenticationService.setEnabled(false);
        stopService(serviceIntent);

        Bundle dataReturn = new Bundle();
        dataReturn.putBoolean("authenticationServiceStatus", false);
        Intent returnIntent = new Intent();
        returnIntent.putExtras(dataReturn);
        setResult(RESULT_OK, returnIntent);
        Log.d("authenticationServiceStatus", "true");
        finish();
    }

    // This function is to check if our gait authentication service is already running.
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
