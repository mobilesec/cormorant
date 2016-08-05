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


/**
 * This class is reponsible for performing [-1, 1] data scaling.
 * It can be extended to achieve different kind of scaling functions.
 *
 * @author Muhammad Muaaz
 * @version 1.0
 */

public class DataScaling {

    //default constructor
    public DataScaling() {

    }

    /**
     * This function is to scale data [-1,1]
     *
     * @param _data
     * @return scaledData
     */
//    public static double[] scaleData(double[] _data) {
//
//        double[] scaledData = new double[_data.length];
//        double minVal = ArrayManupulation.minimum(_data);
//        double maxVal = ArrayManupulation.maximum(_data);
//
//        for (int i = 0; i < _data.length; i++) {
//
//            scaledData[i] = 2 * ((_data[i] - minVal) / (maxVal - minVal)) - 1;
//        }
//        return scaledData;
//    }

    public static double[] scaledData(double[] _data, int _min, int _max){

        double scaledData[] = new double[_data.length];
        double minVal = ArrayManupulation.maximum(_data);
        double maxVal = ArrayManupulation.minimum(_data);
        double range = maxVal-minVal;
        for (int i= 0; i < _data.length; i++){
            scaledData[i] = _data[i] - minVal;
        }
        for (int i= 0; i < _data.length; i++){
            scaledData[i] = (scaledData[i]/range)*(maxVal-minVal);
            scaledData[i] = scaledData[i]+minVal;
        }

        return scaledData;
    }
}
