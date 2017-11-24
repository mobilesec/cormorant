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
package at.usmile.cormorant.framework;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.util.List;

import at.usmile.cormorant.api.CormorantConstants;
import at.usmile.cormorant.api.PermissionUtil;
import at.usmile.cormorant.api.model.StatusDataConfidence;
import at.usmile.cormorant.api.model.StatusDataRisk;
import at.usmile.cormorant.framework.common.TypedServiceConnection;
import at.usmile.cormorant.framework.group.GroupService;
import at.usmile.cormorant.framework.lock.LockService;
import at.usmile.cormorant.framework.messaging.MessagingService;
import at.usmile.cormorant.framework.module.DecisionModule;
import at.usmile.cormorant.framework.module.strategies.DecisionStrategyDefault;
import at.usmile.cormorant.framework.module.strategies.FusionStrategyConfidenceDefault;
import at.usmile.cormorant.framework.module.strategies.FusionStrategyRiskDefault;
import at.usmile.cormorant.framework.plugin.PluginInfo;
import at.usmile.cormorant.framework.plugin.PluginManager;

/**
 * The core service of the AuthenticationFramework.
 * Starts all available plugins on startup (newly installed plugins are started by the PluginPackageChangeReceiver).
 * Plugins bind to this service and communicate through a Messenger.
 * If this service dies, all bound plugins will automatically shutdown.
 *
 * Plugins need the following requirements in order to connect to the AuthenticationFramework:
 * (see confidenceplugin for example usage)
 *
 * Permissions: at.usmile.cormorant.REGISTER_AUTH_PLUGIN
 * Manifest Meta Data:
 * - Plugin Service:
 * -> apiVersion
 * -> pluginType [risk, confidence]
 * -> title
 * -> description
 * -> configurationActivity (optional, SimpleName of the activity for configuration purposes)
 * -> explicitAuthActivity (optional, SimpleName of the activity for explicit authentication)
 * -> implicit [true, false]
 * - Application
 * -> startupService (SimpleName of the plugin service)
 */
public class AuthenticationFrameworkService extends Service {
    private final static String LOG_TAG = AuthenticationFrameworkService.class.getSimpleName();

    private PluginManager pluginManager = PluginManager.getInstance();
    private DecisionModule decisionModule;

    private TypedServiceConnection<MessagingService> messagingService = new TypedServiceConnection() ;
    private TypedServiceConnection<LockService> lockService = new TypedServiceConnection() ;

    class PluginMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CormorantConstants.MSG_ADD_PLUGIN:
                    registerPlugin(msg);
                    break;
                case CormorantConstants.MSG_CONFIDENCE:
                    readData(CormorantConstants.PLUGIN_TYPE.confidence, msg);
                    break;
                case CormorantConstants.MSG_RISK:
                    readData(CormorantConstants.PLUGIN_TYPE.risk, msg);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void registerPlugin(Message msg) {
        ComponentName pluginComponentName = msg.getData().getParcelable(CormorantConstants.KEY_COMPONENT_NAME);
        PluginInfo api = new PluginInfo(msg.replyTo);
        setPluginMetaData(api, pluginComponentName);

        if (pluginComponentName == null || !PermissionUtil.checkRegisterPermission(LOG_TAG, AuthenticationFrameworkService.this, pluginComponentName.getPackageName())) {
            return;
        }

        pluginManager.addPlugin(api);
    }

    private void readData(CormorantConstants.PLUGIN_TYPE type, Message msg) {
        Bundle dataBundle = msg.getData();
        dataBundle.setClassLoader(CormorantConstants.class.getClassLoader());
        ComponentName pluginComponent = dataBundle.getParcelable(CormorantConstants.KEY_COMPONENT_NAME);

        if (pluginComponent == null || !PermissionUtil.checkRegisterPermission(LOG_TAG, AuthenticationFrameworkService.this, pluginComponent.getPackageName())) {
            return;
        }

        if (CormorantConstants.PLUGIN_TYPE.confidence.equals(type)) {
            StatusDataConfidence statusDataConfidence = dataBundle.getParcelable(CormorantConstants.KEY_STATUS_DATA_CONFIDENCE);
            pluginManager.changeConfidence(pluginComponent.getPackageName(), statusDataConfidence);
        } else {
            StatusDataRisk statusDataRisk = msg.getData().getParcelable(CormorantConstants.KEY_STATUS_DATA_RISK);
            pluginManager.changeRisk(pluginComponent.getPackageName(), statusDataRisk);
        }
    }

    final Messenger messenger = new Messenger(new PluginMessageHandler());

    private void initDecisionModule() {
        //Choose module strategies
        decisionModule = new DecisionModule(
                this,
                new DecisionStrategyDefault(),
                new FusionStrategyConfidenceDefault(),
                new FusionStrategyRiskDefault());
        PluginManager.getInstance().addOnChangeListener(decisionModule);
        decisionModule.start();
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "AuthenticationFrameworkService started");
        reconnectAllPlugins();
        initDecisionModule();

        startService(new Intent(this, GroupService.class));
        bindService(new Intent(this, MessagingService.class), messagingService, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, LockService.class), lockService, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "AuthenticationFrameworkService stopped");
        decisionModule.stop();

        if(messagingService.isBound()) unbindService(messagingService);
        if(lockService.isBound()) unbindService(lockService);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    //TODO Handle null and invalid meta values
    private void setPluginMetaData(PluginInfo api, ComponentName componentName) {
        try {
            ServiceInfo serviceInfo = getPackageManager().getServiceInfo(componentName, PackageManager.GET_META_DATA);
            Bundle metaData = serviceInfo.metaData;

            api.setComponentName(componentName);
            api.setConfigurationComponentName(createComponentName(componentName, metaData.getString(CormorantConstants.META_CONFIGURATION)));
            api.setExplicitAuthComponentName(createComponentName(componentName, metaData.getString(CormorantConstants.META_EXPLICIT_AUTH)));
            api.setPluginType(CormorantConstants.PLUGIN_TYPE.valueOf(metaData.getString(CormorantConstants.META_PLUGIN_TYPE)));
            api.setTitle(metaData.getString(CormorantConstants.META_TITLE));
            api.setApiVersion(metaData.getInt(CormorantConstants.META_API_VERSION));
            api.setDescription(metaData.getString(CormorantConstants.META_DESCRIPTION));
            api.setImplicit(metaData.getBoolean(CormorantConstants.META_IMPLICIT));
            api.setIcon(getPackageManager().getApplicationIcon(componentName.getPackageName()));
            api.setStatusDataConfidence(new StatusDataConfidence());
            api.setStatusDataRisk(new StatusDataRisk());
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Failed to load meta-data, NameNotFound: " + e.getMessage());
        }
    }

    private ComponentName createComponentName(ComponentName mainComponent, String className) {
        if (className == null || className == "") return null;
        return new ComponentName(mainComponent.getPackageName(), mainComponent.getPackageName() + "." + className);
    }

    private void reconnectAllPlugins() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                List<PackageInfo> installedPackages = getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA);
                for (PackageInfo eachInstalledPackage : installedPackages) {
                    String packageName = eachInstalledPackage.packageName;
                    Bundle metaData = eachInstalledPackage.applicationInfo.metaData;
                    if (metaData == null) continue;
                    String startupServiceName = metaData.getString(CormorantConstants.META_STARTUP_SERVICE);
                    if (startupServiceName == null) continue;
                    ComponentName serviceComponent = new ComponentName(packageName, packageName + "." + startupServiceName);
                    Log.d(LOG_TAG, "Starting " + serviceComponent.getShortClassName());
                    Intent serviceIntent = new Intent();
                    serviceIntent.setComponent(serviceComponent);
                    startService(serviceIntent);
                }
                return null;
            }
        }.execute();
    }

}
