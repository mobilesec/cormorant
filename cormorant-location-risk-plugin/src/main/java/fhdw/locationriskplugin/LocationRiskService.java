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
package fhdw.locationriskplugin;

import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import at.usmile.cormorant.api.AbstractRiskService;
import at.usmile.cormorant.api.model.StatusDataRisk;

public class LocationRiskService extends AbstractRiskService {
    private Set<GenericCellIdentity> cells;
    private TelephonyManager telephonyManager;

    public LocationRiskService() {
        this.telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        this.cells = new HashSet<>();
    }

    @Override
    protected void onDataUpdateRequest() {
        publishRiskUpdate(new StatusDataRisk()
                .status(StatusDataRisk.Status.OPERATIONAL)
                .risk(0d));
        saveCurrentCells();
    }

    private void saveCurrentCells(){
        //onCellInfoChanged() use for location updateS?
        //cid + lac (btw. tac bei lte) + mcc + mnc = (Global Cell ID)
        Log.d("cells", "Getting cells:");
        List<CellInfo> allCellInfo = telephonyManager.getAllCellInfo();
        if(allCellInfo == null) return;
        for (CellInfo eachCellInfo : allCellInfo) {
            if (eachCellInfo.isRegistered()) cells.add(new GenericCellIdentity(eachCellInfo));
        }
        for (GenericCellIdentity eachCell : cells) {
            Log.d("cells", eachCell.toString());
        }
    }
}
