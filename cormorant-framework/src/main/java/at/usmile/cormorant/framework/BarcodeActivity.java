package at.usmile.cormorant.framework;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;


public class BarcodeActivity extends AppCompatActivity {

    private final static String LOG_TAG = BarcodeActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_barcode);

        Intent messagingServiceIntent = new Intent(this, MessagingService.class);
        startService(messagingServiceIntent);
        bindService(messagingServiceIntent, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                MessagingService.MessagingServiceBinder binder = (MessagingService.MessagingServiceBinder) service;
                MessagingService messagingService = binder.getService();

                ImageView barcodeImageView = (ImageView) findViewById(R.id.barcodeImageView);
                barcodeImageView.setImageBitmap(encodeAsBitmap(messagingService.getDeviceID()));
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
            }
        }, Context.BIND_AUTO_CREATE);
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

}
