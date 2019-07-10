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

package com.google.android.apps.forscience.whistlepunk.sensors;

// added: class imports
import com.google.android.apps.forscience.whistlepunk.DataObject;
import com.google.android.apps.forscience.whistlepunk.DatabaseConnectionService;
//

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import com.google.android.apps.forscience.javalib.DataRefresher;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.AbstractSensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.AvailableSensors;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ReadableSensorOptions;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ScalarSensor;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorEnvironment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorStatusListener;
import com.google.android.apps.forscience.whistlepunk.sensorapi.StreamConsumer;
// added to check if the experiment is active or not
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;


import java.util.Timer;
import java.util.TimerTask;


public class AmbientLightSensor extends ScalarSensor {
    public static final String ID = "AmbientLightSensor";
    private final SystemScheduler mScheduler = new SystemScheduler();
    private SensorEventListener mSensorEventListener;
    private DataRefresher mDataRefresher;

    // declare new variables.
    private DataObject data;
    private float dataValue;
    private Timer timer;
    private int frequencyTime;
    private boolean firstTime = true;

    public AmbientLightSensor() {
        super(ID);
    }

    @Override
    public SensorRecorder makeScalarControl(final StreamConsumer c,
                                               final SensorEnvironment environment, final Context context,
                                               final SensorStatusListener listener) {

        return new AbstractSensorRecorder() {
            @Override
            public void startObserving() {

                // if the sensor is not yet active
                if(!ExperimentDetailsFragment.getTheSensorState(ID)){
                    // now active - so change its state to ACTIVE
                    ExperimentDetailsFragment.changeTheSensorState(ID, true);
                    // retrieve the stored frequency value
                    frequencyTime = ExperimentDetailsFragment.getTheStoredFrequency(ID);

                    mDataRefresher = new DataRefresher(mScheduler, environment.getDefaultClock());
                    listener.onSourceStatus(getId(), SensorStatusListener.STATUS_CONNECTED);
                    SensorManager sensorManager = getSensorManager(context);
                    Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                    if (mSensorEventListener != null) {
                        getSensorManager(context).unregisterListener(mSensorEventListener);
                    }

                    // method to schedule data to be sent to database every 'frequency' milliseconds
                    timer = new Timer();
                    timer.schedule(new sendData(), 0, frequencyTime);

                    mSensorEventListener = new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {

                            // values[0] is the ambient light level in SI lux units.
                            mDataRefresher.setValue(event.values[0]);
                            mDataRefresher.startStreaming();
                            dataValue = event.values[0];
                        }

                        @Override
                        public void onAccuracyChanged(Sensor sensor, int accuracy) {

                        }
                    };
                    sensorManager.registerListener(mSensorEventListener, sensor,
                            SensorManager.SENSOR_DELAY_UI);
                    mDataRefresher.setStreamConsumer(c);
                }
            }

            @Override
            public void stopObserving() {

                boolean sensorActive =  ExperimentDetailsFragment.getTheSensorState(ID);
                // if experiment is no longer active
                if (!(ExperimentDetailsFragment.getIsActiveStatus()) || !(sensorActive)) {

                    if(sensorActive) {
                        // change sensor state to NOT ACTIVE
                        ExperimentDetailsFragment.changeTheSensorState(ID, false);
                    }

                    // stop the timer task as the observing of the sensor is no longer needed
                    if (timer != null)
                        timer.cancel();

                    getSensorManager(context).unregisterListener(mSensorEventListener);
                    listener.onSourceStatus(getId(), SensorStatusListener.STATUS_DISCONNECTED);
                    if (mDataRefresher != null) {
                        mDataRefresher.stopStreaming();
                        mDataRefresher = null;
                    }
                }
                else{
                    System.out.println("------------------------------------------------");
                    System.out.println("------------------------------------------------");
                    System.out.println("        NOT STOPPING light");
                    System.out.println("------------------------------------------------");
                    System.out.println("------------------------------------------------");
                }
            }

            @Override
            public void applyOptions(ReadableSensorOptions settings) {
                // do nothing, no settings apply to collection
            }
        };
    }

    public static boolean isAmbientLightAvailable(AvailableSensors availableSensors) {
        return availableSensors.isSensorAvailable(Sensor.TYPE_LIGHT);
    }

    // this class was added to sends the data to collection class that will then sent to database
    class sendData extends TimerTask {
        public void run() {
            //if data == null without firstTime variable
            if (firstTime) {
                // if first time, create the data object
                data = new DataObject(ID, dataValue);

                try {
                    Thread.sleep(250); // 250 millisecond delay to allow first collection of sensor data.
                    firstTime = false;
                } catch (InterruptedException ex) {}
            }
            // get current data value
            data.setDataValue(dataValue);
            // send the data to the DatabaseConnectionService
            if(DatabaseConnectionService.isConnected())
                DatabaseConnectionService.sendData(data);
            else {
                timer.cancel();
                timer = null;
                DatabaseConnectionService.setCallBack(new Runnable(){
                    @Override
                    public void run() {
                        timer = new Timer();
                        timer.schedule(new sendData(), 0, frequencyTime);
                    }
                });
            }
        }
    }
}

