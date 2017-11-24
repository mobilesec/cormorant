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

import java.util.Objects;
import java.util.UUID;

import at.usmile.cormorant.framework.location.bluetooth.DistanceHelper;

public class TrustedDevice {

    public static final int DEVICE_UNKNOWN_GPS_DISTANCE = -1;

    private String id;

    private String device;

    private double screenSize;

    private String jabberId;

    private UUID uuid;

    private Location location;

    //Distance in meters for GPS location
    private double distanceToOtherDeviceGps;

    //Fuzzy distance for bluetooth distance
    private DistanceHelper.DISTANCE distanceToOtherDeviceBluetooth;

    /*
    * Only to be used by the Group Service for group challenge and response.
    * */
    TrustedDevice(String jabberId) {
        this(null, null, 0, jabberId, null);
    }

    public TrustedDevice(String id, String device, double screenSize, String jabberId, UUID uuid) {
        this.id = id;
        this.device = device;
        this.screenSize = screenSize;
        this.jabberId = jabberId;
        this.uuid = uuid;
        this.distanceToOtherDeviceGps = DEVICE_UNKNOWN_GPS_DISTANCE;
        this.distanceToOtherDeviceBluetooth = DistanceHelper.DISTANCE.UNKNOWN;
    }

    public String getDevice() {
        return device;
    }

    public String getId() {
        return id;
    }

    public double getScreenSize() {
        return screenSize;
    }

    public String getJabberId() {
        return jabberId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Location getLocation() {
        return location;
    }

    public double getDistanceToOtherDeviceGps() {
        return distanceToOtherDeviceGps;
    }

    public DistanceHelper.DISTANCE getDistanceToOtherDeviceBluetooth() {
        return distanceToOtherDeviceBluetooth;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setDistanceToOtherDeviceGps(double distanceToOtherDeviceGps) {
        this.distanceToOtherDeviceGps = distanceToOtherDeviceGps;
    }

    public void setDistanceToOtherDeviceBluetooth(DistanceHelper.DISTANCE distanceToOtherDeviceBluetooth) {
        this.distanceToOtherDeviceBluetooth = distanceToOtherDeviceBluetooth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrustedDevice that = (TrustedDevice) o;
        return Double.compare(that.screenSize, screenSize) == 0 &&
                Objects.equals(id, that.id) &&
                Objects.equals(device, that.device) &&
                Objects.equals(jabberId, that.jabberId) &&
                Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, device, screenSize, jabberId, uuid);
    }

    @Override
    public String toString() {
        return "TrustedDevice{" +
                "id='" + id + '\'' +
                ", device='" + device + '\'' +
                ", screenSize=" + screenSize +
                ", jabberId='" + jabberId + '\'' +
                ", uuid=" + uuid +
                ", location=" + location +
                ", distanceToOtherDeviceGps=" + distanceToOtherDeviceGps +
                ", distanceToOtherDeviceBluetooth=" + distanceToOtherDeviceBluetooth +
                '}';
    }
}
