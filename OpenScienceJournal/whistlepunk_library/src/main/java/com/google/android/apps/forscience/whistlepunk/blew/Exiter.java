package com.google.android.apps.forscience.whistlepunk.blew;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.telecom.Call;
import android.util.Log;

import com.google.android.apps.forscience.whistlepunk.AppSingleton;
import com.google.android.apps.forscience.whistlepunk.DatabaseConnectionService;
import com.google.android.apps.forscience.whistlepunk.MainActivity;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;

public class Exiter extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        this.startForeground(1, new Notification());
        return null;
    }

       @Override
    public void onTaskRemoved(Intent rootIntent){
        BleSensorManager.getInstance().disconnect();
        DatabaseConnectionService.mqttDisconnect();
        AppSingleton.getInstance(this).getSensorRegistry().refreshBuiltinSensors(this);
        stopForeground(true);
        stopSelf();
    }
}
