package com.google.android.apps.forscience.whistlepunk.intro;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.forscience.whistlepunk.MainActivity;
import com.google.android.apps.forscience.whistlepunk.R;

public class ConnectionSetup extends Activity {

    // declare the variables
    private String websiteAddress, websiteToken;
    private Button enterBtn, cancelBtn, confirmBtn;
    private EditText websiteAddressTxtBox, websiteTokenTxtBox;
    private TextView message1, message2, headerMessage;
    private SharedPreferences storedData;
    private static boolean CONNECT_SETUP = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connection_popup);

        // initialise text fields and buttons
        headerMessage = findViewById(R.id.connection_setup_lbl);
        message1 = findViewById(R.id.connection_note_lbl);
        message2 = findViewById(R.id.connection_note_lbl2);
        websiteAddressTxtBox = findViewById(R.id.website_input_textbox);
        websiteTokenTxtBox = findViewById(R.id.token_input_textbox);
        enterBtn = findViewById(R.id.connection_btn);
        cancelBtn = findViewById(R.id.cancel_btn);
        confirmBtn = findViewById(R.id.confirm_btn);

        //hide what we need to hide
        cancelBtn.setVisibility(View.INVISIBLE);
        confirmBtn.setVisibility(View.INVISIBLE);
        message2.setVisibility(View.INVISIBLE);

        //=====================================================================================
        // check if there is stored data for these values
        // Note: The user may want to stay connected the current connection configuration
        //=====================================================================================
        storedData = getSharedPreferences("info", MODE_PRIVATE);

        websiteAddress = storedData.getString("websiteAddress", websiteAddress);
        websiteToken = storedData.getString("websiteToken", websiteToken);

        if(websiteAddress != null && websiteToken != null){
            websiteAddressTxtBox.setText(websiteAddress);
            websiteTokenTxtBox.setText(websiteToken);
        }

        //=====================================================================================
        // call function when 'enter' button is clicked
        //=====================================================================================
        enterBtn.setOnClickListener((View v) -> {

            // collect the input data
            websiteAddress = websiteAddressTxtBox.getText().toString();
            websiteToken = websiteTokenTxtBox.getText().toString();

            //check if there was data inputted
            if (websiteAddress.equals("")) {
                Toast.makeText(getApplicationContext(), "The Website Address is needed.", Toast.LENGTH_SHORT).show();
            }
            if (websiteToken.equals("")) {
                Toast.makeText(getApplicationContext(), "The Website Token is needed.", Toast.LENGTH_SHORT).show();
            }
            if (websiteAddress.equals("") && websiteToken.equals("")) {
                Toast.makeText(getApplicationContext(), "The Website Address and Website Token are needed.", Toast.LENGTH_LONG).show();
            }
            // if the text boxes have data, show confirmation window
            if (!websiteAddress.equals("") && !websiteToken.equals("")) {

                // hide what we need to hide
                headerMessage.setVisibility(View.INVISIBLE);
                message1.setVisibility(View.INVISIBLE);
                enterBtn.setVisibility(View.INVISIBLE);
                // show what we need to show
                message2.setVisibility(View.VISIBLE);
                cancelBtn.setVisibility(View.VISIBLE);
                confirmBtn.setVisibility(View.VISIBLE);
            }
        });

        //=====================================================================================
        // call function when 'confirm' button is clicked
        //=====================================================================================
        confirmBtn.setOnClickListener((View v) -> {

            CONNECT_SETUP = true;

            // interface used for modifying values in a sharedPreference object
            SharedPreferences.Editor editor = storedData.edit();
            editor.putString("websiteAddress", websiteAddress);
            editor.putString("websiteToken", websiteToken);
            editor.putBoolean("CONNECT_SETUP", CONNECT_SETUP);
            //finally, when you are done adding the values, call the commit() method.
            editor.commit();
            finish();
            // go back to the main activity window
            Intent intent = new Intent(ConnectionSetup.this, MainActivity.class);
            ConnectionSetup.this.startActivity(intent);
        });

        //=====================================================================================
        // call function when 'cancel' button is clicked
        //=====================================================================================
        cancelBtn.setOnClickListener((View v) -> {

            // clear the variables
            websiteAddress = "";
            websiteToken = "";

            //clear the text fields and focus on the website input field
            websiteAddressTxtBox.getText().clear();
            websiteAddressTxtBox.requestFocus();

            websiteTokenTxtBox.getText().clear();

            // switch the elements back:
            // show what we need to hide
            headerMessage.setVisibility(View.VISIBLE);
            message1.setVisibility(View.VISIBLE);
            enterBtn.setVisibility(View.VISIBLE);
            // hide what we need to show
            message2.setVisibility(View.INVISIBLE);
            cancelBtn.setVisibility(View.INVISIBLE);
            confirmBtn.setVisibility(View.INVISIBLE);

        });
    }
/*
    private void setDefaultSensors() {
        //==========================================================================================
        // we need to create stored data variables for each new set of sensors as the experiment is created
        //==========================================================================================

        // the 10 sensors we use:
        List newList = new ArrayList();
        newList.add("AmbientLight");
        newList.add("DecibelSource");
        newList.add("LinearAccelerometer");
        newList.add("AccX");
        newList.add("AccY");
        newList.add("AccZ");
        newList.add("Compass");
        newList.add("MagneticRotation");
        newList.add("RemoteTemperature");
        newList.add("RemoteHumidity");

        // get the name of this new stored variable
        //for(int i =0; i< newList.size(); i++) {
        for (int i = 0; i < 1; i++) {

            // name  of the experiment + the current sensor + '_frequency'
            // this is the new stored data variable:
            newDataVariableName = title + "_" + newList.get(i) + "_frequency";

            editor.putInt(newDataVariableName, frequency);
        }
        //finally, when you are done adding the values, call the commit() method to commit all
        editor.commit();
        finish();

        //now check what we have in the stored values:
        int x = storedData.getInt(newDataVariableName, 0);

        System.out.println("======================================");
        System.out.println("                  ");
        System.out.println("======================================");
        System.out.println("1");
        System.out.println("2");
        System.out.println("3     name: " + mActiveExperiment.getValue().getTitle());
        System.out.println("4");
        System.out.println("5");
        System.out.println("======================================");
        System.out.println("                  ");
        System.out.println("======================================");
        //==========================================================================================
*/

}