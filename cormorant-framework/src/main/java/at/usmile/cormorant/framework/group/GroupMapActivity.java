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
            Location deviceLoc = eachDevice.getLocation();

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
