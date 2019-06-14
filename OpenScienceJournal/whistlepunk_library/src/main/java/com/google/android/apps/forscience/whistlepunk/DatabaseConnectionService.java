package com.google.android.apps.forscience.whistlepunk;

import android.util.Log;

import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.Arrays;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DatabaseConnectionService {

    private static String myWebsite ="";
    private static String myWriteToken ="";
    private static String experimentName;

    private static final String mqttURL = "tcp://thingsboard.tec-gateway.com:1883";
    private static final String mqttTag = "v1/devices/me/telemetry";
    private static MqttAndroidClient mqttAndroidClient;

    public static void setMyWebsiteAddress(String website){
        myWebsite = website;
    }

    public static void setMyAccessToken(String token){ myWriteToken = token; }

    public static void sendData(DataObject dataObject){
        // get the experiment name
        experimentName = ExperimentDetailsFragment.getCurrentTitle();

        // check which option was selected:
        sendDataMqtt(dataObject);
        //sendDataHttp(dataObject);
    }

    //==============================================================================================
    //  TEMPORARY CONNECTION USING A STRING
    //==============================================================================================
    public static void sendDataWithTemporaryString(String json){

        String myUrl;
        String  data = json;

        // for thingsBoard:
        //==========================================================================================
        //myWriteToken = "temp";
        myWebsite = "http://thingsboard.tec-gateway.com";          //<-- for testing
        //==========================================================================================
        myUrl = myWebsite + "/api/v1/" + myWriteToken + "/telemetry";

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

                System.out.println("======================================");
                System.out.println("======================================");
                System.out.println( response.body().string());
                System.out.println("    sent: ");
                System.out.println("    website address: " + myWebsite);
                System.out.println("    website token: " + myWriteToken);
                System.out.println("    data : " + data);
                System.out.println("======================================");
                System.out.println("======================================");
            }
        }
        catch (Exception e) {

            System.out.println("\n====================================");
            System.out.println("======================================");
            System.out.println("      Error: " + e );
            System.out.println("======================================");
            System.out.println("======================================");
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

        // for thingsBoard:
        //==========================================================================================
        // myWriteToken = "temp";
        //myWebsite = "http://thingsboard.tec-gateway.com";              //<-- for testing
        //==========================================================================================
        myUrl = myWebsite + "/api/v1/" + myWriteToken + "/telemetry";

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

                System.out.println("======================================");
                System.out.println("======================================");
                System.out.println( response.body().string());
                System.out.println("    sent: ");
                System.out.println("    website address: " + myWebsite);
                System.out.println("    website token: " + myWriteToken);
                System.out.println("    data : " + data);
                System.out.println("======================================");
                System.out.println("======================================");
            }
        }
        catch (Exception e) {
            System.out.println("\n====================================");
            System.out.println("======================================");
            System.out.println("      Error: " + sensorType + " "+ e);
            System.out.println("======================================");
            System.out.println("======================================");
        }
    }

    //==============================================================================================
    //  MQTT CONNECTION
    //==============================================================================================
    public static void sendDataMqtt(DataObject dataObject){
        if(mqttAndroidClient.isConnected()) {

            String jsonData = "{" + ( experimentName + "_" +  dataObject.Id) + ":" + dataObject.dataValue + "}";
            try {
                mqttAndroidClient.publish(mqttTag, jsonData.getBytes(), 0, true);
            } catch (Exception e) {
                e.printStackTrace();
            } ;
        }
    }

    public static void mqttInit(){

        mqttAndroidClient = new MqttAndroidClient( ExperimentDetailsFragment.context, mqttURL, "AppClient");

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setUserName(myWriteToken);

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.e("MQTT Connection", "Success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT Connection", "Failed");
                }
            });
        }catch (Exception e){e.printStackTrace();}
    }

    public static void mqttDisconnect(){
        try {
            mqttAndroidClient.disconnect();
        }catch (Exception e){e.printStackTrace();}
    }
}
