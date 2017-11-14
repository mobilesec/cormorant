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
package at.usmile.gaitmodule;


import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Prefrences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private String DEVICEOWNER;
    private String SecurityLevel;
    private double threshold;

    /* (non-Javadoc)
     * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        getUserSharedPrefrences();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (key.equals("OWNER")) {

            DEVICEOWNER = sp.getString("OWNER", "NA").toUpperCase();
            EditTextPreference editTextPref = (EditTextPreference) findPreference("OWNER");
            editTextPref.setSummary(sp.getString("ONWER", sp.getString("OWNER", "").toUpperCase()));

        }

        if (key.equals("securityLevels")) {

            String securityLevelVal = sp.getString("securityLevels", "1");
            String[] securityLevels = getResources().getStringArray(R.array.securityLevlesSettings);
            int ss = Integer.parseInt(securityLevelVal);
            SecurityLevel = securityLevels[ss - 1];
        }

        if(key.equals("threshold")){
            threshold = Double.parseDouble(sp.getString("threshold","0.25"));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    // Example how to get preferences
    public void getUserSharedPrefrences() {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        DEVICEOWNER = sp.getString("OWNER", "NA").toUpperCase();
        EditTextPreference editTextPref = (EditTextPreference) findPreference("OWNER");
        editTextPref.setSummary(sp.getString("ONWER", sp.getString("OWNER", "").toUpperCase()));

        String securityLevelVal = sp.getString("securityLevels", "1");
        String[] securityLevels = getResources().getStringArray(R.array.securityLevlesSettings);
        int ss = Integer.parseInt(securityLevelVal);
        SecurityLevel = securityLevels[ss - 1];
        threshold = Double.parseDouble(sp.getString("threshold","0.25"));
    }
}
