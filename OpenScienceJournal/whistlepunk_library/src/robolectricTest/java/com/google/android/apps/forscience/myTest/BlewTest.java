package com.google.android.apps.forscience.myTest;

import android.content.Context;
import android.test.mock.MockContext;

import com.google.android.apps.forscience.whistlepunk.blew.BleSensorManager;
import com.google.android.apps.forscience.whistlepunk.blew.MqttManager;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;

import org.apache.tools.ant.taskdefs.Concat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Handler;

import static org.junit.Assert.assertEquals;

public class BlewTest {

    @Mock
    Context context;

    @Test
    public void testMethod(){

        //MockContext
        MqttManager mqttManager = new MqttManager();
        try {
            mqttManager.connect();
        }catch (Exception e){e.printStackTrace();}
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                assertEquals(mqttManager.isConnected(), true);

                assertEquals(0, 0);
            }
        }, 5000);

    }
}
