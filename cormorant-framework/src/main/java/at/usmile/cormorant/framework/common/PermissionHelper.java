package at.usmile.cormorant.framework.common;

import android.app.Activity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.LinkedList;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by fhdwsse
 */

public class PermissionHelper {

    public static void checkAndGetPermissions(Activity activity, String... permissions) {
        List<String> permissionsToAsk = new LinkedList<>();
        for(String eachPermission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, eachPermission) != PERMISSION_GRANTED) {
                permissionsToAsk.add(eachPermission);
            }
        }
        if(permissionsToAsk.isEmpty()) return;
        //TODO React to user feedback and don't pretend he will always click yes.
        ActivityCompat.requestPermissions(activity, permissionsToAsk.toArray(new String[]{}),1);
    }
}
