package com.google.android.apps.forscience.whistlepunk.blew;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import android.os.Handler;
import android.util.Log;

import com.google.android.apps.forscience.whistlepunk.DatabaseConnectionService;

import java.util.List;

public class BleSensorManager {

    private static BleSensorManager bleSensorManager;

    private static boolean ENABLED = false;
    private boolean SCANNING = false;
    private boolean found = false;

    private long dummy_timer = System.currentTimeMillis();
    private final long SCHEDULE = 3000;

    private final int SCANNER_TIMEOUT = 5000;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice bluetoothDeviced;

    private List<BluetoothDevice> bluetoothDeviceList;

    private BleSensorManager(){
        checkPermission();
        checkBluetooth();

        //bluetoothDeviceList = new ArrayList<BluetoothDevice>();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static BleSensorManager getInstance(){
        if(!ENABLED)
            return null;
        else if(bleSensorManager == null )
            bleSensorManager = new BleSensorManager();

        return bleSensorManager;
    }

    public void scan(){
        if(!found) {
            if (!SCANNING) {
                //bluetoothDeviceList.clear();
                SCANNING = true;
                bluetoothAdapter.startLeScan(leScanCallback);
                Log.e("", "");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //bluetoothAdapter.stopLeScan(leScanCallback);
                        //SCANNING = false;
                    }
                }, SCANNER_TIMEOUT);

                Log.e("", "");
            }
        } else {
            getTelemetry(Sensor.TEMP, bluetoothDeviced);
        }
    }

    public void scan(final List<BluetoothDevice> bluetoothDeviceList){
        this.bluetoothDeviceList = bluetoothDeviceList;
        scan();
    }


    public void getTelemetry(Sensor sensor){
        getTelemetry(sensor, bluetoothDeviced);
    }

    public void getTelemetry(Sensor sensor, BluetoothDevice device){

        bluetoothGatt = device.connectGatt(null, false, new BluetoothGattCallback() {

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                BluetoothGattCharacteristic char_write = bluetoothGatt.getService(sensor.getServ()).getCharacteristic(sensor.getWrite());
                char_write.setValue(new byte[]{1});//Enable
                bluetoothGatt.writeCharacteristic(char_write);
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                if(System.currentTimeMillis() - dummy_timer >= SCHEDULE){
                    String result = sensor.parse(characteristic.getValue());
                    DatabaseConnectionService.sendData(result);
                    dummy_timer = System.currentTimeMillis();
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                bluetoothGatt.readCharacteristic(bluetoothGatt.getService(sensor.getServ()).getCharacteristic(sensor.getRead()));
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                if(System.currentTimeMillis() - dummy_timer >= SCHEDULE){
                    String result = sensor.parse(characteristic.getValue());
                    DatabaseConnectionService.sendData(result);
                    //BleObservable.broadcast(result);
                    dummy_timer = System.currentTimeMillis();
                }
            }
        });

        bluetoothGatt.connect();

        bluetoothGatt.discoverServices();


    }

    public static void enable(){
        ENABLED = true;
    }

    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                    //if(!bluetoothDeviceList.contains(bluetoothDevice))
                    // bluetoothDeviceList.add(bluetoothDevice);
                    if(!found) {
                        if (bluetoothDevice.getName() != null) {
                            if (bluetoothDevice.getName().contains("SensorTag")) {
                                found = true;
                                bluetoothDeviced = bluetoothDevice;
                                Log.e("Found: ", "fOUND");
                                bluetoothAdapter.stopLeScan(this);
                                getTelemetry(Sensor.TEMP, bluetoothDevice);

                            }
                        }
                    }
                }
            };

    public void disconnect(){
        bluetoothGatt.disconnect();
    }



    private void checkPermission(){
        //Permissions, do using Permission manager
        //Permissions
/*        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
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
