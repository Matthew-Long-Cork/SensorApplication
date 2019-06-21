package com.google.android.apps.forscience.whistlepunk.blew;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.forscience.javalib.MaybeConsumer;
import com.google.android.apps.forscience.javalib.Success;
import com.google.android.apps.forscience.whistlepunk.AppSingleton;
import com.google.android.apps.forscience.whistlepunk.DataController;
import com.google.android.apps.forscience.whistlepunk.R;
import com.google.android.apps.forscience.whistlepunk.SensorRegistry;
import com.google.android.apps.forscience.whistlepunk.devicemanager.ConnectableSensorRegistry;
import com.google.android.apps.forscience.whistlepunk.devicemanager.ExpandableDeviceAdapter;
import com.google.android.apps.forscience.whistlepunk.devicemanager.ManageDevicesRecyclerFragment;
import com.google.android.apps.forscience.whistlepunk.sensors.TestSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.sensortag.BarometerSensorT;

import junit.framework.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class testBTActivity extends AppCompatActivity {

    //declarations
    private TextView note;
    private Button search_btn, paired_btn, disc_btn;
    private int REQUEST__ENABLE_BT_CODE = 99;
    private boolean isBTAvailable = false, isLocationAvailable = false,
            isBluetoothEnabled = false, isDevicePaired =false, isReceiverRegistered = false;

    private BluetoothAdapter BTAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayList<BluetoothDevice> newDevices = new ArrayList<>();
    private ListView myPairedDeviceListView, myNewDeviceListView;

    private ArrayAdapter<String> BTArrayAdapter;
    private IntentFilter filter;

    private String experimentID;
    private BleSensorManager bleSensorManager;
    private SensorRegistry registry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_bt);

        experimentID = getIntent().getStringExtra("experiment_id");
        bleSensorManager = BleSensorManager.getInstance();
        registry = AppSingleton.getInstance(testBTActivity.this).getSensorRegistry();

        // initialisations
        note = findViewById(R.id.bt_note_tv);
        search_btn = findViewById(R.id.BT_btn);
        paired_btn = findViewById(R.id.BT_btn2);

        disc_btn = findViewById(R.id.disc_btn);

        myPairedDeviceListView = findViewById(R.id.paired_device_list);
        myNewDeviceListView = findViewById(R.id.new_device_list);


        myPairedDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bleSensorManager.stopScan();
                registry.addBuiltInSensor(new TestSensor());
                bleSensorManager.getTelemetry(Sensor.TEMP_AMB, i);

                Toast.makeText(getApplicationContext(), "Bluetooth Device Connected", Toast.LENGTH_SHORT);
            }
        });

        // check for location permission and BT status
        //checkLocationPermission(); // set to true in result()
        // isBTAvailable = checkBluetoothStatus();

        // create the arrayAdapter that contains the BTDevices, and set it to the ListView
        BTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        //==========================================================================================
        // create filter and add the actions
        //==========================================================================================
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        // registered later in code
        //registerReceiver(bReceiver, filter);


        //==========================================================================================
        // this is the search button
        //==========================================================================================
        search_btn.setOnClickListener((View v) -> {
            myPairedDeviceListView.setAdapter(BTArrayAdapter);
            bleSensorManager.scan(BTArrayAdapter);
        });

        //==========================================================================================
        // this is the paired devices button
        //==========================================================================================
        paired_btn.setOnClickListener((View v) -> {

        });

        disc_btn.setOnClickListener((View v) -> {
            registry.refreshBuiltinSensors(testBTActivity.this);
            bleSensorManager.disconnect();
        });
    };

    protected boolean checkBluetoothStatus() {

        BTAdapter = BluetoothAdapter.getDefaultAdapter();

        // if the phone have bluetooth
        if (BTAdapter != null) {
            // if BT not enabled request it
            if (!BTAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST__ENABLE_BT_CODE);
            } else  // if enabled
                isBluetoothEnabled = true;
        }
        // if the phone does not have bluetooth
        else {
            //Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            // or: alert dialog
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            isBluetoothEnabled = false;
        }
        return isBluetoothEnabled;
    }

    //==========================================================================================
    // bluetooth setup onActivityResult()
    //==========================================================================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // receive the result
        super.onActivityResult(requestCode, resultCode, data);

        // If the resultCode is 0, the user selected "No" when prompt to
        // allow the app to enable bluetooth.
        if (requestCode == REQUEST__ENABLE_BT_CODE) {
            if (resultCode == 0) {
                Toast.makeText(this, "You decided to deny bluetooth access",
                        Toast.LENGTH_LONG).show();
                isBluetoothEnabled = false;
            }
        }
        // else the user selected "Yes" and BT is now on
        else {
            isBluetoothEnabled = true;
        }
    }

    //==========================================================================================
    // get paired devices
    //==========================================================================================
    public void getPairedDevices(View view){

        // switch list views
        myNewDeviceListView.setVisibility(View.INVISIBLE);
        myPairedDeviceListView.setVisibility(View.VISIBLE);

        // clear the list & get paired devices
        BTArrayAdapter.clear();
        pairedDevices = BTAdapter.getBondedDevices();

        // put these to the adapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices)
                BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            note.setVisibility(View.VISIBLE);
        }
        else
            note.setVisibility(View.INVISIBLE);
    }

    //==========================================================================================
    // get new devices
    //==========================================================================================

    public void findNewDevices(View view) {

        // if the button is pressed when in the process of discovering, so cancel the discovery
        if (BTAdapter.isDiscovering()) {

            BTAdapter.cancelDiscovery();
            // reset the button text
            search_btn.setText("Search for new devices");
        }
        else {
            // we want to start a new discovery
            // switch list views
            myNewDeviceListView.setVisibility(View.VISIBLE);
            myPairedDeviceListView.setVisibility(View.INVISIBLE);

            //clear BTArrayAdapter and newDevices
            BTArrayAdapter.clear();
            newDevices.clear();

            //get the fresh paired devices list
            pairedDevices = BTAdapter.getBondedDevices();

            System.out.println("======================================");
            System.out.println("======================================");
            System.out.println("1");
            System.out.println("2");
            System.out.println("      number of devices: " + pairedDevices.size());
            System.out.println("4");
            System.out.println("5");
            System.out.println("======================================");
            System.out.println("======================================");


            // start discovery process
            BTAdapter.startDiscovery();

            if (BTAdapter.startDiscovery()) {

                // change the button text to 'cancel' while searching
                search_btn.setText("Cancel Search");
                // if there is a registered receiver, unregister it
                if(isReceiverRegistered)
                    unregisterReceiver(bReceiver);
                // register the new receiver
                registerReceiver(bReceiver, filter);

            } else {
                Toast.makeText(getApplicationContext(),
                        "Something went wrong! Discovery has failed to start.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }


    //private final BroadcastReceiver bReceiver
    //==========================================================================================
    // BroadcastReceiver()
    //==========================================================================================
    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // if the device is not on the paired list or new list
                if (!pairedDevices.contains(device)) {

                    System.out.println("======================================");
                    System.out.println("======================================");
                    System.out.println("1");
                    System.out.println("2");
                    System.out.println("      device: " + device.getAddress()
                            + " is not in paired list");
                    System.out.println("4");
                    System.out.println("5");
                    System.out.println("======================================");
                    System.out.println("======================================");


                    // add to the newDevices[for connecting to BT devices]
                    newDevices.add(device);
                    // and add to newDevicesArrayList[to keep track of available but unconnected devices]
                    // newDevicesArrayList.add(device);
                    // and add to the adapter to display
                    BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

                }
                // if at least one device is mound then show the message
                note.setVisibility(View.VISIBLE);

            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                isReceiverRegistered = true;
                paired_btn.setEnabled(false);
                Toast.makeText(getApplicationContext(), "starting discovery... ",
                        Toast.LENGTH_LONG).show();
            }

            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                search_btn.setText("Search for new devices");
                paired_btn.setEnabled(true);
            }
            else
                // this is to account for the possibility of having no new devices in the list
                note.setVisibility(View.INVISIBLE);
        }
    };

    //==========================================================================================
    // pair the device
    //==========================================================================================
    private boolean pairDevice(BluetoothDevice device) {

        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

            isDevicePaired = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isDevicePaired;
    }

    //==========================================================================================
    // get location permission
    //==========================================================================================
    protected void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //  proceed with the code//
                } else {
                    // shut the app
                    finish();
                }
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        // if we registered the receiver, unregister it
        if(isReceiverRegistered)
            unregisterReceiver(bReceiver);
    }

} // end of class

