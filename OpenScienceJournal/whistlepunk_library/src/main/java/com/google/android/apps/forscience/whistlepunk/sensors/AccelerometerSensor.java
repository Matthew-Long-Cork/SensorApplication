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

public class AccelerometerSensor extends ScalarSensor {
    private Axis mAxis;
    public String ID = "";

    public enum Axis {
        X(0, "AccX"),   // these three axis will be the 3 dimensional ID's
        Y(1, "AccY"),
        Z(2, "AccZ");

        private final int mValueIndex;
        private String mDatabaseTag;

        Axis(int valueIndex, String databaseTag) {
            mValueIndex = valueIndex;
            mDatabaseTag = databaseTag;
        }

        public float getValue(SensorEvent event) {
            return event.values[mValueIndex];
        }

        public String getSensorId() {
            return mDatabaseTag;
        }
    }

    // declare new variables.
    private DataObject data;
    private float dataValue;
    private Timer timer;
    private int frequencyTime;
    private boolean firstTime = true;

    private SensorEventListener mSensorEventListener;

    public AccelerometerSensor(Axis axis) {
        super(axis.getSensorId());
        mAxis = axis;
    }

    @Override
    protected SensorRecorder makeScalarControl(final StreamConsumer c,
                                               final SensorEnvironment environment, final Context context,
                                               final SensorStatusListener listener) {
        return new AbstractSensorRecorder() {
            @Override
            public void startObserving() {

                // if the sensor is not yet active
                if(!ExperimentDetailsFragment.getTheSensorState(mAxis.getSensorId())){
                    // now active - so change its state to ACTIVE
                    ExperimentDetailsFragment.changeTheSensorState(mAxis.getSensorId(), true);
                    // retrieve the stored frequency value
                    frequencyTime = ExperimentDetailsFragment.getTheStoredFrequency(mAxis.getSensorId());

                    System.out.println("======================================");
                    System.out.println("                  ");
                    System.out.println("======================================");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("        Starting Accelerometer sensor " + mAxis.getSensorId());
                    System.out.println("        FrequencyTime in milliseconds: " + frequencyTime);
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("======================================");
                    System.out.println("                  ");
                    System.out.println("======================================");

                    listener.onSourceStatus(getId(), SensorStatusListener.STATUS_CONNECTED);
                    SensorManager sensorManager = getSensorManager(context);
                    Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    if (mSensorEventListener != null) {
                        getSensorManager(context).unregisterListener(mSensorEventListener);
                    }
                    final Clock clock = environment.getDefaultClock();

                    // added: method to schedule data to be sent to database every 'frequency' seconds
                    timer = new Timer();
                    timer.schedule(new sendData(), 0, frequencyTime);

                    mSensorEventListener = new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            c.addData(clock.getNow(), mAxis.getValue(event));
                            // added: collect a copy of the value
                            ID = mAxis.getSensorId();
                            dataValue = mAxis.getValue(event);
                        }

                        @Override
                        public void onAccuracyChanged(Sensor sensor, int accuracy) {

                        }
                    };
                    sensorManager.registerListener(mSensorEventListener, sensor,
                            SensorManager.SENSOR_DELAY_UI);
                } else {
                    System.out.println("======================================");
                    System.out.println("                  ");
                    System.out.println("======================================");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("        +++Acc sensor " + mAxis.getSensorId() + " is already active+++");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("======================================");
                    System.out.println("                  ");
                    System.out.println("======================================");
                }
            }

            @Override
            public void stopObserving() {

                boolean active =  ExperimentDetailsFragment.getTheSensorState(mAxis.getSensorId());
                // if experiment is no longer active

                if (!(ExperimentDetailsFragment.getIsActiveStatus()) || !(active)) {

                    if(active) {
                        // change sensor state to NOT ACTIVE
                        ExperimentDetailsFragment.changeTheSensorState(mAxis.getSensorId(), false);
                    }

                    System.out.println("======================================");
                    System.out.println("                  ");
                    System.out.println("======================================");
                    System.out.println(" ");
                    System.out.println( ExperimentDetailsFragment.getIsActiveStatus());
                    System.out.println("        Stopping Accelerometer sensor " + mAxis.getSensorId());
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("======================================");
                    System.out.println("                  ");
                    System.out.println("======================================");

                    // stop the timer task as the observing of the sensors is no longer needed

                    if(timer != null)
                        timer.cancel();

                    getSensorManager(context).unregisterListener(mSensorEventListener);
                    listener.onSourceStatus(getId(), SensorStatusListener.STATUS_DISCONNECTED);
                }

                else{
                    System.out.println("======================================");
                    System.out.println("======================================");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("         sensor: "+ mAxis.getSensorId());
                    System.out.println("         Experiment is still active. not stopping");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("======================================");
                    System.out.println("======================================");
                }


            }
        };
    }

    public static boolean isAccelerometerAvailable(AvailableSensors availableSensors) {
        return availableSensors.isSensorAvailable(Sensor.TYPE_ACCELEROMETER);
    }

    // this class was added to sends the data to collection class that will then sent to database
    class sendData extends TimerTask {
        public void run() {

            if (firstTime) {
                // if first time, create the data object
                data = new DataObject(mAxis.getSensorId(), dataValue);

                try {
                    Thread.sleep(250); // 250 millisecond delay to allow first collection of sensor data.
                    firstTime = false;
                } catch (InterruptedException ex) {}
            }
            // get current data value
            data.setDataValue(dataValue);
            // send the data to the DatabaseConnectionService
            DatabaseConnectionService.sendData(data);
        }
    }
}
