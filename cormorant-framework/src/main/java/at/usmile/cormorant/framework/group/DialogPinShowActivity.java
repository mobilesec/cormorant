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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.UUID;

import at.usmile.cormorant.framework.R;
import at.usmile.cormorant.framework.common.TypedServiceConnection;

import static at.usmile.cormorant.framework.group.GroupService.CHALLENGE_REQUEST_CANCELED;

public class DialogPinShowActivity extends AppCompatActivity {
    private final static String LOG_TAG = DialogPinShowActivity.class.getSimpleName();

    public static final String KEY_ID = "id";
    public static final String KEY_PIN = "pin";
    public static final int PIN_DEFAULT = -1;

    public static final String COMMAND_CLOSE = "at.usmile.cormorant.framework.group.close";
    public static final String COMMAND_PIN_FAILED = "at.usmile.cormorant.framework.group.pinFailed";


    private TypedServiceConnection<GroupService> groupService = new TypedServiceConnection<>();

    private TextView txtPin;
    private UUID id;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService(new Intent(this, GroupService.class), groupService, Context.BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(COMMAND_CLOSE);
        intentFilter.addAction(COMMAND_PIN_FAILED);
        registerReceiver(updateReceiver, intentFilter);

        setContentView(R.layout.activity_dialog_pin_show);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        txtPin = (TextView) findViewById(R.id.txtPin);
        txtPin.setText(String.valueOf(getIntent().getIntExtra(KEY_PIN, PIN_DEFAULT)));
        id = UUID.fromString(getIntent().getStringExtra(KEY_ID));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(updateReceiver);
        if (groupService.isBound()) unbindService(groupService);
        super.onDestroy();
    }

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()){
                case COMMAND_CLOSE:
                    Log.v(LOG_TAG, "Received close");
                    finish();
                    break;
                case COMMAND_PIN_FAILED:
                    Log.v(LOG_TAG, "Received pinFailed");
                    txtStatus.setText("Adding new device failed");
                    findViewById(R.id.buttonRetryPin).setEnabled(true);
                    break;
                default:
                    Log.w(LOG_TAG, "Action not supported: " + intent.getAction());
            }
        }
    };

    public void retryPin(View view) {
        int pin = groupService.get().sendChallengeRequest(new TrustedDevice(id));
        if(pin == CHALLENGE_REQUEST_CANCELED) finish();
        Log.d(LOG_TAG, "Retrying challenge with id: " + id);
        txtPin.setText(String.valueOf(pin));
        txtStatus.setText("");
        findViewById(R.id.buttonRetryPin).setEnabled(false);
    }

    public void cancelPin(View view) {
        //TODO Remove Challenge + Stop Pin Enter Dialog on new device
        finish();
    }

}
