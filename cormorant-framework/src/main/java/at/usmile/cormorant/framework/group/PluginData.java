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
package at.usmile.cormorant.framework.group;

import java.util.Calendar;
import java.util.Objects;

import at.usmile.cormorant.api.CormorantConstants;
import at.usmile.cormorant.api.model.StatusDataConfidence;
import at.usmile.cormorant.api.model.StatusDataRisk;
import at.usmile.cormorant.framework.plugin.PluginInfo;

/**
 * Uses only the important info of PluginInfo
 */

public class PluginData {
    private String title;
    private String description;
    private Calendar lastUpdate;
    private StatusDataConfidence statusDataConfidence;
    private StatusDataRisk statusDataRisk;
    private CormorantConstants.PLUGIN_TYPE pluginType;

    public PluginData(PluginInfo pluginInfo) {
        this.title = pluginInfo.getTitle();
        this.description = pluginInfo.getDescription();
        this.lastUpdate = pluginInfo.getLastUpdate();
        this.statusDataConfidence = pluginInfo.getStatusDataConfidence();
        this.statusDataRisk = pluginInfo.getStatusDataRisk();
        this.pluginType = pluginInfo.getPluginType();
    }

    public String getTitle() {
        return title;
    }

    public Calendar getLastUpdate() {
        return lastUpdate;
    }

    public StatusDataConfidence getStatusDataConfidence() {
        return statusDataConfidence;
    }

    public StatusDataRisk getStatusDataRisk() {
        return statusDataRisk;
    }

    public String getDescription() {
        return description;
    }

    public CormorantConstants.PLUGIN_TYPE getPluginType() {
        return pluginType;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginData that = (PluginData) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                Objects.equals(lastUpdate, that.lastUpdate) &&
                Objects.equals(statusDataConfidence, that.statusDataConfidence) &&
                Objects.equals(statusDataRisk, that.statusDataRisk) &&
                pluginType == that.pluginType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, lastUpdate, statusDataConfidence, statusDataRisk, pluginType);
    }

    @Override
    public String toString() {
        return "PluginData{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", statusDataConfidence=" + statusDataConfidence +
                ", statusDataRisk=" + statusDataRisk +
                ", pluginType=" + pluginType +
                '}';
    }
}
