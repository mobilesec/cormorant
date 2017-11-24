package at.usmile.cormorant.framework.location;

import android.location.Location;

/**
 * Created by fhdwsse
 */

public class SimpleLocation {
    double latitude;
    double longitude;

    public SimpleLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public SimpleLocation(Location location) {
        this(location.getLatitude(), location.getLongitude());
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

}
