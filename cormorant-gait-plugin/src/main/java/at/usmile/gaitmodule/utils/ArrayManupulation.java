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
 * A helper class to perform operations over the arrays
 * with this class you can acutally perfom various operations
 * on the arrays such as Standard deviation, mean, average,
 * difference between two adjacent values, finding minimum value,
 * finding maximum values, finding index of perticular value,
 * removing duplicate values from array, removing zeros from array,
 * median, mode, sorting the arrays, converting 2D array to 1D.
 * Necessary operations neeeded on arrays from gait
 * authentication prespective are covered in this class.
 * All methods of this class are static and we can keep on adding
 * futher static methods to extend the functionality.
 *
 * @version 1.0
 * @author Muhammad Muaaz
 */
package at.usmile.gaitmodule.utils;

/**
 * This is a arrayUtililty class, 
 * of this class.
 *
 * @author Muhammad Muaaz
 *
 */


import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class ArrayManupulation {

    private double[] T;

    public ArrayManupulation(final double[] timeSeries) {
        this.T = timeSeries;
    }

    /** Method for computing variance of double values*/

    public static double variance(double[] x) {
        double mean = mean(x);
        double squareSum = 0;

        for (int i = 0; i < x.length; i++) {
            squareSum += Math.pow(x[i] - mean, 2);
        }
        return squareSum / x.length;
    }

    /** Method for computing variance of double values*/
    public static float variance(double[] x, int offset, int length) {
        float mean = (float) mean(x);
        float squareSum = 0;

        for (int i = offset; i < offset + length; i++) {
            squareSum += Math.pow(x[i] - mean, 2);
        }
        return squareSum / length;
    }

    /**
     * Method for computing standard deviation of double values
     * @param x
     * @return
     */
    public static double deviation(double[] x) {
        double mean = mean(x);
        double squareSum = 0;

        for (int i = 0; i < x.length; i++) {
            squareSum += Math.pow(x[i] - mean, 2);
        }

        return Math.sqrt((squareSum) / (x.length - 1));
    }

    /** Method for computing deviation of int values*/
    public static double deviation(int[] x) {
        double mean = mean(x);
        double squareSum = 0;

        for (int i = 0; i < x.length; i++) {
            squareSum += Math.pow(x[i] - mean, 2);
        }

        return Math.sqrt((squareSum) / (x.length - 1));
    }

    /** Method for computing mean or average of an array of double values*/
    public static double mean(double[] x) {

        checkArguement(x.length);
        double sum = 0;

        for (int i = 0; i < x.length; i++)
            sum += x[i];

        return sum / x.length;
    }

    /** Method for computing mean of an array of int values*/
    public static double mean(int[] x) {

        checkArguement(x.length);
        int sum = 0;

        for (int i = 0; i < x.length; i++)
            sum += x[i];

        return sum / x.length;
    }

    /** Method for computing mean of an array of int values*/
    public static double mean(float[] x) {

        checkArguement(x.length);
        double sum = 0.0;

        for (int i = 0; i < x.length; i++)
            sum += x[i];

        return sum / x.length;
    }

    /** Method for computing mean of an array of long values*/
    public static double mean(long[] x) throws IllegalArgumentException {

        checkArguement(x.length);
        double sum = 0;
        double average;

        for (int i = 0; i < x.length; i++) {
            sum = sum + x[i];
        }
        average = sum / x.length;
        return average;
    }

    /** Method for computing mean or average of an array of double values*/
    private static double mean(ArrayList<Double> x) {

        double[] arrayForm = new double[x.size()];
        arrayForm = Doubles.toArray(x);
        double mean = mean(arrayForm);
        return mean;
    }

    /** Method for computing sum of an array of double values*/
    public static double sum(double[] x) {

        checkArguement(x.length);
        double sum = 0;

        for (int i = 0; i < x.length; i++)
            sum += x[i];

        return sum;
    }

    /** Method for computing sum of an array of double values*/
    public static double sum(int[] x) {

        checkArguement(x.length);
        double sum = 0;

        for (int i = 0; i < x.length; i++)
            sum += x[i];

        return sum;
    }

    /** Method for computing diff of an array of long values*/
    public static long[] diff(long[] x) {

        checkArguement(x.length);
        long diff[] = new long[x.length - 1];

        for (int i = 0; i < x.length - 1; i++) {

            diff[i] = x[i + 1] - x[i];
        }
        return diff;
    }

    /** Method for computing diff of an array of double values*/
    public static double[] diff(double[] x) {

        double diff[] = new double[x.length - 1];

        for (int i = 0; i < x.length - 1; i++) {

            diff[i] = x[i + 1] - x[i];
        }
        return diff;
    }

    /** Method for computing diff of an array of double values*/
    public static int[] diff(int[] x) {

        checkArguement(x.length);
        int diff[] = new int[x.length - 1];

        for (int i = 0; i < x.length - 1; i++) {

            diff[i] = x[i + 1] - x[i];
        }
        return diff;
    }

    public static double maximum(double... array) {
        double max = Doubles.max(array);
        return max;
    }

    public static void checkArguement(int a) {

        if (a <= 1) {
            throw new IllegalArgumentException("X must have more than one value");
        }

    }

    /** Method for finding a minimum value of an array of double values
     * @param double[] arr
     * @return minimum value from that array.
     *
     * */
    public static double minimum(double[] x) {

        double min = Doubles.min(x);
        return min;
    }

    public static double minimum(int[] x) {

        double min = Ints.min(x);
        return min;
    }

    // Returns index of minimum value back
    public static int indexOfValue(double[] _array, double _minVal) {

        int index = 0;
        for (int i = 0; i < _array.length; i++) {

            if (_array[i] == _minVal) {
                index = i;
            }
        }

        return index;

    }

    /**
     *
     * @param _array
     * @param _minVal
     * @return index of the value to be found
     */
    public static int indexOfValue(int[] _array, double _minVal) {

        int index = 0;
        for (int i = 0; i < _array.length; i++) {

            if (_array[i] == _minVal) {
                index = i;
            }
        }

        return index;

    }

    public static int indexOfValue(int[] _array, int _val) {
        int index = -1;
        for (int i = 0; i < _array.length; i++) {

            if (_array[i] == _val) {
                index = i;
            }

        }

        return index;
    }

    /** Method for removing zeros from the int array
     * @param arr with zeros inside
     * @param arr without any zeros inside
     */
    public static int[] removeZeros(int[] x) {
        int j = 0;
        for (int i = 0; i < x.length; i++) {
            if (x[i] != 0)
                x[j++] = x[i];
        }
        int[] xOut = new int[j];
        System.arraycopy(x, 0, xOut, 0, j);
        return xOut;
    }

    /** Method for removing zeros form double array
     * @param  arr with zeros inside
     * @param  arr without any zeros inside
     * */

    public static double[] removeZeros(double[] x) {
        int j = 0;
        for (int i = 0; i < x.length; i++) {
            if (x[i] != 0.0)
                x[j++] = x[i];
        }
        double[] xOut = new double[j];
        System.arraycopy(x, 0, xOut, 0, j);
        return xOut;


    }

    /**
     * This function can be used to merge arrays
     * @param arrs
     * @return arr
     */
    public static int[] merge(int[]... arrays) {
        int size = 0;
        for (int[] a : arrays)
            size += a.length;

        int[] res = new int[size];

        int destPos = 0;
        for (int i = 0; i < arrays.length; i++) {
            if (i > 0) destPos += arrays[i - 1].length;
            int length = arrays[i].length;
            System.arraycopy(arrays[i], 0, res, destPos, length);
        }

        return res;
    }

    /** Method for removing Duplicates form int array*/
    // removeDuplicates
    //   Given an array, remove all duplicate entries and return
    //   a new array with only unique entries while keeping the original
    //   order.
    public static int[] removeDuplicates(int[] x) {
        // Take care of the cases where the array is null or is empty.
        if (x == null) return null;
        if (x.length == 0) return new int[0];

        // Use a LinkedHashSet as a mean to remove the duplicate entries.
        // The LinkedHashSet has two characteristics that fit for the job:
        // First, it retains the insertion order, which ensure the output's
        // order is the same as the input's. Secondly, by being a set, it
        // only accept each entries once; a LinkedHashSet ignores subsequent
        // insertion of the same entry.
        LinkedHashSet<Integer> set = new LinkedHashSet<Integer>();
        for (int n : x)
            set.add(new Integer(n));

        // At this point, the LinkedHashSet contains only unique entries.
        // Since the function must return an int[], we need to copy entries
        // from the LinkedHashSet to a brand new array.
        int cursor = 0;
        int[] xOut = new int[set.size()];
        for (Integer n : set)
            xOut[cursor++] = n.intValue();
        return xOut;
    }

    /** Method for removing Duplicates form int array
     *   Given an array, remove all duplicate entries and return
     *   a new array with only unique entries while keeping the original
     *   order.
     */
    public static double[] removeDuplicates(double[] x) {
        // Take care of the cases where the array is null or is empty.
        if (x == null) return null;
        if (x.length == 0) return new double[0];

        // Use a LinkedHashSet as a mean to remove the duplicate entries.
        // The LinkedHashSet has two characteristics that fit for the job:
        // First, it retains the insertion order, which ensure the output's
        // order is the same as the input's. Secondly, by being a set, it
        // only accept each entries once; a LinkedHashSet ignores subsequent
        // insertion of the same entry.
        LinkedHashSet<Double> set = new LinkedHashSet<Double>();
        for (double n : x)
            set.add(new Double(n));

        // At this point, the LinkedHashSet contains only unique entries.
        // Since the function must return an int[], we need to copy entries
        // from the LinkedHashSet to a brand new array.
        int cursor = 0;
        double[] xOut = new double[set.size()];
        for (Double n : set)
            xOut[cursor++] = n.intValue();
        return xOut;
    }

    /**
     * This method computes the mode of the values in the
     * input array.
     *
     * @param arr - an array of ints
     * @return mode - the mode of the input array
     */
    public static int calculateMode(int[] arr) {

        int modeCount = 0;    // The count of the mode value
        int mode = 0;        // The value of the mode

        int currCount = 0;
        //int currElement;

        // Iterate through all values in our array and consider it as a possible mode
        for (int candidateMode : arr) {
            // Reset the number of times we have seen the current value
            currCount = 0;

            // Iterate through the array counting the number of times we see the current candidate mode
            for (int element : arr) {
                // If they match, increment the current count
                if (candidateMode == element) {
                    currCount++;
                }
            }

            // We only save this candidate mode, if its count is greater than the current mode
            // we have stored in the "mode" variable
            if (currCount > modeCount) {
                modeCount = currCount;
                mode = candidateMode;
            }
        }

        return mode;
    }

    /**
     * This method computes the median of the values in the
     * input array. First unsorted array is sorted out. If
     * our array's length is even, then we need to find the
     * average of the two centered values. Else if our array's
     * length is odd, then we simply find the value at the
     * center index
     *
     * @param arr - an array of doubles
     * @return median - the median of the input array
     */
    public static double calculateMedian(int[] arr) {

        // Sort our array
        int[] sortedArr = bubbleSort(arr);

        double median = 0;

        // If our array's length is even, then we need to find the average of the two centered values
        if (arr.length % 2 == 0) {
            int indexA = (arr.length - 1) / 2;
            int indexB = arr.length / 2;

            median = ((double) (sortedArr[indexA] + sortedArr[indexB])) / 2;
        }
        // Else if our array's length is odd, then we simply find the value at the center index
        else {
            int index = (sortedArr.length - 1) / 2;
            median = sortedArr[index];
        }

        // Print the values of the sorted array
        for (int v : sortedArr) {
            System.out.println(v);
        }

        return median;
    }

    /**
     * This method computes the median of the values in the
     * input array. First unsorted array is sorted out. If
     * our array's length is even, then we need to find the
     * average of the two centered values. Else if our array's
     * length is odd, then we simply find the value at the
     * center index
     *
     * @param arr - an array of doubles
     * @return median - the median of the input array
     */
    public static double calculateMedian(double[] arr) {

        // Sort our array
        double[] sortedArr = bubbleSort(arr);

        double median = 0;

        // If our array's length is even, then we need to find the average of the two centered values
        if (arr.length % 2 == 0) {
            int indexA = (arr.length - 1) / 2;
            int indexB = arr.length / 2;

            median = (sortedArr[indexA] + sortedArr[indexB]) / 2;
        }
        // Else if our array's length is odd, then we simply find the value at the center index
        else {
            int index = (sortedArr.length - 1) / 2;
            median = sortedArr[index];
        }

        // Print the values of the sorted array
        //for (double v : sortedArr)
        //{
        //		System.out.println(v);
        //}

        return median;
    }

    public static double findDeviation(double[] nums) {
        double mean = mean(nums);
        double squareSum = 0;
        for (int i = 0; i < nums.length; i++) {
            squareSum += Math.pow(nums[i] - mean, 2);
        }
        return Math.sqrt((squareSum) / (nums.length - 1));

    } // End of double findDeviation(double[])

    public static double findDeviation(int[] nums) {
        double mean = mean(nums);
        double squareSum = 0;
        for (int i = 0; i < nums.length; i++) {
            squareSum += Math.pow(nums[i] - mean, 2);
        }
        return Math.sqrt((squareSum) / (nums.length - 1));

    } // End of double findDeviation(double[])

	/* Method for computing deviation of double values*/

    // Beginning of double findDeviation(double[])

    /**
     * This program returns a sorted version of the input array.
     *
     * @param int arr, unsorted
     * @return int arr, sorted
     */
    public static int[] bubbleSort(int[] arr) {
        // We must sort the array.  We will use an algorithm called Bubble Sort.
        boolean performedSwap = true;
        int tempValue = 0;

        // If we performed a swap at some point in an iteration, this means that array
        // wasn't sorted and we need to perform another iteration
        while (performedSwap) {
            performedSwap = false;

            // Iterate through the array, swapping pairs that are out of order.
            // If we performed a swap, we set the "performedSwap" flag to true
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] > arr[i + 1]) {
                    tempValue = arr[i];
                    arr[i] = arr[i + 1];
                    arr[i + 1] = tempValue;

                    performedSwap = true;
                }
            }
        }

        return arr;
    }


	/* Method for computing deviation of double values*/

    // Beginning of double findDeviation(double[])

    /**
     * This program returns a sorted version of the input array.
     *
     * @param  arr, unsorted
     * @return arr, sorted
     */
    public static double[] bubbleSort(double[] arr) {
        // We must sort the array.  We will use an algorithm called Bubble Sort.
        boolean performedSwap = true;
        double tempValue = 0.0;

        // If we performed a swap at some point in an iteration, this means that array
        // wasn't sorted and we need to perform another iteration
        while (performedSwap) {
            performedSwap = false;

            // Iterate through the array, swapping pairs that are out of order.
            // If we performed a swap, we set the "performedSwap" flag to true
            for (int i = 0; i < arr.length - 1; i++) {
                if (arr[i] > arr[i + 1]) {
                    tempValue = arr[i];
                    arr[i] = arr[i + 1];
                    arr[i + 1] = tempValue;

                    performedSwap = true;
                }
            }
        }

        return arr;
    }

    /** This code is used for getting the 1D representaiton
     * from the 2D representation from the array
     * @param  arr, a two dimensional array...
     * @return arr, a one dimensional aaray...
     *
     */

    public static double[] get1DArrayFrom2DArray(double[][] arr) {

        ArrayList<Double> list = new ArrayList<Double>();
        for (int i = 0; i < arr.length; i++) {
            // tiny change 1: proper dimensions
            for (int j = 0; j < arr[i].length; j++) {
                // tiny change 2: actually store the values
                list.add(arr[i][j]);
            }
        }

        //Return ID array
        return Doubles.toArray(list);
    }

    /** Method for printing array */
    public static void printArray(double[] x) {
        for (int i = 0; i < x.length; i++)
            System.out.print(x[i] + " ");
        System.out.println();
    }

    /** Method for printing array */
    public static void printArray(int[] x) {
        for (int i = 0; i < x.length; i++)
            System.out.print(x[i] + " ");
        System.out.println();
    }

    /**
     *  The main function to test the performance of the class.
     *
     */
    public static void main(String[] args) {
        double[] tempData = new double[]{-100, -100, 8.44783172482435, 8.42495396757215, 8.50833008317962, 8.87149485225968, 9.19655096695068, 9.85563326124375, 9.85727301054823, 10.5547073010862, 10.7309014351559, 11.2541676672821, 11.2596918303772, 11.1749596582820, 10.9947783237770, 11.1159343316378, 11.0412703756069, 7.10907570054244, 5.37407908868952, 5.84156162621733, 6.72701289002997, 8.43130260747096, 10.6094635556358, 11.3724365697090, 12.2617556027223, 12.5963993486786, 12.7325063627724, 11.7420365039855, 10.4866561205013, 9.78048797325918, 9.01674206453143, 9.04060989147733, 9.37957451973500, 9.63135992319664, 9.45925475544415, 9.36236491041089, 9.59822119972648, 9.96401466484043, 10.1330456386786, 10.4882918255096, 10.1872387562117, 9.72561596337820, 9.46515382142739, 9.03715332476252, 8.80552681320774, 8.77693474506574, 9.19230544350680, 9.66465846577361, 9.84174511447110, 10.4452665250651, 10.5452494112247, 10.3271166451633, 10.3181671331276, 10.3280266571536, 10.5228300045176, 10.6630197785196, 11.0926751793511, 11.8653312093561, 11.6867391961551, 10.6454935337073, 9.83687094679325, 10.0251344961440, 10.7844343596739, 11.6976138353890, 12.2970041004616, 13.3105240090692, 14.1152630290471, 14.5608291411713, 14.7834992713400, 14.6563508410847, 14.4327797391132, 13.7310778051533, 14.1658553718603, 13.4095849983240, 11.2198638815886, 9.87307147178999, 7.99236322642223, 8.30249470377721, 9.79218398059705, 10.9395090291661, 12.9804999184764, 13.8199601083474, 14.9766779193127, 15.7235441563784, 15.984774,
                15.7940996065779, 13.9559504756812, 12.6340575675187, 11.9516417744412, 11.2174514786978, 10.8791237709362, 10.8478544960987, 10.0822117489791, 10.2260926971970, 10.0286512164047, 9.22314725686143, 7.61590868888167, 6.07994458667827, 5.43806896047310, 5.49447989332536, 5.71162701589333, 5.72045573853501, 5.67301884833700, 5.69148404759179, 5.69070830771852, 4.93192008526216, 4.15249895058297, 3.71779274014278, 3.74880751357692, 4.54322758820114, 5.11305083996470, 6.23077130974274, 7.03568691446082, 8.18163746476378, 9.80154974686048, 12.4357538143085, 14.9346130252640, 18.5055727064439, 22.0633266427696, 23.5955843022861, 22.5356060504065, 18.7294071323131, 15.3744223792553, 12.1337635821097, 10.4580004797458, 9.83344538428537, 9.38454627467069, 9.59659104582380, 11.6669381502178, 13.1676960354913, 14.8572669278856, 15.9794871341805, 13.3410960346210, 9.44873502424725, 7.54006169840064, 7.75426137324024, 8.06343453624178, 7.16655894802495, 10.2152945376501, 12.1959684756463, 12.7295772607567, 12.8580990454504, 12.8888037749796, 12.5171369247781, 11.9847067932953, 11.7435509435322, 10.7545072140973, 10.1005163056088, 9.64414270620711, 8.77662564320125, 8.45446085745236, 8.48780369581887, 8.27194622426609, 8.73713685020205, 9.23122229775095, 9.48256719584232, 9.91305381133739, 9.69194966166923, 9.48462249056455, 9.45189095131461, 9.36979701847885, 8.86864437164700, 8.84225145675184, 8.71087543981178, 8.47253852851670, 8.94262437096736, 9.51483486809235,
                10.4473221848913, 11.0441898981286, 11.5526094125251, 10.8511249303175, 10.7551694748154, 11.1949804290430, 12.1852280338641, 12.1065206856200, 11.3870908345242, 11.4426020409028, 11.5604648523935, 12.1123202572409, 13.7190775533519, 14.7725397378156, 15.6119073754331, 16.0160458300503, 15.7268340000882, 14.7079145472345, 12.5450566021660, 10.7114096886274, 10.2135942683934, 10.7589260370664, 11.5247706021694, 12.8344208271080, 14.4657761158390, 15.0240576538691, 15.3635829394553, 15.3252436527651, 14.9100933628398, 13.9527963018760, 13.0724073659464, 12.7790190817537, 12.5664992229614, 11.7772402268151, 11.4490245365642, 10.1763127713574, 8.77653338483571, 9.46350691436702, 7.61462744155586, 9.04630058291149, 8.34546135475464, 7.51878033911416, 6.26189231457246, 4.46674578202807, 3.55850409948067, 4.63142231048222, 4.49062584649658, 4.45229405421043, 4.24036703814941, 3.94072599349944, 3.68049947986145, 3.65289430603508, 3.76621685042147, 4.70155608549456, 5.51880180588699, 6.41241198271788, 7.61893074971161, 9.54454192729011, 11.5598466787883, 15.8491214618253, 19.7751430049507, 24.1999935658639, 25.3771579573237, 24.1068201451036, 18.8559183698932, 15.5901718668318, 11.4144241453878, 9.77714193594771, 9.86673937634006, 12.0903989876691, 13.9170607712314, 13.2395465266336, 12.4144749319018, 10.3616814416041, 7.45492120582938, 7.41020915333817, 5.95463830484538, 8.51668719748119, 10.3617671280304, 11.3917779165033, 14.5161968616257, 15.2965702127124,
                14.1938151811193, 12.2883375033199, 10.2332807137625, 9.72420097007558, 9.70404587260271, 9.61517164889351, 9.48281449742673, 9.60390030744858, 9.76347669993409, 10.1098237538979, 9.79269048666409, 9.66947512632316, 9.43885922333071, 9.02957240131156, 8.45717948386345, 7.67834155978451, 7.19795118078500, 7.54516808556351, 7.45581775811914, 7.44933582902282, 7.78281050992341, 7.17162220108461, 6.93343289542977, 6.66126598939153, 7.41516070970192, 10.0531666664738, 11.7934003203310, 12.9854562054309, 13.1015321045341, 12.3801067812709, 12.7522344712884, 12.8890372020072, 13.1049315796352, 12.6946763323699, 12.6687522996450, 12.2404156098617, 11.2948372331299, 10.9465520225028, 11.7499857060731, 13.1464795243996, 14.5680900920166, 15.3722915376963, 15.8650071273223, 16.0111196037331, 15.8434894096183, 14.6661060943420, 13.5320641605362, 10.9805035155184, 10.0697203318333, 9.94789433174294, 10.5805812754909, 10.9296793230961, 12.7729146719029, 14.0443313031141, 15.6059431828133, 15.7785593327313, 14.9942735571755, 14.0740493745515, 13.2386324395545, 13.1744209864272, 12.9680919375097, 12.8212188183503, 11.8271123577617, 10.1860487241057, 9.90299555810919, 9.45035426818248, 10.2579275831524, 9.09287952573391, 7.02502847170185, 5.69999817161393, 4.85330137614259, 3.95714774173592, 3.96339531623024, 3.81953606017176, 4.76110917631769, 5.19533875250250, 4.69965863437528, 4.63476817154174, 4.23237576313568, 3.87568862579534, 4.05334957800355, 4.67786806613271,
                5.13608592714243, 6.61156815010018, 7.69312860730659, 10.9285476815419, 12.7741216405368, 16.9310848747941, 19.9042529926297, 22.6992520413457, 21.7853776950678, 18.2821578487713, 15.5166444977946, 12.3104144780368, 11.2415222250337, 10.5297840954863, 11.2319557932696, 14.8084970559738, 15.9208007216514, 14.4911203435314, 13.6988651063319, 12.6720689085543, 9.09268541236026, 6.41613651838806, 5.44042960113373, 7.06672424178510, 8.15172479961183, 9.40964682609056, 12.3190884392800, 13.7651770083551, 14.0320480143886, 13.5825872951209, 12.4585985367224, 11.1645786459938, 10.2016842979464, 9.67224501815027, 9.56789313868511, 9.65937963919355, 10.3055908552804, 10.7624392259535, 10.5287928889639, 9.55784284197715, 8.64007762764842, 8.74278079867385, 9.03467688932846, 8.78787676352286, 8.58095484341961, 8.07548561246307, 7.92021713343132, 8.44205104358219, 8.41535873504409, 8.32917533841348, 8.33898583116514, 8.01683991467224, 7.63883256295487, 7.52191190241582, 8.02065848825845, 8.72348948386602, 9.46067600917032, 10.2308058964031, 11.0123496792412, 11.7900388289018, 13.6827058063392, 14.8803827383035, 15.3079550874626, 15.1757603938356, 13.6521669405299, 12.3071373708361, 12.1033786233450, 12.6019459983862, 12.7983671898617, 12.8151547598614, 12.0337766672096, 11.2571200644679, 9.65363736760393, 9.10902428908463, 9.14830846251446, 10.0509176586801, 9.12546901707046};

        // converting array to arraylist
        ArrayList<Double> tempDataList = new ArrayList<Double>();
        for (int i = 0; i < tempData.length; i++) {
            tempDataList.add(tempData[i]);
        }

        // test begins here

        double average = mean(tempDataList);
        double average1 = mean(tempData);
        double min = minimum(tempData);
        @SuppressWarnings("unused")
        double indx = indexOfValue(tempData, min);
        double max = maximum(tempData);
        System.out.printf("ArrayList Results are %f\n Array Results are %f", average, average1);
        System.out.printf("\n minimum is %f", min);
        System.out.printf("\n maximum is %f", max);
        int x = Doubles.indexOf(tempData, max);
        System.out.printf("\n maximum is %d", x);

        double[] diffres = new double[tempData.length - 1];
        diffres = diff(tempData);
        for (int i = 0; i < diffres.length; i++) {
            System.out.printf("\n Array is is %f", diffres[i]);
        }

    }

    /**
     * @return the t
     */
    public double[] getT() {
        return T;
    }

    /**
     * @param t the t to set
     */
    public void setT(double[] t) {
        T = t;
    }


}
