package at.usmile.cormorant.framework.common;

import at.usmile.cormorant.framework.R;

/**
 * Created by fhdwsse
 */

public class CommonUtils {
    public static int getIconByScreenSize(double screenSize, boolean blue) {
        if (screenSize >= 7) {
            return blue ? R.drawable.ic_computer_blue_24dp : R.drawable.ic_computer_black_24dp;
        }
        if (screenSize < 3) {
            return blue ? R.drawable.ic_watch_blue_24dp : R.drawable.ic_watch_black_24dp;
        } else {
            return blue ? R.drawable.ic_phone_android_blue_24dp : R.drawable.ic_phone_android_black_24dp;
        }
    }

}
