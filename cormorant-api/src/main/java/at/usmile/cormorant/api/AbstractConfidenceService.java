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
package at.usmile.cormorant.api;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import at.usmile.cormorant.api.model.StatusDataConfidence;

/**
 * Base class for all confidence plugin services with convenient methods.
 */
public abstract class AbstractConfidenceService extends AbstractPluginService {

    private ConfidencePluginServiceBinder mBinder = new ConfidencePluginServiceBinder();

    protected void publishConfidenceUpdate(StatusDataConfidence statusDataConfidence) {
        Bundle bundle = createConfidenceDataBundle(statusDataConfidence);
        publishDataUpdate(bundle);
    }

    @Override
    CormorantConstants.PLUGIN_TYPE getPluginType() {
        return CormorantConstants.PLUGIN_TYPE.confidence;
    }

    protected Bundle createConfidenceDataBundle(StatusDataConfidence statusDataConfidence){
        Bundle bundle = new Bundle();
        bundle.putParcelable(CormorantConstants.KEY_STATUS_DATA_CONFIDENCE, statusDataConfidence);
        bundle.putParcelable(CormorantConstants.KEY_COMPONENT_NAME, getServiceComponentName());
        return bundle;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class ConfidencePluginServiceBinder extends Binder {
        public AbstractConfidenceService getService() {
            // Return this instance of AbstractPluginService so clients can call public methods
            return AbstractConfidenceService.this;
        }
    }

}
