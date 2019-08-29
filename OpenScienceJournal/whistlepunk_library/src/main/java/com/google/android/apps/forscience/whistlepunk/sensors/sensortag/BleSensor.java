package com.google.android.apps.forscience.whistlepunk.sensors.sensortag;

import android.content.Context;

import com.google.android.apps.forscience.whistlepunk.Clock;
import com.google.android.apps.forscience.whistlepunk.DataObject;
import com.google.android.apps.forscience.whistlepunk.DatabaseConnectionService;
import com.google.android.apps.forscience.whistlepunk.blew.BleObservable;
import com.google.android.apps.forscience.whistlepunk.blew.BleObserver;
import com.google.android.apps.forscience.whistlepunk.blew.BleSensorManager;
import com.google.android.apps.forscience.whistlepunk.blew.Sensor;
import com.google.android.apps.forscience.whistlepunk.devicemanager.ManageDevicesRecyclerFragment;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.AbstractSensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ScalarSensor;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorEnvironment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorStatusListener;
import com.google.android.apps.forscience.whistlepunk.sensorapi.StreamConsumer;

import java.util.Timer;
import java.util.TimerTask;

public abstract class BleSensor extends ScalarSensor {

    private final String id;
    private BleObserver observer;
    private final Sensor sensor;

    // declare new variables.
    private DataObject data;
    private float dataValue;
    private Timer timer;
    private int frequencyTime;
    private boolean firstTime = true;
    private boolean sensorActive;

    public BleSensor(final String ID, Sensor sensor){
        super(ID);
        this.id = ID;
        this.sensor = sensor;
    }

    @Override
    protected SensorRecorder makeScalarControl(StreamConsumer c, SensorEnvironment environment, Context context, SensorStatusListener listener) {
        return new AbstractSensorRecorder() {
            @Override
            public void startObserving() {

                // if the sensor is not yet active
                if (!ExperimentDetailsFragment.getTheSensorState(getId())) {
                    // now active - so change its state to ACTIVE
                    ExperimentDetailsFragment.changeTheSensorState(getId(), true);
                    // retrieve the stored frequency value
                    frequencyTime = ExperimentDetailsFragment.getTheStoredFrequency(getId());

                    listener.onSourceStatus(getId(), SensorStatusListener.STATUS_CONNECTED);
                    BleSensorManager.getInstance().updateSensor(sensor);
                    final Clock clock = environment.getDefaultClock();

                    // method to schedule data to be sent to database every 'frequency' milliseconds
                    timer = new Timer();
                    timer.schedule(new sendData(), 0, frequencyTime);

                    System.out.println("------------------------------------------------");
                    System.out.println("------------------------------------------------");
                    System.out.println("        STARTING " + getId());
                    System.out.println("------------------------------------------------");
                    System.out.println("------------------------------------------------");

                    observer = new BleObserver() {
                        @Override
                        public void onValueChange(float value) {
                            if(sensorActive) {
                                //Show value on the graph
                                c.addData(clock.getNow(), value);
                                dataValue = value;
                            }
                        }
                    };
                    BleObservable.registerObserver(observer);
                }
            }

                @Override
                public void stopObserving () {

                    sensorActive = ExperimentDetailsFragment.getTheSensorState(getId());
                    // if experiment is no longer active
                    if (!(ExperimentDetailsFragment.getIsActiveStatus()) || !(sensorActive)) {

                        if (sensorActive) {
                            // change sensor state to NOT ACTIVE
                            ExperimentDetailsFragment.changeTheSensorState(getId(), false);
                        }
                        // stop the timer task as the observing of the sensor is no longer needed
                        if (timer != null)
                            timer.cancel();

                        System.out.println("------------------------------------------------");
                        System.out.println("------------------------------------------------");
                        System.out.println("        SHUTTING DOWN " + getId());
                        System.out.println("------------------------------------------------");
                        System.out.println("------------------------------------------------");

                        listener.onSourceStatus(getId(), SensorStatusListener.STATUS_DISCONNECTED);
                        BleObservable.unregisterObserver(observer);
                    } else {
                        System.out.println("------------------------------------------------");
                        System.out.println("------------------------------------------------");
                        System.out.println("        NOT STOPPING " + getId());
                        System.out.println("------------------------------------------------");
                        System.out.println("------------------------------------------------");
                    }
                }
        };
    }

    // this class was added to sends the data to collection class that will then sent to database
    class sendData extends TimerTask {
        public void run() {
            // if this is the first run of the sensor
            if (firstTime) {
                // if first time, create the data object
                data = new DataObject(getId(), dataValue);
                DatabaseConnectionService.sendData(new DataObject(id, dataValue));

                try {
                    Thread.sleep(250); // 250 millisecond delay to allow first collection of sensor data.
                    firstTime = false;
                } catch (InterruptedException ex) {
                }
            }
            // if the the sensorTag is still connected
            if (ManageDevicesRecyclerFragment.bleSensorManager.connected) {
                // get current data value
                data.setDataValue(dataValue);
                // and if the database  connection is still there, send the data to the DatabaseConnectionService
                if (DatabaseConnectionService.isConnected())
                    DatabaseConnectionService.sendData(data);
                else
                    timer.cancel();
            }
            // else shut down the ble sensors and stop displaying them
            else{
                ExperimentDetailsFragment.changeTheSensorState(getId(), false);
            }
        }
    }
}
