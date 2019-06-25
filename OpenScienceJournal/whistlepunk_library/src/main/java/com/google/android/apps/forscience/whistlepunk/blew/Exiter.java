package com.google.android.apps.forscience.whistlepunk.blew;

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
import com.google.android.apps.forscience.whistlepunk.MainActivity;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;

public class Exiter extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        Log.e("Bindded", "Bindded");
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent){

        Log.e("Before: ", "Close!!!!");

        stopForeground(true);
        stopSelf();
        //android.os.Process.killProcess(android.os.Process.myPid());

    }
}
