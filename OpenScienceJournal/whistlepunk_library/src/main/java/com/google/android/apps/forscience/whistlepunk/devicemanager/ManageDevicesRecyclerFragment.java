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

package com.google.android.apps.forscience.whistlepunk.devicemanager;

import android.app.Fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


import com.google.android.apps.forscience.whistlepunk.AppSingleton;
import com.google.android.apps.forscience.whistlepunk.CurrentTimeClock;
import com.google.android.apps.forscience.whistlepunk.DataController;
//import com.google.android.apps.forscience.whistlepunk.DeviceScanner;
import com.google.android.apps.forscience.whistlepunk.LoggingConsumer;
import com.google.android.apps.forscience.whistlepunk.R;
import com.google.android.apps.forscience.whistlepunk.SensorAppearanceProvider;
import com.google.android.apps.forscience.whistlepunk.SensorRegistry;
import com.google.android.apps.forscience.whistlepunk.WhistlePunkApplication;
import com.google.android.apps.forscience.whistlepunk.analytics.UsageTracker;
import com.google.android.apps.forscience.whistlepunk.api.scalarinput.InputDeviceSpec;
import com.google.android.apps.forscience.whistlepunk.blew.BleSensorManager;
import com.google.android.apps.forscience.whistlepunk.blew.Sensor;
import com.google.android.apps.forscience.whistlepunk.filemetadata.Experiment;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ScalarSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.SystemScheduler;
import com.google.android.apps.forscience.whistlepunk.sensors.TestSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.sensortag.BarometerSensorT;
import com.google.android.apps.forscience.whistlepunk.sensors.sensortag.HumiditySensorT;
import com.google.android.apps.forscience.whistlepunk.sensors.sensortag.LightSensorT;
import com.google.android.apps.forscience.whistlepunk.sensors.sensortag.TemperatureSensorT;
import com.squareup.leakcanary.RefWatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Searches for Bluetooth LE devices that are supported.
 */
public class ManageDevicesRecyclerFragment extends Fragment implements DevicesPresenter,
        ManageFragment {
    private static final String TAG = "MDRFragment";
    private static final String KEY_MY_DEVICES = "state_key_my_devices";
    private static final String KEY_AVAILABLE_DEVICES = "state_key_available_devices";

    public static ConnectableSensorRegistry mRegistry;

    private ExpandableDeviceAdapter mMyDevices;
    private ExpandableServiceAdapter mAvailableDevices;
    private Menu mMainMenu;
    //private ConnectableSensorRegistry mRegistry;
    private SensorRegistry mSensorRegistry;
    private BleSensorManager bleSensorManager;

    //Add Sensors Here!
    private final List<ScalarSensor> sensorList = new ArrayList<ScalarSensor>(Arrays.asList(
            new BarometerSensorT(), new TemperatureSensorT(), new LightSensorT(), new HumiditySensorT()));

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSingleton appSingleton = AppSingleton.getInstance(getActivity());
        DataController dc = appSingleton.getDataController();
        Map<String, SensorDiscoverer> discoverers =
                WhistlePunkApplication.getExternalSensorDiscoverers(getActivity());
        DeviceRegistry deviceRegistry = new DeviceRegistry(
                InputDeviceSpec.builtInDevice(getActivity()));
        SensorAppearanceProvider appearanceProvider = appSingleton.getSensorAppearanceProvider();

        UsageTracker tracker = WhistlePunkApplication.getUsageTracker(getActivity());
        mRegistry = new ConnectableSensorRegistry(dc, discoverers, this, new SystemScheduler(),
                new CurrentTimeClock(), ManageDevicesActivity.getOptionsListener(getActivity()),
                deviceRegistry, appearanceProvider, tracker, appSingleton.getSensorConnector());

        mSensorRegistry = appSingleton.getSensorRegistry();
        bleSensorManager = BleSensorManager.getInstance();
        //
        // creating empty adapters
        //
        mMyDevices = ExpandableDeviceAdapter.createEmpty(mRegistry, deviceRegistry,
                appearanceProvider, mSensorRegistry, 0);

        mAvailableDevices = ExpandableServiceAdapter.createEmpty(mSensorRegistry, mRegistry, 1,
               deviceRegistry, getFragmentManager(), appearanceProvider);
        setHasOptionsMenu(true);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_manage_devices, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler);
        HeaderAdapter myHeader = new HeaderAdapter(R.layout.device_header, R.string.my_devices);
        HeaderAdapter availableHeader = new HeaderAdapter(R.layout.device_header,
                R.string.available_devices);

        if (savedInstanceState != null) {
            mMyDevices.onRestoreInstanceState(savedInstanceState.getBundle(KEY_MY_DEVICES));
            mAvailableDevices.onRestoreInstanceState(
                    savedInstanceState.getBundle(KEY_AVAILABLE_DEVICES));
        }

        CompositeRecyclerAdapter adapter = new CompositeRecyclerAdapter(myHeader, mMyDevices
                );//, availableHeader, mAvailableDevices);
        adapter.setHasStableIds(true);
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        // Don't animate on change: https://code.google.com/p/android/issues/detail?id=204277.
        SimpleItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(animator);


        //Init Bluetooth
        Button bluetoothButton = view.findViewById(R.id.ble_con_btn);
        ListView deviceListView = view.findViewById(R.id.ble_device_list);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(ExperimentDetailsFragment.context, android.R.layout.simple_list_item_1);

        if(bleSensorManager.connected) {
            deviceListView.setVisibility(View.GONE);
            bluetoothButton.setText("Disconnect Bluetooth Device");
        } else
            mSensorRegistry.refreshBuiltinSensors(ExperimentDetailsFragment.context);

        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bleSensorManager.connected) {
                    bleSensorManager.disconnect();
                    deviceListView.setVisibility(View.VISIBLE);
                    bluetoothButton.setText("Search Bluetooth Device");
                    mSensorRegistry.refreshBuiltinSensors(ExperimentDetailsFragment.context);
                } else {
                    bleSensorManager.scan(arrayAdapter);
                    deviceListView.setAdapter(arrayAdapter);
                }

            }
        });

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    bleSensorManager.stopScan();

                for(ScalarSensor sensor : sensorList)
                    mSensorRegistry.addBuiltInSensor(sensor);

                    bleSensorManager.connect(i);

                    deviceListView.setVisibility(View.GONE);
                    bluetoothButton.setText("Disconnect Bluetooth Device");
                    //mSensorRegistry.addBuiltInSensor(new TestSensor());
                    //bleSensorManager.getTelemetry(Sensor.TEMP_AMB, i);
                    //Go Back To Previous Menu
                //ManageDevicesRecyclerFragment.this.getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                //ManageDevicesRecyclerFragment.this.getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAfterLoad();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle myDeviceState = new Bundle();
        mMyDevices.onSaveInstanceState(myDeviceState);
        outState.putBundle(KEY_MY_DEVICES, myDeviceState);

        Bundle availableState = new Bundle();
        mAvailableDevices.onSaveInstanceState(availableState);
        outState.putBundle(KEY_AVAILABLE_DEVICES, availableState);
    }

    @Override
    public void onPause() {
        stopScanning();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        stopScanning();
        mMainMenu = null;
        mMyDevices.onDestroy();
        mMyDevices = null;
        super.onDestroy();

        // Make sure we don't leak this fragment.
        RefWatcher watcher = WhistlePunkApplication.getRefWatcher(getActivity());
        watcher.watch(this);
    }

    @Override
    public boolean isDestroyed() {
        return mMyDevices == null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_manage_devices, menu);
        super.onCreateOptionsMenu(menu, inflater);
        mMainMenu = menu;
        refreshScanningUI();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            refresh(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refresh(boolean clearSensorCache) {
        mRegistry.refresh(clearSensorCache, mSensorRegistry);
    }

    public void refreshAfterLoad() {
        setExperimentId(getArguments().getString(ManageDevicesActivity.EXTRA_EXPERIMENT_ID));
    }

    private void setExperimentId(String experimentId) {
        mRegistry.setExperimentId(experimentId, mSensorRegistry);
        AppSingleton.getInstance(getActivity()).getDataController().getExperimentById(
                experimentId,
                new LoggingConsumer<Experiment>(TAG, "load experiment for name") {
                    @Override
                    public void success(Experiment exp) {
                        View view = getView();
                        if (view == null) {
                            return;
                        }
                        TextView selectSensors = (TextView) view.findViewById(
                                R.id.select_sensors);
                        selectSensors.setText(getString(R.string.select_sensors,
                                exp.getDisplayTitle(getActivity())));
                    }
                });
    }

    //==========================================================================================
    //              ADDED CODE BLOCK
    //==========================================================================================



    //==========================================================================================
    //              END OF ADDED CODE BLOCK
    //==========================================================================================
    private void stopScanning() {
        mRegistry.stopScanningInDiscoverers();
    }

    @Override
    public void refreshScanningUI() {
        boolean isScanning = mRegistry.isScanning();

        if (mMainMenu != null) {
            MenuItem refresh = mMainMenu.findItem(R.id.action_refresh);
            refresh.setEnabled(!isScanning);
            if (getActivity() != null) {
                refresh.getIcon().setAlpha(getActivity().getResources().getInteger(
                        isScanning ? R.integer.icon_inactive_alpha : R.integer.icon_active_alpha));
            }
        }
    }

    @Override
    public void showSensorOptions(String experimentId, String sensorId,
            SensorDiscoverer.SettingsInterface settings) {
        if (!isResumed()) {
            // Fragment has paused between pairing and popping up options.
            // TODO: if the sensor says that immediate options must be shown, then in this case
            //       we should probably remember that we never showed the options, and pop them
            //       up on resume.
            return;
        }
        settings.show(experimentId, sensorId, getFragmentManager(), false);
    }


    //==============================================================================================
    //          THESE ARE THE PAIRED DEVICES AND THE NEW DEVICES!!
    //==============================================================================================
    @Override
    public SensorGroup getPairedSensorGroup() {
        return mMyDevices;
    }

    @Override
    public SensorGroup getAvailableSensorGroup() {
        return mAvailableDevices;
    }

    //==============================================================================================
    //
    //==============================================================================================
    @Override
    public void unpair(String experimentId, String sensorId) {
        ((DeviceOptionsDialog.DeviceOptionsListener) getActivity()).onRemoveSensorFromExperiment(
                experimentId, sensorId);
    }
}
