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
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import at.usmile.cormorant.framework.R;
import at.usmile.cormorant.framework.common.CommonUtils;
import at.usmile.cormorant.framework.common.TypedServiceConnection;
import at.usmile.cormorant.framework.location.bluetooth.DistanceHelper;
import at.usmile.cormorant.framework.lock.DeviceLockCommand;
import at.usmile.cormorant.framework.messaging.SignalMessagingService;

public class GroupListActivity extends AppCompatActivity implements GroupChangeListener {

    public static TrustedDevice selectedDevice;

    private ListView listview;
    private TypedServiceConnection<SignalMessagingService> messagingService = new TypedServiceConnection<>();
    private TypedServiceConnection<GroupService> groupService = new TypedServiceConnection<GroupService>() {

        @Override
        public void onServiceConnected(GroupService service) {
            createArrayAdapter();
            service.addGroupChangeListener(GroupListActivity.this);
        }

        @Override
        public void onServiceDisconnected(GroupService service) {
            service.removeGroupChangeListener(GroupListActivity.this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bindService(new Intent(this, GroupService.class), groupService, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, SignalMessagingService.class), messagingService, Context.BIND_AUTO_CREATE);

        setContentView(R.layout.activity_group_list);

        listview = (ListView) findViewById(R.id.group_list_view);

        registerForContextMenu(listview);
        setOnClickForItems();
    }

    @Override
    protected void onDestroy() {
        if (groupService.isBound()) unbindService(groupService);
        if (messagingService.isBound()) unbindService(messagingService);

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        groupChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.group_menu, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.group_list_view) {
            ListView lv = (ListView) v;
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
            TrustedDevice device = (TrustedDevice) lv.getItemAtPosition(acmi.position);


            if (groupService.get().getSelf().equals(device)) {
                menu.add(0, 0, 0, "Leave group");
            } else {
                menu.add(0, 0, 0, "Remove device");
            }
            menu.add(0, 1, 1, "Lock device");
            menu.add(0, 2, 2, "Unlock device");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        TrustedDevice device = (TrustedDevice) listview.getItemAtPosition(acmi.position);

        switch (item.getItemId()) {
            case 0:
                removeDevice(device);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuAddDeviceToGroup:
                startActivity(new Intent(this, BarcodeActivity.class));
                return true;
            case R.id.menuShowMap:
                startActivity(new Intent(this, GroupMapActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setOnClickForItems() {
        listview.setOnItemClickListener((parent, view, position, id) -> {
            selectedDevice = (TrustedDevice) parent.getItemAtPosition(position);
            Intent intent = new Intent(GroupListActivity.this, GroupPluginsActivity.class);
            startActivity(intent);
        });
    }

    public void lockOrUnlockDevice(View view) {
        TrustedDevice deviceToLock = getDeviceFromGroupListView(view);

        messagingService.get().sendMessage(deviceToLock, new DeviceLockCommand());
    }

    public void removeDeviceAction(View view) {
        removeDevice(getDeviceFromGroupListView(view));
    }

    public void removeDevice(TrustedDevice device) {
        selectedDevice = device;
        showRemoveDeviceDialog();
    }

    public void showRemoveDeviceDialog() {
        Intent intent = new Intent(this, DialogRemoveDeviceActivity.class);
        startActivity(intent);
    }

    private void createArrayAdapter() {
        ArrayAdapter<TrustedDevice> adapter =
                new ArrayAdapter<TrustedDevice>(
                        this,
                        R.layout.activity_group_list_row,
                        groupService.get().getGroup()) {

                    @Override
                    public View getView(int position, View contentView, ViewGroup viewGroup) {
                        View view = contentView;
                        if (view == null) {
                            view = getLayoutInflater().inflate(R.layout.activity_group_list_row, viewGroup, false);
                        }

                        TrustedDevice trustedDevice = getItem(position);
                        String gpsString = "GPS: Unknown";
                        String gpsAddress = "Address: UNKNOWN";
                        double gpsDistance = TrustedDevice.DEVICE_UNKNOWN_GPS_DISTANCE;

                        if (trustedDevice.equals(groupService.get().getSelf())) {
                            if (trustedDevice.getLocation() != null) {
                                gpsString = String.format("GPS: %s, %s", trustedDevice.getLocation().getLatitude(),
                                        trustedDevice.getLocation().getLongitude());
                            }
                        } else {
                            gpsDistance = Math.round(trustedDevice.getDistanceToOtherDeviceGps() / 100) / 10d;
                            gpsString = "GPS distance: " + gpsDistance + "km";
                        }

                        if (trustedDevice.getLocation() != null)
                            gpsAddress = trustedDevice.getLocation().getAddress();

                        ((TextView) view.findViewById(R.id.activity_group_list_text1)).setText(trustedDevice.getId().toString());
                        ((TextView) view.findViewById(R.id.activity_group_list_text2)).setText(trustedDevice.getManufacturer() + " " + trustedDevice.getModel());
                        ((TextView) view.findViewById(R.id.activity_group_list_text3)).setText(gpsString);
                        ((TextView) view.findViewById(R.id.activity_group_list_text4)).setText("BT distance: " + trustedDevice.getDistanceToOtherDeviceBluetooth());
                        ((TextView) view.findViewById(R.id.activity_group_list_text5)).setText("Combined distance: " + getDiscreteValue(trustedDevice, gpsDistance));
                        ((TextView) view.findViewById(R.id.activity_group_list_text6)).setText(gpsAddress);
                        ((ImageView) view.findViewById(R.id.activity_group_list_icon)).setImageResource(
                                CommonUtils.getIconByScreenSize(trustedDevice.getScreenSize(), groupService.get().getSelf().equals(trustedDevice)));

                        ((ImageView) view.findViewById(R.id.activity_group_list_lock_icon)).setImageResource(
                                trustedDevice.isLocked() ? R.drawable.ic_lock_black_24dp : R.drawable.ic_lock_open_black_24dp
                        );

                        if (groupService.get().getSelf().equals(trustedDevice)) {
                            ((TextView) view.findViewById(R.id.activity_group_list_text4)).setVisibility(View.GONE);
                            ((TextView) view.findViewById(R.id.activity_group_list_text5)).setVisibility(View.GONE);
                        }
                        return view;
                    }
                };
        listview.setAdapter(adapter);
    }

    private String getDiscreteValue(TrustedDevice trustedDevice, double gpsDistance) {
        DistanceHelper.DISTANCE bleDistance = trustedDevice.getDistanceToOtherDeviceBluetooth();
        String discreteValue = "UNKNOWN";
        if (bleDistance != DistanceHelper.DISTANCE.UNKNOWN) discreteValue = bleDistance.name();
        else if (gpsDistance >= 0 && gpsDistance < 0.1) {
            discreteValue = "< 100 m";
        } else if (gpsDistance >= 0.1 && gpsDistance < 5) {
            discreteValue = "< 5 km";
        } else if (gpsDistance > 5) {
            discreteValue = "> 5 km";
        }
        return discreteValue;
    }

    @Override
    public void groupChanged() {
        if (groupService.isBound()) runOnUiThread(() -> createArrayAdapter());
    }

    private TrustedDevice getDeviceFromGroupListView(View view) {
        int position = listview.getPositionForView(view);
        return (TrustedDevice) listview.getItemAtPosition(position);
    }
}