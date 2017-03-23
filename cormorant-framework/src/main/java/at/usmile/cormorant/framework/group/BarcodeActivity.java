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
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;

import at.usmile.cormorant.framework.R;
import at.usmile.cormorant.framework.common.TypedServiceConnection;
import at.usmile.cormorant.framework.messaging.MessagingService;

import static at.usmile.cormorant.framework.group.GroupService.CHALLENGE_REQUEST_CANCELED;


public class BarcodeActivity extends AppCompatActivity {

    private final static String LOG_TAG = BarcodeActivity.class.getSimpleName();

    private TypedServiceConnection<GroupService> groupService = new TypedServiceConnection<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_barcode);

        Intent intent = new Intent(this, GroupService.class);
        bindService(intent, groupService, Context.BIND_AUTO_CREATE);

        Intent messagingServiceIntent = new Intent(this, MessagingService.class);


        bindService(messagingServiceIntent, new TypedServiceConnection<MessagingService>() {
            public void onServiceConnected(MessagingService service) {
                ImageView barcodeImageView = (ImageView) findViewById(R.id.barcodeImageView);
                barcodeImageView.setImageBitmap(encodeAsBitmap(service.getDeviceID()));
                unbindService(this);
            }

        }, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        if (groupService.isBound()) unbindService(groupService);
        super.onDestroy();
    }

    private Bitmap encodeAsBitmap(String str) {
        try {
            com.google.zxing.Writer writer = new QRCodeWriter();

            BitMatrix bm = writer.encode(str, BarcodeFormat.QR_CODE, 350, 350);
            Bitmap mBitmap = Bitmap.createBitmap(350, 350, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < 350; i++) {
                for (int j = 0; j < 350; j++) {
                    mBitmap.setPixel(i, j, bm.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }

            return mBitmap;
        } catch (WriterException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

    public void addDevice(View view) {
        new IntentIntegrator(this).initiateScan();
    }

    public void showPinDialog(int pin, String jabberId) {
        Intent intent = new Intent(this, DialogPinShowActivity.class);
        intent.putExtra(DialogPinShowActivity.KEY_PIN, pin);
        intent.putExtra(DialogPinShowActivity.KEY_JABBER_ID, jabberId);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String content = result.getContents();
            if (content == null) {
                Toast.makeText(this, "QR Scan cancelled", Toast.LENGTH_LONG).show();
                //FIXME only for developer Nexus 7 debugging
                String nexus7JabberId = "cormorant-c50b4b30-8ac5-42a0-814e-e22121050288@0nl1ne.cc";
                int pin = groupService.get().sendChallengeRequest(new TrustedDevice(nexus7JabberId));
                if (pin != CHALLENGE_REQUEST_CANCELED) showPinDialog(pin, nexus7JabberId);
            } else {
                int pin = groupService.get().sendChallengeRequest(new TrustedDevice(content));
                if (pin != CHALLENGE_REQUEST_CANCELED) showPinDialog(pin, content);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
