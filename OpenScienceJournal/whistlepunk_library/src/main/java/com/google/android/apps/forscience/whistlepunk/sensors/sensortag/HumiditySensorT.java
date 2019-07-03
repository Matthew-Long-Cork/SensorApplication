package com.google.android.apps.forscience.whistlepunk.sensors.sensortag;

import android.content.Context;

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

public class HumiditySensorT extends BleSensor {
    public  HumiditySensorT(){
        super("HumiditySensort", Sensor.HUM);
    }
}
