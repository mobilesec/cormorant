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

/**
 * Simple class to which creats and hold the peaks and vallies....
 *
 * @author Muhammad Muaaz
 * @version 1.0
 */

public class PeaksVallies {


    private int[] peaks;
    private int[] vallies;


    public PeaksVallies() {
        int[] _peaks = null;
        int[] _vallies = null;
        setPeaks(_peaks);
        setVallies(_vallies);
    }

    //getters

    public int[] getPeaks() {
        return peaks;
    }

    public void setPeaks(int[] _peaks) {
        // TODO Auto-generated method stub
        this.peaks = _peaks;
    }

    public int[] getVallies() {
        return vallies;
    }

    public void setVallies(int[] _vallies) {
        // TODO Auto-generated method stub
        this.vallies = _vallies;
    }

}
