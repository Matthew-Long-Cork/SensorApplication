package com.google.android.apps.forscience.whistlepunk;

import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.forscience.whistlepunk.blew.MqttManager;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Arrays;

import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DatabaseConnectionService {

    private static String myWebsite = "", myWriteToken = "", myConnType;
    private static String experimentName;

    private static String mqttURL, url;
    private static int i = 0;
    private static final String mqttTag = "v1/devices/me/telemetry";

    private static MqttManager mqttManager;

    private static MqttAndroidClient mqttAndroidClient;
    //private static boolean isConnected = false;

    public static void setMyWebsiteAddress(String website){
        myWebsite = website;
    }

    public static void setMyAccessToken(String token){ myWriteToken = token; }

    public static void setMyConnectionType(String connType){

        myConnType = connType;
        // if mqtt is selected and there is not a current connection. Connect
        if(myConnType.equals("MQTT Connection")) {
            if (mqttAndroidClient == null) {
                mqttURL = "tcp://" + myWebsite + ":1883";
                mqttManager = new MqttManager();
                //mqttAndroidClient = new MqttAndroidClient(ExperimentDetailsFragment.context, mqttURL, "AppClient");
                try {
                    mqttManager.connect();
                } catch (Exception e) {
                    Log.e("Mqtt Connection: " ,  "Failed");
                }
            }
        }
        else
            url  = "http://" + myWebsite;
    }

    public static void sendData(DataObject dataObject){

        // get the experiment name
        experimentName = ExperimentDetailsFragment.getCurrentTitle();
        // check which option was selected:
        if(myConnType.equals("MQTT Connection")) {
            String jsonData = "{" + ( experimentName + "_" +  dataObject.Id) + ":" + dataObject.dataValue + "}";
            try {
                mqttManager.sendDataMqtt(jsonData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            sendDataHttp(dataObject);
        }
    }

    //==============================================================================================
    //  HTTP CONNECTION
    //==============================================================================================
    public static void sendDataHttp(DataObject dataObject){

        String sensorType,data,myUrl;
        Float sensorValue;
        sensorType = dataObject.Id;
        sensorValue = dataObject.dataValue;

        //data to send
        data = "{" + (experimentName + "_" + sensorType) + ":" + sensorValue + "}";

        myUrl = url + "/api/v1/" + myWriteToken + "/telemetry";

        try{

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
                    .build();

            RequestBody body = RequestBody.create( MediaType.get("application/json; charset=utf-8"),data);
            Request request = new Request.Builder()
                    .url(myUrl)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
            }
        }
        catch (Exception e) {
            System.out.println("    Error: " + sensorType + " "+ e);
        }
    }

    public static boolean isConnected(){
        if(myConnType.equals("MQTT Connection")){
            return  mqttManager.isConnected();
        } else {
            return  true;
        }
    }

    public static void kill() throws MqttException {
        mqttManager.kill();
    }
}