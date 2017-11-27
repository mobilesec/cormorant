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
package fhdw.locationriskplugin;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import at.usmile.cormorant.api.AbstractRiskService;
import at.usmile.cormorant.api.PermissionUtil;
import at.usmile.cormorant.api.model.StatusDataRisk;

public class LocationRiskService extends AbstractRiskService implements LocationListener {
    private LocationManager locationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    protected void onDataUpdateRequest() {
        updateMacroLocation();
    }

    private void updateMacroLocation() {
        try {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                Geocoder gcd = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = gcd.getFromLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), 1);

                if (addresses.size() > 0) {
                    Address currentAddress = addresses.get(0);
                    Double macroRiskLevel = MacroLocationRiskLevel.getRiskLevelForCountry(currentAddress.getCountryCode());

                    if (macroRiskLevel != null) {
                        publishRiskUpdate(new StatusDataRisk()
                                .status(StatusDataRisk.Status.OPERATIONAL)
                                .risk(macroRiskLevel)
                                .info(renderAddress(currentAddress)));
                    } else {
                        // We do not have macro risk information:
                        publishRiskUpdate(new StatusDataRisk()
                                .status(StatusDataRisk.Status.UNKNOWN)
                                .risk(null));
                    }

                }
            } else {
                PermissionUtil.requestPermissions(getApplicationContext(),
                        new ActivityCompat.OnRequestPermissionsResultCallback() {
                            @Override
                            public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
                                Log.d("saveCurrentCells", "onRequestPermissionsResult");
                            }
                        },
                        1,
                        R.mipmap.ic_launcher, android.Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String renderAddress(Address address) {
        ArrayList<String> addressFragments = new ArrayList<String>();

        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
            addressFragments.add(address.getAddressLine(i));
        }

        return TextUtils.join(System.getProperty("line.separator"), addressFragments);
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }
}
