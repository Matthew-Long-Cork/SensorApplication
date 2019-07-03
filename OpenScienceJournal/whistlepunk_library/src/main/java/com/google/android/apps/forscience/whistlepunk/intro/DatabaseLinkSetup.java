package com.google.android.apps.forscience.whistlepunk.intro;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.forscience.whistlepunk.DatabaseConnectionService;
import com.google.android.apps.forscience.whistlepunk.MainActivity;
import com.google.android.apps.forscience.whistlepunk.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseLinkSetup extends Activity {

    // declare the variables
    private String websiteAddress, currentWebsiteAddress, defaultAddress, websiteAddressType;
    private Button enterBtn, cancelBtn, confirmBtn;
    private EditText websiteAddressTxtBox;
    private Spinner spinner;
    private TextView message1, message2, headerMessage;
    private SharedPreferences storedData;
    private static boolean CONNECTION_SETUP;
    private  Set<String> experimentNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_database_link_setup);

        // initialise text fields and buttons
        headerMessage = findViewById(R.id.connection_setup_lbl);
        message1 = findViewById(R.id.connection_note_lbl);
        message2 = findViewById(R.id.connection_note_lbl2);
        spinner = (Spinner) findViewById(R.id.website_address_type_spinner);
        websiteAddressTxtBox = findViewById(R.id.website_address_input_textbox);
        enterBtn = findViewById(R.id.enter_btn);
        cancelBtn = findViewById(R.id.cancel_btn);
        confirmBtn = findViewById(R.id.confirm_btn);

        //hide what we need to hide
        cancelBtn.setVisibility(View.INVISIBLE);
        confirmBtn.setVisibility(View.INVISIBLE);
        message2.setVisibility(View.INVISIBLE);

        //=====================================================================================
        defaultAddress = "thingsboard.tec-gateway.com";
        //=====================================================================================
        // check if there is stored data for these values
        // Note: The user may want to stay connected the current connection configuration
        //=====================================================================================

        storedData = getSharedPreferences("info", MODE_PRIVATE);
        // get the stored web address type, if any
        websiteAddressType = storedData.getString("websiteAddressType", "");
        // get the stored web address, if any
        currentWebsiteAddress = storedData.getString("websiteAddress", "");

        //if there is currently a connection type saved, get it and display it
        if (websiteAddressType != null) {
            if (websiteAddressType.equals("Default")) {
                spinner.setSelection(1);
                websiteAddressTxtBox.setEnabled(false);
                websiteAddressTxtBox.setText(defaultAddress);
            }
            if (websiteAddressType.equals("Custom")) {
                spinner.setSelection(2);
                websiteAddressTxtBox.setEnabled(true);
                if(!(currentWebsiteAddress.equals(defaultAddress))){
                    websiteAddressTxtBox.setText(currentWebsiteAddress);
                }
            }
        }

        // when option selected
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // get option
                int optionIndex = spinner.getSelectedItemPosition();
                if (optionIndex == 0){
                    // if zero
                    websiteAddressTxtBox.setText("");
                    websiteAddressTxtBox.setEnabled(false);
                }
                if (optionIndex == 1){
                    // if one, default
                    websiteAddressTxtBox.setText(defaultAddress);
                    websiteAddressType = "Default";
                    websiteAddressTxtBox.setEnabled(false);
                }
                if (optionIndex == 2){
                    // allow text input
                    websiteAddressType = "Custom";
                    websiteAddressTxtBox.setEnabled(true);
                    // check if that value is not the default
                    if(currentWebsiteAddress.compareTo(defaultAddress)==0)
                        websiteAddressTxtBox.setText("");
                    else
                        websiteAddressTxtBox.setText(currentWebsiteAddress);
                }
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }
        });


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

            experimentNames = new HashSet<>();

            CONNECTION_SETUP = true;
            // send this to the DatabaseConnectionService.java to be used later
            DatabaseConnectionService.setMyWebsiteAddress(websiteAddress);
            // interface used for modifying values in a sharedPreference object
            SharedPreferences.Editor editor = storedData.edit();
            editor.putString("websiteAddress", websiteAddress);
            editor.putString("websiteAddressType", websiteAddressType);
            editor.putBoolean("CONNECTION_SETUP", CONNECTION_SETUP);
            editor.putStringSet("experimentNames",experimentNames);
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

            // clear the value
            websiteAddress = "";
            //clear the text field
            websiteAddressTxtBox.getText().clear();
            // disable the text box
            websiteAddressTxtBox.setEnabled(false);
            spinner.setSelection(0);
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