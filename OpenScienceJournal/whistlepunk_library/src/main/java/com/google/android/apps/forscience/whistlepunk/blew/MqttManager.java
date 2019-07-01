package com.google.android.apps.forscience.whistlepunk.blew;

import android.widget.Toast;

import com.google.android.apps.forscience.whistlepunk.DataObject;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import android.util.Log;

public class MqttManager {

    private final String mqttTag = "v1/devices/me/telemetry";
    private final String mqttURL = "tcp://thingsboard.tec-gateway.com:1883";
    private final String deviceToken = "password123";

    private MqttAndroidClient mqttAndroidClient;
    private MqttConnectOptions mqttConnectOptions;

    public MqttManager(){
        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setUserName(deviceToken);
    }

    public void connect() throws Exception{
        mqttAndroidClient = new MqttAndroidClient(ExperimentDetailsFragment.context,
                mqttURL, "AppClient");

        mqttAndroidClient.connect(mqttConnectOptions, ExperimentDetailsFragment.context, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.e("Connected: ", "True");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e("Connected: ", "False");
            }
        });
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
        mqttDisconnect();
        mqttAndroidClient.unregisterResources();
        mqttAndroidClient.close();
        mqttAndroidClient = null;
    }

    public boolean isConnected(){
        return  mqttAndroidClient.isConnected();
    }

}
