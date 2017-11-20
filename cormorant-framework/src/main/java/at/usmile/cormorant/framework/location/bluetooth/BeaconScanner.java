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
package at.usmile.cormorant.framework.location.bluetooth;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.UUID;

import at.usmile.cormorant.framework.location.bluetooth.DistanceHelper.DISTANCE;

/**
 * Created by fhdwsse
 */

public class BeaconScanner {
    private Context context;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private List<BeaconDistanceResultListener> beaconDistanceResultListeners = new LinkedList<>();
    private Map<UUID, Integer> beaconTimeouts = new HashMap<>();
    private DistanceHelper distanceHelper = new DistanceHelper();

    private final static String LOG_TAG = BeaconPublisher.class.getSimpleName();
    private final static int BEACON_TIMEOUT = 5 * 1000; //seconds * 1000

    public BeaconScanner(BluetoothLeScanner bluetoothLeScanner, Context context) {
        this.bluetoothLeScanner = bluetoothLeScanner;
        this.scanCallback = createScanCallback();
        this.context = context;
    }

    public BeaconScanner(BluetoothLeScanner bluetoothLeScanner, Context context, BeaconDistanceResultListener beaconDistanceResultListener) {
        this(bluetoothLeScanner, context);
        addOnScanResultListener(beaconDistanceResultListener);
    }

    public void startScanner() {
        bluetoothLeScanner.startScan(Arrays.asList(createScanFilter()), createScanSettings(), scanCallback);
        Log.d(LOG_TAG, "Bluetooth LE Scanner started");
    }

    public void stopScanner() {
        bluetoothLeScanner.stopScan(scanCallback);
    }

    private ScanSettings createScanSettings() {
        ScanSettings.Builder mBuilder = new ScanSettings.Builder();
        mBuilder.setReportDelay(0);
        mBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        return mBuilder.build();
    }

    //Creates a filter which only accepts cormorant beacons
    private ScanFilter createScanFilter() {
        ScanFilter.Builder mBuilder = new ScanFilter.Builder();
        ByteBuffer mManufacturerData = ByteBuffer.allocate(24);
        ByteBuffer mManufacturerDataMask = ByteBuffer.allocate(24);

        mManufacturerData.put(18, BeaconPublisher.MAJOR_MINOR_VALUES[0]); // first Major
        mManufacturerData.put(19, BeaconPublisher.MAJOR_MINOR_VALUES[1]); // second Major
        mManufacturerData.put(20, BeaconPublisher.MAJOR_MINOR_VALUES[2]); // first minor
        mManufacturerData.put(21, BeaconPublisher.MAJOR_MINOR_VALUES[3]); // second minor

        for (int i=18; i<=21; i++) {
            mManufacturerDataMask.put(i, (byte)0x01);
        }

        mBuilder.setManufacturerData(BeaconPublisher.MANUFACTURER_ID, mManufacturerData.array(), mManufacturerDataMask.array());
        return mBuilder.build();
    }

    private ScanCallback createScanCallback() {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.v(LOG_TAG, "Scan Result: " + result);

                ScanRecord scanRecord = result.getScanRecord();
                if(scanRecord == null) {
                    Log.w(LOG_TAG, "ScanRecord was null");
                    return;
                }

                byte[] manufacturerData = scanRecord.getManufacturerSpecificData(BeaconPublisher.MANUFACTURER_ID);
                if(manufacturerData == null) {
                    Log.w(LOG_TAG, "ManufacturerSpecificData was null");
                    return;
                }

                calcDistance(result, manufacturerData);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.w(LOG_TAG, "Bluetooth LE Scan failed: " + errorCode);
                Toast.makeText(context, "Bluetooth LE Scan failed: " + errorCode, Toast.LENGTH_SHORT).show();
            }
        };
    }

    //TODO review this clunky timeout approach
    private void createBeaconTimeout(UUID uuid) {
        final Integer lastTick = beaconTimeouts.get(uuid);
        if(lastTick == null){
            beaconTimeouts.put(uuid, 1);
            return;
        }
        else {
            beaconTimeouts.put(uuid, lastTick + 1);
        }

        new CountDownTimer(BEACON_TIMEOUT, BEACON_TIMEOUT) {
            public void onFinish() {
                if((beaconTimeouts.get(uuid)-1) == lastTick || beaconTimeouts.get(uuid) == 1){
                    Log.d(LOG_TAG, String.format("Beacon %s timed out", uuid));
                    beaconDistanceResultListeners.forEach(eachListener ->
                            eachListener.onResult(null, DISTANCE.UNKNOWN, uuid));
                }
            }

            @Override
            public void onTick(long l) {}
        }.start();

    }

    private void calcDistance(ScanResult result, byte[] manufacturerData) {
        int rssi = result.getRssi();
        int txPowerLevel = manufacturerData[22];
        double acu = distanceHelper.calculateAccuracy(txPowerLevel, rssi);
        UUID uuid = getUuidFromManufacturerData(manufacturerData);
        DISTANCE distance = distanceHelper.estimateDistance(acu);

        DISTANCE averagedDistance = distanceHelper.averageDistance(distance, uuid);
        if(averagedDistance == null) return;

        for (BeaconDistanceResultListener eachBeaconDistanceResultListener : beaconDistanceResultListeners) {
            eachBeaconDistanceResultListener.onResult(result, averagedDistance, uuid);
        }

        createBeaconTimeout(uuid);
    }

    private UUID getUuidFromManufacturerData(byte[] manufacturerData){
        byte[] uuidAsBytes = new byte[16];
        for (int i=2; i<=17; i++) {
            uuidAsBytes[i-2] = manufacturerData[i];
        }
        return UuidHelper.getUuidFromBytes(uuidAsBytes);
    }

    public interface BeaconDistanceResultListener {
        void onResult(ScanResult result, DISTANCE distance, UUID uuid);
    }

    public void addOnScanResultListener(BeaconDistanceResultListener beaconDistanceResultListener) {
        this.beaconDistanceResultListeners.add(beaconDistanceResultListener);
    }

    public void removeOnScanResultListener(BeaconDistanceResultListener beaconDistanceResultListener) {
        this.beaconDistanceResultListeners.remove(beaconDistanceResultListener);
    }

}
