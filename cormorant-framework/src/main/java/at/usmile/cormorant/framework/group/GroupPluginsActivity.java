package at.usmile.cormorant.framework.group;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.LinkedList;

import at.usmile.cormorant.api.CormorantConstants;
import at.usmile.cormorant.api.model.StatusDataConfidence;
import at.usmile.cormorant.framework.R;

public class GroupPluginsActivity extends AppCompatActivity {
    private ListView listview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_plugins);

        listview = (ListView) findViewById(R.id.group_plugins_view);
        createArrayAdapter();
    }

    private void createArrayAdapter() {
        ArrayAdapter<PluginData> adapter =
                new ArrayAdapter<PluginData>(
                        this,
                        R.layout.listitem_plugins,
                        GroupListActivity.selectedDevice.getActivePlugins()) {

                    @Override
                    public View getView(int position, View contentView, ViewGroup viewGroup) {
                        View view = contentView;
                        if (view == null) {
                            view = getLayoutInflater().inflate(R.layout.listitem_plugins, viewGroup, false);
                        }

                        PluginData pluginData = getItem(position);
                        String pluginValue = "Value: UNKNOWN";
                        String pluginState = "State: UNKNOWN";

                        if(CormorantConstants.PLUGIN_TYPE.confidence.equals(pluginData.getPluginType())){
                            pluginValue = "Confidence: " + String.format("%.2f", pluginData.getStatusDataConfidence().getConfidence());
                            pluginState = "State: " + pluginData.getStatusDataConfidence().getStatus();
                        }
                        else if(CormorantConstants.PLUGIN_TYPE.confidence.equals(pluginData.getPluginType())) {
                            pluginValue = "Risk: " + String.format("%.2f", pluginData.getStatusDataRisk().getRisk());
                            pluginState = "State: " + pluginData.getStatusDataRisk().getStatus();
                        }

                        view.findViewById(R.id.icon).setVisibility(View.GONE);
                        ((TextView) view.findViewById(R.id.rowTitle)).setText(pluginData.getTitle());
                        ((TextView) view.findViewById(R.id.rowDescription)).setText(pluginData.getDescription());
                        ((TextView) view.findViewById(R.id.rowValue)).setText(pluginValue);
                        ((TextView) view.findViewById(R.id.rowLastUpdated)).setText("Last updated: " +
                                (pluginData.getLastUpdate() != null ? pluginData.getLastUpdate().toString()
                                        : "UNKNOWN")
                        );
                        ((TextView) view.findViewById(R.id.rowState)).setText(pluginValue);
                        return view;
                    }
                };
        listview.setAdapter(adapter);
    }
}
