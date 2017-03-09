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

public class CormorantConstants {
    public static final int MSG_ADD_PLUGIN = 1;
    public static final int MSG_CONFIDENCE = 2;
    public static final int MSG_RISK = 3;
    public static final int MSG_POLL_DATA = 4;

    public static final String ACTION_LOCAL_SEND_CONFIDENCE = "actionLocalSendConfidence";
    public static final String ACTION_LOCAL_SEND_RISK = "actionLocalSendRisk";

    public static final String KEY_COMPONENT_NAME = "keyComponentName";
    public static final String KEY_STATUS_DATA_CONFIDENCE = "keyStatusDataConfidence";
    public static final String KEY_STATUS_DATA_RISK = "keyStatusDataRisk";

    public static final String CORMORANT_PACKAGE = "at.usmile.cormorant.framework";
    public static final String CORMORANT_SERVICE = ".AuthenticationFrameworkService";

    public static final String META_API_VERSION = "apiVersion";
    public static final String META_PLUGIN_TYPE = "pluginType";
    public static final String META_TITLE = "title";
    public static final String META_DESCRIPTION = "description";
    public static final String META_CONFIGURATION = "configurationActivity";
    public static final String META_EXPLICIT_AUTH = "explicitAuthActivity";
    public static final String META_STARTUP_SERVICE = "startupService";
    public static final String META_IMPLICIT = "implicit";

    public enum PLUGIN_TYPE {
        confidence, risk
    }
}
