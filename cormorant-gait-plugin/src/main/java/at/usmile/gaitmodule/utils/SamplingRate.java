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
package at.usmile.gaitmodule.utils;

import android.util.Log;

public class SamplingRate {


    /**
     * This function is responsible for computing sampling rate
     *
     * @param timeStamps
     * @return
     */
    public static double computeSamplingRate(double[] timeStamps) {
        double[] diffTS = ArrayManupulation.diff(timeStamps);
        double meanDiffTS = Math.floor(ArrayManupulation.mean(diffTS));
        Log.i("MeanTS", "" + meanDiffTS);
        double samplingRate = Math.floor(1000.0 / meanDiffTS);
        Log.i("SR", "" + samplingRate);
        return samplingRate;
    }
}
