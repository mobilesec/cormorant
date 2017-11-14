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

/**
 * This class is used to set parameters for gait authentication
 * 1-- Sampling Rate, we compute it directly from the sensor time stamps.
 * 2-- Interpolation Frequency = RoundOff sampling frequency becomes our interpolation frequency
 * 3-- Threshold to remove unusual gait cycles == 1/2(sampling frequency)
 * 4-- BaseLineSize = sampling frequency - 20;
 *
 * @author Muhammad Muaaz
 */

public class GaitParameters {


    public static int maxQueueCapacityTrainingData = 2000;
    public static String dataRecordingMsgFirstTime = "Since you are recording gait data first time please read " +
            "information below carefully. You must be wearing trouser " +
            "with not-too-loose front pockets and comfortable shoes. " +
            "Pick a straight walking place 30 meter long. Go to the " +
            "start line of your walking place and Complete 4 rounds of " +
            "that place. Walk at your normal pace as you do in routine life. " +
            "Visit this video link for detail information about gait data recording.";
    public static String getDataRecordingMsg = "This time you will record data as you have done last time";
    // At the moment we are using manual assignment but in application these all will be replaced by auto assign methods.
    // Sampling frequency of the the sensor.
    private static double samplingFrequency;
    // Frequency at which we want to interpolate the sensor data
    private static double interpolationFrequency;
    // Base line used to determin gait cycle length
    private static int baseLineSize;
    // This threshold values is used to remove unusual git cycles
    private static double thresholdToRemoveGaitCycles =0.5;

    public GaitParameters() {
        // TODO Auto-generated constructor stub
    }

    public static int getSamplingFrequency() {
        return (int) samplingFrequency;
    }

    public static void setSamplingFrequency(double _samplingFrequency) {

        samplingFrequency = _samplingFrequency;

        // We set sampling frequency equals to the interpolation frequency.
        setInterpolationFrequency(_samplingFrequency);

        // BaseLine size is always set at difference of 20 from main samplingFrequency
        setBaselinesize();

        // Threshoold to remove unusual gait cycles is always set at half of the sampling rate/.
        setThresholdToRemoveUnusualGaitCycles(_samplingFrequency / 2.0);
    }

    public static double getInterpolationFrequency() {
        return interpolationFrequency;
    }

    public static void setInterpolationFrequency(double _interpolationFrequency) {
        interpolationFrequency = _interpolationFrequency;
    }

    public static double getThresholdToRemoveUnusualGaitCycles() {
        return thresholdToRemoveGaitCycles;
    }

    public static void setThresholdToRemoveUnusualGaitCycles(
            double _thresholdToRemoveUnusualGaitCycles) {
        thresholdToRemoveGaitCycles = (int) samplingFrequency / 2.0;
    }

    public static int getBaselinesize() {

        return baseLineSize;
    }

    public static void setBaselinesize() {
        baseLineSize = (int) (samplingFrequency - 20);
    }
}
