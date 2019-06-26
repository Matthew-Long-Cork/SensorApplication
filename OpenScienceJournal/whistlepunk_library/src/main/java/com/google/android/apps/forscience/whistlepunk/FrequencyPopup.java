package com.google.android.apps.forscience.whistlepunk;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;
import com.google.android.apps.forscience.whistlepunk.sensorapi.AbstractSensorRecorder;
import com.google.android.apps.forscience.whistlepunk.sensors.AmbientLightSensor;
import com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPicker;

import java.util.ArrayList;
import java.util.List;

public class FrequencyPopup extends Activity {

    private TextView myTextView, mySelectedSensor;
    private Spinner mySpinner;
    private String currentlySelectedUnits;
    private NumberPicker numberPicker, millisecondsNumberPicker;
    private Button setFrequencyBtn;

    boolean change;

    // stored data in app
    private SharedPreferences storedData;
    // get the title of the current experiment
    private String databaseName, experimentName, currentSensorName, experimentName_sensor_frequency;
    private int selectedValue, numberPicked, frequencyPicked;
    private List<String> spinnerArray = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_frequency_popup);

        // initialise text fields and buttons
        mySelectedSensor = findViewById(R.id.selected_sensor_display_lbl);
        myTextView = findViewById(R.id.select_frequency_lbl);
        mySpinner = findViewById(R.id.sensor_spinner);
        numberPicker = findViewById(R.id.number_picker);
        millisecondsNumberPicker = findViewById(R.id.number_picker2_because_number_pickers_are_stupid);
        setFrequencyBtn = findViewById(R.id.set_frequency_btn);

        // get the stored data
        storedData = getSharedPreferences("info", MODE_PRIVATE);
        // get the name from fragment
        experimentName = ExperimentDetailsFragment.getCurrentTitle();
        // get the current sensor from the intent
        currentSensorName = getIntent().getStringExtra("currentSensor");
        //display the currently selected sensor
        mySelectedSensor.setText(currentSensorName);
        // get the experiment title
        experimentName = ExperimentDetailsFragment.getCurrentTitle();
        // do not show the frequency label or spinner until time type is selected
        numberPicker.setVisibility(View.INVISIBLE);
        myTextView.setVisibility(View.INVISIBLE);
        //get the stored connection type for this experiment
        String word = experimentName + "_experimentConnectionType";
        String connType = storedData.getString(word, "");

        if (connType.equals("MQTT Connection")) {
            spinnerArray.add("Please Select");
            spinnerArray.add("Millisecond(s)");
            spinnerArray.add("Second(s)");
        }
        if (connType.equals("HTTP Connection")) {
            spinnerArray.add("Please Select");
            spinnerArray.add("Second(s)");
            spinnerArray.add("Minute(s)");
            spinnerArray.add("Hour(s)");
        }

        //create an adapter from the list
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinnerArray);
        //set the adapter on the spinner
        mySpinner.setAdapter(adapter);

        //==========================================================================================
        // this is the units dropdown
        //==========================================================================================
        mySpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                        // get the selected item
                        currentlySelectedUnits = mySpinner.getItemAtPosition(i).toString();

                        if(currentlySelectedUnits.equals("Please Select")){
                            numberPicker.setVisibility(View.INVISIBLE);
                            millisecondsNumberPicker.setVisibility(View.INVISIBLE);
                            myTextView.setVisibility(View.INVISIBLE);
                        }
                        // get the selected units and set the number picker values
                        if(currentlySelectedUnits.equals("Millisecond(s)")){
                            millisecondsNumberPicker.setMinValue(1);
                            millisecondsNumberPicker.setMaxValue(10);
                            millisecondsNumberPicker.setDisplayedValues( new String[] { "0", "100", "200", "300", "400", "500", "600", "700", "800", "900"  } );
                            numberPicker.setVisibility(View.INVISIBLE);
                            millisecondsNumberPicker.setVisibility(View.VISIBLE);
                            myTextView.setVisibility(View.VISIBLE);
                            //===============================================
                        }
                        if(currentlySelectedUnits.equals("Second(s)")|| currentlySelectedUnits.equals("Minute(s)")){
                            numberPicker.setMinValue(0);
                            numberPicker.setMaxValue(59);
                            millisecondsNumberPicker.setVisibility(View.INVISIBLE);
                            numberPicker.setVisibility(View.VISIBLE);
                            myTextView.setVisibility(View.VISIBLE);
                        }
                        if(currentlySelectedUnits.equals("Hour(s)")){
                            numberPicker.setMinValue(0);
                            numberPicker.setMaxValue(24);
                            millisecondsNumberPicker.setVisibility(View.INVISIBLE);
                            numberPicker.setVisibility(View.VISIBLE);
                            myTextView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                }
        );

        //==========================================================================================
        // this is the enter button
        //==========================================================================================
        setFrequencyBtn.setOnClickListener((View v)-> {

            if(!currentlySelectedUnits.equals("Please Select")) {
                if (currentlySelectedUnits.equals("Millisecond(s)")) {
                    if (millisecondsNumberPicker.getValue() == 1)
                        Toast.makeText(this, "Please select the frequency", Toast.LENGTH_SHORT).show();
                    else {
                        selectedValue = 100 * (millisecondsNumberPicker.getValue() - 1); // starts with '0' so -1 from value
                        processChange(selectedValue);
                    }
                }
                if (currentlySelectedUnits.equals("Second(s)")) {
                    if (numberPicker.getValue() == 0)
                        Toast.makeText(this, "Please select the frequency", Toast.LENGTH_SHORT).show();
                    else {
                        selectedValue = 1000 * numberPicker.getValue(); // starts with '0' so -1 from value
                        processChange(selectedValue);
                    }
                }
                if (currentlySelectedUnits.equals("Minute(s)")) {
                    if (numberPicker.getValue() == 0)
                        Toast.makeText(this, "Please select the frequency", Toast.LENGTH_SHORT).show();
                    else {
                        selectedValue = 60000 * numberPicker.getValue(); // starts with '0' so -1 from value
                        processChange(selectedValue);
                    }
                }
                if (currentlySelectedUnits.equals("Hour(s)")) {
                    if (numberPicker.getValue() == 0)
                        Toast.makeText(this, "Please select the frequency", Toast.LENGTH_SHORT).show();
                    else {
                        selectedValue = 3600000 * numberPicker.getValue(); // starts with '0' so -1 from value
                        processChange(selectedValue);
                    }
                }
            }
            else
                // if the time units are not selected
                Toast.makeText(this, "Pleas select the time units" , Toast.LENGTH_SHORT).show();
        });
    }// end of onCreate()

    private  void processChange(int selectedValue) {

        // dataTitle = 'experimentName'_'TheSensor'_'frequency'
        experimentName_sensor_frequency = experimentName + "_" + currentSensorName + "_frequency";
        //interface used for modifying values in a sharedPreference object
        SharedPreferences.Editor editor = storedData.edit();
        //==================================================================================
        // now we store the variable name= 'experimentName_sensor_frequency' in milliSeconds
        // put into the stored data
        editor.putInt(experimentName_sensor_frequency, selectedValue);
        // call the commit() method.
        editor.commit();
        // stop observing the sensor
        RecorderControllerImpl.stopObservingSelectedSensor(currentSensorName, "observerIdUnknown");
        // then finish
        finish();
    }
}// end of class


