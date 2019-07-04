package com.google.android.apps.forscience.whistlepunk.blew;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.android.apps.forscience.whistlepunk.DatabaseConnectionService;
import com.google.android.apps.forscience.whistlepunk.MainActivity;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorObserver;

import java.util.ArrayList;
import java.util.List;

public class BleSensorManager {

    private static BleSensorManager bleSensorManager;

    private static boolean ENABLED = true;
    public boolean connected = false;
    private boolean serviceDiscovered = false;

    private final long SCANNER_TIMEOUT = 5000;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice device;

    private List<BluetoothDevice> bluetoothDeviceList;
    private ArrayAdapter<String> bluetoothDeviceArray;

    private Sensor currentSensor;
    private Sensor nextSensor;

    private BleSensorManager(){
        bluetoothManager = (BluetoothManager) ExperimentDetailsFragment.context.getSystemService(Context.BLUETOOTH_SERVICE);
        connected = false;
        //currentSensor = Sensor.VOID;
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
        if(bluetoothGatt != null && serviceDiscovered && connected) {
            nextSensor = sensor;

            if (currentSensor != null) {
                if (!nextSensor.equals(currentSensor)) {
                    disableSensor(currentSensor);
                }
            }else {

                turnSensor(sensor, 1);

            }

        } else{
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateSensor(sensor);
                }

            }, 1000);

        }
    }

    public void connect(int devicePos){
        if(!bluetoothDeviceList.isEmpty()) {
            device = bluetoothDeviceList.get(devicePos);
            /*device.connectGatt(null, false, new BluetoothGattCallback() {
                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorWrite(gatt, descriptor, status);
                }
            })*/
            bluetoothGatt = device.connectGatt(null, false, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    if(status == 0) {
                        connected = true;
                        bluetoothGatt.discoverServices();
                    }else if (status == 8) {
                        connected = false;
                        disconnect();
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    serviceDiscovered = true;
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    BleObservable.broadcast(currentSensor.parseFloat(characteristic.getValue()));
                    monitor(characteristic, true);
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    BleObservable.broadcast(currentSensor.parseFloat(characteristic.getValue()));
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);

                    if (currentSensor != null && !nextSensor.equals((currentSensor))) {
                        turnSensor(nextSensor, 1);
                        currentSensor = nextSensor;
                    } else {
                        currentSensor = nextSensor;
                        read(currentSensor);
                    }
                }

                    @Override
                    public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
                        super.onDescriptorWrite(gatt, descriptor, status);
                        if (currentSensor != null && !nextSensor.equals((currentSensor))) {
                            turnSensor(currentSensor, 0);
                        }
                    }

            });

            connected = bluetoothGatt.connect();
        } else
            Log.e("Bluetooth:", "No device found");
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
        serviceDiscovered = false;
        currentSensor = null;
    }

    public void stopScan(){
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    private void turnSensor(Sensor sensor, int b){
        BluetoothGattCharacteristic char_write = bluetoothGatt.getService(sensor.getServ()).getCharacteristic(sensor.getWrite());
        char_write.setValue(new byte[] {(byte)b});//Enable/Disable
        bluetoothGatt.writeCharacteristic(char_write);
    }

    private void monitor(BluetoothGattCharacteristic characteristic, boolean monitor){
        byte[] descriptorOption = monitor ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE:
                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;

        BluetoothGattDescriptor descriptor = characteristic.getDescriptors().get(0);
        descriptor.setValue(descriptorOption);
        bluetoothGatt.writeDescriptor(descriptor);
        /*for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
            descriptor.setValue(descriptorOption);
            bluetoothGatt.writeDescriptor(descriptor);
        }*/

        bluetoothGatt.setCharacteristicNotification(characteristic, monitor);
    }

    private void monitor(Sensor sensor, boolean monitor){
        monitor(bluetoothGatt.getService(sensor.getServ())
                .getCharacteristic(sensor.getRead()) , monitor);

    }

    public boolean isConnected(){
        return connected;
    }

    private void disableSensor(Sensor sensor){

        monitor(sensor, false);
        turnSensor(sensor, 0);
    }

    private void read(Sensor sensor){
        bluetoothGatt.readCharacteristic(bluetoothGatt.getService(sensor.getServ())
                .getCharacteristic(sensor.getRead()));
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
