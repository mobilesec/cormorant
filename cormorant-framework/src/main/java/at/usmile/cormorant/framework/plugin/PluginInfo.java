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

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.os.Messenger;

import java.util.Calendar;

import at.usmile.cormorant.api.CormorantConstants;
import at.usmile.cormorant.api.model.StatusDataConfidence;
import at.usmile.cormorant.api.model.StatusDataRisk;

/**
 * Holds all pieces of information that a plugin can deliver.
 */
public class PluginInfo implements Comparable<PluginInfo> {

    private String title;
    private String description;
    private int apiVersion;
    private boolean implicit;
    private Calendar lastUpdate;
    private Drawable icon;
    private Messenger messenger;
    private ComponentName componentName;
    private ComponentName configurationComponentName;
    private ComponentName explicitAuthComponentName;
    private StatusDataConfidence statusDataConfidence;
    private StatusDataRisk statusDataRisk;
    private CormorantConstants.PLUGIN_TYPE pluginType;

    public PluginInfo(Messenger messenger) {
        this.messenger = messenger;
    }

    @Override
    public int compareTo(PluginInfo another) {
        return componentName.compareTo(another.componentName);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(int apiVersion) {
        this.apiVersion = apiVersion;
    }

    public boolean isImplicit() {
        return implicit;
    }

    public void setImplicit(boolean implicit) {
        this.implicit = implicit;
    }

    public Calendar getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Calendar lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public void setMessenger(Messenger messenger) {
        this.messenger = messenger;
    }

    public ComponentName getComponentName() {
        return componentName;
    }

    public void setComponentName(ComponentName componentName) {
        this.componentName = componentName;
    }

    public ComponentName getConfigurationComponentName() {
        return configurationComponentName;
    }

    public void setConfigurationComponentName(ComponentName configurationComponentName) {
        this.configurationComponentName = configurationComponentName;
    }

    public ComponentName getExplicitAuthComponentName() {
        return explicitAuthComponentName;
    }

    public void setExplicitAuthComponentName(ComponentName explicitAuthComponentName) {
        this.explicitAuthComponentName = explicitAuthComponentName;
    }

    public StatusDataConfidence getStatusDataConfidence() {
        return statusDataConfidence;
    }

    public void setStatusDataConfidence(StatusDataConfidence statusDataConfidence) {
        this.statusDataConfidence = statusDataConfidence;
    }

    public StatusDataRisk getStatusDataRisk() {
        return statusDataRisk;
    }

    public void setStatusDataRisk(StatusDataRisk statusDataRisk) {
        this.statusDataRisk = statusDataRisk;
    }

    public CormorantConstants.PLUGIN_TYPE getPluginType() {
        return pluginType;
    }

    public void setPluginType(CormorantConstants.PLUGIN_TYPE pluginType) {
        this.pluginType = pluginType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PluginInfo that = (PluginInfo) o;

        if (apiVersion != that.apiVersion) return false;
        if (implicit != that.implicit) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null)
            return false;
        if (lastUpdate != null ? !lastUpdate.equals(that.lastUpdate) : that.lastUpdate != null)
            return false;
        if (icon != null ? !icon.equals(that.icon) : that.icon != null) return false;
        if (messenger != null ? !messenger.equals(that.messenger) : that.messenger != null)
            return false;
        if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null)
            return false;
        if (configurationComponentName != null ? !configurationComponentName.equals(that.configurationComponentName) : that.configurationComponentName != null)
            return false;
        if (explicitAuthComponentName != null ? !explicitAuthComponentName.equals(that.explicitAuthComponentName) : that.explicitAuthComponentName != null)
            return false;
        if (statusDataConfidence != null ? !statusDataConfidence.equals(that.statusDataConfidence) : that.statusDataConfidence != null)
            return false;
        if (statusDataRisk != null ? !statusDataRisk.equals(that.statusDataRisk) : that.statusDataRisk != null)
            return false;
        return pluginType == that.pluginType;

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + apiVersion;
        result = 31 * result + (implicit ? 1 : 0);
        result = 31 * result + (lastUpdate != null ? lastUpdate.hashCode() : 0);
        result = 31 * result + (icon != null ? icon.hashCode() : 0);
        result = 31 * result + (messenger != null ? messenger.hashCode() : 0);
        result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
        result = 31 * result + (configurationComponentName != null ? configurationComponentName.hashCode() : 0);
        result = 31 * result + (explicitAuthComponentName != null ? explicitAuthComponentName.hashCode() : 0);
        result = 31 * result + (statusDataConfidence != null ? statusDataConfidence.hashCode() : 0);
        result = 31 * result + (statusDataRisk != null ? statusDataRisk.hashCode() : 0);
        result = 31 * result + (pluginType != null ? pluginType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PluginInfo{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", apiVersion=" + apiVersion +
                ", implicit=" + implicit +
                ", lastUpdate=" + lastUpdate +
                ", icon=" + icon +
                ", messenger=" + messenger +
                ", componentName=" + componentName +
                ", configurationComponentName=" + configurationComponentName +
                ", explicitAuthComponentName=" + explicitAuthComponentName +
                ", statusDataConfidence=" + statusDataConfidence +
                ", statusDataRisk=" + statusDataRisk +
                ", pluginType=" + pluginType +
                '}';
    }
}

