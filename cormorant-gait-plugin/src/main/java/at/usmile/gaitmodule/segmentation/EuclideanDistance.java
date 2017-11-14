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
/**
 *
 */
package at.usmile.gaitmodule.segmentation;

/**
 * @author Muhammad Muaaz
 * @version 1.0
 */
public class EuclideanDistance {
    /**
     * Calculates the square of the Euclidean distance between two points.
     *
     * @param p1 The first point.
     * @param p2 The second point.
     * @return The Square of Euclidean distance.
     */
    public static double distance2(double p1, double p2) {
        return (p1 - p2) * (p1 - p2);
    }

    /**
     * Calculates the square of the Euclidean distance between two points represented by the vector.
     *
     * @param point1 The first point.
     * @param point2 The second point.
     * @return The Euclidean distance.
     * @throws IllegalArgumentException In the case of error.
     */
    public static double distance2(double[] point1, double[] point2) throws IllegalArgumentException {
        if (point1.length == point2.length) {
            Double sum = 0D;
            for (int i = 0; i < point1.length; i++) {
                sum = sum + (point2[i] - point1[i]) * (point2[i] - point1[i]);
            }
            return sum;
        } else {
            throw new IllegalArgumentException("Exception in Euclidean distance: array lengths are not equal");
        }
    }

    /**
     * Calculates the square of the Euclidean distance between two points represented by the vector.
     *
     * @param point1 The first point.
     * @param point2 The second point.
     * @return The Euclidean distance.
     * @throws IllegalArgumentException In the case of error.
     */
    private static double distance2(int[] point1, int[] point2) throws IllegalArgumentException {
        if (point1.length == point2.length) {
            Double sum = 0D;
            for (int i = 0; i < point1.length; i++) {
                sum = sum
                        + (Integer.valueOf(point2[i]).doubleValue() - Integer.valueOf(point1[i]).doubleValue())
                        * (Integer.valueOf(point2[i]).doubleValue() - Integer.valueOf(point1[i]).doubleValue());
            }
            return sum;
        } else {
            throw new IllegalArgumentException("Exception in Euclidean distance: array lengths are not equal");
        }
    }

    /**
     * Calculates the Euclidean distance between two points.
     *
     * @param p1 The first point.
     * @param p2 The second point.
     * @return The Euclidean distance.
     */
    public static double distance(double p1, double p2) {
        double d = (p1 - p2) * (p1 - p2);
        return Math.sqrt(d);
    }

    /**
     * Calculates the Euclidean distance between two points.
     *
     * @param point1 The first point.
     * @param point2 The second point.
     * @return The Euclidean distance.
     * @throws IllegalArgumentException In the case of error.
     */
    public static double distance(double[] point1, double[] point2) throws IllegalArgumentException {
        return Math.sqrt(distance2(point1, point2));
    }

    /**
     * Calculates the Euclidean distance between two points.
     *
     * @param point1 The first point.
     * @param point2 The second point.
     * @return The Euclidean distance.
     * @throws IllegalArgumentException In the case of error.
     */
    public static double distance(int[] point1, int[] point2) throws IllegalArgumentException {
        return Math.sqrt(distance2(point1, point2));
    }

    /**
     * Calculates euclidean distance between two time-series of equal length.
     *
     * @param query    The timeseries1.
     * @param template The timeseries2.
     * @return The eclidean distance.
     * @throws IllegalArgumentException if error occures.
     */
    public static double seriesDistance(double[] query, double[] template) throws IllegalArgumentException {
        int loopTill = query.length;
        if (loopTill == template.length) {
            Double res = 0D;

            for (int i = 0; i < loopTill; i++) {
                res = res + distance2(query[i], template[i]);
            }
            return Math.sqrt(res);
        } else {
            throw new IllegalArgumentException("Exception in Euclidean distance: array lengths are not equal");
        }
    }

    /**
     * Calculates euclidean distance between two time-series of equal length.
     *
     * @param query    The timeseries1.
     * @param template The timeseries2.
     * @return The eclidean distance.
     * @throws IllegalArgumentException if error occures.
     */
    public static double seriesDistance(double[][] query, double[][] template) throws IllegalArgumentException {
        if (query.length == template.length) {
            Double res = 0D;
            for (int i = 0; i < query.length; i++) {
                res = res + distance2(query[i], template[i]);
            }
            return Math.sqrt(res);
        } else {
            throw new IllegalArgumentException("Exception in Euclidean distance: array lengths are not equal");
        }
    }


}
