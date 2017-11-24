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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import at.usmile.cormorant.api.model.StatusDataConfidence;
import at.usmile.cormorant.api.model.StatusDataRisk;

/**
 * Manages the connection to the framework through service binding and messages.
 */
abstract class AbstractPluginService extends Service {
    private String logTag = getClass().getSimpleName();
    private final Messenger callbackMessenger = new Messenger(new MessageHandler());
    private Messenger messenger = null;

    abstract CormorantConstants.PLUGIN_TYPE getPluginType();

    /**
     * Implement what happens if the AuthenticationFramework asks for a data update.
     * Data can be send with the publishConfidenceUpdate resp. publishRiskUpdate method.
     */
    protected abstract void onDataUpdateRequest();

    protected void publishDataUpdate(Bundle dataBundle) {
        if(messenger == null) {
            Log.w(logTag, "Can't send " + getPluginType().name() + ": No connection to framework");
            return;
        }
        Message msg = Message.obtain(null, (CormorantConstants.PLUGIN_TYPE.confidence.equals(getPluginType()) ?
                        CormorantConstants.MSG_CONFIDENCE : CormorantConstants.MSG_RISK));
        msg.setData(dataBundle);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            Log.e(logTag, "Can't send " + getPluginType().name() + "Data", e);
        }
    }

    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CormorantConstants.MSG_POLL_DATA:
                    Log.d(logTag, "Received poll request for data");
                    onDataUpdateRequest();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            messenger = new Messenger(service);

            try {
                Bundle bundle = new Bundle();
                bundle.putParcelable(CormorantConstants.KEY_COMPONENT_NAME, getServiceComponentName());
                Message msg = Message.obtain(null, CormorantConstants.MSG_ADD_PLUGIN);
                msg.setData(bundle);
                msg.replyTo = callbackMessenger;
                messenger.send(msg);
                Log.d(logTag, AbstractPluginService.this.getClass().getName() + " connected");
            } catch (RemoteException e) {
                Log.e(logTag, "Can't connect to Framework", e);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            messenger = null;
            stopSelf();
            Log.d(logTag, AbstractPluginService.this.getClass().getName() + " disconnected");
        }
    };

    private void bindToFramework() {
        Intent intent = new Intent();
        ComponentName frameworkComponent = new ComponentName(CormorantConstants.CORMORANT_PACKAGE,
                CormorantConstants.CORMORANT_PACKAGE + CormorantConstants.CORMORANT_SERVICE);

        //Check if there is a valid Framework installed
        if (!PermissionUtil.checkReadPluginDataPermission(logTag, this, frameworkComponent.getPackageName())) {
            Toast.makeText(this, "Can't bind to Framework: Service has not the required " + Permissions.READ_PLUGIN_DATA
                    + " permission", Toast.LENGTH_LONG).show();
        }
        intent.setComponent(frameworkComponent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindFromFramework() {
        unbindService(serviceConnection);
        //TODO it should also send a remove msg to the framework to update the plugin list or use packageChanged in pluginReceiver...
    }

    protected ComponentName getServiceComponentName() {
        return new ComponentName(getPackageName(), getClass().getName());
    }

    private void setupLocalDataReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(CormorantConstants.ACTION_LOCAL_SEND_CONFIDENCE);
        filter.addAction(CormorantConstants.ACTION_LOCAL_SEND_RISK);

        LocalBroadcastManager.getInstance(this).registerReceiver(localDataReceiver, filter);
    }

    @Override
    public void onCreate() {
        bindToFramework();
        setupLocalDataReceiver();
        Log.d(logTag, getClass().getName() + " created");
    }

    @Override
    public void onDestroy() {
        unbindFromFramework();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localDataReceiver);
        Log.d(logTag, getClass().getName() + " destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding is not supported");
    }

    private BroadcastReceiver localDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(CormorantConstants.KEY_COMPONENT_NAME, getServiceComponentName());

            if(CormorantConstants.ACTION_LOCAL_SEND_CONFIDENCE.equals(intent.getAction())) {
                StatusDataConfidence statusDataConfidence =
                        intent.getParcelableExtra(CormorantConstants.KEY_STATUS_DATA_CONFIDENCE);
                bundle.putParcelable(CormorantConstants.KEY_STATUS_DATA_CONFIDENCE, statusDataConfidence);
            }
            else {
                StatusDataRisk statusDataRisk =
                        intent.getParcelableExtra(CormorantConstants.KEY_STATUS_DATA_RISK);
                bundle.putParcelable(CormorantConstants.KEY_STATUS_DATA_RISK, statusDataRisk);
            }
            publishDataUpdate(bundle);
        }
    };

}
