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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.google.android.apps.forscience.whistlepunk.Clock;
import com.google.android.apps.forscience.whistlepunk.DataObject;
import com.google.android.apps.forscience.whistlepunk.DatabaseConnectionService;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.AbstractSensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ReadableSensorOptions;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ScalarSensor;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorEnvironment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorObserver;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorStatusListener;
import com.google.android.apps.forscience.whistlepunk.sensorapi.StreamConsumer;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Displays sound pressure levels in uncalibrated decibels (I have not tried to figure out the
 * 16-bit integer corresponding to 20 micro-Pascals, nor am I convinced it is the same from
 * device to device).  Results should be comparable between readings on the same device, but not
 * necessarily between devices.
 */
public class DecibelSensor extends ScalarSensor {
    private static final int SAMPLE_RATE_IN_HZ = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final String ID = "DecibelSource";
    private AudioRecord mRecord;
    private final ExecutorService mExecutor;
    private final int mBytesInBuffer;
    AtomicBoolean mRunning = new AtomicBoolean(false);

    // declare new variables.
    private DataObject data;
    private float dataValue;
    private Timer timer;
    private int frequencyTime;
    private boolean firstTime = true;

    public DecibelSensor() {
        super(ID);
        int minBufferSize =
                AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT);
        mBytesInBuffer = minBufferSize;
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected SensorRecorder makeScalarControl(final StreamConsumer c,
            final SensorEnvironment environment, Context context,
            final SensorStatusListener listener) {
        final Clock clock = environment.getDefaultClock();
        return new AbstractSensorRecorder() {
            @Override
            public void startObserving() {

                // if the sensor is not yet active
                if(!ExperimentDetailsFragment.getTheSensorState(ID)){
                    // retrieve the stored frequency value
                    frequencyTime = ExperimentDetailsFragment.getTheStoredFrequency(ID);

                    System.out.println("======================================");
                    System.out.println("======================================");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("        Starting sound sensor ");
                    System.out.println("        FrequencyTime in milliseconds: " + frequencyTime);
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("======================================");
                    System.out.println("======================================");

                    listener.onSourceStatus(getId(), SensorStatusListener.STATUS_CONNECTED);
                    if (mBytesInBuffer < 0) {
                        // If this is the case, AudioRecord.getMinBufferSize returned an error.
                        listener.onSourceError(getId(), SensorStatusListener.ERROR_FAILED_TO_CONNECT,
                                "Could not connect to microphone, no sound detected");
                        return;
                    }
                    mRunning.set(true);
                    // Use VOICE_COMMUNICATION to filter out audio coming from the speakers
                    mRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                            SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, mBytesInBuffer);


                    //==============================================================================
                    //              issue - this check fails on the very first time, works then
                    //                    -  is it needed???
                    //==============================================================================

                    //==========================================================================

                    if (mRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                      //  listener.onSourceError(getId(), SensorStatusListener.ERROR_FAILED_TO_CONNECT,
                       //         "Could not connect to microphon");
                        return;
                    }

                    //==========================================================================

                    // now active - so change its state to ACTIVE
                    ExperimentDetailsFragment.changeTheSensorState(ID, true);

                    mRecord.startRecording();
                    // Check to see if we actually started recording before continuing.
                    // AudioRecord#startRecording() logs an error but it has no return value and
                    // doesn't throw an exception when someone else is using the mic.
                    if (mRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                        listener.onSourceError(getId(), SensorStatusListener.ERROR_FAILED_TO_CONNECT,
                                "Microphone in use by another application");
                        return;
                    }

                    // added: method to schedule data to be sent to database every 'frequency' seconds
                    timer = new Timer();
                    timer.schedule(new sendData(), 0, frequencyTime);

                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            short[] tempBuffer = new short[mBytesInBuffer];

                            while (mRunning.get()) {
                                int readShorts = mRecord.read(tempBuffer, 0, mBytesInBuffer);
                                if (readShorts > 0) {

                                    sendBuffer(tempBuffer, readShorts);
                                }
                            }
                        }

                        private void sendBuffer(short[] tempBuffer, int readShorts) {
                            final long timestampMillis = clock.getNow();
                            double totalSquared = 0;

                            for (int i = 0; i < readShorts; i++) {
                                short soundbits = tempBuffer[i];
                                totalSquared += soundbits * soundbits;
                            }

                            // https://en.wikipedia.org/wiki/Sound_pressure
                            final double quadraticMeanPressure =
                                    Math.sqrt(totalSquared / readShorts);
                            final double uncalibratedDecibels =
                                    20 * Math.log10(quadraticMeanPressure);

                            if (isValidReading(uncalibratedDecibels)) {
                                c.addData(timestampMillis, uncalibratedDecibels);
                                dataValue = (float) uncalibratedDecibels;
                            }
                        }
                    });
                }
                // else if it is active. Ignore
                else{
                System.out.println("======================================");
                System.out.println("                  ");
                System.out.println("======================================");
                System.out.println(" ");
                System.out.println(" ");
                System.out.println("        +++ambient light sensor is already active+++");
                System.out.println(" ");
                System.out.println(" ");
                System.out.println("======================================");
                System.out.println("                  ");
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
                    System.out.println("                  ");
                    System.out.println("======================================");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("        Stopping sound sensor");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("======================================");
                    System.out.println("                  ");
                    System.out.println("======================================");

                    // added: stop the timer task as the observing of the sensors is no longer needed
                    if(timer != null){
                        timer.cancel();
                    }

                    mRunning.set(false);
                    if (mRecord != null) {
                        if (mRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                            mRecord.stop();
                        }
                        mRecord.release();
                    }
                    mRecord = null;
                    listener.onSourceStatus(getId(), SensorStatusListener.STATUS_DISCONNECTED);
                }
                else{
                    System.out.println("======================================");
                    System.out.println("======================================");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("         sensor: "+ ID);
                    System.out.println("         Experiment is still active. not stopping");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("======================================");
                    System.out.println("======================================");
                }
            }

            @Override
            public void applyOptions(ReadableSensorOptions settings) {
                // do nothing, no settings apply to collection
            }
        };
    }
    /* Some devices (mostly Samsung) seem to spit out
    some -Double.MAX_VALUE points when the mic starts
    which mess up graphing/audio due to an absurd yMin
    so we drop them since they are bad data anyways.
    */
    private boolean isValidReading(double reading) {
        return reading > -Double.MAX_VALUE;
    }

    // added: this class was added to sends the data to collection class that will then sent to database
    class sendData extends TimerTask {
        public void run() {

            if (firstTime) {
                data = new DataObject(ID, dataValue);

                try {
                    Thread.sleep(250); // 250 millisecond delay to allow first collection of sensor data.
                    firstTime = false;
                } catch (InterruptedException ex) {}
            }
            data.setDataValue(dataValue);
            // send the data to the DatabaseConnectionService
            DatabaseConnectionService.sendData(data);
        }
    }
}

