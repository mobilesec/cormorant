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
package fhdw.locationriskplugin;

import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;

public class GenericCellIdentity {
    private int cellId;
    private int locationId;
    private int mcc;
    private int mnc;

    private String globalCellId;
    private CELL_TYPE cellType;

    public GenericCellIdentity(CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoGsm) {
            CellIdentityGsm cellIdentity = ((CellInfoGsm) cellInfo).getCellIdentity();
            this.cellId = cellIdentity.getCid();
            this.locationId = cellIdentity.getLac();
            this.mcc = cellIdentity.getMcc();
            this.mnc = cellIdentity.getMnc();
            this.cellType = CELL_TYPE.GSM;
        } else if (cellInfo instanceof CellInfoWcdma) {
            CellIdentityWcdma cellIdentity = ((CellInfoWcdma) cellInfo).getCellIdentity();
            this.cellId = cellIdentity.getCid();
            this.locationId = cellIdentity.getLac();
            this.mcc = cellIdentity.getMcc();
            this.mnc = cellIdentity.getMnc();
            this.cellType = CELL_TYPE.WCDMA;
        } else if (cellInfo instanceof CellInfoLte) {
            CellIdentityLte cellIdentity = ((CellInfoLte) cellInfo).getCellIdentity();
            this.cellId = cellIdentity.getCi();
            this.locationId = cellIdentity.getTac();
            this.mcc = cellIdentity.getMcc();
            this.mnc = cellIdentity.getMnc();
            this.cellType = CELL_TYPE.LTE;
        } else {
            throw new RuntimeException("Unknown CellInfo");
        }
        this.globalCellId = new StringBuilder()
                .append(this.cellId)
                .append(this.locationId)
                .append(this.mcc)
                .append(this.mnc)
                .toString();
    }

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public int getMcc() {
        return mcc;
    }

    public void setMcc(int mcc) {
        this.mcc = mcc;
    }

    public int getMnc() {
        return mnc;
    }

    public void setMnc(int mnc) {
        this.mnc = mnc;
    }

    public String getGlobalCellId() {
        return globalCellId;
    }

    public void setGlobalCellId(String globalCellId) {
        this.globalCellId = globalCellId;
    }

    public CELL_TYPE getCellType() {
        return cellType;
    }

    public void setCellType(CELL_TYPE cellType) {
        this.cellType = cellType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenericCellIdentity that = (GenericCellIdentity) o;

        return globalCellId != null ? globalCellId.equals(that.globalCellId) : that.globalCellId == null;

    }

    @Override
    public int hashCode() {
        return globalCellId != null ? globalCellId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "GenericCellIdentity{" +
                "cellId=" + cellId +
                ", locationId=" + locationId +
                ", mcc=" + mcc +
                ", mnc=" + mnc +
                ", globalCellId='" + globalCellId + '\'' +
                ", cellType=" + cellType +
                '}';
    }

    public static enum CELL_TYPE {
        GSM, WCDMA, LTE
    }
}
