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
package at.usmile.cormorant.framework.module.strategies;

import at.usmile.cormorant.api.CormorantConstants;
import at.usmile.cormorant.framework.module.FusionStrategy;
import at.usmile.cormorant.framework.plugin.PluginInfo;
import at.usmile.cormorant.framework.plugin.PluginManager;

public class FusionStrategyRiskDefault implements FusionStrategy {

    @Override
    public double fuseData() {
        double sum = 0;
        int count = 0;
        for (PluginInfo eachApi : PluginManager.getInstance().getPluginListReadOnly()) {
            if(CormorantConstants.PLUGIN_TYPE.risk.equals(eachApi.getPluginType())){
                sum += eachApi.getStatusDataRisk().getRisk();
                count++;
            }
        }
        return sum / count;
    }
}
