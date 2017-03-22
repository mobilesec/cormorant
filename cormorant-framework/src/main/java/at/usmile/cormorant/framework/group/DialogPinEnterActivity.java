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
import android.widget.EditText;

import at.usmile.cormorant.framework.R;

public class DialogPinEnterActivity extends AppCompatActivity {
    public static final String KEY_SENDER_JABBER_ID = "senderJabberId";
    private String senderJabberId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_pin_enter);
        senderJabberId = getIntent().getStringExtra(KEY_SENDER_JABBER_ID);

        Intent intent = new Intent(this, GroupService.class);
        bindService(intent, groupServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        if (groupServiceBound) unbindService(groupServiceConnection);
        super.onDestroy();
    }

    public void setPin(View view) {
        EditText editPin = (EditText) findViewById(R.id.editPin);
        groupService.respondToChallengeRequest(Integer.parseInt(editPin.getText().toString()), senderJabberId);
        finish();
    }


    private GroupService groupService;
    private boolean groupServiceBound = false;

    private ServiceConnection groupServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            GroupService.GroupServiceBinder binder = (GroupService.GroupServiceBinder) service;
            groupService = binder.getService();
            groupServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            groupServiceBound = false;
        }
    };
}
