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
import android.view.View;
import android.widget.EditText;

import com.google.common.base.Preconditions;

import java.util.UUID;

import at.usmile.cormorant.framework.R;
import at.usmile.cormorant.framework.common.TypedServiceConnection;

public class DialogPinEnterActivity extends AppCompatActivity {

    public static final String KEY_SENDER_ID = "senderId";

    private UUID senderId;

    private TypedServiceConnection<GroupService> groupService = new TypedServiceConnection<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_pin_enter);

        String uuidString =  getIntent().getStringExtra(KEY_SENDER_ID);
        Preconditions.checkNotNull(uuidString);

        senderId = UUID.fromString(uuidString);

        bindService(new Intent(this, GroupService.class), groupService, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        if (groupService.isBound()) unbindService(groupService);
        super.onDestroy();
    }

    public void setPin(View view) {
        EditText editPin = (EditText) findViewById(R.id.editPin);
        groupService.get().respondToChallengeRequest(Integer.parseInt(editPin.getText().toString()), new TrustedDevice(senderId));
        finish();
    }

}
