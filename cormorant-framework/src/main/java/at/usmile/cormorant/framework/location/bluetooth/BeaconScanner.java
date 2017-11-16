package at.usmile.cormorant.framework.location.bluetooth;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import at.usmile.cormorant.framework.location.bluetooth.DistanceHelper.DISTANCE;

/**
 * Created by fhdwsse
 */

public class BeaconScanner {
    private Context context;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private List<OnScanResultListener> onScanResultListeners = new LinkedList<>();
    private DistanceHelper distanceHelper = new DistanceHelper();

    private final static String LOG_TAG = BeaconPublisher.class.getSimpleName();

    public BeaconScanner(BluetoothLeScanner bluetoothLeScanner, Context context) {
        this.bluetoothLeScanner = bluetoothLeScanner;
        this.scanCallback = createScanCallback();
        this.context = context;
    }

    public BeaconScanner(BluetoothLeScanner bluetoothLeScanner, Context context, OnScanResultListener onScanResultListener) {
        this(bluetoothLeScanner, context);
        addOnScanResultListener(onScanResultListener);
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

    private void calcDistance(ScanResult result, byte[] manufacturerData) {
        int rssi = result.getRssi();
        int txPowerLevel = manufacturerData[22];
        double acu = distanceHelper.calculateAccuracy(txPowerLevel, rssi);
        UUID uuid = getUuidFromManufacturerData(manufacturerData);
        DISTANCE distance = distanceHelper.estimateDistance(acu);

        DISTANCE averagedDistance = distanceHelper.averageDistance(distance, uuid);
        if(averagedDistance == null) return;

        for (OnScanResultListener eachOnScanResultListener : onScanResultListeners) {
            eachOnScanResultListener.onResult(result, averagedDistance, uuid);
        }
    }

    private UUID getUuidFromManufacturerData(byte[] manufacturerData){
        byte[] uuidAsBytes = new byte[16];
        for (int i=2; i<=17; i++) {
            uuidAsBytes[i-2] = manufacturerData[i];
        }
        return UuidHelper.getUuidFromBytes(uuidAsBytes);
    }

    public interface OnScanResultListener {
        void onResult(ScanResult result, DISTANCE distance, UUID uuid);
    }

    public void addOnScanResultListener(OnScanResultListener onScanResultListener) {
        this.onScanResultListeners.add(onScanResultListener);
    }

    public void removeOnScanResultListener(OnScanResultListener onScanResultListener) {
        this.onScanResultListeners.remove(onScanResultListener);
    }

}
