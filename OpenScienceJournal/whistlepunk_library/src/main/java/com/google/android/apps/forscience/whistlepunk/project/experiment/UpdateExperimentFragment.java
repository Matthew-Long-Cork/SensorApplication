/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.android.apps.forscience.whistlepunk.project.experiment;

import android.app.Activity;

import android.content.Context;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.apps.forscience.javalib.Success;
import com.google.android.apps.forscience.whistlepunk.AccessibilityUtils;
import com.google.android.apps.forscience.whistlepunk.AppSingleton;
import com.google.android.apps.forscience.whistlepunk.CurrentTimeClock;
import com.google.android.apps.forscience.whistlepunk.DataController;
import com.google.android.apps.forscience.whistlepunk.LoggingConsumer;
import com.google.android.apps.forscience.whistlepunk.PermissionUtils;
import com.google.android.apps.forscience.whistlepunk.PictureUtils;
import com.google.android.apps.forscience.whistlepunk.R;
import com.google.android.apps.forscience.whistlepunk.RxEvent;
import com.google.android.apps.forscience.whistlepunk.WhistlePunkApplication;
import com.google.android.apps.forscience.whistlepunk.analytics.TrackerConstants;
import com.google.android.apps.forscience.whistlepunk.filemetadata.Experiment;
import com.google.android.apps.forscience.whistlepunk.filemetadata.FileMetadataManager;
import com.google.android.apps.forscience.whistlepunk.sensors.AccelerometerSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.AmbientLightSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.CompassSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.DecibelSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.LinearAccelerometerSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.MagneticStrengthSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.sensortag.BarometerSensorT;
import com.google.android.apps.forscience.whistlepunk.sensors.sensortag.HumiditySensorT;
import com.google.android.apps.forscience.whistlepunk.sensors.sensortag.LightSensorT;
import com.google.android.apps.forscience.whistlepunk.sensors.sensortag.TemperatureSensorT;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.reactivex.subjects.BehaviorSubject;

import static android.content.Context.MODE_PRIVATE;

/**
 * Fragment for saving/updating experiment details (title, description...etc).
 */
public class UpdateExperimentFragment extends Fragment {

    private static final String TAG = "UpdateExperimentFrag";

    /**
     * Indicates the experiment ID we're currently updating.
     */
    public static final String ARG_EXPERIMENT_ID = "experiment_id";

    private static final String KEY_SAVED_PICTURE_PATH = "picture_path";

    private String mExperimentId;
    private BehaviorSubject<Experiment> mExperiment = BehaviorSubject.create();
    private ImageView mPhotoPreview;
    private String mPictureLabelPath = null;
    private RxEvent mSaved = new RxEvent();

    //==============================================================================================
    private SharedPreferences storedData;
    private int Frequency = 5000;
    private int BleFrequency = 5000;
    private String newSensorVariableFrequency;
    private String newSensorVariableState;
    private String defaultTitle = "Untitled Experiment";
    private String currentTitle;
    private String newValue;
    private static List sensorsList;
    private boolean swap = false, state;
    private String variableName, variableName2;
    private boolean ignoreChange = false;
    Set<String> existingExperiments;
    private static Context context;
    //==============================================================================================

    public UpdateExperimentFragment() {

    }
    public static List getSensorsList(){
        return sensorsList;
    }

    public static UpdateExperimentFragment newInstance(String experimentId) {
        UpdateExperimentFragment fragment = new UpdateExperimentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXPERIMENT_ID, experimentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExperimentId = getArguments().getString(ARG_EXPERIMENT_ID);
        if (savedInstanceState != null) {
            mPictureLabelPath = savedInstanceState.getString(KEY_SAVED_PICTURE_PATH);
        }
        getDataController().getExperimentById(mExperimentId,
                new LoggingConsumer<Experiment>(TAG, "load experiment") {
                    @Override
                    public void success(Experiment experiment) {
                        attachExperimentDetails(experiment);
                    }
                });

        context = getContext();

        // get preferences
        storedData = this.getContext().getSharedPreferences("info", MODE_PRIVATE);
        // set of already created titles
        existingExperiments = storedData.getStringSet("experimentNames",null);

        getActivity().setTitle(getString(R.string.title_activity_update_experiment));
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_update_experiment, menu);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        actionBar.setHomeActionContentDescription(android.R.string.cancel);

        super.onCreateOptionsMenu(menu, inflater);

    }


//==================================================================================================

/*
    AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            final Drawable upArrow = ContextCompat.getDrawable(activity,
                    R.drawable.ic_arrow_back_white_24dp);
            {
                upArrow.setAlpha(
                        getResources().getInteger(R.integer.home_enabled_drawable_alpha));
            }
            actionBar.setHomeAsUpIndicator(upArrow);
        }
    }
        */
//==================================================================================================






    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            // respond to save clicked
            mSaved.onHappened();
            WhistlePunkApplication.getUsageTracker(getActivity())
                                  .trackEvent(TrackerConstants.CATEGORY_EXPERIMENTS,
                                          TrackerConstants.ACTION_EDITED,
                                          TrackerConstants.LABEL_UPDATE_EXPERIMENT, 0);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        mSaved.onDoneHappening();
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        WhistlePunkApplication.getUsageTracker(getActivity()).trackScreenView(
                TrackerConstants.SCREEN_UPDATE_EXPERIMENT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_experiment, container, false);
        EditText newTitle = (EditText) view.findViewById(R.id.experiment_title);
        mPhotoPreview = (ImageView) view.findViewById(R.id.experiment_cover);
        Button chooseButton = (Button) view.findViewById(R.id.btn_choose_photo);
        ImageButton takeButton = (ImageButton) view.findViewById(R.id.btn_take_photo);

        // Set the color of the placeholder drawable. This isn't used anywhere else
        // so we don't need to mutate() the drawable.
        mPhotoPreview.getDrawable().setColorFilter(
                mPhotoPreview.getResources().getColor(R.color.text_color_light_grey),
                PorterDuff.Mode.SRC_IN);

        mExperiment.subscribe(experiment -> {

            newTitle.setText(currentTitle);
            newTitle.setSelection(currentTitle.length()); // focus for next char

            mSaved.happens().subscribe(o -> {
                //get the user input
                String input =  newTitle.getText().toString().trim();
                // if new experiment, check for valid title
                if (currentTitle.equals("Untitled Experiment")) {
                    // it the title is blank
                    if(input.equals(null) || input.equals(""))
                        Toast.makeText(getContext(), "Please enter the experiment title.", Toast.LENGTH_SHORT).show();
                    else
                        saveChanges(experiment, newTitle);
                }
                // else if title is bring modified
                else if (!(currentTitle.equals("Untitled Experiment"))) {
                    // it the title is still the same
                    if (currentTitle.equals(input))
                        Toast.makeText(getContext(), "Title is still the same.", Toast.LENGTH_SHORT).show();
                    // else if the title is now blank
                    else if(input.equals(null) || input.equals(""))
                        Toast.makeText(getContext(), "Please enter the experiment title.", Toast.LENGTH_SHORT).show();
                    else if(existingExperiments.contains(newValue))
                        notifyUser();
                    else {
                        // inform the user of the changes that are about to happen.
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        //Yes button clicked
                                        saveChanges(experiment, newTitle);
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        //No button clicked
                                        Toast.makeText(getContext(), "Changes cancelled.", Toast.LENGTH_SHORT).show();
                                        getActivity().finish();
                                        break;
                                }
                            }
                        };

                        //  yes/no? prompt for user
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage("Changing the title will change the name of the values being sent to Thingsboard.com").setPositiveButton("Continue", dialogClickListener)
                                .setNegativeButton("Cancel", dialogClickListener).show();
                    }
                }
            });

            if (!TextUtils.isEmpty(experiment.getExperimentOverview().imagePath)) {
                // Load the current experiment photo
                PictureUtils.loadExperimentOverviewImage(mPhotoPreview,
                        experiment.getExperimentOverview().imagePath);
            }
            chooseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PictureUtils.launchPhotoPicker(UpdateExperimentFragment.this);
                }
            });
            takeButton.setOnClickListener(new View.OnClickListener() {
                // TODO: Take photo
                @Override
                public void onClick(View view) {
                    PermissionUtils.tryRequestingPermission(getActivity(),
                            PermissionUtils.REQUEST_CAMERA,
                            new PermissionUtils.PermissionListener() {
                                @Override
                                public void onPermissionGranted() {
                                    mPictureLabelPath =
                                            PictureUtils.capturePictureLabel(getActivity(),
                                                    mExperimentId, mExperimentId);
                                }

                                @Override
                                public void onPermissionDenied() {

                                }

                                @Override
                                public void onPermissionPermanentlyDenied() {

                                }
                            });
                }
            });
        });

        newTitle.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE) {
                newTitle.clearFocus();
                newTitle.setFocusable(false);
            }
            return false;
        });
        newTitle.setOnTouchListener((v, e) -> {

            newTitle.setFocusableInTouchMode(true);
            newTitle.requestFocus();
            return false;
        });

        newTitle.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // when user starts typing:
                // need the ignoreChange boolean to stop loop
                if(currentTitle == defaultTitle && !ignoreChange) {
                    ignoreChange = true;

                    // delete/backspace pressed
                    if (before > count) {
                        newTitle.setText("");
                    } else {
                        if (Character.isLetterOrDigit(s.charAt(start - 1))) {
                            newTitle.setText(String.valueOf(s.charAt(newTitle.length() - 1)).toUpperCase());
                            newTitle.setSelection(1);
                        }
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return view;
    }

    private void saveChanges(Experiment experiment, EditText newTitle){

        // interface used for modifying values in a sharedPreference object
        SharedPreferences.Editor editor = storedData.edit();

        // get the user input
        newValue = newTitle.getText().toString().trim();
        // check if 'newValue' is in set
        if(existingExperiments.contains(newValue)){
          notifyUser();
        }
        //compare to currently stored title
        else if(!newValue.equals(currentTitle) && (!existingExperiments.contains(newValue))) {
            // change the title
            experiment.setTitle(newValue);
            // pass to ExperimentDetailsFragment to reference later
            ExperimentDetailsFragment.setCurrentTitle(newValue);
            //save the title to the set
            existingExperiments.add(newValue);
            //======================================================================================
            //if old title exists, remove it from the set
            if(existingExperiments.contains(currentTitle))
                existingExperiments.remove(currentTitle);

            // check if there is a token set for this experiment and a connection type
            String accessToken = storedData.getString(currentTitle + "_experimentAccessToken", "");
            String connectionType = storedData.getString(currentTitle + "_experimentConnectionType", "");
            // if there is a token entered
            if (!accessToken.equals(null)) {
                // remove the old variable as it will no longer be referenced
                storedData.edit().remove(currentTitle + "_experimentAccessToken");
                // then add the new token
                editor.putString(newValue + "_experimentAccessToken", accessToken);
            } else {
                // put in a default websiteAccessToken
                editor.putString(newValue + "_experimentAccessToken", null);
            }
            // if there is a connection type selected
            if (!connectionType.equals(null)) {
                // remove the old variable as it will no longer be referenced
                storedData.edit().remove(currentTitle + "_experimentConnectionType");
                // then add the new token
                editor.putString(newValue + "_experimentConnectionType", connectionType);
            } else {
                // put in a default connection type
                editor.putString(newValue + "_experimentConnectionType", null);
            }
            // if either values are null then prompt user
            if (accessToken.equals("") || connectionType.equals("")) {
                updateConnectionSetup();
            }
            //======================================================================================
            //  experiment variables
            //======================================================================================
            // if the current title is not the default title
            if (!currentTitle.equals("Untitled Experiment")) {
                // user is renaming the experiment
                swap = true;
            }
            makeDefaultListOfSensors();
            // create the variables for this newly titled experiment
            for (int i = 0; i < sensorsList.size(); i++) {
                //default sensor state
                state = false;
                // if we are swapping the stored frequency values to the new title
                if (swap) {
                    // get the sensor frequency
                    variableName = currentTitle + "_" + sensorsList.get(i) + "_frequency";
                    Frequency = storedData.getInt(variableName, 0);
                    // then remove old variable
                    storedData.edit().remove(variableName);

                    // get the sensor state
                    variableName2 = currentTitle + "_" + sensorsList.get(i) + "_state";
                    state = storedData.getBoolean(variableName2, false);
                    // then remove old variable
                    storedData.edit().remove(variableName2);
                }

                // this is the new stored data variable:
                newSensorVariableFrequency = newValue + "_" + sensorsList.get(i) + "_frequency";
                editor.putInt(newSensorVariableFrequency, Frequency);

                // this is the new stored data variable:
                newSensorVariableState = newValue + "_" + sensorsList.get(i) + "_state";
                editor.putBoolean(newSensorVariableState, state);
            }
            // when you are done adding/changing the values, call the commit() method to commit all
            editor.commit();
            //==============================================================================
            // finally save the experiment
            saveExperiment();
            //==============================================================================
        }
    }

    private void notifyUser(){
        // inform the user of the changes that are about to happen.
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        break;
                }
            }
        };

        //  yes/no? prompt for user
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("An experiment with this name already exists.").setPositiveButton("OK", dialogClickListener).show();
    }

    private void makeDefaultListOfSensors(){
        // the 12 sensors we use:

        sensorsList = new ArrayList();
        sensorsList.add(AmbientLightSensor.ID);
        sensorsList.add(DecibelSensor.ID);
        sensorsList.add(LinearAccelerometerSensor.ID);
        sensorsList.add(AccelerometerSensor.Axis.X.getSensorId());
        sensorsList.add(AccelerometerSensor.Axis.Y.getSensorId());
        sensorsList.add(AccelerometerSensor.Axis.Z.getSensorId());
        sensorsList.add(CompassSensor.ID);
        sensorsList.add(MagneticStrengthSensor.ID);
        sensorsList.add(TemperatureSensorT.ID);
        sensorsList.add(BarometerSensorT.ID);
        sensorsList.add(HumiditySensorT.ID);
        sensorsList.add(LightSensorT.ID);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PictureUtils.REQUEST_SELECT_PHOTO && resultCode == Activity.RESULT_OK
                && data.getData() != null) {
            boolean success = false;
            File imageFile = null;
            // Check for non-null Uri here because of b/27899888
            try {
                // The ACTION_GET_CONTENT intent give temporary access to the
                // selected photo. We need to copy the selected photo to
                // to another file to get the real absolute path and store
                // that file's path into the experiment.

                // Use the experiment ID to name the project image.
                imageFile = PictureUtils.createImageFile(getActivity(), mExperimentId,
                        mExperimentId);
                copyUriToFile(getActivity(), data.getData(), imageFile);
                success = true;
            } catch (IOException e) {
                Log.e(TAG, "Could not save file", e);
            }
            if (success) {
                String relativePathInExperiment =
                        FileMetadataManager.getRelativePathInExperiment(mExperimentId, imageFile);
                String overviewPath =
                        PictureUtils.getExperimentOverviewRelativeImagePath(mExperimentId,
                                relativePathInExperiment);
                setImagePath(overviewPath);
                PictureUtils.loadExperimentOverviewImage(mPhotoPreview, overviewPath);
            }
            return;
        } else if (requestCode == PictureUtils.REQUEST_TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                String overviewPath =
                        PictureUtils.getExperimentOverviewRelativeImagePath(mExperimentId,
                        mPictureLabelPath);
                setImagePath(overviewPath);
                PictureUtils.loadExperimentImage(getActivity(), mPhotoPreview, mExperimentId,
                        mPictureLabelPath);
            } else {
                mPictureLabelPath = null;
            }
            // TODO: cancel doesn't restore old picture path.
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setImagePath(String overviewPath) {
        mExperiment.firstElement().subscribe(e -> {
            e.setImagePath(overviewPath);
            saveExperiment();
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_SAVED_PICTURE_PATH, mPictureLabelPath);
        super.onSaveInstanceState(outState);
    }

    /**
     * Copies a content URI returned from ACTION_GET_CONTENT intent to another file.
     * @param uri A content URI to get the content from using content resolver.
     * @param destFile A destination file to store the copy into.
     * @throws IOException
     */
    public static void copyUriToFile(Context context, Uri uri, File destFile) throws IOException {
        try (InputStream source = context.getContentResolver().openInputStream(uri);
             FileOutputStream dest = new FileOutputStream(destFile)) {
            ByteStreams.copy(source, dest);
        }
    }

    private void attachExperimentDetails(final Experiment experiment) {

        // check for the title, if new experiment set a default title for now
        currentTitle = experiment.getTitle();
        if(currentTitle.equals(""))
            currentTitle = defaultTitle;

        mExperiment.onNext(experiment);
    }

    private DataController getDataController() {
        return AppSingleton.getInstance(getActivity()).getDataController();
    }

    /**
     * Save the experiment
     */
    private void saveExperiment() {
        getDataController().updateExperiment(mExperimentId,
                new LoggingConsumer<Success>(TAG, "update experiment") {
                    @Override
                    public void fail(Exception e) {
                        super.fail(e);
                        AccessibilityUtils.makeSnackbar(
                                getView(),
                                getResources().getString(R.string.experiment_save_failed),
                                Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void success(Success value) {
                        // Do nothing
                        getActivity().finish();
                    }
                });
    }

    private void updateConnectionSetup(){

            Intent SetupIntent = new Intent(getContext(), AccessTokenSetupAndConnType.class);
            SetupIntent.putExtra( "CURRENT_TITLE", newValue);
            SetupIntent.putExtra( "OLD_TITLE", currentTitle);
            startActivity(SetupIntent);
    }
}