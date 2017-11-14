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
package at.usmile.gaitmodule.accel;

import java.util.Date;

/**
 * This class is used to create an object of every event value
 * Where an event is basically a sensor event, can be an accel
 * erometer event, gyroScope event or event of any other sensor.
 */

public class SensorSample {

    private Date timestampDateF;
    private long timestamp;
    private float x;
    private float y;
    private float z;
    private double magnitude;
    public SensorSample (long _timeStamp, double magnitude){
        this.timestamp = _timeStamp;
        this.magnitude = magnitude;
    }
    public SensorSample(long _timestamp, float _x, float _y, float _z) {
        timestamp = _timestamp;
        timestampDateF = new Date();
        x = _x;
        y = _y;
        z = _z;
    }

    public long getTS() {

        return timestamp;
    }

    public double getMagnitude(){
        return magnitude;
    }

    public void setTS(long _timestamp) {
        timestamp = _timestamp;

    }

    public Date getTimestamp() {
        return timestampDateF;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }


}
