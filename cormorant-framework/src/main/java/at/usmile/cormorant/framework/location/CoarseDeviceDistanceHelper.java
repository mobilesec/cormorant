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
package at.usmile.cormorant.framework.location;

import android.Manifest;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import at.usmile.cormorant.api.PermissionUtil;
import at.usmile.cormorant.framework.common.PermissionHelper;
import at.usmile.cormorant.framework.group.TrustedDevice;

import static at.usmile.cormorant.framework.group.TrustedDevice.DEVICE_UNKNOWN_GPS_DISTANCE;

/**
 * Created by fhdwsse
 */

public class CoarseDeviceDistanceHelper {
    public static final int GPS_UPDATE_INTERVAL = 1 * 60 * 1000; //1 minute between each gps pull
    private final static String LOG_TAG = CoarseDeviceDistanceHelper.class.getSimpleName();

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Context context;
    private List<CoarseDistanceListener> coarseDistanceListeners = new LinkedList<>();

    public CoarseDeviceDistanceHelper(Context context) {
        this.context = context;
        createLocationRequest();
        createLocationCallback();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public CoarseDeviceDistanceHelper(Context context, CoarseDistanceListener coarseDistanceListener) {
        this(context);
        addCoarseDistanceListener(coarseDistanceListener);
    }

    public void calculateDistances(List<TrustedDevice> deviceGroup, TrustedDevice self) {
        deviceGroup.forEach(eachDevice -> {
            eachDevice.setDistanceToOtherDeviceGps(calculateDeviceDistance(self, eachDevice));
        });
    }

    public void subscribeToLocationUpdates() {
        try {
            if (PermissionHelper.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            }
            else {
                Log.w(LOG_TAG, "Location permission not granted. GPS data won't be available");
            }
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Location permission not granted.", e);
        }
    }

    public void unsubscribeFromLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void getCurrentLocation() {
        try {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(
                    location -> Log.d(LOG_TAG, "Current location: " + location))
                .addOnFailureListener
                    (error -> Log.w(LOG_TAG, "Current location could not be retrieved: " + error));
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Location permission not granted.", e);
        }
    }

    //TODO set intervall to 1 minute
    private void createLocationRequest(){
        locationRequest = new LocationRequest();
        locationRequest.setInterval(GPS_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(GPS_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                Log.v(LOG_TAG, "Location updated: " + location);
                coarseDistanceListeners.forEach(eachListener -> eachListener.onLocationChanged(location));
            }
        };
    }

    //Calculates distance between two devices in meters
    public double calculateDeviceDistance(TrustedDevice deviceSelf, TrustedDevice deviceOther) {
        SimpleLocation locSelf = deviceSelf.getLocation();
        SimpleLocation locOther = deviceOther.getLocation();
        if(locSelf == null || locOther == null) {
            Log.v(LOG_TAG, String.format("Distance to %s could not be calculated since location for one of the devices is not available.", deviceOther));
            return DEVICE_UNKNOWN_GPS_DISTANCE;
        }
        float[] distanceInMeter = new float[1];
        Location.distanceBetween(locSelf.getLatitude(), locSelf.getLongitude(),
                locOther.getLatitude(), locOther.getLongitude(), distanceInMeter);
        return Math.round(distanceInMeter[0]);
    }

    public interface CoarseDistanceListener {
        void onLocationChanged(Location location);
    }

    public void addCoarseDistanceListener(CoarseDistanceListener coarseDistanceListener) {
        this.coarseDistanceListeners.add(coarseDistanceListener);
    }

    public void removeCoarseDistanceListener(CoarseDistanceListener coarseDistanceListener) {
        this.coarseDistanceListeners.remove(coarseDistanceListener);
    }

}
