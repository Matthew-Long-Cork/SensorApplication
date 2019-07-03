package com.google.android.apps.forscience.whistlepunk.sensors.sensortag;

import android.content.Context;

import com.google.android.apps.forscience.whistlepunk.Clock;
import com.google.android.apps.forscience.whistlepunk.blew.BleObservable;
import com.google.android.apps.forscience.whistlepunk.blew.BleObserver;
import com.google.android.apps.forscience.whistlepunk.blew.BleSensorManager;
import android.hardware.SensorManager;

import com.google.android.apps.forscience.whistlepunk.blew.Sensor;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.AbstractSensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ScalarSensor;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorEnvironment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorStatusListener;
import com.google.android.apps.forscience.whistlepunk.sensorapi.StreamConsumer;

public class LightSensorT extends ScalarSensor {
    public static final String ID = "LuxSensorT";
    private static boolean available = true;
    private BleObserver observer;
    private final Sensor sensor = Sensor.LUX;
    private int frequencyTime;

    public LightSensorT(){super(ID); }

    public static boolean isAvailable(){
        return available;
    }

    @Override
    protected SensorRecorder makeScalarControl(final StreamConsumer c,
            final SensorEnvironment environment, final Context context,
            final SensorStatusListener listener) {

        return new AbstractSensorRecorder(){
            @Override
            public void startObserving() {

                // if the sensor is not yet active
                if (!ExperimentDetailsFragment.getTheSensorState(ID)) {
                    // now active - so change its state to ACTIVE
                    ExperimentDetailsFragment.changeTheSensorState(ID, true);
                    // retrieve the stored frequency value
                    frequencyTime = ExperimentDetailsFragment.getTheStoredFrequency(ID);

                    System.out.println("======================================");
                    System.out.println("======================================");
                    System.out.println("        STARTING the sensorTag-light ");
                    System.out.println("        FrequencyTime in milliseconds: " + frequencyTime);
                    System.out.println("======================================");
                    System.out.println("======================================");

                    //=========================================================================

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

                    //=========================================================================
                }
                else{
                    System.out.println("======================================");
                    System.out.println("======================================");
                    System.out.println("    sensorTag-light is ALREADY ACTIVE");
                    System.out.println("======================================");
                    System.out.println("======================================");
                }
            }

            @Override
            public void stopObserving() {
                listener.onSourceStatus(getId(), SensorStatusListener.STATUS_DISCONNECTED);
                BleObservable.unregisterObserver(observer);
            }
        };
    }
}
