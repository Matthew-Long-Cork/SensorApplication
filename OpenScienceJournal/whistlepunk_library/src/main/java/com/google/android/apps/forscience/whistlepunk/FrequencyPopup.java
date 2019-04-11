package com.google.android.apps.forscience.whistlepunk;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;
import com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPicker;

import java.util.ArrayList;
import java.util.List;

public class FrequencyPopup extends Activity {

    private Spinner mySpinner;
    private TextView myTextView, mySelectedSensor;
    private String currentlySelected;
    private ScrollableNumberPicker myScrollableNumberPicker;
    private Button setFrequencyBtn;

    // stored data in app
    private SharedPreferences storedData;
    // get the title of the current experiment
    private String experimentName, currentSensorName, mySensorDisplayName, databaseName, sensorID, experimentName_sensor_frequency;
    private int numberPicked, frequencyPicked, milliSeconds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_frequency_popup);

        // initialise text fields and buttons
        //mySpinner = findViewById(R.id.sensor_spinner);
        mySelectedSensor = findViewById(R.id.selected_sensor_display_lbl);
        myTextView = findViewById(R.id.frequency_units_lbl);
        mySpinner = findViewById(R.id.sensor_spinner);
        myScrollableNumberPicker = findViewById(R.id.frequency_picker);
        setFrequencyBtn = findViewById(R.id.set_frequency_btn);

        // get the intent and get the string passed in
        currentSensorName = getIntent().getStringExtra("currentSensor");
        //display the currently selected sensor
        mySelectedSensor.setText(currentSensorName);
        // get the experiment title
        experimentName = ExperimentDetailsFragment.getCurrentTitle();
        // this need to be called after the spinner is initialised
        setupSensorDisplayName();

        // if one of the below sensors the time units will be in milliseconds:second
        if (mySensorDisplayName.equals("General Acceleration") ||
                mySensorDisplayName.equals("X-axis Acceleration") ||
                mySensorDisplayName.equals("Y-axis Acceleration") ||
                mySensorDisplayName.equals("Z-axis Acceleration")) {

            addItemsToMySpinner(1);
        }
        // else the units will be second:minutes:hours
        else {
            addItemsToMySpinner(2);
        }

        //==========================================================================================
        // this is the second dropdown - units
        //==========================================================================================
        mySpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                        // get the selected item
                        currentlySelected = mySpinner.getItemAtPosition(i).toString();

                        if(currentlySelected.equals("Please Select")){
                            //setFrequencyBtn.setEnabled(false);
                        }
                        // get the selected units anmd set the number picker values
                        if(currentlySelected.equals("Millisecond(s)")){
                            milliSeconds = 1;
                            showPicker(100, 900,100, 100);
                        }
                        if(currentlySelected.equals("Second(s)")){
                            milliSeconds = 1000;
                            showPicker(1, 59,1, 1);
                        }
                        if(currentlySelected.equals("Minute(s)")){
                            milliSeconds = 60000;
                            showPicker(1, 59,1, 1);
                        }
                        if(currentlySelected.equals("Hour(s)")){
                            milliSeconds = 3600000;
                            showPicker(1, 24,1, 1);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                }
        );

        //use the getPostion() -1 to get sensor ID and change frequency with that... not the spinner title.... duuh!!

        //==========================================================================================
        // this is the enter button
        //==========================================================================================
        setFrequencyBtn.setOnClickListener((View v)-> {

            // if the time units are not selected
            if(currentlySelected.equals("Please Select")) {
                Toast.makeText(this, "Pleas select the time units" , Toast.LENGTH_SHORT).show();
            }
            // all good. Store the new data
            else {
                // the database name we will send to ( for toast message only)
                databaseName = experimentName + "_" + currentSensorName;
                // dataTitle = 'experimentName'_'TheSensor'_'frequency'
                experimentName_sensor_frequency =  experimentName + "_" + currentSensorName + "_frequency";
                //==================================================================================
                // get the number picker value
                numberPicked = myScrollableNumberPicker.getValue();
                //==================================================================================
                // get the stored data
                storedData = getSharedPreferences("info", MODE_PRIVATE);
                //interface used for modifying values in a sharedPreference object
                SharedPreferences.Editor editor = storedData.edit();
                //==================================================================================
                // now we store the variable name= 'experimentName_sensor_frequency' in milliSeconds
                frequencyPicked = numberPicked*milliSeconds;
                // put into the stored data
                editor.putInt(experimentName_sensor_frequency, frequencyPicked );
                //finally, call the commit() method.
                editor.commit();
                Toast.makeText(this, mySensorDisplayName + " Sensor will send data every " + frequencyPicked + " milliseconds to " + databaseName, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }// end of onCreate()


    private void setupSensorDisplayName(){

        // get the intent and get the string passed in
        currentSensorName = getIntent().getStringExtra("currentSensor");

        if(currentSensorName.equals("AmbientLightSensor"))
            mySensorDisplayName = "Light";
        if(currentSensorName.equals("DecibelSource"))
            mySensorDisplayName = "Sound";
        if(currentSensorName.equals("LinearAccelerometerSensor"))
            mySensorDisplayName = "General Acceleration";
        if(currentSensorName.equals("AccX"))              // left/right tilt
            mySensorDisplayName = "X-axis Acceleration";
        if(currentSensorName.equals("AccY"))              // front/back tilt
            mySensorDisplayName = "Y-axis Acceleration";
        if(currentSensorName.equals("AccZ"))              // up/down tilt
            mySensorDisplayName = "Z-axis Acceleration";
        if(currentSensorName.equals("CompassSensor"))
            mySensorDisplayName = "Compass Degrees";
        if(currentSensorName.equals("MagneticRotationSensor"))
            mySensorDisplayName = "Magnetic Level";

        //display the currently selected sensor
        mySelectedSensor.setText(mySensorDisplayName);
    }

    // add items to the second spinner
    public void addItemsToMySpinner(int x) {

        List<String> spinnerArray = new ArrayList<>();

        if (x== 1) {
            spinnerArray.add("Please Select");
            spinnerArray.add("Millisecond(s)");
            spinnerArray.add("Second(s)");
        }
        else{
            spinnerArray.add("Please Select");
            spinnerArray.add("Second(s)");
            spinnerArray.add("Minute(s)");
            spinnerArray.add("Hour(s)");
        }
        //create an adapter from the list
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinnerArray);
        //set the adapter on the spinner
        mySpinner.setAdapter(adapter);
    }

    public void showPicker(int x, int y, int z, int z2) {

        // show the number picker amd set the correct values
        myScrollableNumberPicker.setVisibility(View.VISIBLE);
        myScrollableNumberPicker.setMinValue(x);
        myScrollableNumberPicker.setMaxValue(y);
        myScrollableNumberPicker.setValue(z);
        myScrollableNumberPicker.setStepSize(z2);
    }
}


