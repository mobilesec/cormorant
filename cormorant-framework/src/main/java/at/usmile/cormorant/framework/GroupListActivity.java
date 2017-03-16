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
package at.usmile.cormorant.framework;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import at.usmile.cormorant.framework.group.GroupService;
import at.usmile.cormorant.framework.group.TrustedDevice;

public class GroupListActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, GroupService.class);
        bindService(intent, groupServiceConnection, Context.BIND_AUTO_CREATE);

        setContentView(R.layout.activity_group_list);
    }

    @Override
    protected void onDestroy() {
        if (groupServiceBound) unbindService(groupServiceConnection);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.group_menu, menu);
        return true;
    }

    private void createArrayAdapter(){
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

                        if(groupService.getSelf().equals(p)) view.setBackgroundColor(Color.GREEN);

                        ((TextView) view.findViewById(R.id.activity_group_list_text1)).setText(p.getId());
                        ((TextView) view.findViewById(R.id.activity_group_list_text2)).setText(p.getDevice());
                        ((ImageView) view.findViewById(R.id.activity_group_list_icon)).setImageResource(getIconByScreenSize(p.getScreenSize()));

                        return view;
                    }

                };
        // Bind to our new adapter.
        setListAdapter(adapter);
    }

    private int getIconByScreenSize(double screenSize) {
        if(screenSize >= 7) {
            return R.drawable.ic_computer_black_24dp;
        } if(screenSize < 3) {
            return R.drawable.ic_watch_black_24dp;
        } else {
            return R.drawable.ic_phone_android_black_24dp;
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
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            groupServiceBound = false;
        }
    };
}