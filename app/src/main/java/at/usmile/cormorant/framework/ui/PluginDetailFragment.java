/**
 * Copyright 2016 - Daniel Hintze <daniel.hintze@fhdw.de>
 * 				 Sebastian Scholz <sebastian.scholz@fhdw.de>
 * 				 Rainhard D. Findling <rainhard.findling@fh-hagenberg.at>
 * 				 Muhammad Muaaz <muhammad.muaaz@usmile.at>
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
package at.usmile.cormorant.framework.ui;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;

import at.usmile.cormorant.api.CormorantConstants;
import at.usmile.cormorant.framework.plugin.PluginInfo;
import at.usmile.cormorant.framework.plugin.PluginManager;
import at.usmile.cormorant.framework.R;

public class PluginDetailFragment extends Fragment implements View.OnClickListener, PluginManager.OnChangeListener {
    private final static String ARG_PLUGIN_PACKAGE = "arg_plugin_package";

    private PluginDetailFragmentCallbacks callbacks;
    private PluginInfo currentApi;
    private TextView txtDescription;
    private TextView txtComponentName;
    private TextView txtAuthValue;
    private TextView txtStatus;
    private TextView txtType;
    private TextView txtAuthValueLabel;
    private Button buttonRequest;
    private Button buttonConfig;
    private TextView txtLastUpdate;


    public PluginDetailFragment() {
    }

    public static PluginDetailFragment newInstance(String pluginPackage) {
        Bundle args = new Bundle();
        args.putString(ARG_PLUGIN_PACKAGE, pluginPackage);
        PluginDetailFragment fragment = new PluginDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_plugin_detail, container, false);

        txtDescription = (TextView) rootView.findViewById(R.id.plugin_detail_description);
        txtComponentName = (TextView) rootView.findViewById(R.id.plugin_detail_component_name);
        txtAuthValue = (TextView) rootView.findViewById(R.id.plugin_detail_auth_value);
        txtLastUpdate = (TextView) rootView.findViewById(R.id.plugin_detail_last_update);
        txtStatus = (TextView) rootView.findViewById(R.id.plugin_detail_status);
        txtType = (TextView) rootView.findViewById(R.id.plugin_detail_type);
        txtAuthValueLabel = (TextView) rootView.findViewById(R.id.plugin_detail_auth_value_label);

        buttonRequest = (Button) rootView.findViewById(R.id.plugin_detail_request_authentication);
        buttonRequest.setOnClickListener(this);
        buttonConfig = (Button) rootView.findViewById(R.id.plugin_detail_show_configuration);
        buttonConfig.setOnClickListener(this);

        return rootView;
    }

    private void setDetails(){
        currentApi = PluginManager.getInstance().getPluginInfo(getArguments().getString(ARG_PLUGIN_PACKAGE));
        if(currentApi == null){
            Toast.makeText(getActivity(), "Plugin " + getActivity().getTitle() + " is no longer available", Toast.LENGTH_LONG).show();
            callbacks.onPluginRemoved();
            return;
        }

        getActivity().setTitle(currentApi.getTitle());

        if(currentApi.getExplicitAuthComponentName() == null) buttonRequest.setVisibility(View.GONE);
        if(currentApi.getConfigurationComponentName() == null) buttonConfig.setVisibility(View.GONE);

        txtDescription.setText(currentApi.getDescription());
        txtComponentName.setText(currentApi.getComponentName().getPackageName());

        if(currentApi.getLastUpdate() != null) {
            txtLastUpdate.setText(new SimpleDateFormat("dd-M-yyyy HH:mm:ss").format(currentApi.getLastUpdate().getTime()));
        }

        if(CormorantConstants.PLUGIN_TYPE.confidence.equals(currentApi.getPluginType())){
            txtAuthValueLabel.setText("Confidence");
            txtAuthValue.setText(String.format("%.2f", currentApi.getStatusDataConfidence().getConfidence()));
            txtStatus.setText(currentApi.getStatusDataConfidence().getStatus().toString());
            txtType.setText(CormorantConstants.PLUGIN_TYPE.confidence.toString().toUpperCase());
        }
        else {
            txtAuthValueLabel.setText("Risk");
            txtAuthValue.setText(String.format("%.2f", currentApi.getStatusDataRisk().getRisk()));
            txtStatus.setText(currentApi.getStatusDataRisk().getStatus().toString());
            txtType.setText(CormorantConstants.PLUGIN_TYPE.risk.toString().toUpperCase());
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        if(v.getId() == R.id.plugin_detail_request_authentication){
            intent.setComponent(currentApi.getExplicitAuthComponentName());
        }
        else if (v.getId() == R.id.plugin_detail_show_configuration){
            intent.setComponent(currentApi.getConfigurationComponentName());
        }
        startActivity(intent);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            callbacks = (PluginDetailFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement PluginListFragmentCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        setDetails();
        PluginManager.getInstance().addOnChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        PluginManager.getInstance().removeOnChangeListener(this);
    }

    @Override
    public void onPluginsChanged() {
        setDetails();
    }

    public interface PluginDetailFragmentCallbacks {
        void onPluginRemoved();
    }
}
