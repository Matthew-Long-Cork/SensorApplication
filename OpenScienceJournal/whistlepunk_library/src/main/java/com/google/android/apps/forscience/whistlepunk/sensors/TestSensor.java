package com.google.android.apps.forscience.whistlepunk.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.google.android.apps.forscience.whistlepunk.Clock;
import com.google.android.apps.forscience.whistlepunk.sensorapi.AbstractSensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ScalarSensor;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorEnvironment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorStatusListener;
import com.google.android.apps.forscience.whistlepunk.sensorapi.StreamConsumer;

public class TestSensor extends ScalarSensor {
    public static final String ID = "TestSensor";

    public TestSensor(){super(ID); }

    @Override
    protected SensorRecorder makeScalarControl(StreamConsumer c, SensorEnvironment environment, Context context, SensorStatusListener listener) {

        return new AbstractSensorRecorder(){
            @Override
            public void startObserving() {
                listener.onSourceStatus(getId(), SensorStatusListener.STATUS_CONNECTED);
                /*SensorManager sensorManager = getSensorManager(context);
                Sensor sensor = sensorManager.getDefaultSensor(
                        Sensor.);*/
                /*if (mSensorEventListener != null) {
                    getSensorManager(context).unregisterListener(mSensorEventListener);
                }*/

                final Clock clock = environment.getDefaultClock();

                c.addData(clock.getNow(), 1f);

            }

            @Override
            public void stopObserving() {
                listener.onSourceStatus(getId(), SensorStatusListener.STATUS_DISCONNECTED);
            }
        };
    }
}
