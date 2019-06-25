package com.google.android.apps.forscience.whistlepunk.blew;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.android.apps.forscience.whistlepunk.DatabaseConnectionService;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorObserver;

import java.util.ArrayList;
import java.util.List;

public class BleSensorManager {

    private static BleSensorManager bleSensorManager;

    private static boolean ENABLED = true;
    public boolean connected = false;

    private final long SCANNER_TIMEOUT = 5000;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice device;

    private List<BluetoothDevice> bluetoothDeviceList;
    private ArrayAdapter<String> bluetoothDeviceArray;

    private Sensor currentSensor;

    private BleSensorManager(){
        connected = false;
        bluetoothDeviceList = new ArrayList<BluetoothDevice>();
        checkPermission();
        checkBluetooth();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static BleSensorManager getInstance(){
        if(!ENABLED)
            return null;
        else if(bleSensorManager == null )
            bleSensorManager = new BleSensorManager();

        return bleSensorManager;
    }

    public void scan(final ArrayAdapter bluetoothDeviceArray){
        this.bluetoothDeviceArray = bluetoothDeviceArray;
        bluetoothDeviceArray.clear();
        bluetoothDeviceList.clear();
        bluetoothAdapter.startLeScan(leScanCallback);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.stopLeScan(leScanCallback);
            }
        }, SCANNER_TIMEOUT);
    }

    public void updateSensor(Sensor sensor){
        if(currentSensor != null) {
            turnSensor(currentSensor, (byte) 0);
            BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(currentSensor.getServ())
                    .getCharacteristic(currentSensor.getWrite());
            monitor(characteristic, false);
        }

        monitorTelemetry(sensor);
        currentSensor = sensor;
    }

    public void connect(int devicePos){
        if(!bluetoothDeviceList.isEmpty()) {
            device = bluetoothDeviceList.get(devicePos);
            bluetoothGatt = device.connectGatt(null, false, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    bluetoothGatt.discoverServices();
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    monitor(characteristic, true);
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    BleObservable.broadcast(currentSensor.parseFloat(characteristic.getValue()));
                }
            });

            bluetoothGatt.connect();
            connected = true;
        } else
            Log.e("Bluetooth:", "No device found");
    }

    private void monitorTelemetry(Sensor sensor){
        if(bluetoothGatt.getServices().size() != 0) {
            turnSensor(sensor, (byte) 1);
            BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(sensor.getServ())
                    .getCharacteristic(sensor.getRead());
            monitor(characteristic,true);
        } else {
            //If is still Discovering
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    monitorTelemetry(sensor);
                }
            }, 500);
        }
    }

    public static void enable(){
        ENABLED = true;
    }

    private BluetoothAdapter.LeScanCallback leScanCallback =
        new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                if (bluetoothDevice.getName() != null
                        && bluetoothDevice.getName().contains("SensorTag"))
                    if(!bluetoothDeviceList.contains(bluetoothDevice)) {
                        bluetoothDeviceList.add(bluetoothDevice);
                        bluetoothDeviceArray.add(bluetoothDevice.getName() + "  "
                                + bluetoothDevice.getAddress());
                    }
                }
            };

    public void disconnect(){
        if(bluetoothGatt != null && connected)
            bluetoothGatt.disconnect();

        if(bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        connected = false;
    }

    public void stopScan(){
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    private void turnSensor(Sensor sensor, byte b){
        BluetoothGattCharacteristic char_write = bluetoothGatt.getService(sensor.getServ()).getCharacteristic(sensor.getWrite());
        char_write.setValue(new byte[]{b});//Enable
        bluetoothGatt.writeCharacteristic(char_write);
    }

    private void monitor(BluetoothGattCharacteristic characteristic, boolean monitor){
        bluetoothGatt.setCharacteristicNotification(characteristic, monitor);
    }

    public boolean isConnected(){
        return connected;
    }

    private void checkPermission(){
        //Permissions, do using Permission manager
        //Permissions

        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 2);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, 2);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 2);*/
    }

    private  void checkBluetooth(){
      /*  if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }*/
    }
}
