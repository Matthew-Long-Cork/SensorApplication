/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.android.apps.forscience.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.util.ArrayMap;

import com.google.android.apps.forscience.whistlepunk.devicemanager.WhistlepunkBleDevice;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Discovers BLE devices and tracks when they come and go.
 */
public abstract class DeviceDiscoverer {
    public static boolean isBluetoothEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }

    /**
     * Receives notification of devices being discovered or errors.
     */
    public static class Callback {
        public void onDeviceFound(DeviceRecord record) {}

        public void onError(int error) {
            // TODO: define error codes
        }
    }

    /**
     * Describes a Bluetooth device which was discovered.
     */
    public static class DeviceRecord {

        /**
         * Device that was found.
         */
        public WhistlepunkBleDevice device;

        /**
         * Last time this device was seen, in uptimeMillis.
         */
        public long lastSeenTimestampMs;

        /**
         * Last RSSI value seen.
         */
        public int lastRssi;
    }

    private final Context mContext;
    private final BluetoothAdapter mBluetoothAdapter;
    private final ArrayMap<String, DeviceRecord> mDevices;
    private Callback mCallback;

    public static DeviceDiscoverer getNewInstance(Context context) {
        DeviceDiscoverer discoverer;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            //================================================
            // v21 extends abstract DeviceDiscoverer
            discoverer = new DeviceDiscovererV21(context);              // if API 21+
            //================================================
        } else {
            //================================================
            // Legacy extends abstract DeviceDiscoverer
            discoverer = new DeviceDiscovererLegacy(context);           // if under API 21
            //================================================
        }
        return discoverer;
    }

    protected DeviceDiscoverer(Context context) {
        mContext = context.getApplicationContext();
        BluetoothManager manager = (BluetoothManager) mContext.getSystemService(
                Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        mDevices = new ArrayMap<>();
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public void startScanning(Callback callback) {

        if (callback == null) {
            throw new IllegalArgumentException(" DeviceDiscoverer - Callback must not be null");
        }

        //mDevices.clear();
        //boolean isEmpty = mDevices.isEmpty();

        mCallback = callback;
        onStartScanning();
    }

    public void onStartScanning(){}

    public void stopScanning() {
        onStopScanning();
        mCallback = null;
    }

    public abstract void onStopScanning();

    public boolean canScan() {
        return mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    protected void addOrUpdateDevice(WhistlepunkBleDevice device, int rssi) {

        int number = mDevices.size();//  for debug

        // temporary device record object
        // check if 'found device' address matches a device in mDevices
        // if not a match, 'deviceRecord' value will be null
        DeviceRecord deviceRecord = mDevices.get(device.getAddress());

        String address = device.getAddress();//  for debug
        String name = device.getName();//  for debug

        // if not a match, boolean will be false
        boolean previouslyFound = deviceRecord != null;

        // if not previously found
        if (!previouslyFound) {
            //create new device record of device
            deviceRecord = new DeviceRecord();
            deviceRecord.device = device;

            // if the name is not null we will add this device to the available BT device list
            //if(name != null) {  //<-- this is the scann list ..
                // add MAC and the object itself to the found devices list
                mDevices.put(device.getAddress(), deviceRecord);

                System.out.println("\n======================================");
                System.out.println(" ");
                System.out.println("======================================");
                System.out.println(" ");
                System.out.println(" ");
                System.out.println(" new device address: " + address);
                System.out.println(" new device name: " + name);
                System.out.println(" new device number: " + number);
                System.out.println(" mDevices.size(): " + mDevices.size());
                System.out.println("\n");

                int i=0;
                for (ArrayMap.Entry<String,DeviceRecord> entry : mDevices.entrySet()) {
                    //String address = entry.getKey();
                    DeviceRecord value = entry.getValue();
                    String currentName = value.device.getName();

                    System.out.println(" device no." + i + ": " + currentName );
                    i++;
                }

                System.out.println(" ");
                System.out.println(" ");
                System.out.println("======================================");
                System.out.println(" ");
                System.out.println("======================================");
                number ++;
            //}
        }
        // Update the last RSSI and last seen
        deviceRecord.lastRssi = rssi;
        deviceRecord.lastSeenTimestampMs = SystemClock.uptimeMillis();

        // if the device is a new device & there currently is a callback active &
        // the device has a name not just mac
        if (!previouslyFound && mCallback != null && name != null){
            mCallback.onDeviceFound(deviceRecord);
        }
    }
}
