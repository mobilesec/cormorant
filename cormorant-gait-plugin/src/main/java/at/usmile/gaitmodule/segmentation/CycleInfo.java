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
package at.usmile.gaitmodule.segmentation;

import com.google.common.primitives.Ints;

import java.util.ArrayList;

import Jama.Matrix;

public class CycleInfo {

    /**
     * This CycleInfoObject is a container object which contains all crusial information
     * such as, remainedGaitCycles, deteledGaitCycles, IDs of remained and deleted gait
     * cycles.
     */
    private Matrix remainedCycles;
    private Matrix deletedCycles;
    private int[] keptCycleIDs;
    private int[] removedCycleIDs;
    private double[] bestCycles;

    public CycleInfo(Matrix _remainedCycles, Matrix _deletedCycles, ArrayList<Integer> _keptCycleIDs, ArrayList<Integer> _removedCycleIDs, double[] _bestCycle) {
        super();
        setRemainedCycles(_remainedCycles);
        setDeletedCycles(_deletedCycles);
        setKeptCycleIDs(_keptCycleIDs);
        setRemovedCycleIDs(_removedCycleIDs);
        setBestCycle(_bestCycle);
    }

    public CycleInfo(Matrix _remainedCycles, Matrix _deletedCycles, ArrayList<Integer> _keptCycleIDs, ArrayList<Integer> _removedCycleIDs) {
        super();
        setRemainedCycles(_remainedCycles);
        setDeletedCycles(_deletedCycles);
        setKeptCycleIDs(_keptCycleIDs);
        setRemovedCycleIDs(_removedCycleIDs);
    }

    public Matrix getRemainedCycles() {
        return remainedCycles;
    }

    private void setRemainedCycles(Matrix _remainedCycles) {
        remainedCycles = _remainedCycles;
    }

    public Matrix getDeletedCycles() {
        return deletedCycles;
    }

    private void setDeletedCycles(Matrix _deletedCycles) {
        deletedCycles = _deletedCycles;
    }

    public int[] getKeptCycleIDs() {
        return keptCycleIDs;
    }

    private void setKeptCycleIDs(ArrayList<Integer> _keptCycleIDs) {
        keptCycleIDs = Ints.toArray(_keptCycleIDs);
    }

    public int[] getRemovedCycleIDs() {
        return removedCycleIDs;

    }

    private void setRemovedCycleIDs(ArrayList<Integer> _removedCycleIDs) {
        removedCycleIDs = Ints.toArray(_removedCycleIDs);
    }

    public double[] getBestCycle() {
        return bestCycles;
    }

    private void setBestCycle(double[] _bestCycle) {
        bestCycles = _bestCycle;
    }

}