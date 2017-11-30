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
package at.usmile.cormorant.framework.module;

import android.content.Context;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import at.usmile.cormorant.framework.plugin.PluginManager;

/**
 * Behavior is customizable by using different DecisionStrategy implementations.
 * Polls data from plugins based on a given pollingInterval from the used DecisionStrategy.
 * If a plugin has changed it will trigger the FusionModule.
 * The decision making process is started from the FusionModule after the data has been fused.
 */
public class DecisionModule implements PluginManager.PluginChangeListener {
    public static final String LOG_TAG = DecisionModule.class.getSimpleName();
    private DecisionStrategy decisionStrategy;
    private FusionModule fusionModule;
    private Timer timer;
    private int pollingInterval; //ms
    private Context context;

    public DecisionModule(Context context, DecisionStrategy decisionStrategy, FusionStrategy confidenceFusionStrategy,
                          FusionStrategy riskFusionStrategy) {
        this.context = context;
        this.decisionStrategy = decisionStrategy;
        this.fusionModule = new FusionModule(confidenceFusionStrategy, riskFusionStrategy);
        this.timer = new Timer();
        this.pollingInterval = decisionStrategy.getPollingInterval();
    }

    public void start() {
        Log.d(LOG_TAG, "Started with strategy: " + decisionStrategy.getClass());
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                PluginManager.getInstance().pollDataFromPlugins();
            }
        }, 0, pollingInterval);
    }

    public void stop() {
        timer.cancel();
        Log.d(LOG_TAG, "Stopped");
    }

    public void makeDecision(double confidenceFusionResult, double riskFusionResult) {
        decisionStrategy.processData(
                context,
                confidenceFusionResult,
                riskFusionResult);
    }

    @Override
    public void onPluginsChanged() {
        fusionModule.processFusion(DecisionModule.this);
    }
}
