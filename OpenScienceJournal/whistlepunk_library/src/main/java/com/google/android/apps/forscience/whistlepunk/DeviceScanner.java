/*
package com.google.android.apps.forscience.whistlepunk;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;

public class DeviceScanner {

    private boolean mScanning;
    private Handler handler;
    private BluetoothAdapter.LeScanCallback leScanCallback;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothAdapter bluetoothAdapter;

    private DeviceScanner deviceScanner;

    private final int REQUEST_ENABLE_BT = 210;
    private Context context;

    public DeviceScanner(BluetoothAdapter bluetoothAdapter){

        this.bluetoothAdapter = bluetoothAdapter;
    }

    public void setContext(Context context){
        this.context = context;
    }

    public void setLeScanCallback(BluetoothAdapter.LeScanCallback leScanCallback){
        this.leScanCallback = leScanCallback;
    }

    public void scanLeDevice(final boolean enable) {
        handler = new Handler();
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }
}
*/