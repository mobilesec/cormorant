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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

public class PermissionUtil {

    private static final String KEY_PERMISSIONS = "permissions";
    private static final String KEY_GRANT_RESULTS = "grantResults";
    private static final String KEY_RESULT_RECEIVER = "resultReceiver";
    private static final String KEY_REQUEST_CODE = "requestCode";
    private static final int NOTIFICATION_INTENT_REQUEST = 1;

    public static boolean checkRegisterPermission(String logTag, Context context, String packageName) {
        return checkForPermission(logTag, context, packageName, Permissions.REGISTER_AUTH_PLUGIN);
    }

    public static boolean checkReadPluginDataPermission(String logTag, Context context, String packageName) {
        return checkForPermission(logTag, context, packageName, Permissions.READ_PLUGIN_DATA);
    }

    private static boolean checkForPermission(String logTag, Context context, String packageName, String permission) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (packageInfo.requestedPermissions != null) {
                for (String eachRequestPermission : packageInfo.requestedPermissions) {
                    if (permission.equals(eachRequestPermission)) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(logTag, "Failed to load permissions, NameNotFound: " + e.getMessage());
        }

        Log.d(logTag, "Plugin " + packageName + " has not the required permissions to connect to the Authentication Framework");
        Toast.makeText(context, packageName + " has not the required permissions to connect to the Authentication Framework", Toast.LENGTH_LONG).show();
        return false;
    }

    public static void requestPermissions(final Context context, final OnRequestPermissionsResultCallback callback, String[] permissions, int requestCode,  int notificationIcon) {
        ResultReceiver resultReceiver = new ResultReceiver(new Handler(Looper.getMainLooper())) {
            @Override
            protected void onReceiveResult (int resultCode, Bundle resultData) {
                String[] outPermissions = resultData.getStringArray(KEY_PERMISSIONS);
                int[] grantResults = resultData.getIntArray(KEY_GRANT_RESULTS);
                callback.onRequestPermissionsResult(resultCode, outPermissions, grantResults);
            }
        };

        Intent permIntent = new Intent(context, PermissionRequestActivity.class);
        permIntent.putExtra(KEY_RESULT_RECEIVER, resultReceiver);
        permIntent.putExtra(KEY_PERMISSIONS, permissions);
        permIntent.putExtra(KEY_REQUEST_CODE, requestCode);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(permIntent);

        PendingIntent permPendingIntent =
                stackBuilder.getPendingIntent(
                        NOTIFICATION_INTENT_REQUEST,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "default")
                .setSmallIcon(notificationIcon)
                .setContentTitle("Additional permissions required")
                .setContentText("Tap to manage permissions")
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setWhen(0)
                .setContentIntent(permPendingIntent)
                .setStyle(null);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(requestCode, builder.build());
    }

    private static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    public static class PermissionRequestActivity extends AppCompatActivity {
        ResultReceiver resultReceiver;
        String[] permissions;
        int requestCode;

        @Override
        public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
            Bundle resultData = new Bundle();
            resultData.putStringArray(KEY_PERMISSIONS, permissions);
            resultData.putIntArray(KEY_GRANT_RESULTS, grantResults);
            resultReceiver.send(requestCode, resultData);
            finish();
        }

        @Override
        protected void onStart() {
            super.onStart();

            resultReceiver = this.getIntent().getParcelableExtra(KEY_RESULT_RECEIVER);
            permissions = this.getIntent().getStringArrayExtra(KEY_PERMISSIONS);
            requestCode = this.getIntent().getIntExtra(KEY_REQUEST_CODE, 0);

            ActivityCompat.requestPermissions(this, permissions, requestCode);
        }
    }
}
