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

    // added: declare new variables.
    private Timer timer;
    private float dataValue;
    boolean firstTime = true;
    private int frequencyTime;

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

                // retrieve the stored frequency value. as these are sensors 3in1 check the ID
                frequencyTime = ExperimentDetailsFragment.getTheStoredFrequency(mAxis.getSensorId());

                System.out.println("======================================");
                System.out.println("                  ");
                System.out.println("======================================");
                System.out.println(" ");
                System.out.println(" ");
                System.out.println("  starting Accelerometer sensor " + mAxis.getSensorId());
                System.out.println("  frequencyTime: " + frequencyTime);
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
                timer.schedule(new sendData(), 0, 2000);

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

            }

            @Override
            public void stopObserving() {

                System.out.println("======================================");
                System.out.println("                  ");
                System.out.println("======================================");
                System.out.println("1");
                System.out.println("2");
                System.out.println("3  stopping Accelerometer sensor " + mAxis.getSensorId() );
                System.out.println("4");
                System.out.println("5");
                System.out.println("======================================");
                System.out.println("                  ");
                System.out.println("======================================");

                // added: stop the timer task as the observing of the sensors is no longer needed
                timer.cancel();

                getSensorManager(context).unregisterListener(mSensorEventListener);
                listener.onSourceStatus(getId(), SensorStatusListener.STATUS_DISCONNECTED);
            }
        };
    }

    public static boolean isAccelerometerAvailable(AvailableSensors availableSensors) {
        return availableSensors.isSensorAvailable(Sensor.TYPE_ACCELEROMETER);
    }

    // added: this class was added to sends the data to collection class that will then sent to database
    class sendData extends TimerTask {
        public void run() {

            // added: data object that will be sent to connection class to then go to the Database
            DataObject data = new DataObject(ID, dataValue);

            if (firstTime) {
                try {
                    Thread.sleep(250); // 250 millisecond delay to allow first collection of sensor data
                    // set the ID (as it can change) and set the value
                    data.setDataId(mAxis.getSensorId());
                    data.setDataValue(dataValue);
                    firstTime = false;
                } catch (InterruptedException ex) {}
            }

            // send the data to the DatabaseConnectionService
            DatabaseConnectionService.sendData(data);
            //======================================
            // connection to database
            //======================================
        }
    }
}