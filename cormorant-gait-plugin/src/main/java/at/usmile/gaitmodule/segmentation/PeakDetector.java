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
package at.usmile.gaitmodule.segmentation;

import com.google.common.primitives.Ints;

import java.util.ArrayList;

/**
 * A standard peak detector in time series.
 * <p>
 * The goal of this class is to identify peaks in a 1D time series (float[]).
 * It simply implements G.K. Palshikar's <i>Simple Algorithms for Peak Detection
 * in Time-Series</i> ( Proc. 1st Int. Conf. Advanced Data Analysis, Business
 * Analytics and Intelligence (ICADABAI2009), Ahmedabad, 6-7 June 2009),
 * We retained the first "spikiness" function he proposed, based on computing
 * the max signed distance to left and right neighbors.
 * SimpleAlgorithmsforPeakDetectioninTimeSeriesACADABAI_2009.pdf
 * @author Muhammad muaaz May January, 2015
 */
public class PeakDetector {

    private double[] T;

    /**
     * Create a peak detector for the given time series.
     */
    public PeakDetector(final double[] timeSeries) {
        double []tS = timeSeries;
        int loopTill = timeSeries.length;
        for(int i = 0; i< loopTill; i++){
            tS[i] = -1*timeSeries[i];
        }
        this.T = tS;
    }

    /**
     * Return the peak locations as array index for the time series set at creation.
     * @param windowSize  the window size to look for peaks. a neighborhood of +/- windowSize
     * will be inspected to search for peaks. Typical values start at 3.
     * @param stringency  threshold for peak values. Peak with values lower than <code>
     * mean + stringency * std</code> will be rejected. <code>Mean</code> and <code>std</code> are calculated on the
     * spikiness function. Typical values range from 1 to 3.
     * @return an int array, with one element by retained peak, containing the index of
     * the peak in the time series array.
     */
    public int[] process(final int windowSize, final double stringency) {

        // Compute peak function values
        double[] S = new double[T.length];
        double maxLeft, maxRight;
        for (int i = windowSize; i < S.length - windowSize; i++) {

            maxLeft = T[i] - T[i-1];
            maxRight = T[i] - T[i+1];
            for (int j = 2; j <= windowSize; j++) {
                if (T[i]-T[i-j] > maxLeft)
                    maxLeft = T[i]-T[i-j];
                if (T[i]-T[i+j] > maxRight)
                    maxRight = T[i]-T[i+j];
            }
            S[i] = 0.5f * (maxRight + maxLeft);

        }

        // Compute mean and std of peak function
        double mean = 0;
        int n = 0;
        double M2 = 0;
        double delta;
        for (int i = 0; i < S.length; i++) {
            n = n + 1;
            delta = S[i] - mean;
            mean = mean + delta/n;
            M2 = M2 + delta*(S[i] - mean) ;
        }

        double variance = M2/(n - 1);
        float std = (float) Math.sqrt(variance);

        // Collect only large peaks
        ArrayList<Integer> peakLocations = new ArrayList<Integer>();
        for (int i = 0; i < S.length; i++) {
            if (S[i] > 0 && (S[i]-mean) > stringency * std) {
                peakLocations.add(i);
            }
        }

        // Remove peaks too close
        ArrayList<Integer> toPrune = new ArrayList<Integer>();
        int peak1, peak2, weakerPeak;
        for (int i = 0; i < peakLocations.size()-1; i++) {
            peak1 = peakLocations.get(i);
            peak2 = peakLocations.get(i+1);

            if (peak2 - peak1 < windowSize) {
                // Too close, prune the smallest one
                if (T[peak2] > T[peak1])
                    weakerPeak = peak1;
                else
                    weakerPeak = peak2;
                toPrune.add(weakerPeak);
            }
        }
        peakLocations.removeAll(toPrune);

        // Convert to int[]
        return Ints.toArray(peakLocations);
    }

}
