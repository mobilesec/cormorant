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
package at.usmile.gaitmodule.dataProcessing;

import android.util.Log;

import com.google.common.primitives.Doubles;

import org.JMathStudio.Exceptions.IllegalArgumentException;
import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;

import Jama.Matrix;
import at.usmile.gaitmodule.extras.DataStorageLocation;
import at.usmile.gaitmodule.segmentation.CycleInfo;
import at.usmile.gaitmodule.segmentation.EstimateGaitCycleLength;
import at.usmile.gaitmodule.segmentation.ExtractCycles;
import at.usmile.gaitmodule.segmentation.GaitParameters;
import at.usmile.gaitmodule.utils.ArrayManupulation;
import at.usmile.gaitmodule.utils.Interpolation;
import at.usmile.gaitmodule.utils.MeanNormalization;
import at.usmile.gaitmodule.utils.SGFilterModified;
import edu.umbc.cs.maple.utils.JamaUtils;
import at.usmile.gaitmodule.extras.LogMessage;

public class GaitDataProcessingSteps {

    private static final String TAG = "GaitDataProcessingSteps";
    // --Commented out by Inspection (07.08.2015 10:14):static File[] files;
    private static Matrix dataMatrixAndroid;
    private static double[] cycleLengths;

    private static Matrix remainedGaitCyclesTemplate;
    private static Matrix bestGaitCyclesTemplate;
    private static Matrix AllRemainedGaitCycles;
    private static Matrix AllBestGaitCycles;
    //private static Matrix processedGaitData;

    public static double[] getCycleLengths() {
        return cycleLengths;
    }

    public static ArrayList<Template> generateTemplate(String _dataPath, String _userName, boolean appendData) throws IOException, IllegalArgumentException {

        LogMessage.setStatus(TAG, "Generating Gait Template");
        Log.i(TAG, _dataPath);
        Template gaitTemplate;
        ArrayList<Template> templates = new ArrayList<>();
        File gaitTrainingDataFiles = new File(_dataPath.concat(_userName));
        File[] files = getFiles(gaitTrainingDataFiles);
        if (files != null) {
            Log.i(TAG, "Files found: " + files.length);
            if (createDirectories(_userName)) {
                for (File mfile : files) {
                    gaitTemplate = getGaitTemplateFromFile(mfile, _userName, appendData);
                    if (gaitTemplate != null) {
                        templates.add(gaitTemplate);
                    }
                }
            }
        }
        return templates;
    }

    // Function to check if training data Files are present
    private static File[] getFiles(File trainDataFilesLocation) {

        File[] files;
        if (trainDataFilesLocation.isDirectory()) {
            files = trainDataFilesLocation.listFiles();
            return files;
        } else {
            return null;
        }
    }

    private static Template getGaitTemplateFromFile(File file, String _userName, boolean appendData) throws IOException, IllegalArgumentException {

        Log.i(TAG, "Loading RAW" + file.toString());
        // since we store our recording data in file on SDCard first we load that data
        loadRawData(file);

        Matrix resultantDataMatrix = JamaUtils.getcol(dataMatrixAndroid, 1);
        // Find active segments in that loaded Data
        int[][] limits = getActiveSegments(resultantDataMatrix);
        if (limits != null) {
            // Process each active segment
            return processActiveSegments(limits, _userName, appendData);

        } else {
            //If active segment detection is failed we delete processed and Raw data files
            DataStorageLocation.deleteDirectory(new File(DataStorageLocation.TRAIN_RAW_DATA_PATH.concat(_userName)));
            DataStorageLocation.deleteDirectory(new File(DataStorageLocation.TRAIN_PROCESSED_DATA_PATH.concat(_userName)));
            return null;
        }
    }

    private static int[][] getActiveSegments(Matrix resultantDataMatrix) {

        LogMessage.setStatus(TAG, "Detecting Walk Segments");
        int[][] limits = WalkSeparator.getAllActiveSegments(resultantDataMatrix);

        if (limits == null) {

            LogMessage.setStatus(TAG, "Walk is too short to find active segment");

            return null;

        } else {

            return limits;
        }

    }

    private static Template processActiveSegments(int[][] limits, String _userName, boolean appendData) throws IOException, IllegalArgumentException {

        // These files are created to store step by step results
        File remainedDataFile = new File(DataStorageLocation.TRAIN_PROCESSED_DATA_PATH.concat(_userName).concat("/RemainedGaitCycles/"));
        File bestGaitCycles = new File(DataStorageLocation.TRAIN_PROCESSED_DATA_PATH.concat(_userName).concat("/BestGaitCycles/"));
        File allBestGaitCycles = new File(DataStorageLocation.ALL_TEMPLATES_PATH.concat("/BestGaitCycles/"));
        File allRemainedGaiCycles = new File(DataStorageLocation.ALL_TEMPLATES_PATH.concat("/RemainedGaitCycles"));

        Template gaitTemplate = null;

        // How many segments we have detected
        int segmentsCount = limits[0].length;
        int[] upper = limits[1];
        int[] lower = limits[0];
        cycleLengths = new double[segmentsCount];

        if (segmentsCount <= 0) {

            LogMessage.setStatus(TAG, "No Walk Segment is detected");

            if (!appendData) {

                DataStorageLocation.deleteDirectory(new File(DataStorageLocation.TRAIN_RAW_DATA_PATH.concat(_userName)));
            }

            return null;
        } else {
            // Here we should add user Name to the list instead of adding in main activity
            //LogMessage.setStatus(TAG, "" + segmentsCount + " Walk segments are detected");
        }

        for (int i = 0; i < segmentsCount; i++) {

            // Here we will process each Segment
            LogMessage.setStatus(TAG, "Working on segment #: " + i);

            // Now we pick segment from the data matrix
            Matrix segmentToProcess = dataMatrixAndroid.getMatrix(lower[i], upper[i], 0, dataMatrixAndroid.getColumnDimension() - 1);

            // Now we process picked active walk segment
            CycleInfo ci = processEachSegment(segmentToProcess, i, appendData, _userName);

            if (ci != null) {
                remainedGaitCyclesTemplate = ci.getRemainedCycles();
                // We are not using best gait cycles
                //bestGaitCyclesTemplate = new Matrix(ci.getBestCycle(), 1).transpose(); // to keep remained and best cycles in same format


                // if there are more than one active segment then we must append
                // extracted best and remained gait cycles in one matrix
                if (i == 0) {
                    AllRemainedGaitCycles = remainedGaitCyclesTemplate;
                    //we are not using best gait cycles
                    //AllBestGaitCycles = bestGaitCyclesTemplate;

                    // Code for testing purpose only
                    // =============================
                    File prRemained = new File(remainedDataFile.getAbsolutePath().concat("/Remain_").concat(_userName).concat("_" + i).concat(".txt"));
                    writeMatrix2File(prRemained, remainedGaitCyclesTemplate);

                    //File prBest = new File(bestGaitCycles.getAbsolutePath().concat("/Best_").concat(_userName).concat("_" + i).concat(".txt"));
                    //writeMatrix2File(prBest, bestGaitCyclesTemplate);
                    // =============================

                } else {
                    AllRemainedGaitCycles = JamaUtils.columnAppend(AllRemainedGaitCycles, remainedGaitCyclesTemplate);
                    //we are not using best gait cycles
                    //AllBestGaitCycles = JamaUtils.columnAppend(AllBestGaitCycles, bestGaitCyclesTemplate);
                }

                gaitTemplate = new Template(AllRemainedGaitCycles);
            }
        }

        // Code only for testing purpose
        // =============================
        File allRemained = new File(allRemainedGaiCycles.getAbsolutePath().concat("/").concat(_userName).concat(".txt"));
        writeMatrix2File(allRemained, AllRemainedGaitCycles);

        //File allBest = new File(allBestGaitCycles.getAbsolutePath().concat("/").concat(_userName).concat(".txt"));
        //writeMatrix2File(allBest, AllBestGaitCycles);
        // =============================
        return gaitTemplate;
    }

    public static CycleInfo processEachSegment(Matrix segmentToProcess, int segmentNumber, boolean appendData, String _userName) throws IOException, IllegalArgumentException {

        Log.i(TAG, "Processing Raw Data");
        LogMessage.setStatus(TAG, "Postprocessing Segment #" + segmentNumber);
        Matrix processedGaitData = GaitDataProcessingSteps.processWalkData(segmentToProcess);

        // Only for testing purpose
        // ===========================================================
        File processedFile = new File(DataStorageLocation.TRAIN_PROCESSED_DATA_PATH.concat(_userName).concat("/ProcessedData/"));
        File file1 = new File(processedFile.getAbsolutePath().concat("/").concat(_userName).concat("_").concat("" + segmentNumber).concat(".txt"));
        writeMatrix2File(file1, processedGaitData);
        // ============================================================

        //LogMessage.setStatus(TAG, "Estimating Gait Cycle Length");

        Log.i(TAG, "Estimating Gait Cycle Length");
        EstimateGaitCycleLength myEstimator = new EstimateGaitCycleLength(JamaUtils.getcol(processedGaitData, 1).getColumnPackedCopy());
        double gaitCycleLength = myEstimator.calculateEstimatedCycleLength();
        Log.i("Estimated CycleLength =", "" + gaitCycleLength);
        LogMessage.setStatus(TAG, "CycleLength is =" + gaitCycleLength);
        if (gaitCycleLength == -1) {
            LogMessage.setStatus(TAG, "Failed to estimate gait cycle length");
            if (!appendData) {
                DataStorageLocation.deleteDirectory(new File(DataStorageLocation.TRAIN_PROCESSED_DATA_PATH.concat(_userName)));
            }
            return null;
        }
        cycleLengths[segmentNumber] = gaitCycleLength;

        // While testing in android studio I get exception only once
        // could not reproduce it, It was caused by currentSegment array
        // length was -1
        if (null == myEstimator.detectGaitCycleStarts()) {
            //LogMessage.setStatus(TAG, "Failed to detect any gait cycel");
            return null;
        }

        int[] gaitCyclesStart = myEstimator.detectGaitCycleStarts();
        LogMessage.setStatus(TAG, "Now Extracting Gait Template from walk # " + segmentNumber);
        Log.i(TAG, "Extracting Template");

        ExtractCycles gaitCycleExtractor = new ExtractCycles(gaitCyclesStart);
        gaitCycleExtractor.extractAllGaitCyclesFromWalk(JamaUtils.getcol(processedGaitData, 1).getColumnPackedCopy());
        CycleInfo ci2 = gaitCycleExtractor.removeUnusualGaitCycles();

        if (ci2.getKeptCycleIDs().length == 0 || ci2.getRemovedCycleIDs().length == gaitCycleExtractor.getGaitCycleMatrix().getColumnDimension()) {
            if (!appendData) {
                LogMessage.setStatus(TAG, "All of your gait cycles are different and all are removed");
                DataStorageLocation.deleteDirectory(new File(DataStorageLocation.TRAIN_RAW_DATA_PATH.concat(_userName)));
                DataStorageLocation.deleteDirectory(new File(DataStorageLocation.TRAIN_PROCESSED_DATA_PATH.concat(_userName)));
            }
            return null;
        }

        return ci2;

    }

    private static boolean createDirectories(String _userName) {

        File processedFile = new File(DataStorageLocation.TRAIN_PROCESSED_DATA_PATH.concat(_userName).concat("/ProcessedData/"));
        File remainedDataFile = new File(DataStorageLocation.TRAIN_PROCESSED_DATA_PATH.concat(_userName).concat("/RemainedGaitCycles/"));
        File bestGaitCycles = new File(DataStorageLocation.TRAIN_PROCESSED_DATA_PATH.concat(_userName).concat("/BestGaitCycles/"));
        File allBestGaitCycles = new File(DataStorageLocation.ALL_TEMPLATES_PATH.concat("/BestGaitCycles/"));
        File allRemainedGaiCycles = new File(DataStorageLocation.ALL_TEMPLATES_PATH.concat("/RemainedGaitCycles"));

        if (processedFile.exists() && remainedDataFile.exists() && bestGaitCycles.exists() && allBestGaitCycles.exists() && allRemainedGaiCycles.exists()) {
            return true;
        }
        if (!processedFile.exists()) {

            boolean success = processedFile.mkdirs();
            if (!success) {
                LogMessage.setStatus(TAG, "Directory for processedData creation Failed remainedData");
                return false;
            }
        }

        if (!remainedDataFile.exists()) {
            boolean success = remainedDataFile.mkdirs();
            if (!success) {
                LogMessage.setStatus(TAG, "Directory for remainedData creation Failed remainedData");
                return false;
            }
        }
        if (!bestGaitCycles.exists()) {
            boolean success = bestGaitCycles.mkdirs();
            if (!success) {
                LogMessage.setStatus(TAG, "Directory for bestGaitData creation Failed bestData");
                return false;
            }
        }
        if (!allBestGaitCycles.exists()) {
            boolean success = allBestGaitCycles.mkdirs();
            if (!success) {
                LogMessage.setStatus(TAG, "Directory for allBestGaitCycles creation Failed allBestData");
                return false;
            }
        }
        if (!allRemainedGaiCycles.exists()) {
            boolean success = allRemainedGaiCycles.mkdirs();
            if (!success) {
                LogMessage.setStatus(TAG, "Directory aalRemainedGaitCycles creation Failed allRemainedData");
                return false;
            }
        }
        return true;
    }


    private static void writeMatrix2File(File file, Matrix mat) throws IOException {

        //mat.print(5,5);

        PrintWriter pr = new PrintWriter(file, "UTF-8");
        mat.print(pr, 5, 5);
        pr.flush();
        pr.close();
    }

    public static Template gaitTestTemplateGeneration(Matrix _testDataMatrix) throws IOException, IllegalArgumentException {
        LogMessage.setStatus(TAG, "Processing Test Data");

        // computed gait parameters
        LogMessage.setStatus(TAG, "" + GaitParameters.getInterpolationFrequency() + ":" + GaitParameters.getSamplingFrequency() + ":" + GaitParameters.getBaselinesize() + ":" + GaitParameters.getThresholdToRemoveUnusualGaitCycles());
        Template gaitTestTemplate = null;

        // _testDataMatrix contains two columns, timestamps are in column 0 and data in column 1
        Matrix resultantDataMatrix = JamaUtils.getcol(_testDataMatrix, 1);

        // here we find out active Segments start and end in data
        int[][] limits = getActiveSegments(resultantDataMatrix);

        // If any active segment is found we process that active segment else we return template = null;
        if (limits != null) {
            gaitTestTemplate = processTestActiveSegments(limits, _testDataMatrix);
        }

        return gaitTestTemplate;
    }

    private static Template processTestActiveSegments(int[][] limits, Matrix _testDataMatrix) throws IOException, IllegalArgumentException {

        Template gaitTemplate = null;
        Matrix remainedGaitCyclesTemplate;
        Matrix AllRemainedGaitCycles = null;
        // How many active segments are detected
        int segmentsCount = limits[0].length;
        // End of every active segments is stored in upped and start is stored in lower
        int[] upper = limits[1];
        int[] lower = limits[0];

        // return null if segment count is 0
        if (segmentsCount <= 0) {

            LogMessage.setStatus(TAG, "No Walk Segment is detected");

            return null;
        } else {
            //LogMessage.setStatus(TAG, "" + segmentsCount + " We detected walk segments");
        }

        for (int i = 0; i < segmentsCount; i++) {

            // Here we will process each Segment
            // pick submatrix from _testDataMatrix as per limits of active segment
            LogMessage.setStatus(TAG, "Working on segment #: " + i);
            Matrix segmentToProcess = _testDataMatrix.getMatrix(lower[i], upper[i], 0, _testDataMatrix.getColumnDimension() - 1);

            // Now we process picked sub matrix
            CycleInfo ci = processEachTestSegment(segmentToProcess, i);
            if (ci != null) {
                remainedGaitCyclesTemplate = ci.getRemainedCycles();

                // If we are interested in using best Gait cycles only, results will be worst
                // bestGaitCyclesTemplate = new Matrix(ci.getBestCycle(), 1).transpose(); // to keep remained and best cycles in same format

                // if there are more than one active segment then we must append
                // extracted best and remained gait cycles in one matrix
                if (i == 0) {
                    AllRemainedGaitCycles = remainedGaitCyclesTemplate;
                    // AllBestGaitCycles = bestGaitCyclesTemplate;

                } else {
                    AllRemainedGaitCycles = JamaUtils.columnAppend(AllRemainedGaitCycles, remainedGaitCyclesTemplate);

                    //AllBestGaitCycles = JamaUtils.columnAppend(AllBestGaitCycles, bestGaitCyclesTemplate);
                }

                gaitTemplate = new Template(AllRemainedGaitCycles);
            }
        }

        // Finally if all steps are clear will have template
        return gaitTemplate;
    }

    public static CycleInfo processEachTestSegment(Matrix segmentToProcess, int segmentNumber) throws IllegalArgumentException {

        Log.i(TAG, "Processing Raw Data");
        LogMessage.setStatus(TAG, "Postprocessing Segment #" + segmentNumber);
        Matrix processedGaitData = GaitDataProcessingSteps.processWalkData(segmentToProcess);

        //LogMessage.setStatus(TAG, "Estimating Gait Cycle Length");
        Log.i(TAG, "Estimating Gait Cycle Length");
        Long ta = System.currentTimeMillis();
        EstimateGaitCycleLength myEstimator = new EstimateGaitCycleLength(JamaUtils.getcol(processedGaitData, 1).getColumnPackedCopy());
        double gaitCycleLength = myEstimator.calculateEstimatedCycleLength();
        Log.i("Estimated CycleLength =", "" + gaitCycleLength);
        LogMessage.setStatus(TAG, "CycleLength is =" + gaitCycleLength);

        if (gaitCycleLength == -1) {
            LogMessage.setStatus(TAG, "Failed to estimate gait cycle length");
            return null;
        }
        // While testing in android studio I get exception only once
        // could not reproduce it, It was caused by currentSegment array
        // length was -1
        if (null == myEstimator.detectGaitCycleStarts()) {
            LogMessage.setStatus(TAG, "Failed to detect any gait cycle");
            return null;
        }
        int[] gaitCyclesStart = myEstimator.detectGaitCycleStarts();
        LogMessage.setStatus(TAG, "Now Extracting Gait Template from walk # " + segmentNumber);
        Log.i(TAG, "Extracting Template");

        ExtractCycles gaitCycleExtractor = new ExtractCycles(gaitCyclesStart);
        gaitCycleExtractor.extractAllGaitCyclesFromWalk(JamaUtils.getcol(processedGaitData, 1).getColumnPackedCopy());
        CycleInfo ci2 = gaitCycleExtractor.removeUnusualGaitCycles();
        Log.i("Cycle Extraction Time: ", "" + (System.currentTimeMillis() - ta));
        if (ci2.getKeptCycleIDs().length == 0 || ci2.getRemovedCycleIDs().length == gaitCycleExtractor.getGaitCycleMatrix().getColumnDimension()) {
            LogMessage.setStatus(TAG, "Your walk is not as human walk");
            return null;
        }

        return ci2;

    }

    public static Matrix getAllRemainedGaitCycles() {
        return AllRemainedGaitCycles;
    }

    public static void setAllRemainedGaitCycles(Matrix allRemainedGaitCycles) {
        AllRemainedGaitCycles = allRemainedGaitCycles;
    }

    private static void loadRawData(File mfile) throws IOException {

        ArrayList<Double> timeStamps = new ArrayList<>();
        ArrayList<Double> resultantData = new ArrayList<>();


        BufferedReader br = new BufferedReader(new FileReader(mfile.getPath()));

        try {
            String line;
            while ((line = br.readLine()) != null) {
                String tokens[];

                // Just for testing purpose
                tokens = line.split("\t");
                if (tokens.length == 2) {
                    timeStamps.add(Double.parseDouble(tokens[0]));
                    resultantData.add(Double.parseDouble(tokens[1]));
                }
                // When we are reading timeStamps, X, Y, Z, and magnitude data
                if (tokens.length == 5) {
                    timeStamps.add(Double.parseDouble(tokens[0]));
                    resultantData.add(Double.parseDouble(tokens[4]));

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            br.close();
            setDataMatrixAndroid(timeStamps, resultantData);
        }

    }

    public static Matrix loadRawData1(File mfile) throws IOException {

        ArrayList<Double> timeStamps = new ArrayList<>();
        ArrayList<Double> resultantData = new ArrayList<>();


        BufferedReader br = new BufferedReader(new FileReader(mfile.getPath()));

        try {
            String line;
            while ((line = br.readLine()) != null) {
                String tokens[];

                // Just for testing purpose
                tokens = line.split("\t");
                if (tokens.length == 2) {
                    timeStamps.add(Double.parseDouble(tokens[0]));
                    resultantData.add(Double.parseDouble(tokens[1]));
                }
                // When we are reading timeStamps, X, Y, Z, and magnitude data
                if (tokens.length == 5) {
                    timeStamps.add(Double.parseDouble(tokens[0]));
                    resultantData.add(Double.parseDouble(tokens[4]));

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            br.close();

        }

        return setDataMatrixAndroid1(timeStamps, resultantData);
    }


    public static Matrix setDataMatrixAndroid1(ArrayList<Double> _timeStamps, ArrayList<Double> _resultantData) {

        double[] timeStamps = Doubles.toArray(_timeStamps);
        double[] resultantData = Doubles.toArray(_resultantData);
        int tLen = timeStamps.length;
        int rDLen = resultantData.length;
        Matrix dataMatrixAndroid = null;
        // Case if timestamps and data matrix has equal length
        if (tLen == rDLen) {
            dataMatrixAndroid = new Matrix(resultantData.length, 2);
            for (int i = 0; i < tLen; i++) {

                dataMatrixAndroid.set(i, 0, timeStamps[i]);
                dataMatrixAndroid.set(i, 1, resultantData[i]);
            }
        }

        // Case if time stamps and data matrix has unequal length
        if (tLen != rDLen) {

            // We initialize matrix rows equal to the smaller of both time stamps and data length
            if (tLen < rDLen) {
                dataMatrixAndroid = new Matrix(timeStamps.length, 2);

                for (int i = 0; i < tLen; i++) {
                    dataMatrixAndroid.set(i, 0, timeStamps[i]);
                    dataMatrixAndroid.set(i, 1, resultantData[i]);
                }
            }
            //Here we initialize matrix rows equal to the smaller of both time stamps and datal length
            if (tLen > rDLen) {

                dataMatrixAndroid = new Matrix(resultantData.length, 2);
                for (int i = 0; i < rDLen; i++) {
                    dataMatrixAndroid.set(i, 0, timeStamps[i]);
                    dataMatrixAndroid.set(i, 1, resultantData[i]);
                }
            }
        }

        return dataMatrixAndroid;
    }


    public static void setDataMatrixAndroid(ArrayList<Double> _timeStamps, ArrayList<Double> _resultantData) {

        double[] timeStamps = Doubles.toArray(_timeStamps);
        double[] resultantData = Doubles.toArray(_resultantData);
        int tLen = timeStamps.length;
        int rDLen = resultantData.length;

        // Case if timestamps and data matrix has equal length
        if (tLen == rDLen) {
            dataMatrixAndroid = new Matrix(resultantData.length, 2);
            for (int i = 0; i < tLen; i++) {

                dataMatrixAndroid.set(i, 0, timeStamps[i]);
                dataMatrixAndroid.set(i, 1, resultantData[i]);
            }
        }

        // Case if time stamps and data matrix has unequal length
        if (tLen != rDLen) {

            // We initialize matrix rows equal to the smaller of both time stamps and data length
            if (tLen < rDLen) {
                dataMatrixAndroid = new Matrix(timeStamps.length, 2);

                for (int i = 0; i < tLen; i++) {
                    dataMatrixAndroid.set(i, 0, timeStamps[i]);
                    dataMatrixAndroid.set(i, 1, resultantData[i]);
                }
            }
            //Here we initialize matrix rows equal to the smaller of both time stamps and datal length
            if (tLen > rDLen) {

                dataMatrixAndroid = new Matrix(resultantData.length, 2);
                for (int i = 0; i < rDLen; i++) {
                    dataMatrixAndroid.set(i, 0, timeStamps[i]);
                    dataMatrixAndroid.set(i, 1, resultantData[i]);
                }
            }
        }
    }


    /**
     * All steps of data processing are performed here.
     *
     * @param dataMatrix
     * @return
     */
    private static Matrix processWalkData(Matrix dataMatrix) {

        //LogMessage.setStatus(TAG, "Starting Data processing");
        //Picktime stamps
        double[] timeStamps = JamaUtils.getcol(dataMatrix, 0).getColumnPackedCopy();

        double minVal = ArrayManupulation.minimum(computeAbsDiffArray(ArrayManupulation.diff(timeStamps)));
        if (minVal == 0.0) {

            dataMatrix = removeDoubleValues(timeStamps, dataMatrix);
            timeStamps = JamaUtils.getcol(dataMatrix, 0).getColumnPackedCopy();

        }

        double[] t1 = new double[timeStamps.length];
        int timeStampLen = timeStamps.length;
        for (int i = 0; i < timeStampLen; i++) {
            t1[i] = timeStamps[i] - timeStamps[0];
        }
        double samplingFreq = GaitParameters.getSamplingFrequency();
        Log.i(TAG, "" + samplingFreq);
        // This function below is used for preparing new data steps which are according to the sampling frequency...
        double[] timeIntrep = Interpolation.prepareForInterpolation(timeStamps, GaitParameters.getSamplingFrequency());

        // Prepare a new matrix to hold the preprocessed data... during interpolation number of sample may change...
        // but number of axis will always stay the same
        Matrix processedDataMatrix = new Matrix(timeIntrep.length, dataMatrix.getColumnDimension());

        JamaUtils.setcol(processedDataMatrix, 0, timeIntrep);

        // This loops is just process all the columns of the Matrix where each column is basically a data vector...
        // Col 1 = TimeStamps, Col 2 = X Axis data, Col 3 = Y Axis Data, Col 4 is Z Axis data and Col 5 is XYZ Resultant data...
        for (int i = 1; i < dataMatrix.getColumnDimension(); i++) {
            double[] data = JamaUtils.getcol(dataMatrix, i).getColumnPackedCopy();

            double[] interpolatedData = Interpolation.interpLinear(t1, data, timeIntrep);

            double[] zeroNormalizedData = zeroNormalizedWalkData(interpolatedData);

            SGFilterModified sgFilter = new SGFilterModified(11, 11);

            double[] coeffs = SGFilterModified.computeSGCoefficients(11, 11, 3);
            double[] smoothSignal = sgFilter.smooth(zeroNormalizedData, coeffs);

            JamaUtils.setcol(processedDataMatrix, i, smoothSignal);
        }

        timeIntrep = null;
        return processedDataMatrix;
    }

    /**
     * This method is used to compute abs difference array.
     *
     * @param _diffArray
     * @return
     */
    private static double[] computeAbsDiffArray(double[] _diffArray) {

        int arrayLength = _diffArray.length;
        double[] absDiffArray = new double[_diffArray.length];
        for (int j = 0; j < arrayLength; j++) {
            absDiffArray[j] = Math.abs(_diffArray[j]);
        }
        return absDiffArray;
    }

    /**
     * We must remove double values before interpolation is performed
     *
     * @param _timeStamps
     * @param _dataMatrix
     * @return
     */
    private static Matrix removeDoubleValues(double[] _timeStamps, Matrix _dataMatrix) {

        while (ArrayManupulation.minimum(computeAbsDiffArray(ArrayManupulation.diff(_timeStamps))) == 0.0) {

            double[] di = ArrayManupulation.diff(_timeStamps);
            int indx = 0;
            int j = 1;
            int diLen = di.length;
            for (int i = 0; i < diLen; i++) {

                if (di[i] == 0.0) {
                    indx = i;
                }
            }
            if (indx + j <= diLen) {
                while (di[indx + j] == 0.0) {
                    j = j + 1;

                    if (indx + j > diLen) {
                        break;
                    }
                }
            }
            // Get the data values
            Matrix da = _dataMatrix.getMatrix(indx, indx + j, 0, _dataMatrix.getColumnDimension() - 1);
            Matrix meanDa = JamaUtils.colsum(da).times(1.0 / da.getRowDimension());
            int deleteRows = indx;
            while (deleteRows < indx + j) {
                _dataMatrix = JamaUtils.deleteRow(_dataMatrix, indx + 1);
                _timeStamps = ArrayUtils.remove(_timeStamps, indx + 1);
                deleteRows = deleteRows + 1;
            }

            JamaUtils.setrow(_dataMatrix, indx, meanDa);
        }

        return _dataMatrix;
    }

    private static double[] zeroNormalizedWalkData(double[] _data) {

        return MeanNormalization.zeroMeanNormalization(_data);
    }

}