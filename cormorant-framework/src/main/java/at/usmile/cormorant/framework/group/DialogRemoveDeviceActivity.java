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
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import at.usmile.cormorant.framework.R;
import at.usmile.cormorant.framework.common.TypedServiceConnection;

public class DialogRemoveDeviceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_remove_device);
        Intent intent = new Intent(this, GroupService.class);
        bindService(intent, groupService, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        if (groupService.isBound()) unbindService(groupService);
        super.onDestroy();
    }

    private TypedServiceConnection<GroupService> groupService = new TypedServiceConnection<GroupService>() {

        @Override
        public void onServiceConnected(GroupService service) {
            if(GroupListActivity.selectedDevice.equals(service.getSelf())) setTitle("Leave group?");
            else setTitle("Remove device?");
        }
    };

    public void ok(View view) {
        groupService.get().removeTrustedDevice(GroupListActivity.selectedDevice);
        GroupListActivity.selectedDevice = null;
        finish();
    }

    public void cancel(View view) {
        GroupListActivity.selectedDevice = null;
        finish();
    }

}
