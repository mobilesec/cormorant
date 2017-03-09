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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

public class Utils {

    public static boolean checkRegisterPermission(String logTag, Context context, String packageName){
        return checkForPermission(logTag, context, packageName, Manifest.permission.REGISTER_AUTH_PLUGIN);
    }

    public static boolean checkReadPluginDataPermission(String logTag, Context context, String packageName){
        return checkForPermission(logTag, context, packageName, Manifest.permission.READ_PLUGIN_DATA);
    }

    private static boolean checkForPermission(String logTag, Context context, String packageName, String permission){
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (packageInfo.requestedPermissions != null) {
                for (String eachRequestPermission : packageInfo.requestedPermissions) {
                    if (permission.equals(eachRequestPermission)) {
                        return true;
                    }
                }
            }
        }
        catch (PackageManager.NameNotFoundException e) {
            Log.e(logTag, "Failed to load permissions, NameNotFound: " + e.getMessage());
        }
        Log.d(logTag, "Plugin " + packageName + " has not the required permissions to connect to the Authentication Framework");
        Toast.makeText(context, packageName + " has not the required permissions to connect to the Authentication Framework", Toast.LENGTH_LONG).show();
        return false;
    }
}
