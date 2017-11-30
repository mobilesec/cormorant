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

import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import at.usmile.cormorant.api.CormorantConstants;
import at.usmile.cormorant.api.model.StatusDataConfidence;
import at.usmile.cormorant.api.model.StatusDataRisk;

/**
 * Manages the list of active plugins and caches their data.
 * Informs other classes about plugin changes, if they implement the OnChangeListener
 */
public class PluginManager {
    private final static String LOG_TAG = PluginManager.class.getSimpleName();
    private static PluginManager instance;

    private List<PluginChangeListener> pluginChangeListeners = new LinkedList<>();
    private List<PluginInfo> pluginList = new ArrayList<>(); //TODO persistence required?

    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public PluginInfo getPluginInfo(String packageName){
        for(PluginInfo eachPluginInfo : pluginList){
            if(eachPluginInfo.getComponentName().getPackageName().equals(packageName)){
                return eachPluginInfo;
            }
        }
        return null;
    }

    public void changeConfidence(String packageName, StatusDataConfidence statusDataConfidence){
        PluginInfo api = getPluginInfo(packageName);
        api.setStatusDataConfidence(statusDataConfidence);
        api.setLastUpdate(Calendar.getInstance());
        notifyOnChangeListeners();

        Log.d(LOG_TAG, "Confidence for " + api.getTitle() + " changed to " + statusDataConfidence.getConfidence());
    }

    public void changeRisk(String packageName, StatusDataRisk statusDataRisk){
        PluginInfo api = getPluginInfo(packageName);
        api.setStatusDataRisk(statusDataRisk);
        api.setLastUpdate(Calendar.getInstance());
        notifyOnChangeListeners();

        Log.d(LOG_TAG, "Risk for " + api.getTitle() + " changed to " + statusDataRisk.getRisk());
    }

    public void removePlugin(String packageName){
        pluginList.remove(getPluginInfo(packageName));
        notifyOnChangeListeners();
        Log.d(LOG_TAG, "PluginList size after remove: " + pluginList.size());
    }

    public void addPlugin(PluginInfo pluginInfo) {
        pluginList.add(pluginInfo);
        notifyOnChangeListeners();
        Log.d(LOG_TAG, "PluginList size after add: " + pluginList.size());
    }

    public List<PluginInfo> getPluginListReadOnly() {
        return Collections.unmodifiableList(pluginList);
    }

    public void addPluginChangeListener(PluginChangeListener listener) {
        pluginChangeListeners.add(listener);
    }

    public void removePluginChangeListener(PluginChangeListener listener) {
        pluginChangeListeners.remove(listener);
    }

    public void pollDataFromPlugins(){
        Log.v(LOG_TAG, "Requesting data from plugins");
        for (PluginInfo eachPluginInfo : PluginManager.getInstance().getPluginListReadOnly()) {
            Message msg = Message.obtain(null, CormorantConstants.MSG_POLL_DATA);
            try {
                eachPluginInfo.getMessenger().send(msg);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Can't request data from " + eachPluginInfo.getTitle(), e);
            }
        }
    }

    private void notifyOnChangeListeners(){
        for (PluginChangeListener eachPluginChangeListener : pluginChangeListeners) {
            eachPluginChangeListener.onPluginsChanged();
        }
    }

    public static interface PluginChangeListener {
        void onPluginsChanged();
    }

}
