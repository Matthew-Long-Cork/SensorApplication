package com.google.android.apps.forscience.whistlepunk.project.experiment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.forscience.whistlepunk.R;

public class AccessTokenSetupAndConnType extends AppCompatActivity  {

    // declare the variables
    private String currentExperimentAccessToken, experimentAccessToken, currentExperimentConnectionType, experimentConnectionType;
    private Button enterBtn, cancelBtn, confirmBtn;
    private Spinner spinner;
    private EditText websiteTokenTxtBox;
    private TextView message1, message2, headerMessage;
    private SharedPreferences storedData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_access_token_setup);

        // initialise text fields and buttons
        headerMessage = findViewById(R.id.connection_setup_lbl);
        message1 = findViewById(R.id.connection_note_lbl);
        message2 = findViewById(R.id.connection_note_lbl2);
        websiteTokenTxtBox = findViewById(R.id.token_input_textbox);

        spinner = (Spinner) findViewById(R.id.connection_spinner);
        enterBtn = findViewById(R.id.connection_btn);
        cancelBtn = findViewById(R.id.cancel_btn);
        confirmBtn = findViewById(R.id.confirm_btn);

        //hide what we need to hide
        cancelBtn.setVisibility(View.INVISIBLE);
        confirmBtn.setVisibility(View.INVISIBLE);
        message2.setVisibility(View.INVISIBLE);

        // get the stored data
        storedData = getSharedPreferences("info", MODE_PRIVATE);

        //get the current intent
        Intent intent = getIntent();
        String currentTitle = intent.getStringExtra("CURRENT_TITLE");

        // get the stored web token, if any
        currentExperimentAccessToken = storedData.getString( currentTitle + "_experimentAccessToken", experimentAccessToken);
        // get the stored connection type, if any
        currentExperimentConnectionType = storedData.getString( currentTitle + "_experimentConnectionType", experimentConnectionType);

        //if there is currently a token saved, get it and display it
        if(!currentExperimentAccessToken.equals("")){
            websiteTokenTxtBox.setText(currentExperimentAccessToken);
        }

        //if there is currently a connection type saved, get it and display it
        if (currentExperimentConnectionType != null) {
            if (currentExperimentConnectionType.equals("HTTP Connection"))
                spinner.setSelection(1);
            else if(currentExperimentConnectionType.equals("MQTT Connection"))
                spinner.setSelection(2);
        }

        //=====================================================================================
        // call function when 'enter' button is clicked
        //=====================================================================================
        enterBtn.setOnClickListener((View v) -> {

            // collect the input data
            experimentAccessToken = websiteTokenTxtBox.getText().toString();
            experimentConnectionType = spinner.getSelectedItem().toString();

            // if nothing is selected
            if (experimentAccessToken.equals("") && (experimentConnectionType.equals("Please Select"))) {
                Toast.makeText(getApplicationContext(), "Please enter data.", Toast.LENGTH_SHORT).show();
            }
            //check for the token
            else if (experimentAccessToken.equals("")) {
                Toast.makeText(getApplicationContext(), "The access token is needed.", Toast.LENGTH_SHORT).show();
            }
            //check for the selected connection
            else if (experimentConnectionType.equals("Please Select")) {
                Toast.makeText(getApplicationContext(), "The connection type is needed.", Toast.LENGTH_SHORT).show();
            }

            // if the boxes have data, show confirmation window
            if (!experimentAccessToken.equals("") && !experimentConnectionType.equals("Please Select")) {

                // hide what we need to hide
                message1.setVisibility(View.INVISIBLE);
                enterBtn.setVisibility(View.INVISIBLE);
                // show what we need to show
                message2.setVisibility(View.VISIBLE);
                cancelBtn.setVisibility(View.VISIBLE);
                confirmBtn.setVisibility(View.VISIBLE);
                // disable input as the user must confirm or cancel
                websiteTokenTxtBox.setEnabled(false);
                spinner.setEnabled(false);
            }
        });

        //=====================================================================================
        // call function when 'confirm' button is clicked
        //=====================================================================================
        confirmBtn.setOnClickListener((View v) -> {

            // interface used for modifying values in a sharedPreference
            SharedPreferences.Editor editor = storedData.edit();

            // before we go swapping out stored values, check if they differ from the current values
            if(currentExperimentAccessToken != experimentAccessToken)
                editor.putString(currentTitle + "_experimentAccessToken", experimentAccessToken);

            if(currentExperimentConnectionType != experimentConnectionType)
                editor.putString(currentTitle + "_experimentConnectionType", experimentConnectionType);

            //finally, when you are done adding the values, call the apply() method.
            editor.apply();  // done in the background
            // returns RESULT_OK
            Intent resultIntent = new Intent();
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        //=====================================================================================
        // call function when 'cancel' button is clicked
        //=====================================================================================
        cancelBtn.setOnClickListener((View v) -> {

            // clear the variable
            experimentAccessToken = "";

            // enable user input
            websiteTokenTxtBox.setEnabled(true);
            spinner.setEnabled(true);

            //clear the text fields and focus on the website input field
            websiteTokenTxtBox.getText().clear();
            websiteTokenTxtBox.requestFocus();

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
}
