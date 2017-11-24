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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.annotation.DrawableRes;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.security.acl.Group;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import at.usmile.cormorant.framework.R;
import at.usmile.cormorant.framework.common.CommonUtils;
import at.usmile.cormorant.framework.common.TypedServiceConnection;
import at.usmile.cormorant.framework.location.SimpleLocation;

/**
 * Created by fhdwsse
 */
public class GroupMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final double MAP_PADDING_FACTOR = 0.15;
    private TypedServiceConnection<GroupService> groupService = new TypedServiceConnection<>();
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_map);

        bindService(new Intent(this, GroupService.class), groupService, Context.BIND_AUTO_CREATE);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onDestroy() {
        if (groupService.isBound()) unbindService(groupService);
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        if(groupService.isBound()) setupMap(addMarkerForDevices(groupService.get().getGroup()));
    }

    private List<Marker> addMarkerForDevices(List<TrustedDevice> devices) {
        List<Marker> createdMarkers = new LinkedList<>();
        devices.forEach(eachDevice -> {
            int iconByScreenSize = CommonUtils.getIconByScreenSize(eachDevice.getScreenSize(),
                    groupService.get().getSelf().equals(eachDevice));
            BitmapDescriptor markerIcon = convertVectorToBitmap(iconByScreenSize);
            SimpleLocation deviceLoc = eachDevice.getLocation();

            if(deviceLoc == null) return;

            Marker mapMarker = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(deviceLoc.getLatitude(), deviceLoc.getLongitude()))
                    .icon(markerIcon)
                    .anchor(0.5f, 0.5f)
                    .snippet((eachDevice.getDistanceToOtherDeviceGps() / 100) / 10d + "km")
                    .title(eachDevice.getDevice()));
            createdMarkers.add(mapMarker);
            mapMarker.showInfoWindow();
        });
        return createdMarkers;
    }

    private void setupMap(List<Marker> markers) {
        if(markers.isEmpty()) return;
        
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();

        int padding = (int) (getResources().getDisplayMetrics().widthPixels * MAP_PADDING_FACTOR);
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        googleMap.setOnMapLoadedCallback(() -> googleMap.animateCamera(cu));
    }

    private BitmapDescriptor convertVectorToBitmap(@DrawableRes int id) {
        Drawable vectorDrawable = ResourcesCompat.getDrawable(getResources(), id, null);
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

}
