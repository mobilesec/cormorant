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

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;

import java.util.List;

import at.usmile.cormorant.api.CormorantConstants;
import at.usmile.cormorant.framework.common.PermissionHelper;
import at.usmile.cormorant.framework.group.GroupListActivity;
import at.usmile.cormorant.framework.plugin.PluginManager;
import at.usmile.cormorant.framework.ui.PluginDetailFragment;
import at.usmile.cormorant.framework.ui.PluginListFragment;

public class MainActivity extends AppCompatActivity implements PluginListFragment.PluginListFragmentCallbacks, PluginDetailFragment.PluginDetailFragmentCallbacks {
    private final static String LOG_TAG = MainActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionHelper.checkAndGetPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION);

        startService(new Intent(this, AuthenticationFrameworkService.class));
        showMainFragment();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                     System.out.print("CORMORANT onConnected");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        System.out.print("CORMORANT onConnectionSuspended");

                    }
                })
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        System.out.print("CORMORANT onConnectionFailed");
                    }
                })
                .build();
    }

    private void switchFragment(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .addToBackStack(null)
                .replace(R.id.container, fragment, tag)
                .commit();
    }

    private void showMainFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, new PluginListFragment())
                .commit();
    }

    private void removeAllPlugins() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                List<PackageInfo> installedPackages = getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA);
                for (PackageInfo eachInstalledPackage : installedPackages) {
                    String packageName = eachInstalledPackage.packageName;
                    Bundle metaData = eachInstalledPackage.applicationInfo.metaData;
                    if (metaData == null) continue;
                    String startupServiceName = metaData.getString(CormorantConstants.META_STARTUP_SERVICE);
                    if (startupServiceName == null) continue;
                    Log.d(LOG_TAG, "Removing " + packageName);
                    removePackage(packageName);
                }
                return null;
            }
        }.execute();
    }

    private void removePackage(String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
    }

    @Override
    public void onPluginListItemSelected(int position) {
        String pluginPackage = PluginManager.getInstance().getPluginListReadOnly().get(position).getComponentName().getPackageName();
        switchFragment(PluginDetailFragment.newInstance(pluginPackage), PluginDetailFragment.class.getSimpleName());
    }

    @Override
    public void onPluginRemoved() {
        getFragmentManager().popBackStack();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuTrustedDevices:
                startActivity(new Intent(this, GroupListActivity.class));
                return true;
            case R.id.menuRemovePlugins:
                removeAllPlugins();
                return true;
            case R.id.menuRemoveFramework:
                removePackage("at.usmile.cormorant.framework");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
