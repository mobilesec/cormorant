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

import android.location.Location;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import at.usmile.cormorant.framework.location.SimpleLocation;
import at.usmile.cormorant.framework.location.bluetooth.DistanceHelper;

public class TrustedDevice {

    public static final int DEVICE_UNKNOWN_GPS_DISTANCE = -1;

    private UUID id;

    private String manufacturer;

    private String model;

    private double screenSize;

    private SimpleLocation location;

    //Distance in meters for GPS location
    private double distanceToOtherDeviceGps;

    //Fuzzy distance for bluetooth distance
    private DistanceHelper.DISTANCE distanceToOtherDeviceBluetooth;

    private boolean isLocked;

    private List<PluginData> activePlugins;

    /*
    * Only to be used by the Group Service for group challenge and response.
    * */
    TrustedDevice(UUID id) {
        this(id, null, null, 0);
    }

    public TrustedDevice(UUID id, String manufacturer, String model, double screenSize) {
        this.id = id;
        this.manufacturer = manufacturer;
        this.model = model;
        this.screenSize = screenSize;
        this.distanceToOtherDeviceGps = DEVICE_UNKNOWN_GPS_DISTANCE;
        this.distanceToOtherDeviceBluetooth = DistanceHelper.DISTANCE.UNKNOWN;
        this.activePlugins = new LinkedList<>();
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public UUID getId() {
        return id;
    }

    public double getScreenSize() {
        return screenSize;
    }

    public SimpleLocation getLocation() {
        return location;
    }

    public void setLocation(SimpleLocation location) {
        this.location = location;
    }

    public void setLocation(Location location) {
        this.location = new SimpleLocation(location);
    }

    public double getDistanceToOtherDeviceGps() {
        return distanceToOtherDeviceGps;
    }

    public void setDistanceToOtherDeviceGps(double distanceToOtherDeviceGps) {
        this.distanceToOtherDeviceGps = distanceToOtherDeviceGps;
    }

    public DistanceHelper.DISTANCE getDistanceToOtherDeviceBluetooth() {
        return distanceToOtherDeviceBluetooth;
    }

    public void setDistanceToOtherDeviceBluetooth(DistanceHelper.DISTANCE distanceToOtherDeviceBluetooth) {
        this.distanceToOtherDeviceBluetooth = distanceToOtherDeviceBluetooth;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public List<PluginData> getActivePlugins() {
        if (this.activePlugins == null) this.activePlugins = new LinkedList<>();

        return activePlugins;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public void setActivePlugins(List<PluginData> activePlugins) {
        // I don't understand how this can be null at this point?
        if (this.activePlugins == null) this.activePlugins = new LinkedList<>();

        this.activePlugins.clear();
        this.activePlugins.addAll(activePlugins);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrustedDevice that = (TrustedDevice) o;

        if (Double.compare(that.screenSize, screenSize) != 0) return false;
        if (Double.compare(that.distanceToOtherDeviceGps, distanceToOtherDeviceGps) != 0)
            return false;
        if (isLocked != that.isLocked) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (manufacturer != null ? !manufacturer.equals(that.manufacturer) : that.manufacturer != null)
            return false;
        if (model != null ? !model.equals(that.model) : that.model != null) return false;
        if (location != null ? !location.equals(that.location) : that.location != null)
            return false;
        if (distanceToOtherDeviceBluetooth != that.distanceToOtherDeviceBluetooth) return false;
        return activePlugins != null ? activePlugins.equals(that.activePlugins) : that.activePlugins == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id != null ? id.hashCode() : 0;
        result = 31 * result + (manufacturer != null ? manufacturer.hashCode() : 0);
        result = 31 * result + (model != null ? model.hashCode() : 0);
        temp = Double.doubleToLongBits(screenSize);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (location != null ? location.hashCode() : 0);
        temp = Double.doubleToLongBits(distanceToOtherDeviceGps);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (distanceToOtherDeviceBluetooth != null ? distanceToOtherDeviceBluetooth.hashCode() : 0);
        result = 31 * result + (isLocked ? 1 : 0);
        result = 31 * result + (activePlugins != null ? activePlugins.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TrustedDevice{" +
                "id='" + id + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", model='" + model + '\'' +
                ", screenSize=" + screenSize +
                ", location=" + location +
                ", distanceToOtherDeviceGps=" + distanceToOtherDeviceGps +
                ", distanceToOtherDeviceBluetooth=" + distanceToOtherDeviceBluetooth +
                ", isLocked=" + isLocked +
                ", activePlugins=" + activePlugins +
                '}';
    }
}
