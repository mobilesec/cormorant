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

import java.util.UUID;

public class TrustedDevice {

    private String id;

    private String device;

    private double screenSize;

    private String jabberId;

    private UUID uuid;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrustedDevice that = (TrustedDevice) o;

        if (Double.compare(that.screenSize, screenSize) != 0) return false;
        if (!id.equals(that.id)) return false;
        if (!device.equals(that.device)) return false;
        if (!uuid.equals(that.device)) return false;
        return jabberId.equals(that.jabberId);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id.hashCode();
        result = 31 * result + device.hashCode();
        temp = Double.doubleToLongBits(screenSize);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + jabberId.hashCode();
        result = 31 * result + uuid.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TrustedDevice{" +
                "id='" + id + '\'' +
                ", device='" + device + '\'' +
                ", screenSize=" + screenSize +
                ", jabberId='" + jabberId + '\'' +
                ", uuid=" + uuid +
                '}';
    }
}
