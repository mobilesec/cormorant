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
package at.usmile.gaitmodule.recievers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import at.usmile.gaitmodule.services.StepDetectorService;
//import at.usmile.gait_authentication.unused.GaitAuthenticationService;

public class ScreenOffBroadCastReciever extends BroadcastReceiver {

    private boolean screenOff;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {


            screenOff = true;
            Log.i("BroadCast", "" + screenOff);
            Intent i = new Intent(context, StepDetectorService.class);
            i.putExtra("screen_state", screenOff);
            context.startService(i);

        }
        /*	if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
				screenOff = false;
				Log.i("BroadCast", ""+screenOff);
								Intent i = new Intent(context, GaitAuthenticationService2.class);
				i.putExtra("screen_state", screenOff);
				context.startService(i);
			}*/
    }

}



