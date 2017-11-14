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

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import at.usmile.cormorant.api.AbstractConfidenceService;
import at.usmile.cormorant.api.model.StatusDataConfidence;
import at.usmile.gaitmodule.database.DataBaseHelper;
import at.usmile.gaitmodule.services.StepDetectorService;

public class GaitAuthenticationModule extends AbstractConfidenceService {

    AuthenticationResultReceiver resultReceiver;
    private String TAG = "GaitAuthenticationModule";
    private DataBaseHelper gaitDataBase;

    /**
     * receives authentication information from the explicit authentication
     * Activity.
     */

    public class AuthenticationResultReceiver extends BroadcastReceiver {
        public static final String ON_AUTHENTICATION = "ON_AUTHENTICATION";
        public static final String CONFIDENCE = "CONFIDENCE";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceiveBroadcast(" + intent + ")");
            // reported confidence from the used authentication
            double confidence = intent.getDoubleExtra(CONFIDENCE, 0.0);
            Log.i(TAG, "confidence:: = " + confidence);
            publishConfidenceUpdate(new StatusDataConfidence().status(
                    StatusDataConfidence.Status.OPERATIONAL).confidence(confidence));
        }
    }


    @Override
    protected void onDataUpdateRequest() {
        //TODO implement
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Framework service is started");
        super.onCreate();
        gaitDataBase = new DataBaseHelper(this);

        if (templatePresent() && !isMyServiceRunning(StepDetectorService.class)) {
            Intent serviceIntent = new Intent(this, StepDetectorService.class);
            startService(serviceIntent);
        }
        resultReceiver = new AuthenticationResultReceiver();
        Log.i(TAG, "Starting gait authenticaiton module service");
        registerReceiver(resultReceiver, new IntentFilter(
                AuthenticationResultReceiver.ON_AUTHENTICATION));
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(resultReceiver);
        Log.i(TAG, "Stoping gait authenticaiton module service");
        super.onDestroy();

    }

    protected void onUpdateAuthenticationStatus(int reason) {

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


    // Function to check if specific service is running or not
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}