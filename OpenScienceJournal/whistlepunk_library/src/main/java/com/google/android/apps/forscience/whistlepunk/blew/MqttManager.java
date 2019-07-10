package com.google.android.apps.forscience.whistlepunk.blew;

import android.os.Handler;

import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.util.Log;
import android.widget.Toast;

public class MqttManager {

    private final int RECONNECT_DELAY = 15000;

    private final String mqttTag = "v1/devices/me/telemetry";
    public static String mqttURL;
    public static String deviceToken;

    private Runnable sucRunnable;

    private MqttAndroidClient mqttAndroidClient;
    private static MqttConnectOptions mqttConnectOptions;

    private static MqttManager mqttManager;

    private long currentTime;

    private MqttManager(String URL, String deviceToken){
        mqttURL = "tcp://" + URL + ":1883";
        this.deviceToken = deviceToken;

        sucRunnable = new Runnable() {
            @Override
            public void run() {

            }
        };

        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setAutomaticReconnect(false);
        mqttConnectOptions.setConnectionTimeout(15000);
        mqttConnectOptions.setUserName(deviceToken);

        mqttAndroidClient = new MqttAndroidClient(ExperimentDetailsFragment.context,
                mqttURL, "AppClient");

        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e("MQTT: ", "Connection Lost" + (System.currentTimeMillis() - currentTime));
                //Start Connection Retry
                try {
                    connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception { }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) { }
        });
    }

    private MqttManager(){}

    public static MqttManager getInstance(){
        return new MqttManager();
    }

    public static MqttManager getInstance(String mqttURLT, String deviceTokenT){
       if(mqttManager == null)
            mqttManager = new MqttManager(mqttURLT, deviceTokenT);
       else{
           mqttURL = mqttURLT;
           deviceToken = deviceTokenT;
           mqttConnectOptions.setUserName(deviceToken);
       }

        return mqttManager;
    }

    private IMqttActionListener mqttActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.e("Connected: ", "True");
            sucRunnable.run();
            currentTime = System.currentTimeMillis();
            Toast.makeText(ExperimentDetailsFragment.context, "Mqtt Connected!!!!", Toast.LENGTH_LONG);
            //Ideally throw a Toast
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
          Log.e("Connected: ", "False ");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        //Should be interrupted OnExperimentFragmentDestory
                        mqttAndroidClient.connect(mqttConnectOptions, ExperimentDetailsFragment.context, mqttActionListener);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, RECONNECT_DELAY);
        }
    };

    public void connect() throws Exception{
        if(!mqttAndroidClient.isConnected())
            mqttAndroidClient.connect(mqttConnectOptions, ExperimentDetailsFragment.context, mqttActionListener);
    }

   public void sendDataMqtt(String jsonObject) throws Exception{
        mqttAndroidClient.publish(mqttTag, jsonObject.getBytes(), 0, true);
    }

    public void mqttDisconnect() throws MqttException {
        mqttAndroidClient.disconnect().setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.e("MQTT disconnect", "Success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e("MQTT disconnect", exception.getMessage());
            }
        });
    }

    public void kill() throws MqttException {
        mqttAndroidClient.disconnect().setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.e("MQTT disconnect", "Success");
                mqttAndroidClient.unregisterResources();
                mqttAndroidClient.close();
                mqttAndroidClient = null;
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e("MQTT disconnect", exception.getMessage());
            }
        });
    }

    public boolean isConnected(){
        try {
            return mqttAndroidClient != null && mqttAndroidClient.isConnected();
        } catch (Exception e){
            return false;
        }
    }

    public void setSuccessCallback(Runnable runnable){
        sucRunnable = runnable;
    }
}
