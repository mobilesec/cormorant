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
package at.usmile.cormorant.framework.module;

import android.util.Log;

/**
 * Behavior is customizable through FusionStrategies.
 * Is triggered by the DecisionModule after plugin changes.
 * Starts the decision process of the DecisionModule after the plugin data has been fused.
 */
public class FusionModule {
    private FusionStrategy fusionStrategyConfidence;
    private FusionStrategy fusionStrategyRisk;

    public FusionModule(FusionStrategy fusionStrategyConfidence, FusionStrategy fusionStrategyRisk) {
        this.fusionStrategyConfidence = fusionStrategyConfidence;
        this.fusionStrategyRisk = fusionStrategyRisk;
    }

    public void processFusion(DecisionModule decisionModule) {
        double confidenceResult = fusionStrategyConfidence.fuseData();
        double riskResult = fusionStrategyRisk.fuseData();
        Log.v(FusionModule.class.getSimpleName(), "ConfidenceFusion result with " + fusionStrategyConfidence
                .getClass().getSimpleName() + ": " + confidenceResult);
        Log.v(FusionModule.class.getSimpleName(), "RiskFusion result with " + fusionStrategyRisk
                .getClass().getSimpleName() + ": " + riskResult);
        decisionModule.makeDecision(confidenceResult, riskResult);
    }
}
