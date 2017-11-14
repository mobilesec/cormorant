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
package at.usmile.gaitmodule.extras;

import android.util.Log;

import com.google.common.primitives.Doubles;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import Jama.Matrix;
import at.usmile.gaitmodule.utils.ArrayManupulation;
import edu.umbc.cs.maple.utils.JamaUtils;

public class GaitDecisionModule {

    // This is threshold value is computed from our dataSet.
    // that this threshold clearly separates genuine and Impostor user.
    private static double THRESHOLD;
    private static final String TAG = "DicisionModule";
    public static int verifyUser(Matrix _trainMat, Matrix _testMat, double securityLevel, double threshold) throws FileNotFoundException, IOException {
        LogMessage.setStatus("GDM", "InVerfyMethod");
        THRESHOLD = threshold;
        double matchRate = computeDTWDifferencesRvR(_trainMat, _testMat);
        LogMessage.setStatus("GDM", "InDecisionMoudle" + matchRate);
        if (matchRate >= (securityLevel/100)) {
            return 1;
        } else {
            return 0;
        }

    }

    /**
     * Remained versus Remained gait cycle approach
     *
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static double computeDTWDifferencesRvR1(Matrix _trainMat, Matrix _testMat) throws FileNotFoundException, IOException {
        LogMessage.setStatus("GDM", "inComputeDTWDistance1");
        ArrayList<Double> distancesOnly = new ArrayList<>();


        double matchRate;

        int remainedCyclesTemplate = _trainMat.getColumnDimension();
        int remainedCyclesTest = _testMat.getColumnDimension();
        double[] tempDistances = new double[remainedCyclesTemplate];
        //Loop over all test Gait cycles
        int s = 0;
        while (s < remainedCyclesTest) {
            //Loop over all remained gait cycles
            int k = 0;
            while (k < remainedCyclesTemplate) {

                DTWDistance dtw = new DTWDistance(JamaUtils.getcol(_testMat, s).getColumnPackedCopy(), JamaUtils.getcol(_trainMat, k).getColumnPackedCopy());
                tempDistances[k] = dtw.getWarpingDistance();
                //System.out.println("tempDis::"+ s +"::"+tempDistances[k]);
                k = k + 1;
            }

            distancesOnly.add(ArrayManupulation.minimum(tempDistances));
            s = s + 1;
        }

        double[] distances = Doubles.toArray(distancesOnly);
        Log.i("Dicision Modules:", "Computing FMR/FNMR");
        matchRate = calculateFMFNM(distances);
        LogMessage.setStatus(TAG, Arrays.toString(distances));
        return matchRate;
    }

    // This function returns 1 if template matches with test walk and 0 for noMatch
    private static double calculateFMFNM(double[] distances) {
        LogMessage.setStatus("GDM", "InFMRFNMR");
        int[] matchNonMatchCol = new int[distances.length];
        int[] majorityMatchNonMatchCol = new int[distances.length];

        for (int j = 0; j < distances.length; j++) {

            if (distances[j] < THRESHOLD) {
                matchNonMatchCol[j] = 1;

            } else {
                matchNonMatchCol[j] = 0;
            }

        }

        if(matchNonMatchCol.length>1) {
            double matches = ArrayManupulation.sum(matchNonMatchCol);
            LogMessage.setStatus("GDM", "Matches" + matches + "/" + distances.length);
            return matches / distances.length;
        }
        if(matchNonMatchCol.length == 1){
            return matchNonMatchCol[0]/distances.length;
        }else{

            return 0.0;
        }

    }

    /**
     * Remained versus Remained gait cycle approach
     *
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static double computeDTWDifferencesRvR(Matrix _trainMat, Matrix _testMat) throws FileNotFoundException, IOException {
        LogMessage.setStatus("GDM", "inComputeDTWDistance1");
        double matchRate;
        int remainedCyclesTemplate = _trainMat.getColumnDimension();
        int remainedCyclesTest = _testMat.getColumnDimension();
        ArrayList<Double> tempDistances = new ArrayList<>(remainedCyclesTest*remainedCyclesTemplate);
        //Loop over all test Gait cycles
        int s = 0;
        while (s < remainedCyclesTemplate) {
            //Loop over all remained gait cycles
            int k = 0;
            while (k < remainedCyclesTest) {
                //LogMessage.setStatus(TAG, "Comparing: TemplateCycle: "+s + " with TestCycle: " +k);
                DTWDistance dtw = new DTWDistance(JamaUtils.getcol(_trainMat, s).getColumnPackedCopy(), JamaUtils.getcol(_testMat, k).getColumnPackedCopy());
                tempDistances.add(dtw.getWarpingDistance());
                k = k + 1;
            }
            s = s + 1;
        }
        double[] distances = Doubles.toArray(tempDistances);
        Log.i("Dicision Modules:", "Computing FMR/FNMR");
        matchRate = calculateFMFNM(distances);
        //LogMessage.setStatus(TAG, Arrays.toString(distances));
        return matchRate;
    }
}
