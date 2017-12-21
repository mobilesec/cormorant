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
package at.usmile.cormorant.framework.common;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.LinkedList;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by fhdwsse
 */

public class PermissionHelper {

    public static void checkAndGetPermissions(Activity activity, String... permissions) {
        List<String> permissionsToAsk = new LinkedList<>();
        for(String eachPermission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, eachPermission) != PERMISSION_GRANTED) {
                permissionsToAsk.add(eachPermission);
            }
        }
        if(permissionsToAsk.isEmpty()) return;
        //TODO React to user feedback and don't pretend he will always click yes.
        ActivityCompat.requestPermissions(activity, permissionsToAsk.toArray(new String[]{}),1);
    }

    public static boolean hasPermission(Context context, String permission) {
        final PackageManager packageManager = context.getPackageManager();
        return packageManager.checkPermission(permission,
                context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
    }

}
