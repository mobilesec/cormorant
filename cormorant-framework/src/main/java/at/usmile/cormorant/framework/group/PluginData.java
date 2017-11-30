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
