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

public class DatabaseLinkSetup extends Activity {

    // declare the variables
    private String websiteAddress;
    private Button enterBtn, cancelBtn, confirmBtn;
    private EditText websiteAddressTxtBox;
    private TextView message1, message2, headerMessage;
    private SharedPreferences storedData;
    private static boolean CONNECTION_SETUP = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_database_link_setup);

        // initialise text fields and buttons
        headerMessage = findViewById(R.id.connection_setup_lbl);
        message1 = findViewById(R.id.connection_note_lbl);
        message2 = findViewById(R.id.connection_note_lbl2);
        websiteAddressTxtBox = findViewById(R.id.token_input_textbox);
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
        // get the stored web address, if any
        websiteAddress = storedData.getString("websiteAddress", websiteAddress);

        if (websiteAddress != null) {
            websiteAddressTxtBox.setText(websiteAddress);
        }

        //=====================================================================================
        // call function when 'enter' button is clicked
        //=====================================================================================
        enterBtn.setOnClickListener((View v) -> {

            // collect the input data
            websiteAddress = websiteAddressTxtBox.getText().toString();

            //check if there was data inputted
            if (websiteAddress.equals("")) {
                Toast.makeText(getApplicationContext(), "The Website Address is needed.", Toast.LENGTH_SHORT).show();
            }

            // if the text boxes have data, show confirmation window
            if (!websiteAddress.equals("")) {

                // hide what we need to hide
                message1.setVisibility(View.INVISIBLE);
                enterBtn.setVisibility(View.INVISIBLE);
                // show what we need to show
                message2.setVisibility(View.VISIBLE);
                cancelBtn.setVisibility(View.VISIBLE);
                confirmBtn.setVisibility(View.VISIBLE);
                // disable the text box as the user must confirm or cancel
                websiteAddressTxtBox.setEnabled(false);
            }
        });

        //=====================================================================================
        // call function when 'confirm' button is clicked
        //=====================================================================================
        confirmBtn.setOnClickListener((View v) -> {

            CONNECTION_SETUP = true;
            // interface used for modifying values in a sharedPreference object
            SharedPreferences.Editor editor = storedData.edit();
            editor.putString("websiteAddress", websiteAddress);
            editor.putBoolean("CONNECTION_SETUP", CONNECTION_SETUP);
            //finally, when you are done adding the values, call the commit() method.
            editor.commit();
            finish();
            // go back to the main activity window
            Intent intent = new Intent(DatabaseLinkSetup.this, MainActivity.class);
            DatabaseLinkSetup.this.startActivity(intent);
        });

        //=====================================================================================
        // call function when 'cancel' button is clicked
        //=====================================================================================
        cancelBtn.setOnClickListener((View v) -> {

            // clear the variables
            websiteAddress = "";

            // enable the text box for text enter
            websiteAddressTxtBox.setEnabled(true);

            //clear the text fields and focus on the website input field
            websiteAddressTxtBox.getText().clear();
            websiteAddressTxtBox.requestFocus();

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