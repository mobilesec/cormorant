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
package at.usmile.cormorant.framework.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import at.usmile.cormorant.api.CormorantConstants;
import at.usmile.cormorant.framework.plugin.PluginInfo;
import at.usmile.cormorant.framework.R;

public class PluginListAdapter extends ArrayAdapter<PluginInfo> {
    private final Context context;
    private List<PluginInfo> plugins;

    public PluginListAdapter(Context context, List<PluginInfo> values) {
        super(context, R.layout.listitem_plugins, values);
        this.context = context;
        this.plugins = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.listitem_plugins, parent, false);

        TextView rowTitle = (TextView) rowView.findViewById(R.id.rowTitle);
        TextView rowDesc = (TextView) rowView.findViewById(R.id.rowDescription);
        TextView rowValue = (TextView) rowView.findViewById(R.id.rowValue);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

        PluginInfo pluginInfo = plugins.get(position);

        rowTitle.setText(pluginInfo.getTitle());
        rowDesc.setText(pluginInfo.getDescription());
        if(CormorantConstants.PLUGIN_TYPE.confidence.equals(pluginInfo.getPluginType())){
            rowValue.setText("Confidence: " + String.format("%.2f", pluginInfo.getStatusDataConfidence().getConfidence()));
        }
        else {
            rowValue.setText("Risk: " + String.format("%.2f", pluginInfo.getStatusDataRisk().getRisk()));
        }

        imageView.setImageDrawable(pluginInfo.getIcon());
        return rowView;
    }
}
