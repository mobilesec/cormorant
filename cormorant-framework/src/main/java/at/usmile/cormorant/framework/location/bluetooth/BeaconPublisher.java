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

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by fhdwsse on 15.11.2017.
 */

public class BeaconPublisher {

    //See https://www.bluetooth.com/specifications/assigned-numbers/company-identifiers
    //We use a custom MANUFACTURER_ID, which is just over nine thousand!!!.
    public static final int MANUFACTURER_ID = 9001;
    public static final int DEFAULT_TX_POWER = -75;
    public static final byte[] MAJOR_MINOR_VALUES = new byte[] {(byte)0x02, (byte)0x01, (byte)0x01, (byte)0x02};


    private final static String LOG_TAG = BeaconPublisher.class.getSimpleName();

    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private AdvertiseCallback advertiseCallback;

    public BeaconPublisher(BluetoothLeAdvertiser bluetoothLeAdvertiser) {
        this.bluetoothLeAdvertiser = bluetoothLeAdvertiser;
        this.advertiseCallback = createAdvertiseCallback();
    }

    public void startBeacon(UUID uuid) {
        bluetoothLeAdvertiser.startAdvertising(createAdvertiseSettings(), createAdvertiseData(uuid), advertiseCallback);
    }

    public void startBeacon(UUID uuid, int txPower) {
        bluetoothLeAdvertiser.startAdvertising(createAdvertiseSettings(), createAdvertiseData(uuid, txPower), advertiseCallback);
    }

    public void stopBeacon() {
        bluetoothLeAdvertiser.stopAdvertising(createAdvertiseCallback());
    }


    private AdvertiseData createAdvertiseData(UUID uuid, int txPower) {
        byte[] uuidAsBytes = UuidHelper.getBytesFromUuid(uuid);
        AdvertiseData.Builder mBuilder = new AdvertiseData.Builder();
        ByteBuffer mManufacturerData = ByteBuffer.allocate(24);

        mManufacturerData.put(0, (byte)0xBE); // BeaconId
        mManufacturerData.put(1, (byte)0xAC); // BeaconId
        for (int i=2; i<=17; i++) {
            mManufacturerData.put(i, uuidAsBytes[i-2]); // UUID
        }

        //Major Minor values in order to be unique
        mManufacturerData.put(18, MAJOR_MINOR_VALUES[0]); // first Major
        mManufacturerData.put(19, MAJOR_MINOR_VALUES[1]); // second Major
        mManufacturerData.put(20, MAJOR_MINOR_VALUES[2]); // first minor
        mManufacturerData.put(21, MAJOR_MINOR_VALUES[3]); // second minor

        //TxPower is device specific, but often around -75 for Android devices
        mManufacturerData.put(22, Byte.parseByte(String.valueOf(txPower))); // txPower
        mBuilder.addManufacturerData(MANUFACTURER_ID, mManufacturerData.array());
        return mBuilder.build();
    }

    private AdvertiseData createAdvertiseData (UUID uuid) {
        return createAdvertiseData(uuid, DEFAULT_TX_POWER);
    }

    private AdvertiseSettings createAdvertiseSettings() {
        AdvertiseSettings.Builder mBuilder = new AdvertiseSettings.Builder();
        mBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        mBuilder.setConnectable(false);
        mBuilder.setTimeout(0);
        mBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
        return mBuilder.build();
    }

    private AdvertiseCallback createAdvertiseCallback() {
        return new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(LOG_TAG, "Bluetooth LE Beacon started");
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.w(LOG_TAG, "Bluetooth LE Beacon startup failed - ErrorCode: " + errorCode);
            }
        };
    }
}
