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
package at.usmile.cormorant.framework.plugin;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import at.usmile.cormorant.api.CormorantConstants;
import at.usmile.cormorant.api.Utils;

/**
 * Removes plugins if they are uninstalled and starts plugins if they are installed.
 */
//TODO package update (=replace)? (for non adb updates) - react to compontent changes (start/stop =change)
public class PluginPackageChangeReceiver extends BroadcastReceiver {
    final static String LOG_TAG = PluginPackageChangeReceiver.class.getSimpleName();
    private PluginManager pluginManager;

    public PluginPackageChangeReceiver() {
        pluginManager = PluginManager.getInstance();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = intent.getData().getSchemeSpecificPart();
        Log.d(LOG_TAG, "Change: action: " + intent.getAction() + " package: " + packageName);

        if(Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())){
            pluginManager.removePlugin(packageName);
        }

        if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
            ComponentName serviceComponent = getStartupServiceComponent(context, packageName);
            if(serviceComponent != null){
                Log.d(LOG_TAG, "Starting " + serviceComponent.getShortClassName());
                if(!Utils.checkRegisterPermission(LOG_TAG, context, packageName)) return;
                Intent serviceIntent = new Intent();
                serviceIntent.setComponent(serviceComponent);
                context.startService(serviceIntent);
            }
        }
    }

    private ComponentName getStartupServiceComponent(Context context, String packageName) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA + PackageManager.GET_PERMISSIONS);
            Bundle metaData = applicationInfo.metaData;

            if(metaData == null) {
                Log.d(LOG_TAG, "MetaData: " + CormorantConstants.META_STARTUP_SERVICE + " not found for: " + packageName);
                return null;
            }
            String startupServiceName = metaData.getString( CormorantConstants.META_STARTUP_SERVICE);

            if(startupServiceName == null || "".equals(startupServiceName)){
                Log.d(LOG_TAG, "MetaData: " + CormorantConstants.META_STARTUP_SERVICE + " not found for: " + packageName);
                return null;
            }
            return new ComponentName(packageName, packageName + "." + startupServiceName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "getStartupServiceComponent failed - nameNotFound:" + e.getMessage());
        }
        return null;
    }
}
