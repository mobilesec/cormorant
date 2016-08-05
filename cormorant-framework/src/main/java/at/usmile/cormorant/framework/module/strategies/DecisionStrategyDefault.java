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
package at.usmile.cormorant.framework.module.strategies;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import at.usmile.cormorant.framework.AdminReceiver;
import at.usmile.cormorant.framework.module.DecisionStrategy;

public class DecisionStrategyDefault implements DecisionStrategy {
    private final static String LOG_TAG = DecisionStrategyDefault.class.getSimpleName();

    @Override
    public void processData(Context context, double confidenceLevel, double riskLevel) {
        double authLevel = -1;
        if(Double.isNaN(confidenceLevel) && Double.isNaN(riskLevel)) {
            Log.d(LOG_TAG, "ConfidenceLevel and RiskLevel not available");
            authLevel = 0;
        }
        else if(!Double.isNaN(confidenceLevel) && Double.isNaN(riskLevel)){
            Log.d(LOG_TAG, "RiskLevel not available");
            authLevel = confidenceLevel;
        }
        else if(Double.isNaN(confidenceLevel) && !Double.isNaN(riskLevel)){
            Log.d(LOG_TAG, "ConfidenceLevel not available");
            authLevel = riskLevel;
        }
        else {
            authLevel = (confidenceLevel + riskLevel) / 2;
        }
        Log.d(LOG_TAG, "AuthLevel result: " + authLevel);

        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if(authLevel > 0.99 && devicePolicyManager.isAdminActive(getActiveComponentName(context))){
            Log.d(LOG_TAG, "AuthLevel too high - locking device"); //used high number, so the device won't lock at startup
            devicePolicyManager.lockNow();
        }
    }

    private ComponentName getActiveComponentName(Context context) {
        return new ComponentName(context, AdminReceiver.class);
    }

    @Override
    public int getPollingInterval() {
        return 15000;
    }
}
