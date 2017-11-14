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

import java.util.ArrayList;

import Jama.Matrix;
import at.usmile.gaitmodule.extras.DTWDistance;
import at.usmile.gaitmodule.utils.ArrayManupulation;
import at.usmile.gaitmodule.utils.Interpolation;
import edu.umbc.cs.maple.utils.JamaUtils;

/**
 * @author Muhammad Muaaz
 * @version 1.0
 */

public class ExtractCycles {

    // we initalize this class by pass it the gait cycle starts...
    private int[] gaitCycleStarts;
    private double[] walkSignal;
    private double gaitCycleLength;
    private Matrix gaitAllCycleMatrix;
    private ArrayList<Integer> keptCycleIDsArrayList;
    private ArrayList<Integer> removedCycleIDsArrayList;
    private Matrix remainingGaitCycles;
    private Matrix removedGaitCycles;
    private double[] closestCycle;
    private CycleInfo ci;
    //private static final int SAMPLING_FREQUNECY = 100;
    //private double THRESHOLD_TO_REMOVE_Unusual_Cycles = 50;

    /**
     * Constructor
     */
    public ExtractCycles(int[] _gaitCyclesStarts) {

        this.gaitCycleStarts = _gaitCyclesStarts;
    }


    public ExtractCycles(int[] _gaitCyclesStart, double[] _walk) {

        this.gaitCycleStarts = _gaitCyclesStart;
        this.walkSignal = _walk;
    }


    public void setGaitCycle(double[] _gaitWalk) {

        walkSignal = _gaitWalk;
    }

    public double getGaitCycleLength() {

        return gaitCycleLength;
    }

    public void setGaitCycleLength(double _gaitCycleLength) {

        gaitCycleLength = _gaitCycleLength;
    }

    private void setGaitAllCycleMatrix(Matrix _gaitCycles) {
        gaitAllCycleMatrix = _gaitCycles;
        //gaitAllCycleMatrix.print(10, 4);
    }

    public Matrix getGaitCycleMatrix() {
        return gaitAllCycleMatrix;
    }

    // Method to extract gait cycles from gait walk
    // This will store the those gait cycles in matrix
    // In order to extract gait cycles we must pass walk



    public void extractAllGaitCyclesFromWalk(double[] _gaitWalk) {

        setGaitCycle(_gaitWalk);
        Matrix gaitCycles = new Matrix(GaitParameters.getSamplingFrequency(), gaitCycleStarts.length - 1);

        for (int i = 0; i < gaitCycleStarts.length - 1; i++) {
            int cycleStart = gaitCycleStarts[i];
            int cycleEnd = gaitCycleStarts[i + 1];
            int cycleLength = cycleEnd - cycleStart;

            double[] temp = new double[cycleLength];
            System.arraycopy(walkSignal, cycleStart, temp, 0, cycleLength);
            // pass this tmep array for interpolation
            // Once array will be interpolated we will store it to the matrix
            // each column of the matrix is basically an interpolated gait cycle
            double[] interpolatedTemp = interpolateCycle(temp);
            JamaUtils.setcol(gaitCycles, i, interpolatedTemp);
        }
        setGaitAllCycleMatrix(gaitCycles);
    }

    private double[] interpolateCycle(double[] _temp) {

        //Interpolation interp1 = new Interpolation();
        int orignalLength = _temp.length;
        if (orignalLength < 3) {

            throw new RuntimeException("To less samples to interpolate");
        }
        // prepare old steps...
        double[] originalSteps = new double[orignalLength];
        for (int i = 0; i < orignalLength; i++) {
            originalSteps[i] = (float) (i) / (orignalLength - 1);

        }

        //prepare new steps...
        double[] newSteps = new double[GaitParameters.getSamplingFrequency()];

        for (int i = 0; i < GaitParameters.getSamplingFrequency(); i++) {

            newSteps[i] = (double) i / GaitParameters.getSamplingFrequency();

        }

        return Interpolation.interpLinear(originalSteps, _temp, newSteps);
    }


    /**
     * This Function is to compute average gait cycles.
     *
     * @return A double[] array containing averageGaitCycle
     * @author Muhammad
     */
    public double[] getAverageCycle(Matrix _gaitCycleMatrix) {

        int rows = _gaitCycleMatrix.getRowDimension(); // get # of rows
        int cols = _gaitCycleMatrix.getColumnDimension();//get # of columns
        double[] averageGaitCycle = new double[rows]; // array to hold average cycle
        for (int i = 0; i < rows; i++) // loop through all rows
        {
            averageGaitCycle[i] = JamaUtils.rowsum(_gaitCycleMatrix, i) / cols; //compute rowsum and devide it by # of clcumns and store it to averageGaitCycle
        }
        ArrayManupulation.printArray(averageGaitCycle); // To print averagerGaitCycle
        return averageGaitCycle;
    }



    public CycleInfo removeUnusualGaitCycles() {

        // get number of gaitcycles
        int numCycles = gaitAllCycleMatrix.getColumnDimension();
        //look for the cycle which is closest to all others

        // distance Matrix to store distances between the detected gait cycles...
        Matrix distMat = new Matrix(numCycles, numCycles);

        // Populating the distance matrix....
        for (int i = 0; i < numCycles; i++) {

            //Pick cycle
            double[] cycle11D = JamaUtils.getcol(gaitAllCycleMatrix, i).getColumnPackedCopy();

            for (int j = i; j < numCycles; j++) {

                if (i == j) {
                    // distance between two same cycles will be zeros...
                    double distance = 0.0;
                    distMat.set(i, j, distance);
                } else {
                    // We pick each column and from that column we make Array...
                    double[] cycle21D = JamaUtils.getcol(gaitAllCycleMatrix, j).getColumnPackedCopy();

                    // Compute distance between the cycels...
                    DTWDistance dtw = new DTWDistance(cycle11D, cycle21D); // calling the Dynamic warping distance matric
                    double distance = dtw.getAccumulatedDistance();

                    distMat.set(i, j, distance);
                    // Since dtw(i,j) = dtw(j,i) so we set it in the matrix...
                    distMat.set(j, i, distance);
                }
            }
        }
        //System.out.println("Distance between gait Cycles..........");
        //distMat.print(10, 4);

        // Delete the cycles which have a distance of at least 'thres' to at least half
        // of the cycles. We maintain two arraylists, removedCycleIDsArrayList and keptCycleIDsArrayList.
        // Our distance matrix is of N*N where N is number of cycles. for example if N = 41 we will have
        // a matrix of dimension 41*41. In our distance matrix we will have same number of rows and same
        // number number of columns. We walk columnwise for each selected row and check if distance is
        // above the threshold value. If distance is above that threshold value we set it to flag 1 for
        // that perticular cell. Then we sum all those which are greater than threhsold.

        //		boolean enoughCycles = false;
        //		while(!enoughCycles) {

		/*		removedCycleIDsArrayList = new ArrayList<>();
        keptCycleIDsArrayList = new ArrayList<>();
		for(int i = 0; i < distMat.getRowDimension(); i++) 
		{
			double [] dist = new double [distMat.getRowDimension()];
			for(int j = 0; j < distMat.getColumnDimension(); j++){

				if (distMat.get(i,j) > GaitParameters.getThresholdToRemoveUnusualGaitCycles()) {

					dist[j] = -1.0;
				}	
			}
			double sum = 0.0;
			for (int k = 0; k < dist.length; k++) {
				sum = sum + Math.abs(dist[k]); 
			}

			System.out.println("The sum of col is:"+ i + "="+sum);
			if(sum <= (distMat.getColumnDimension()*1.0/2)) {

				keptCycleIDsArrayList.add(i);


			}else {
				removedCycleIDsArrayList.add(i);
			}
		}


		remainingGaitCycles = new Matrix(gaitAllCycleMatrix.getRowDimension(),keptCycleIDsArrayList.size());
		//Check your printed and remained cycles....
		for (int k = 0; k <keptCycleIDsArrayList.size();k++) {
			JamaUtils.setcol(remainingGaitCycles,k,JamaUtils.getcol(gaitAllCycleMatrix,keptCycleIDsArrayList.get(k)));
		}

		removedGaitCycles = new Matrix(gaitAllCycleMatrix.getRowDimension(),removedCycleIDsArrayList.size());
		for (int r = 0; r < removedCycleIDsArrayList.size();r++) {
			JamaUtils.setcol(removedGaitCycles,r,JamaUtils.getcol(gaitAllCycleMatrix,removedCycleIDsArrayList.get(r)));
		}*/

        // This portion of code is used to determine the closest gait cycle from the remaining gait cycles...
        //int numRemainingCycles = remainingGaitCycles.getColumnDimension();
        //if (numRemainingCycles>0){
		/*	double [] distSum = new double [numRemainingCycles];
			if (numRemainingCycles > 2) {
				for (int i = 0; i < numRemainingCycles; i++) {
					for (int j = i+1; j <numRemainingCycles; j++) {

						double []c1 = JamaUtils.getcol(gaitAllCycleMatrix,i).getColumnPackedCopy(); 
						double []c2 = JamaUtils.getcol(gaitAllCycleMatrix,j).getColumnPackedCopy();

						DTWDistance mDtw = new DTWDistance(c1, c2);
						distSum[i]+= mDtw.getAccumulatedDistance();
						distSum[j]+= mDtw.getAccumulatedDistance();
					}	
				}			
				double minDis = ArrayManupulation.minimum(distSum);
				int minDisID = ArrayManupulation.indexOfValue(distSum, minDis);
				closestCycle = ArrayManupulation.get1DArrayFrom2DArray(JamaUtils.getcol(remainingGaitCycles,minDisID).getArray());
				enoughCycles = true;
			}else {
				GaitParameters.setThresholdToRemoveUnusualGaitCycles(GaitParameters.getThresholdToRemoveUnusualGaitCycles()+5);
			}*/
        //}
        // this matrix will be initalized once all the cycles will be removed


        checkifCycleRemained(distMat, 0);
        if (keptCycleIDsArrayList.size() == 2) {
            int thresholdIncrement = 10;
            checkifCycleRemained(distMat, thresholdIncrement);

        }

        ci = new CycleInfo(remainingGaitCycles, removedGaitCycles, keptCycleIDsArrayList, removedCycleIDsArrayList);
        //}
        return ci;

    }

    public int checkifCycleRemained(Matrix distMat, int thrseholdIncrement) {

        removedCycleIDsArrayList = new ArrayList<>();
        keptCycleIDsArrayList = new ArrayList<>();
        for (int i = 0; i < distMat.getRowDimension(); i++) {
            double[] dist = new double[distMat.getRowDimension()];
            for (int j = 0; j < distMat.getColumnDimension(); j++) {

                if (distMat.get(i, j) > GaitParameters.getThresholdToRemoveUnusualGaitCycles() + thrseholdIncrement) {

                    dist[j] = -1.0;
                }
            }
            double sum = 0.0;
            for (int k = 0; k < dist.length; k++) {
                sum = sum + Math.abs(dist[k]);
            }

            System.out.println("The sum of col is:" + i + "=" + sum);
            if (sum <= (distMat.getColumnDimension() * 1.0 / 2)) {

                keptCycleIDsArrayList.add(i);


            } else {
                removedCycleIDsArrayList.add(i);
            }
        }


        remainingGaitCycles = new Matrix(gaitAllCycleMatrix.getRowDimension(), keptCycleIDsArrayList.size());
        //Check your printed and remained cycles....
        for (int k = 0; k < keptCycleIDsArrayList.size(); k++) {
            JamaUtils.setcol(remainingGaitCycles, k, JamaUtils.getcol(gaitAllCycleMatrix, keptCycleIDsArrayList.get(k)));
        }

        removedGaitCycles = new Matrix(gaitAllCycleMatrix.getRowDimension(), removedCycleIDsArrayList.size());
        for (int r = 0; r < removedCycleIDsArrayList.size(); r++) {
            JamaUtils.setcol(removedGaitCycles, r, JamaUtils.getcol(gaitAllCycleMatrix, removedCycleIDsArrayList.get(r)));
        }

        if (keptCycleIDsArrayList.size() == 0) {
            return 0;
        } else {

            // This portion of code is used to determine the closest gait cycle from the remaining gait cycles...
            // Since we are not using concept of best Gait cycles anymore we are not using this piece of code
//            int numRemainingCycles = remainingGaitCycles.getColumnDimension();
//            if (numRemainingCycles > 0) {
//                double[] distSum = new double[numRemainingCycles];
//                if (numRemainingCycles > 2) {
//                    for (int i = 0; i < numRemainingCycles; i++) {
//                        for (int j = i + 1; j < numRemainingCycles; j++) {
//
//                            double[] c1 = JamaUtils.getcol(remainingGaitCycles, i).getColumnPackedCopy();
//                            double[] c2 = JamaUtils.getcol(remainingGaitCycles, j).getColumnPackedCopy();
//
//                            DTWDistance mDtw = new DTWDistance(c1, c2);
//                            distSum[i] += mDtw.getAccumulatedDistance();
//                            distSum[j] += mDtw.getAccumulatedDistance();
//                        }
//                    }
//                    double minDis = ArrayManupulation.minimum(distSum);
//                    int minDisID = ArrayManupulation.indexOfValue(distSum, minDis);
//                    closestCycle = ArrayManupulation.get1DArrayFrom2DArray(JamaUtils.getcol(remainingGaitCycles, minDisID).getArray());
//
//                }
//            }
            return keptCycleIDsArrayList.size();
        }
    }
}



