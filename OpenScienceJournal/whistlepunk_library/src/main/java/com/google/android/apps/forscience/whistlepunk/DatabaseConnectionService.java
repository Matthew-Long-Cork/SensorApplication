package com.google.android.apps.forscience.whistlepunk;

import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;

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

    public static void setData(String website, String token){

        myWebsite = website;
        myWriteToken = token;
    }

    public static void sendData(DataObject dataObject){

       String sensorType;
       Float sensorValue;
       String dataField = null;
       String data;
       String experimentName;
       String myUrl;

       sensorType = dataObject.Id;
       sensorValue = dataObject.dataValue;

       experimentName = ExperimentDetailsFragment.getCurrentTitle();

       if(sensorType == "AmbientLightSensor")       // light
           dataField = experimentName + "_" + "AmbientLight";
       if(sensorType == "DecibelSource")            // sound
           dataField = experimentName + "_" + "DecibelSource";
       if(sensorType == "LinearAccelerometerSensor")// accelerometer
           dataField = experimentName + "_" + "GeneralAcceleration";
       if(sensorType == "AccX")       // left/right tilt     (AccelerometerSensor.java)
           dataField = experimentName + "_" + "X-axisAcceleration";
       if(sensorType == "AccY")              // front/back tilt      (AccelerometerSensor.java)
           dataField = experimentName + "_" + "Y-axisAcceleration";
       if(sensorType == "AccZ")              // up/down tilt         (AccelerometerSensor.java)
           dataField = experimentName + "_" + "Z-axisAcceleration";
       if(sensorType == "CompassSensor")            // compass degrees
           dataField = experimentName + "_" + "CompassDegrees";
       if(sensorType == "MagneticRotationSensor")   // magnetic levels
           dataField = experimentName + "_" + "MagneticLevel";

       //==========================================================================
       // this is the thingsBoard connection
       // ==========================================================================
       //data to send
       data = "{" + dataField + ":" + sensorValue + "}";
       // for thingsBoard: token/website/the full URL link.
       myWriteToken = "I6oQFSEuGEOyt0MapzOq";
       myWebsite = "http://thingsboard.tec-gateway.com";
       myUrl = myWebsite + "/api/v1/" + myWriteToken + "/telemetry";

        System.out.println("======================================");
        System.out.println("                  ");
        System.out.println("======================================");
        System.out.println("1");
        System.out.println("2");
        System.out.println("    sending data: ");
        System.out.println("    data : " + data);
        System.out.println("4");
        System.out.println("5");
        System.out.println("======================================");
        System.out.println("                  ");
        System.out.println("======================================");


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
               System.out.println( response.body().string());
           }

       }
       catch (Exception e) {

           System.out.println("\n====================================");
           System.out.println("                  ");
           System.out.println("======================================");
           System.out.println("1");
           System.out.println("2");
           System.out.println("3     Error: " + sensorType);
           System.out.println("4");
           System.out.println("5");
           System.out.println("1");
           System.out.println("2");
           System.out.println("3       " + e);
           System.out.println("4");
           System.out.println("5");
           System.out.println("======================================");
           System.out.println("======================================");

       }
   }
}
