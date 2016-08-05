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
package at.usmile.cormorant.api;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import at.usmile.cormorant.api.model.StatusDataConfidence;
import at.usmile.cormorant.api.model.StatusDataRisk;

/**
 * Base class for plugin activities with convenient methods for sending data to the framework.
 */
public class AbstractPluginActivity extends AppCompatActivity {
    protected void publishConfidenceData(StatusDataConfidence statusDataConfidence) {
        Intent intent = new Intent(CormorantConstants.ACTION_LOCAL_SEND_CONFIDENCE);
        intent.putExtra(CormorantConstants.KEY_STATUS_DATA_CONFIDENCE, statusDataConfidence);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    protected void publishRiskData(StatusDataRisk statusDataRisk) {
        Intent intent = new Intent(CormorantConstants.ACTION_LOCAL_SEND_RISK);
        intent.putExtra(CormorantConstants.KEY_STATUS_DATA_RISK, statusDataRisk);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
