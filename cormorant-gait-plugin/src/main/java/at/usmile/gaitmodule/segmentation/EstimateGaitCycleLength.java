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
/**
 * Muhammad Muaaz
 * 19-08-2014
 */
package at.usmile.gaitmodule.segmentation;


import android.util.Log;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import org.JMathStudio.Exceptions.IllegalArgumentException;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;

import at.usmile.gaitmodule.utils.ArrayManupulation;

/**
 *
 * This class impliments the methods of gait cyclelength estimation Most of the code used here based on Omer's approach is Code here is 
 * written and I have implimented the adapted version of gait cycle length presented in the MoMM2014 paper. We do use Automatic peak 
 * detection algorithim  "An Efficient Algorithm for Automatic Peak Detection in Noisy Periodic and Quasi-Periodic Signals" 
 * Abstract: We present a new method for automatic detection of peaks in noisy periodic and quasi-periodic signals. The new method, called 
 * automatic multiscale-based peak detection (AMPD), is based on the calculation and analysis of the local maxima scalogram, a matrix 
 * comprising the scale-dependent occurrences of local maxima. The usefulness of the proposed method is shown by applying the AMPD 
 * algorithm to simulated and real-world signals. http://www.mdpi.com/1999-4893/5/4/588 (Link to the paper)
 *
 * @version 1.0
 * @author Muhammad Muaaz
 *
 */
public class EstimateGaitCycleLength {


    private double[] walkSignal;
    private int walkLength;
    private double segCenter;
    private double estimatedCycleLength; // will get initialized to zero when


    /**
     * Constructor
     */
    public EstimateGaitCycleLength(double[] _walkSingleAxis) throws IllegalArgumentException {
        walkLength = _walkSingleAxis.length;

        if (walkLength < 2) {
            throw new IllegalArgumentException("To short walk, can't estimate the cyclelength");
        }

        walkSignal = new double[walkLength];
        this.walkSignal = _walkSingleAxis;
    }


    /**
     * get walk segment length
     */
    public int getWalkLength() {
        return walkLength;
    }

    /**
     * this will return walk segment
     */
    public double[] getWalk() {
        return walkSignal;
    }

    /**
     * this will retrun estimated segment length
     */
    public double getCycleLength()
    {
        return estimatedCycleLength;
    }

    /**
     * this will retrun estimated segment length
     *
     * @return
     */
    private void setEstimatedCycleLength(double _cycleLength) {
        estimatedCycleLength = _cycleLength;
    }

    /**
     * this will return segmentCenter which is bassically the center point of the walk
     */
    private int getSegmentCenter() {
        return (int) (segCenter = Math.round(walkLength / 2));

    }

    /**
     * Call this function to calculate the Estimated CycleLength
     */
    public double calculateEstimatedCycleLength() throws RuntimeException {
        Log.i("baseLine is", "" + GaitParameters.getBaselinesize());

        int baseLineSize = GaitParameters.getBaselinesize();
        int baseLineStartIndx = getSegmentCenter() - baseLineSize;

        double[] baseLineVec = new double[GaitParameters.getBaselinesize()];
        System.arraycopy(walkSignal, baseLineStartIndx, baseLineVec, 0, baseLineSize);

        if (baseLineVec.length != GaitParameters.getBaselinesize()) {
            throw new RuntimeException("Length of baseLineVec doesnot match with BASELINE size constant");
        }

        double estimatedGaitCycleLength = getEstimatedCycleLength(baseLineVec);
        setEstimatedCycleLength(estimatedGaitCycleLength);
        return estimatedGaitCycleLength;
    }

    /**
     * This function is only visible to this class and is used by calculateEstimatedCycleLength() function
     */
    private double getEstimatedCycleLength(double[] baseLineVector) {

        int baseLineSize = GaitParameters.getBaselinesize();
        //First we start form value zero or the first index of the walkSignal
        double[] subSegment = new double[baseLineSize];
        ArrayList<Double> distanceList = new ArrayList<Double>();
        int looTill = walkLength-baseLineSize;
        for (int i = 0; i <= looTill; i++) {

            System.arraycopy(walkSignal, i, subSegment, 0, baseLineSize);
            // add distance to distanceList
            distanceList.add(EuclideanDistance.seriesDistance(baseLineVector, subSegment));
        }
        double[] distanceArray = Doubles.toArray(distanceList);

        // Pass distance array to peakDector... to find Peaks and Vallies
        // Here we have two algos we are using peakDetector.
        // AMPDAlgo myAmpd = new AMPDAlgo(distanceArray);
        // int[] vallies = myAmpd.ampdVallies();
        // vallies = pruneVallies(distanceArray, vallies);
        // Log.i("Valies","");
        // ArrayManupulation.printArray(vallies);

        PeakDetector myPeakDetector = new PeakDetector(distanceArray);
        int [] mins = myPeakDetector.process(11,3);
        // Log.i("mins","");
        // ArrayManupulation.printArray(mins);
        int[] differenceVector = ArrayManupulation.diff(mins);
        return computeAverageCycleLength(differenceVector);
    }

    //Method to compute average cycle length form the the difference vector...
    private double computeAverageCycleLength(int[] _differenceVector) {
        int baseLineSize = GaitParameters.getBaselinesize();
        double gaitCycleLength = 0;

        ArrayList<Integer> valuesToKeepList = new ArrayList<Integer>();
        int loopTill = _differenceVector.length;
        for (int i = 0; i < loopTill; i++)
            if (_differenceVector[i] > baseLineSize && _differenceVector[i] < 1.3 * GaitParameters.getInterpolationFrequency()) {
                valuesToKeepList.add(_differenceVector[i]);
            }


        int[] valuesToKeepArray = Ints.toArray(valuesToKeepList);
        if (valuesToKeepArray.length >= 2) {
            gaitCycleLength = ArrayManupulation.mean(valuesToKeepArray);
        }
        if (valuesToKeepArray.length < 2) {
            gaitCycleLength = -1;
        }
        return gaitCycleLength;
    }


    // This method with return an array of gaitCycleStart...
    public int[] detectGaitCycleStarts() {
        // Pick area around the center of the walk
        int areaToSearchStart = (int) (segCenter - estimatedCycleLength);
        int areaToSearchEnd = (int) (segCenter + estimatedCycleLength - 1);

        Log.i("Test", walkLength+":"+areaToSearchStart+":"+areaToSearchEnd);
        double[] centerSegment = new double[areaToSearchEnd - areaToSearchStart];

        //Copy that region from the walk segment
        System.arraycopy(walkSignal, areaToSearchStart, centerSegment, 0, centerSegment.length);

        //Find the minimumValue in this segment...
        double minVal = ArrayManupulation.minimum(centerSegment);
        int minValIndex = ArrayManupulation.indexOfValue(centerSegment, minVal);
        int indexInWalkOfMinVal = areaToSearchStart + minValIndex;


        // Now we will do forward and backword search to find all minimum in the walk....
        // Forward search this forward search does not help us much. So we perform backwork
        // search find first minimun in the data and from there we start forward search so
        // entire process completer in one and half cycle of search
        int[] forwardSearchMinIndex = forwardSearchForMinimumValues(indexInWalkOfMinVal, walkSignal.length);
        int[] backwardSearchMinIndex = backWardSearchForMinimumValues(indexInWalkOfMinVal);
        int[] detectedCycleStartsMin = ArrayManupulation.merge(backwardSearchMinIndex, forwardSearchMinIndex);

        return detectedCycleStartsMin;

    }

    /**
     * This function finds the minimum value going forwards
     *
     * @param _indexInWalk
     * @param size
     * @return
     */
    private int[] forwardSearchForMinimumValues(int _indexInWalk, int size) {
        ArrayList<Integer> forwardCycleStartsMin = new ArrayList<Integer>();

        // check if found min is in list of Vallies found by APMD algo

        forwardCycleStartsMin.add(_indexInWalk);
        int end = (int) (_indexInWalk + estimatedCycleLength);
        int searchOffSet = (int) Math.ceil(0.2 * estimatedCycleLength);

        double[] temp = new double[(int) (Math.ceil(0.5 * estimatedCycleLength) + Math.ceil(0.5 * searchOffSet))];
        int loopTill = size -(int) Math.ceil(estimatedCycleLength / 2);
        while (end < loopTill) {
            System.arraycopy(walkSignal, (int) (end - Math.ceil(0.5 * estimatedCycleLength)), temp, 0, (int) (Math.ceil(0.5 * estimatedCycleLength) + Math.ceil(0.5 * searchOffSet)));
            double minVal = ArrayManupulation.minimum(temp);
            int index = ArrayManupulation.indexOfValue(temp, minVal);
            int trueEnd = (int) (end - Math.ceil(0.5 * estimatedCycleLength)) + index;
            end = trueEnd + (int) estimatedCycleLength;
            forwardCycleStartsMin.add(trueEnd);
        }

        return Ints.toArray(forwardCycleStartsMin);
    }

    /**
     * This function finds minimum value going backward. Since start of the signal
     * and end of the signal is not same going forward gives best result can only
     * be achieved going forward. Therefore after finding the first value in the
     * signal we start from that signal and move forward.
     *
     * @param _indexInWalk
     * @return
     */
    private int[] backWardSearchForMinimumValues(int _indexInWalk) {

        ArrayList<Integer> backWardCycleStartsMin = new ArrayList<Integer>();

        int end = (int) (_indexInWalk - Math.ceil(estimatedCycleLength));
        int searchOffSet = (int) (Math.ceil(0.2 * estimatedCycleLength));

        double[] temp = new double[(int) (Math.ceil(0.5 * estimatedCycleLength) + Math.ceil(0.5 * searchOffSet))];

        int loopTill = (int)Math.ceil(estimatedCycleLength / 2);
        while (end > loopTill) {
            System.arraycopy(walkSignal, (int) (end - Math.ceil(0.5 * estimatedCycleLength)), temp, 0, (int) (Math.ceil(0.5 * estimatedCycleLength) + Math.ceil(0.5 * searchOffSet)));
            double minVal = ArrayManupulation.minimum(temp);
            int index = ArrayManupulation.indexOfValue(temp, minVal);

            int trueEnd = (int) (end - Math.ceil(0.5 * estimatedCycleLength)) + index;
            end = trueEnd - (int) estimatedCycleLength;
            backWardCycleStartsMin.add(trueEnd);
        }

        Collections.sort(backWardCycleStartsMin);
        return forwardSearchForMinimumValues(backWardCycleStartsMin.get(0), _indexInWalk);
    }

    /**
     * This function is responsible for finding only those minimas which are very close to each other
     */
    // This function is being used ...
    public int[] pruneVallies(double[] _signal, int[] _minimas)

    {
        // index1 is index of most minimum value
        int indx1 = ArrayManupulation.indexOfValue(_signal, ArrayManupulation.minimum(_signal));
        // presence of the minimum value in the minimas
        int indx = ArrayUtils.indexOf(_minimas, indx1);
        int j = indx;
        while (j < _minimas.length - 1) {
            if (_minimas[j + 1] - _minimas[j] <= GaitParameters.getBaselinesize()) {
                _minimas = ArrayUtils.remove(_minimas, j + 1);
            }
            j = j + 1;
        }

        int k = indx;
        while (k > 0) {
            if (_minimas[k] - _minimas[k - 1] <= GaitParameters.getBaselinesize()) {
                _minimas = ArrayUtils.remove(_minimas, k - 1);
            }
            k = k - 1;
        }

        return _minimas;
    }
}