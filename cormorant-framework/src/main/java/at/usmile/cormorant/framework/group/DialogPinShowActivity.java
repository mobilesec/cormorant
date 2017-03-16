package at.usmile.cormorant.framework.group;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import at.usmile.cormorant.framework.R;

public class DialogPinShowActivity extends AppCompatActivity {
    private final static String LOG_TAG = DialogPinShowActivity.class.getSimpleName();

    public static final String KEY_JABBER_ID = "jabberId";
    public static final String KEY_PIN = "pin";
    public static final int PIN_DEFAULT = -1;

    public static final String COMMAND_CLOSE = "at.usmile.cormorant.framework.group.close";
    public static final String COMMAND_PIN_FAILED = "at.usmile.cormorant.framework.group.pinFailed";

    private TextView txtPin;
    private String jabberId;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, GroupService.class);
        bindService(intent, groupServiceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(COMMAND_CLOSE);
        intentFilter.addAction(COMMAND_PIN_FAILED);
        registerReceiver(updateReceiver, intentFilter);

        setContentView(R.layout.activity_dialog_pin_show);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        txtPin = (TextView) findViewById(R.id.txtPin);
        txtPin.setText(String.valueOf(getIntent().getIntExtra(KEY_PIN, PIN_DEFAULT)));
        jabberId = getIntent().getStringExtra(KEY_JABBER_ID);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(updateReceiver);
        if (groupServiceBound) unbindService(groupServiceConnection);
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
        Log.d(LOG_TAG, "Retrying challenge with jabberId: " + jabberId);
        txtPin.setText(String.valueOf(groupService.sendChallengeRequest(jabberId)));
        txtStatus.setText("");
        findViewById(R.id.buttonRetryPin).setEnabled(false);
    }

    public void cancelPin(View view) {
        //TODO Remove Challenge + Stop Pin Enter Dialog on new device
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
