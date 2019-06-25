package com.google.android.apps.forscience.whistlepunk.sensors.sensortag;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.google.android.apps.forscience.whistlepunk.Clock;
import com.google.android.apps.forscience.whistlepunk.blew.BleObservable;
import com.google.android.apps.forscience.whistlepunk.blew.BleObserver;
import com.google.android.apps.forscience.whistlepunk.blew.BleSensorManager;
import com.google.android.apps.forscience.whistlepunk.blew.Sensor;
import com.google.android.apps.forscience.whistlepunk.sensorapi.AbstractSensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ScalarSensor;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorEnvironment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorStatusListener;
import com.google.android.apps.forscience.whistlepunk.sensorapi.StreamConsumer;

public class TemperatureSensorT extends ScalarSensor {
    public static final String ID = "TemperatureSensorT";
    private static boolean available = true;
    private BleObserver observer;
    private final Sensor sensor = Sensor.TEMP_AMB;

    public TemperatureSensorT(){super(ID); }

    public static boolean isAvailable(){
        return available;
    }

    @Override
    protected SensorRecorder makeScalarControl(StreamConsumer c, SensorEnvironment environment, Context context, SensorStatusListener listener) {

        return new AbstractSensorRecorder(){
            @Override
            public void startObserving() {
                listener.onSourceStatus(getId(), SensorStatusListener.STATUS_CONNECTED);
                BleSensorManager.getInstance().updateSensor(sensor);
                final Clock clock = environment.getDefaultClock();

                observer = new BleObserver() {
                    @Override
                    public void onValueChange(float value) {
                        c.addData(clock.getNow(), value);
                    }
                };

                BleObservable.registerObserver(observer);
            }

            @Override
            public void stopObserving() {
                listener.onSourceStatus(getId(), SensorStatusListener.STATUS_DISCONNECTED);
                BleObservable.unregisterObserver(observer);
            }
        };
    }
}
