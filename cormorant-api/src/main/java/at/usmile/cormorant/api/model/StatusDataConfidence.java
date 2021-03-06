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
package at.usmile.cormorant.api.model;

import android.os.Parcel;
import android.os.Parcelable;

public class StatusDataConfidence implements Parcelable {
    public static final int PARCELABLE_VERSION = 1;

    /**
     * The number of fields in this version of the parcelable.
     */
    public static final int PARCELABLE_SIZE = 2;

    public enum Status {
        TRAINING, OPERATIONAL, UNKNOWN
    }

    private Status status = Status.UNKNOWN;
    private double confidence = 0.0;

    public StatusDataConfidence() {
    }

    public Status getStatus() {
        return status;
    }

    public StatusDataConfidence status(Status status) {
        this.status = status;
        return this;
    }

    public Double getConfidence() {
        return confidence;
    }

    public StatusDataConfidence confidence(Double confidence) {
        this.confidence = confidence;
        return this;
    }

    /**
     * @see Parcelable
     */
    public static final Creator<StatusDataConfidence> CREATOR = new Creator<StatusDataConfidence>() {
        public StatusDataConfidence createFromParcel(Parcel in) {
            return new StatusDataConfidence(in);
        }

        public StatusDataConfidence[] newArray(int size) {
            return new StatusDataConfidence[size];
        }
    };

    private StatusDataConfidence(Parcel in) {
        int parcelableVersion = in.readInt();
        int parcelableSize = in.readInt();
        // Version 1 below
        if (parcelableVersion >= 1) {
            this.status = Status.valueOf(in.readString());
            this.confidence = in.readDouble();
        }
        // Version 2 below

        // Skip any fields we don't know about. For example, if our current
        // version's
        // PARCELABLE_SIZE is 6 and the input parcelableSize is 12, skip the 6
        // fields we
        // haven't read yet (from above) since we don't know about them.
        in.setDataPosition(in.dataPosition() + (PARCELABLE_SIZE - parcelableSize));
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        /**
         * NOTE: When adding fields in the process of updating this API, make sure to bump {@link #PARCELABLE_VERSION} and modify
         * {@link #PARCELABLE_SIZE}.
         */
        parcel.writeInt(PARCELABLE_VERSION);
        parcel.writeInt(PARCELABLE_SIZE);

        // Version 1 below
        parcel.writeString(status.toString());
        parcel.writeDouble(confidence);

        // Version 2 below
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(confidence);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        StatusDataConfidence other = (StatusDataConfidence) obj;
        if (Double.doubleToLongBits(confidence) != Double.doubleToLongBits(other.confidence)) return false;
        if (status != other.status) return false;
        return true;
    }

    public void clean() {
        if (status == null) status = Status.UNKNOWN;
        if (confidence > 1.0 || confidence < -1.0) confidence = 0.0;
    }

}