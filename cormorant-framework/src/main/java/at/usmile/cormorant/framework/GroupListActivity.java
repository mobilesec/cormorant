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
package at.usmile.cormorant.framework;

import android.app.ListActivity;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;

import at.usmile.cormorant.framework.group.TrustedDevice;

public class GroupListActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_group_list);

        ArrayAdapter<TrustedDevice> adapter =
                new ArrayAdapter<TrustedDevice>(
                        this,
                        R.layout.activity_group_list_row,
                        R.id.activity_group_list_text1,
                        Arrays.asList(
                                new TrustedDevice("A", android.os.Build.DEVICE, 2, ""),
                                new TrustedDevice(Build.MANUFACTURER, Build.MODEL, 5, ""),
                                new TrustedDevice(Build.MANUFACTURER, Build.MODEL, 10, ""))) {


                    @Override
                    public View getView(int position, View contentView, ViewGroup viewGroup) {
                        View view = contentView;
                        if (view == null) {
                            view = getLayoutInflater().inflate(R.layout.activity_group_list_row, viewGroup, false);
                        }

                        TrustedDevice p = getItem(position);

                        ((TextView) view.findViewById(R.id.activity_group_list_text1)).setText(p.getId());
                        ((TextView) view.findViewById(R.id.activity_group_list_text2)).setText(p.getDevice());
                        ((ImageView) view.findViewById(R.id.activity_group_list_icon)).setImageResource(getIconByScreenSize(p.getScreenSize()));

                        return view;
                    }

                };


        // Bind to our new adapter.
        setListAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.group_menu, menu);
        return true;
    }

    private int getIconByScreenSize(double screenSize) {
        if(screenSize >= 7) {
            return R.drawable.ic_computer_black_24dp;
        } if(screenSize < 3) {
            return R.drawable.ic_watch_black_24dp;
        } else {
            return R.drawable.ic_phone_android_black_24dp;
        }
    }

    private double getScreenSize() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        double wi = (double) width / (double) dm.xdpi;
        double hi = (double) height / (double) dm.ydpi;
        double x = Math.pow(wi, 2);
        double y = Math.pow(hi, 2);
        double screenInches = Math.sqrt(x + y);

        return screenInches;
    }
}