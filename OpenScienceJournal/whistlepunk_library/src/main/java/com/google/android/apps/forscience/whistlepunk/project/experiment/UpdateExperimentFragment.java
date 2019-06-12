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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
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

import com.google.android.apps.forscience.javalib.Success;
import com.google.android.apps.forscience.whistlepunk.AccessibilityUtils;
import com.google.android.apps.forscience.whistlepunk.AppSingleton;
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
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
    private int defaultFrequency = 5000;
    private String newSensorVariableFrequency;
    private String newSensorVariableState;
    private String previousTitle = "";
    private String newValue;
    //==============================================================================================
    private static Context context;

    public UpdateExperimentFragment() {

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            mSaved.onHappened();
            WhistlePunkApplication.getUsageTracker(getActivity())
                                  .trackEvent(TrackerConstants.CATEGORY_EXPERIMENTS,
                                          TrackerConstants.ACTION_EDITED,
                                          TrackerConstants.LABEL_UPDATE_EXPERIMENT, 0);
            getActivity().finish();
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
        EditText title = (EditText) view.findViewById(R.id.experiment_title);
        mPhotoPreview = (ImageView) view.findViewById(R.id.experiment_cover);
        Button chooseButton = (Button) view.findViewById(R.id.btn_choose_photo);
        ImageButton takeButton = (ImageButton) view.findViewById(R.id.btn_take_photo);




        // Set the color of the placeholder drawable. This isn't used anywhere else
        // so we don't need to mutate() the drawable.
        mPhotoPreview.getDrawable().setColorFilter(
                mPhotoPreview.getResources().getColor(R.color.text_color_light_grey),
                PorterDuff.Mode.SRC_IN);



        mExperiment.subscribe(experiment -> {

            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, 2);
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WAKE_LOCK, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, 2);
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, 2);



            title.setText(experiment.getTitle());
            previousTitle = experiment.getTitle();

            mSaved.happens().subscribe(o -> {
                newValue = title.getText().toString().trim();
                if (!newValue.equals(previousTitle)) {

                    //==============================================================================
                    // here we set the title so we pass that through to ExperimentDetailsFragment
                    // to keep track of the title future on
                    experiment.setTitle(newValue);
                    ExperimentDetailsFragment.setCurrentTitle(newValue);

                    //==============================================================================
                    // now we need to create stored data variables for each new set of sensors as
                    // the experiment is created
                    //===========================================================================

                    storedData = this.getContext().getSharedPreferences("info", MODE_PRIVATE);
                    // interface used for modifying values in a sharedPreference object
                    SharedPreferences.Editor editor = storedData.edit();
                    // the 10 sensors we use:
                    List newList = new ArrayList();
                    newList.add("AmbientLightSensor");
                    newList.add("DecibelSource");
                    newList.add("LinearAccelerometerSensor");
                    newList.add("AccX");
                    newList.add("AccY");
                    newList.add("AccZ");
                    newList.add("CompassSensor");
                    newList.add("MagneticRotationSensor");
                    newList.add("RemoteTemperature");               //<-- class not modified for this version
                    newList.add("RemoteHumidity");                  //<-- class not modified for this version

                    // create all the names of these new stored variables for this experiment
                    for(int i =0; i< newList.size(); i++) {

                        // starting with the sensors
                        // name  of the experiment + the current sensor + '_frequency'
                        // this is the new stored data variable:
                        newSensorVariableFrequency = newValue + "_" + newList.get(i) + "_frequency";
                        editor.putInt(newSensorVariableFrequency, defaultFrequency);

                        // then the sensor state
                        // name  of the experiment + the current sensor + '_state'
                        // this is the new stored data variable:
                        newSensorVariableState = newValue + "_" + newList.get(i) + "_state";
                        editor.putBoolean(newSensorVariableState, false);
                    }

                    // put in a default websiteAccessToken
                    editor.putString(newValue + "_experimentAccessToken", null);
                    // keep the access token up to date
                    updateAccessToken();

                    // when you are done adding the values, call the commit() method to commit all
                    editor.commit();
                    //==============================================================================
                    // finally save the experiment
                    saveExperiment();
                    //==============================================================================
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

        title.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE) {
                title.clearFocus();
                title.setFocusable(false);
            }
            return false;
        });
        title.setOnTouchListener((v, e) -> {
            title.setFocusableInTouchMode(true);
            title.requestFocus();
            return false;
        });
        return view;
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
                    }
                });
    }

    private void updateAccessToken(){

            Intent SetupIntent = new Intent(getContext(), AccessTokenSetup.class);
            SetupIntent.putExtra( "CURRENT_TITLE", newValue);
            SetupIntent.putExtra( "OLD_TITLE", previousTitle);
            startActivity(SetupIntent);

    }

}