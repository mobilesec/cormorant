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
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import at.usmile.cormorant.framework.plugin.PluginManager;
import at.usmile.cormorant.framework.R;

public class PluginListFragment extends Fragment implements PluginManager.OnChangeListener {
    private ListView listView;
    private PluginListFragmentCallbacks callbacks;

    public PluginListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_plugin_list, container, false);
        listView = (ListView) rootView.findViewById(R.id.listPlugins);
        initList(rootView);
        getActivity().setTitle(getString(R.string.app_name));
        return rootView;
    }

    private void initList(View rootView) {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                callbacks.onPluginListItemSelected(position);
            }
        });

        listView.setAdapter(new PluginListAdapter(rootView.getContext(), PluginManager.getInstance().getPluginListReadOnly()));
    }

    public interface PluginListFragmentCallbacks {
        void onPluginListItemSelected(int position);
    }

    @Override
    public void onPluginsChanged() {
        listView.invalidateViews();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        PluginManager.getInstance().addOnChangeListener(this);
        try {
            callbacks = (PluginListFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement PluginListFragmentCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        PluginManager.getInstance().removeOnChangeListener(this);
        callbacks = null;
    }
}
