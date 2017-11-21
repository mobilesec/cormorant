/**
 * Copyright 2016 - 2017
 * <p>
 * Daniel Hintze <daniel.hintze@fhdw.de>
 * Sebastian Scholz <sebastian.scholz@fhdw.de>
 * Rainhard D. Findling <rainhard.findling@fh-hagenberg.at>
 * Muhammad Muaaz <muhammad.muaaz@usmile.at>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import at.usmile.cormorant.framework.lock.DeviceLockCommand;
import at.usmile.cormorant.framework.messaging.MessagingService;

public class GroupListActivity extends AppCompatActivity implements GroupChangeListener {

    public static TrustedDevice deviceToRemove;

    private ListView listview;
    private TypedServiceConnection<MessagingService> messagingService = new TypedServiceConnection<>();
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
        bindService(new Intent(this, MessagingService.class), messagingService, Context.BIND_AUTO_CREATE);

        setContentView(R.layout.activity_group_list);

        listview = (ListView) findViewById(R.id.group_list_view);

        registerForContextMenu(listview);
    }

    @Override
    protected void onDestroy() {
        if (groupService.isBound()) unbindService(groupService);
        if (messagingService.isBound()) unbindService(messagingService);

        super.onDestroy();
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

    public void lockOrUnlockDevice(View view) {
        TrustedDevice deviceToLock = getDeviceFromGroupListView(view);

        messagingService.get().sendMessage(deviceToLock, new DeviceLockCommand());
    }

    public void removeDeviceAction(View view) {
        removeDevice(getDeviceFromGroupListView(view));
    }

    public void removeDevice(TrustedDevice device) {
        deviceToRemove = device;
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
                        R.id.activity_group_list_text1,
                        groupService.get().getGroup()) {

                    @Override
                    public View getView(int position, View contentView, ViewGroup viewGroup) {
                        View view = contentView;
                        if (view == null) {
                            view = getLayoutInflater().inflate(R.layout.activity_group_list_row, viewGroup, false);
                        }

                        TrustedDevice p = getItem(position);

                        if(groupService.get().getSelf().equals(p)){
                            ((TextView) view.findViewById(R.id.activity_group_list_text3)).setVisibility(View.GONE);
                            ((TextView) view.findViewById(R.id.activity_group_list_text4)).setVisibility(View.GONE);
                        }

                        ((TextView) view.findViewById(R.id.activity_group_list_text1)).setText(p.getId());
                        ((TextView) view.findViewById(R.id.activity_group_list_text2)).setText(p.getDevice());
                        ((TextView) view.findViewById(R.id.activity_group_list_text3)).setText("GPS distance: " + p.getDistanceToOtherDeviceGps() + "m");
                        ((TextView) view.findViewById(R.id.activity_group_list_text4)).setText("BT distance: " + p.getDistanceToOtherDeviceBluetooth());
                        ((ImageView) view.findViewById(R.id.activity_group_list_icon)).setImageResource(
                                CommonUtils.getIconByScreenSize(p.getScreenSize(), groupService.get().getSelf().equals(p)));

                        return view;
                    }
                };
        listview.setAdapter(adapter);
    }

    @Override
    public void groupChanged() {
        runOnUiThread(() -> ((ArrayAdapter) listview.getAdapter()).notifyDataSetChanged());
    }

    private TrustedDevice getDeviceFromGroupListView(View view) {
        int position = listview.getPositionForView(view);
        return (TrustedDevice) listview.getItemAtPosition(position);
    }
}