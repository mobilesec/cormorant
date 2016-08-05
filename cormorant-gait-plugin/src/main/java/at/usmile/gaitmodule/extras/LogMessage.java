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
package at.usmile.gaitmodule.extras;

import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// A class to print logs
public class LogMessage {

    // In LogbackFile all logs will appear with name gaitLogs
    // Class name can also be used gaitLogs are just used to keep
    // shorter logs.
    static private final Logger LOG = LoggerFactory.getLogger("gaitLogs:");

    // If we are using class specific logs
    public static void setStatus(Logger LOG, String _tag, String _msg) {
        Log.i(_tag, ":" + _msg);
        LOG.info(_tag + ":" + _msg);
    }

    // If we want to put all logs under gaitLogs name, however here _tag
    // parameter represents class name.
    public static void setStatus(String _tag, String _msg) {

        Log.i(_tag, ":" + _msg);
        LOG.info(_tag + ":" + _msg);
    }
}
