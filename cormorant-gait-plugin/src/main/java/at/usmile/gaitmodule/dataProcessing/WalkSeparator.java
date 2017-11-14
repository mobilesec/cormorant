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
package at.usmile.gaitmodule.dataProcessing;


import android.util.Log;

import com.google.common.primitives.Ints;

import java.util.ArrayList;

import Jama.Matrix;
import at.usmile.gaitmodule.extras.LogMessage;
import at.usmile.gaitmodule.utils.ArrayManupulation;
import edu.umbc.cs.maple.utils.JamaUtils;
/**
 *
 */

/**
 * This class is basically responsible for separating walks into the active walk segments. It is based on Rene' Mayrhofers
 * Variance based thresholding method, we have used this method of finding active segments in (Gait-and-Shake-MoMM2014) papers.
 * I have fully tested this implementation and I found it is working quite well. I have cross compared results with same algorithms
 * Implementation in matlab. Time to find Active segments is same we have in MATLAB. Probably MATLAB implementation is faster.
 * Changes as compared to the original algo. In original algo we are only concerned with first active segment. However by using
 * this algorithm we can simply detect all available active segments in the walk, which has length according to our requirements.
 * Modification made to this algo are also fully tested and working. Checked by varying thresholding values and by changing windowSize
 * and segment length...
 *
 * @author Muhammad Muaaz
 * @version 1.0
 */
public class WalkSeparator {
    //static private final Logger LOG = LoggerFactory.getLogger(WalkSeparator.class);
    private static final String TAG = "WalkSeparator";
    private static int windowSize = 100;
    private static int minimumLength = 1500; //minimumLength The minimum length of a segment to be regarded as significant, as the number of data points in the segment.
    private static float varianceThreshold = 0.9f;

    // Best Settings for walk separation windowSize100, minimumLength = 2000, varianceThreshold = 0.8f

    /**
     * This function determined the transitions points between quiescent and active phases in the input data set.
     * The distinction is made by calculating the variances of each dimension in the data set over a sliding time
     * window. If the variance of at least one of the dimensions is equal to or higher then the threshold, then
     * this segment is taken to be active. The index of each change, determined is the first index used for the
     * sliding time window, is returned. The last index that can be returned is size(in,1)-windowsize+1.
     *
     * @param data,              The data set with one data vector in each row.
     * @param windowSize,        The number of data points to use in the sliding time window.
     * @param varianceThreshold, If the variance is higher then this, a segment is regarded as active.
     * @return The returned matrix has 2 dimensions, where the first dimension gives the index at
     * which the change starts and the second dimension signifies if a silent or an active phase
     * starts, with 0 meaning silent and 1 meaning active.
     */

    public static int[][] determinTransitionPoints(Matrix data, int windowSize, double varianceThreshold) throws IllegalArgumentException {
        Log.i("FindingTransitionPoints", "started");
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Invalid windowSize, can't be <= 0");
        }

        if (varianceThreshold <= 0) {
            throw new IllegalArgumentException("Invalid varianceThreshold, can't be <= 0");
        }

        int lastState = -1;
        ArrayList<Integer> out1 = new ArrayList<>(200);
        ArrayList<Integer> out2 = new ArrayList<>(200);

        double[] temp = new double[windowSize];
        double[] dataColarr = JamaUtils.getcol(data, 0).getColumnPackedCopy();
        //ArrayManupulation.printArray(dataColarr);
        int loopTill = data.getRowDimension() - windowSize;
        for (int i = 0; i < loopTill; i++) {
            int activeFound = 0;
            System.arraycopy(dataColarr, i, temp, 0, windowSize);
            double var = ArrayManupulation.variance(temp);

            if (var >= varianceThreshold) {
                activeFound = 1;
            }

            if (activeFound == 1 && lastState != 1) {
                out1.add(i + windowSize);
                out2.add(1);
                lastState = 1;
            } else if (activeFound == 0 && lastState != 0) {
                out1.add(i);
                out2.add(0);
                lastState = 0;
            }

        }

        return new int[][]{Ints.toArray(out1), Ints.toArray(out2)};
    }

    /**
     * @param _indicies   - array containing the indicies of the active segment start and end.
     * @param _indicators - array indicating the start and end of active segments
     *                    The transitition points between active and passive segments.
     * @return The lower and upper indices of the first segment.
     */

    private static int[][] getSignificantActiveSegments(int[] _indicies, int[] _indicators) throws IllegalArgumentException {

        ArrayList<Integer> lower = new ArrayList<>();
        ArrayList<Integer> upper = new ArrayList<>();
        int[][] limits = null;

        if (_indicies.length < 2) {
            LogMessage.setStatus(TAG, "Failed to find any active segment");
            limits = null;
            return limits;
        }

        if (minimumLength < 1) {
            //throw new IllegalArgumentException("Minimum length needs to be at least 1");
            return limits;
        }

        int i = 0; //i +=_index;
        int found = 0;

        while (i < _indicies.length && found == 0) {
            if (_indicators[i] == 1 && _indicators[i + 1] == 0 && _indicies[i + 1] - _indicies[i] >= minimumLength) {
                found = 1;
                if (found == 1) {
                    lower.add(_indicies[i]);
                    upper.add(_indicies[i + 1]);
                }
                i = i + 1;
                found = 0;
            } else {
                i = i + 1;
            }
        }
        if (lower != null && upper != null) {
            limits = new int[][]{Ints.toArray(lower), Ints.toArray(upper)};
        }
        return limits;
    }

    /**
     * This function returns all active gait segments in a walk.
     *
     * @param data - Matrix containing 1 or multidimensional data.
     * @return limits - Array of arrays. start and end of the active walk segments.
     * Where starting index of walk segments are stored in the
     * first array and ending index of the gait active walk segment
     * is stored in the 2nd array.
     */

    public static int[][] getAllActiveSegments(Matrix data) {


        int[][] ind = determinTransitionPoints(data, windowSize, varianceThreshold);
        int[] indices = ind[0];
        int[] indicators = ind[1];

        return getSignificantActiveSegments(indices, indicators);
    }

}
