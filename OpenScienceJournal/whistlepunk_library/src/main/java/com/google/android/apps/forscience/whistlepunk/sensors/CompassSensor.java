/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.google.android.apps.forscience.whistlepunk.Clock;
import com.google.android.apps.forscience.whistlepunk.DataObject;
import com.google.android.apps.forscience.whistlepunk.DatabaseConnectionService;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.AbstractSensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.AvailableSensors;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ScalarSensor;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorEnvironment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorStatusListener;
import com.google.android.apps.forscience.whistlepunk.sensorapi.StreamConsumer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Class to create a compass sensor from the magnetic field and accelerometer.
 */
public class CompassSensor extends ScalarSensor {
    public static final String ID = "CompassSensor";
    private SensorEventListener mSensorEventListener;

    public CompassSensor() {
        super(ID);
    }

    // added: declare new variables.
    private double doubleValue;
    // declare new variables.
    private DataObject data;
    private float dataValue;
    private Timer timer;
    private int frequencyTime;
    private boolean firstTime = true;

    @Override
    protected SensorRecorder makeScalarControl(StreamConsumer c, SensorEnvironment environment,
            Context context, SensorStatusListener listener) {
        return new AbstractSensorRecorder() {
            @Override
            public void startObserving() {

                // if the sensor is not yet active
                if(!ExperimentDetailsFragment.getTheSensorState(ID)){
                    // now active - so change its state to ACTIVE
                    ExperimentDetailsFragment.changeTheSensorState(ID, true);
                    // retrieve the stored frequency value
                    frequencyTime = ExperimentDetailsFragment.getTheStoredFrequency(ID);

                    System.out.println("======================================");
                    System.out.println("======================================");
                    System.out.println("        Starting compass sensor");
                    System.out.println("        FrequencyTime in milliseconds: " + frequencyTime);
                    System.out.println("======================================");
                    System.out.println("======================================");

                    listener.onSourceStatus(getId(), SensorStatusListener.STATUS_CONNECTED);
                    SensorManager sensorManager = getSensorManager(context);
                    Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                    Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    if (mSensorEventListener != null) {
                        getSensorManager(context).unregisterListener(mSensorEventListener);
                    }
                    final Clock clock = environment.getDefaultClock();

                    // added: method to schedule data to be sent to database every 'frequency' seconds
                    timer = new Timer();
                    timer.schedule(new sendData(), 0, frequencyTime);

                    mSensorEventListener = new SensorEventListener() {
                        private float[] orientation = new float[3];
                        private float[] magneticRotation;
                        private float[] acceleration;
                        private float[] rotation = new float[9];
                        private float[] inclination = new float[9];

                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                                acceleration = event.values;
                            } else {
                                magneticRotation = event.values;
                            }
                            // Update data as long as we have a value for both. This is the highest
                            // rate of update.
                            // If we want a slower rate, we can update when *both* values have changed,
                            // or only when magneticRotation changes, for example.
                            if (acceleration == null || magneticRotation == null) {
                                return;
                            }
                            boolean hasRotation = SensorManager.getRotationMatrix(rotation, inclination,
                                    acceleration, magneticRotation);
                            if (hasRotation) {
                                SensorManager.getOrientation(rotation, orientation);
                                // Use a positive angle in degrees between 0 and 360.
                                c.addData(clock.getNow(), 360 - (360 - (Math.toDegrees(orientation[0])))
                                        % 360);

                                // added: this is for the data collection for database
                                doubleValue = 360 - (360 - (Math.toDegrees(orientation[0]))) % 360;
                                // convert doubleValue to float
                                dataValue = (float) doubleValue;
                            }
                        }

                        @Override
                        public void onAccuracyChanged(Sensor sensor, int accuracy) {

                        }
                    };
                    sensorManager.registerListener(mSensorEventListener, magnetometer,
                            SensorManager.SENSOR_DELAY_UI);
                    sensorManager.registerListener(mSensorEventListener, accelerometer,
                            SensorManager.SENSOR_DELAY_UI);
                    // else if it is active. Ignore
                }
                else{
                        System.out.println("======================================");
                        System.out.println("======================================");
                        System.out.println("       compass sensor is ALREADY ACTIVE");
                        System.out.println("======================================");
                        System.out.println("======================================");
                    }
                }

            @Override
            public void stopObserving() {

                boolean active =  ExperimentDetailsFragment.getTheSensorState(ID);
                // if experiment is no longer active
                if (!(ExperimentDetailsFragment.getIsActiveStatus()) || !(active)) {

                    if(active) {
                        // change sensor state to NOT ACTIVE
                        ExperimentDetailsFragment.changeTheSensorState(ID, false);
                    }

                    System.out.println("======================================");
                    System.out.println("======================================");
                    System.out.println("        STOPPING compass sensor");
                    System.out.println("======================================");
                    System.out.println("======================================");

                    // added: stop the timer task as the observing of the sensors is no longer needed
                    timer.cancel();

                    getSensorManager(context).unregisterListener(mSensorEventListener);
                    listener.onSourceStatus(getId(), SensorStatusListener.STATUS_DISCONNECTED);
               }
               else {
                    System.out.println("======================================");
                    System.out.println("======================================");
                    System.out.println("         sensor: " + ID);
                    System.out.println("         Experiment is still active. DATA STILL BEING SENT");
                    System.out.println("======================================");
                    System.out.println("======================================");
                }
            }
        };
    }

    public static boolean isCompassSensorAvailable(AvailableSensors availableSensors) {
        return availableSensors.isSensorAvailable(Sensor.TYPE_ACCELEROMETER) &&
                availableSensors.isSensorAvailable(Sensor.TYPE_MAGNETIC_FIELD);
    }

    // added: this class was added to sends the data to collection class that will then sent to database
    class sendData extends TimerTask {
        public void run() {

            // added: data object that will be sent to connection class to then go to the Database
            data = new DataObject(ID, dataValue);

            if (firstTime) {
                try {
                    Thread.sleep(250); // 250 millisecond delay to allow first collection of sensor data.
                    // as this sensor records movement
                    data.setDataValue(dataValue);
                    firstTime = false;
                } catch (InterruptedException ex) {}
            }

            // send the data to the DatabaseConnectionService
            DatabaseConnectionService.sendData(data);
        }
    }
}
