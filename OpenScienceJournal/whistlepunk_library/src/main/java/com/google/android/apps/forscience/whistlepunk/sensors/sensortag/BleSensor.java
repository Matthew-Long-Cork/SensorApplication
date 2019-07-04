package com.google.android.apps.forscience.whistlepunk.sensors.sensortag;

import android.content.Context;

import com.google.android.apps.forscience.whistlepunk.Clock;
import com.google.android.apps.forscience.whistlepunk.DataObject;
import com.google.android.apps.forscience.whistlepunk.DatabaseConnectionService;
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

public abstract class BleSensor extends ScalarSensor {

    private final String id;
    private static boolean available = true;
    private BleObserver observer;
    private final Sensor sensor;

    public BleSensor(final String ID, Sensor sensor){
        super(ID);
        this.id = ID;
        this.sensor = sensor;
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
                        //Show value on the graph
                        c.addData(clock.getNow(), value);
                        //Send Data to the thingsboard
                        DatabaseConnectionService.sendData(new DataObject(id, value));
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
