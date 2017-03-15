package at.usmile.cormorant.framework.group;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class DialogPinShow extends DialogFragment {
    public static final String KEY_PIN = "pin";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        int pin = getArguments().getInt(KEY_PIN);
        builder.setMessage(String.valueOf(pin))
            .setTitle("PIN");
        return builder.create();
    }

}
