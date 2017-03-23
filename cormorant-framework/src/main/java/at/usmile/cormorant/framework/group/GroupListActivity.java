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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import at.usmile.cormorant.framework.R;

public class GroupListActivity extends AppCompatActivity implements GroupChangeListener {
    public static TrustedDevice deviceToRemove;

    private ListView listview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, GroupService.class);
        bindService(intent, groupServiceConnection, Context.BIND_AUTO_CREATE);

        setContentView(R.layout.activity_group_list);

        listview = (ListView) findViewById(R.id.group_list_view);
    }

    @Override
    protected void onDestroy() {
        if (groupServiceBound) unbindService(groupServiceConnection);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuTrustedDevices:
                startActivity(new Intent(this, GroupListActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void removeDevice(View view) {
        int position = listview.getPositionForView(view);
        deviceToRemove = (TrustedDevice) listview.getItemAtPosition(position);
        showRemoveDeviceDialog();
    }

    public void showRemoveDeviceDialog() {
        Intent intent = new Intent(this, DialogRemoveDeviceActivity.class);
        startActivity(intent);
    }

    //TODO Better icon for device removal
    private void createArrayAdapter() {
        ArrayAdapter<TrustedDevice> adapter =
                new ArrayAdapter<TrustedDevice>(
                        this,
                        R.layout.activity_group_list_row,
                        R.id.activity_group_list_text1,
                        groupService.getGroup()) {

                    @Override
                    public View getView(int position, View contentView, ViewGroup viewGroup) {
                        View view = contentView;
                        if (view == null) {
                            view = getLayoutInflater().inflate(R.layout.activity_group_list_row, viewGroup, false);
                        }

                        TrustedDevice p = getItem(position);


                        ((TextView) view.findViewById(R.id.activity_group_list_text1)).setText(p.getId());
                        ((TextView) view.findViewById(R.id.activity_group_list_text2)).setText(p.getDevice());
                        ((ImageView) view.findViewById(R.id.activity_group_list_icon)).setImageResource(getIconByScreenSize(p.getScreenSize(), groupService.getSelf().equals(p)));
                        //FIXME why is explicit setImageResource needed instead of xml defined icon?
                        //((ImageView) view.findViewById(R.id.activity_group_list_remove_icon)).setImageResource(R.drawable.ic_line_weight_black_24dp);


                        return view;
                    }

                };
        // Bind to our new adapter.
        listview.setAdapter(adapter);
    }

    @Override
    public void groupChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ArrayAdapter) listview.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    private int getIconByScreenSize(double screenSize, boolean blue) {
        if (screenSize >= 7) {
            return blue ? R.drawable.ic_computer_blue_24dp : R.drawable.ic_computer_black_24dp;
        }
        if (screenSize < 3) {
            return blue ? R.drawable.ic_watch_blue_24dp : R.drawable.ic_watch_black_24dp;
        } else {
            return blue ? R.drawable.ic_phone_android_blue_24dp : R.drawable.ic_phone_android_black_24dp;
        }
    }

    private GroupService groupService;
    private boolean groupServiceBound = false;

    private ServiceConnection groupServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            GroupService.GroupServiceBinder binder = (GroupService.GroupServiceBinder) service;
            groupService = binder.getService();
            groupServiceBound = true;
            createArrayAdapter();
            groupService.addGroupChangeListener(GroupListActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            groupServiceBound = false;
            groupService.removeGroupChangeListener(GroupListActivity.this);
        }
    };
}