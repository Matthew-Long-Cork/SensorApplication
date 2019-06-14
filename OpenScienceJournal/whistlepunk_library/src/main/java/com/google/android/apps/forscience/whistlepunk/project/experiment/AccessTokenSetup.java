package com.google.android.apps.forscience.whistlepunk.project.experiment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.forscience.whistlepunk.DatabaseConnectionService;
import com.google.android.apps.forscience.whistlepunk.R;

public class AccessTokenSetup extends AppCompatActivity  {

    // declare the variables
    private String experimentAccessToken;
    private Button enterBtn, cancelBtn, confirmBtn;
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
        experimentAccessToken = storedData.getString( currentTitle + "_experimentAccessToken", experimentAccessToken);

        //if there is currently a token saved, get it and display it
        if (experimentAccessToken != null) {
            websiteTokenTxtBox.setText(experimentAccessToken);
        }

        //if the old title is not "" then get it and display it
        if(!currentTitle.equals("")){
            experimentAccessToken = storedData.getString( currentTitle + "_experimentAccessToken", experimentAccessToken);
            websiteTokenTxtBox.setText(experimentAccessToken);
        }

        //=====================================================================================
        // call function when 'enter' button is clicked
        //=====================================================================================
        enterBtn.setOnClickListener((View v) -> {

            // collect the input data
            experimentAccessToken = websiteTokenTxtBox.getText().toString();

            //check if there was data inputted
            if (experimentAccessToken.equals("")) {
                Toast.makeText(getApplicationContext(), "The access token is needed.", Toast.LENGTH_SHORT).show();
            }

            // if the text boxes have data, show confirmation window
            if (!experimentAccessToken.equals("")) {

                // hide what we need to hide
                message1.setVisibility(View.INVISIBLE);
                enterBtn.setVisibility(View.INVISIBLE);
                // show what we need to show
                message2.setVisibility(View.VISIBLE);
                cancelBtn.setVisibility(View.VISIBLE);
                confirmBtn.setVisibility(View.VISIBLE);
                // disable the text box as the user must confirm or cancel
                websiteTokenTxtBox.setEnabled(false);
            }
        });

        //=====================================================================================
        // call function when 'confirm' button is clicked
        //=====================================================================================
        confirmBtn.setOnClickListener((View v) -> {

            // collect the input data
            experimentAccessToken = websiteTokenTxtBox.getText().toString();

            //check if there was data inputted
            if (experimentAccessToken.equals("")) {
                Toast.makeText(getApplicationContext(), "The access token is needed.", Toast.LENGTH_SHORT).show();
            }

            // if the text boxes have data, show confirmation window
            if (!experimentAccessToken.equals("")) {
                // interface used for modifying values in a sharedPreference
                SharedPreferences.Editor editor = storedData.edit();

                String currentToken = currentTitle + "_experimentAccessToken";
                // remove the old variable as it will no longer be referenced
                storedData.edit().remove(currentToken).commit();
                // then add the new token
                editor.putString(currentTitle + "_experimentAccessToken", experimentAccessToken);

                //finally, when you are done adding the values, call the commit() method.
                editor.commit();

                // returns RESULT_OK
                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        //=====================================================================================
        // call function when 'cancel' button is clicked
        //=====================================================================================
        cancelBtn.setOnClickListener((View v) -> {

            // clear the variables
            experimentAccessToken = "";

            // enable the text box for text enter
            websiteTokenTxtBox.setEnabled(true);

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
