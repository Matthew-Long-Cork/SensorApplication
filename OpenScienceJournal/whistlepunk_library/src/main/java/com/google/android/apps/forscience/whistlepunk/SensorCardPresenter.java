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

package com.google.android.apps.forscience.whistlepunk;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.TabLayout;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.apps.forscience.whistlepunk.audiogen.SonificationTypeAdapterFactory;
import com.google.android.apps.forscience.whistlepunk.blew.BleSensorManager;
import com.google.android.apps.forscience.whistlepunk.blew.Sensor;
import com.google.android.apps.forscience.whistlepunk.data.GoosciSensorLayout;
import com.google.android.apps.forscience.whistlepunk.devicemanager.ManageDevicesActivity;
import com.google.android.apps.forscience.whistlepunk.filemetadata.Experiment;
import com.google.android.apps.forscience.whistlepunk.filemetadata.Label;
import com.google.android.apps.forscience.whistlepunk.filemetadata.SensorTrigger;
import com.google.android.apps.forscience.whistlepunk.metadata.TriggerListActivity;
import com.google.android.apps.forscience.whistlepunk.project.experiment.ExperimentDetailsFragment;
import com.google.android.apps.forscience.whistlepunk.scalarchart.ScalarDisplayOptions;
import com.google.android.apps.forscience.whistlepunk.sensorapi.BlankReadableSensorOptions;
import com.google.android.apps.forscience.whistlepunk.sensorapi.DataViewOptions;
import com.google.android.apps.forscience.whistlepunk.sensorapi.NewOptionsStorage;
import com.google.android.apps.forscience.whistlepunk.sensorapi.OptionsListener;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ReadableSensorOptions;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorChoice;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorObserver;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorPresenter;
import com.google.android.apps.forscience.whistlepunk.sensorapi.SensorStatusListener;
import com.google.android.apps.forscience.whistlepunk.sensorapi.StreamStat;
import com.google.android.apps.forscience.whistlepunk.sensors.AccelerometerSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.AmbientLightSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.BarometerSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.CompassSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.DecibelSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.LinearAccelerometerSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.MagneticStrengthSensor;
import com.google.android.apps.forscience.whistlepunk.sensors.TestSensor;
import com.google.android.apps.forscience.whistlepunk.wireapi.RecordingMetadata;
import com.google.common.collect.Lists;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;
import com.jakewharton.rxbinding2.view.RxView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PrimitiveIterator;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * Holds the data and objects necessary for a sensor view.
 */
public class SensorCardPresenter {
    private static final String TAG = "SensorCardPres";
    private String sensorListAsString = "";
    private String mySensorDisplayName;
    private int REQUENCY_CHANGED = 7;

    @VisibleForTesting
    public static class CardStatus {
        private int mSourceStatus = SensorStatusListener.STATUS_CONNECTING;
        private boolean mHasError = false;

        public void setStatus(int status) {
            mSourceStatus = status;
        }

        public void setHasError(boolean hasError) {
            mHasError = hasError;
        }

        public boolean shouldShowConnecting() {
            return mSourceStatus == SensorStatusListener.STATUS_CONNECTING && !mHasError;
        }

        public boolean isConnected() {
            return !mHasError && mSourceStatus == SensorStatusListener.STATUS_CONNECTED;
        }

        public boolean shouldShowRetry() {
            // if there's an error, or the sensor disconnected, we should allow an attempt to
            // reconnect.
            return mHasError || mSourceStatus == SensorStatusListener.STATUS_DISCONNECTED;
        }

        public boolean hasError() {
            return mHasError;
        }

        @Override
        public String toString() {
            return "CardStatus{" +
                    "mSourceStatus=" + mSourceStatus +
                    ", mHasError=" + mHasError +
                    '}';
        }
    }

    /**
     * Object listening for when Sensor Selector tab items are clicked.
     */
    public interface OnSensorClickListener {
        /**
         * Called when user is requesting to move
         * a sensor
         */
        void onSensorClicked(String sensorId);
    }

    private OnSensorClickListener mOnSensorClickListener;

    /**
     * Object listening for when the close button is clicked.
     */
    public interface OnCloseClickedListener {
        /**
         * Called when the close button is selected.
         */
        void onCloseClicked();
    }

    private OnCloseClickedListener mCloseListener;

    // The height of a sensor presenter when multiple cards are visible is 60% of maximum.
    private static final double MULTIPLE_CARD_HEIGHT_PERCENT = 0.6;

    // The sensor ID ordering.
    private static final String[] SENSOR_ID_ORDER = {AmbientLightSensor.ID, DecibelSensor.ID,
            LinearAccelerometerSensor.ID, AccelerometerSensor.Axis.X.getSensorId(),
            AccelerometerSensor.Axis.Y.getSensorId(), AccelerometerSensor.Axis.Z.getSensorId(),
            BarometerSensor.ID, CompassSensor.ID, MagneticStrengthSensor.ID, TestSensor.ID};

    // Update the back data textview every .25 seconds maximum.
    private static final int MAX_TEXT_UPDATE_TIME_MS = 250;

    // Update the back data imageview every .05 seconds maximum.
    private static final int MAX_ICON_UPDATE_TIME_MS = 25;

    public static final int ANIMATION_TIME_MS = 200;

    private long mRecordingStart = RecordingMetadata.NOT_RECORDING;
    private List<String> mAvailableSensorIds;
    private String mSensorDisplayName = "";
    private String mUnits = "";
    private String mSensorId;
    private final String mExperimentId;
    private SensorAnimationBehavior mSensorAnimationBehavior;
    private SensorChoice mCurrentSource = null;
    private SensorStatusListener mSensorStatusListener = null;
    private CardViewHolder mCardViewHolder;
    private TabLayout.OnTabSelectedListener mOnTabSelectedListener;
    private DataViewOptions mDataViewOptions;
    private int mSingleCardPresenterHeight;
    private String mInitialSourceTagToSelect;
    private boolean mIsSingleCard = true;
    private View.OnClickListener mRetryClickListener;
    private boolean mPaused = false;
    private final RecordFragment mParentFragment;
    private PopupMenu mPopupMenu;
    private boolean mAllowRetry = true;
    private CardTriggerPresenter mCardTriggerPresenter;
    private ExternalAxisController.InteractionListener mInteractionListener;
    private final CardStatus mCardStatus = new CardStatus();

    private OptionsListener mCommitListener = new OptionsListener() {
        @Override
        public void applyOptions(ReadableSensorOptions settings) {
            mRecorderController.applyOptions(mSensorId,
                    AbstractReadableSensorOptions.makeTransportable(settings));
        }
    };

    // The last timestamp when the back of the card data was update.
    // This works unless your phone thinks it is 1970 or earlier!
    private long mLastUpdatedIconTimestamp = -1;
    private long mLastUpdatedTextTimestamp = -1;
    private boolean mTextTimeHasElapsed = false;

    private interface ValueFormatter {
        String format(String valueString, String units);
    }

    private ValueFormatter mDataFormat;
    private NumberFormat mNumberFormat;
    private LocalSensorOptionsStorage mCardOptions = new LocalSensorOptionsStorage();

    /**
     * A SensorPresenter that can respond to further UI events and update the capture
     * display, or null if the sensor doesn't expect to respond to any events from outside
     * the content view.
     */
    private SensorPresenter mSensorPresenter = null;
    private String mObserverId;

    private SensorAppearanceProvider mAppearanceProvider;
    private SensorSettingsController mSensorSettingsController;
    private final RecorderController mRecorderController;

    private GoosciSensorLayout.SensorLayout mLayout;

    private boolean mIsActive = false;
    private boolean mFirstObserving = true;

    public SensorCardPresenter(DataViewOptions dataViewOptions,
                               SensorSettingsController sensorSettingsController,
                               RecorderController recorderController, GoosciSensorLayout.SensorLayout layout,
                               String experimentId, ExternalAxisController.InteractionListener interactionListener,
                               RecordFragment fragment) {
        mDataViewOptions = dataViewOptions;
        mSensorSettingsController = sensorSettingsController;
        mRecorderController = recorderController;
        mInteractionListener = interactionListener;
        mAvailableSensorIds = new ArrayList<>();
        mLayout = layout;
        mCardOptions.putAllExtras(layout.extras);
        mExperimentId = experimentId;
        mParentFragment = fragment; // TODO: Should this use a weak reference?
        mCardTriggerPresenter = new CardTriggerPresenter(
                new CardTriggerPresenter.OnCardTriggerClickedListener() {
                    @Override
                    public void onCardTriggerIconClicked() {
                        if (!isRecording() && mCardStatus.isConnected()) {
                            startSetTriggersActivity();
                        }
                    }
                }, mParentFragment);
    }

    public void onNewData(long timestamp, SensorObserver.Data bundle) {
        if (mSensorPresenter == null) {
            return;
        }
        mSensorPresenter.onNewData(timestamp, bundle);
        boolean iconTimeHasElapsed =
                timestamp > mLastUpdatedIconTimestamp + MAX_ICON_UPDATE_TIME_MS;
        mTextTimeHasElapsed = timestamp > mLastUpdatedTextTimestamp + MAX_TEXT_UPDATE_TIME_MS;
        if (!mTextTimeHasElapsed && !iconTimeHasElapsed) {
            return;
        }

        if (mTextTimeHasElapsed) {
            mLastUpdatedTextTimestamp = timestamp;
        }
        if (iconTimeHasElapsed) {
            mLastUpdatedIconTimestamp = timestamp;
        }
        if (mCardViewHolder == null) {
            return;
        }
        if (bundle.hasValidValue()) {
            double value = bundle.getValue();
            if (mTextTimeHasElapsed) {
                String valueString = mNumberFormat.format(value);
                mCardViewHolder.meterLiveData.setText(mDataFormat.format(valueString, mUnits));
            }
            if (iconTimeHasElapsed && mSensorPresenter != null) {
                mSensorAnimationBehavior.updateImageView(mCardViewHolder.meterSensorIcon,
                        value, mSensorPresenter.getMinY(), mSensorPresenter.getMaxY(),
                        mCardViewHolder.screenOrientation);
            }
        } else {
            // TODO: Show an error state for no numerical value.
            mCardViewHolder.meterLiveData.setText("");
            mCardViewHolder.meterSensorIcon.setImageLevel(0);
        }
    }

    public void onSensorTriggerFired() {
        mCardTriggerPresenter.onSensorTriggerFired();
    }

    public void onSourceStatusUpdate(String id, int status) {
        if (!TextUtils.equals(id, mSensorId)) {
            return;
        }
        mCardStatus.setStatus(status);
        if (!mPaused) {
            updateStatusUi();
        }
    }

    public void onSourceError(boolean hasError) {
        mCardStatus.setHasError(hasError);
        updateStatusUi();
    }

    private void updateAudio(boolean enabled, String sonificationType) {
        mSensorPresenter.updateAudioSettings(enabled, sonificationType);
        if (mCardViewHolder != null) {
            updateAudioEnabledUi(enabled);
        }
    }

    private void updateAudioEnabledUi(boolean isEnabled) {
        String display = isEnabled ? String.format(
                mCardViewHolder.getContext().getString(R.string.audio_enabled_format),
                mSensorDisplayName) : mSensorDisplayName;
        mCardViewHolder.headerText.setText(display);
    }

    private void updateStatusUi() {
        // Turn off the audio unless it is connected.
        if (mSensorPresenter != null) {
            if (mCardStatus.isConnected() && mCurrentSource != null) {
                updateAudio(mLayout.audioEnabled, getSonificationType(
                        mParentFragment.getActivity()));
            } else {
                updateAudio(false, SonificationTypeAdapterFactory.DEFAULT_SONIFICATION_TYPE);
            }
        }
        if (mCardViewHolder == null) {
            return;
        }
        updateSensorTriggerUi();
        updateCardMenu();
        if (mCardStatus.isConnected()) {
            // We are connected with no error! Set everything back to normal.
            mCardViewHolder.statusViewGroup.setVisibility(View.GONE);
            mCardViewHolder.graphViewGroup.setVisibility(View.VISIBLE);
            return;
        }
        mCardViewHolder.statusViewGroup.bringToFront();
        mCardViewHolder.statusViewGroup.setVisibility(View.VISIBLE);
        mCardViewHolder.statusRetryButton.setVisibility(View.GONE);

        // Make the graph view group not explorable in TalkBack, so the user can't find
        // those views underneath the error state view.
        mCardViewHolder.graphViewGroup.setVisibility(View.GONE);



        if (mCardStatus.shouldShowConnecting()) {
            // Show a progress bar inside the card while connecting.
            mCardViewHolder.statusMessage.setText(
                    mCardViewHolder.getContext().getText(R.string.sensor_card_loading_text));
            mCardViewHolder.statusProgressBar.setVisibility(View.VISIBLE);
        } else {
            mCardViewHolder.statusMessage.setText(
                    mCardViewHolder.getContext().getText(R.string.sensor_card_error_text));
            mCardViewHolder.statusProgressBar.setVisibility(View.GONE);

            // An error
            if (mCardStatus.shouldShowRetry() && mRetryClickListener != null && mAllowRetry) {
                mCardViewHolder.statusRetryButton.setVisibility(View.VISIBLE);
                mCardViewHolder.statusRetryButton.setOnClickListener(mRetryClickListener);
            }
        }
    }

    public void startObserving(SensorChoice sensorChoice, SensorPresenter sensorPresenter,
                               ReadableSensorOptions readOptions, Experiment experiment,
                               SensorRegistry sensorRegistry) {
        final ReadableSensorOptions nonNullOptions = BlankReadableSensorOptions.blankIfNull(
                readOptions);
        mCurrentSource = sensorChoice;
        mSensorPresenter = sensorPresenter;
        List<SensorTrigger> triggers;
        if (mLayout.activeSensorTriggerIds.length == 0) {
            updateCardMenu();
            triggers = Collections.emptyList();
        } else {
            triggers = experiment.getActiveSensorTriggers(mLayout);
        }
        mCardTriggerPresenter.setSensorTriggers(triggers);
        mObserverId = mRecorderController.startObserving(mCurrentSource.getId(), triggers,
                new SensorObserver() {
                    @Override
                    public void onNewData(long timestamp, Data value) {
                        SensorCardPresenter.this.onNewData(timestamp, value);
                    }
                }, getSensorStatusListener(),
                AbstractReadableSensorOptions.makeTransportable(nonNullOptions), sensorRegistry);
        if (mCardStatus.isConnected() && mParentFragment != null) {
            updateAudio(mLayout.audioEnabled, getSonificationType(mParentFragment.getActivity()));
        }
        mSensorPresenter.setShowStatsOverlay(mLayout.showStatsOverlay);
        mSensorPresenter.setTriggers(triggers);
        if (mFirstObserving) {
            // The first time we start observing on a sensor, we can load the minimum and maximum
            // y values from the layout. If the sensor is changed, we don't want to keep loading the
            // old min and max values.
            if (mLayout.minimumYAxisValue < mLayout.maximumYAxisValue) {
                mSensorPresenter.setYAxisRange(mLayout.minimumYAxisValue,
                        mLayout.maximumYAxisValue);
            }
            mFirstObserving = false;
        }
        if (mCardViewHolder != null) {
            mSensorPresenter.startShowing(mCardViewHolder.chartView, mInteractionListener);
            updateSensorTriggerUi();
            updateLearnMoreButton();
        }
        // It is possible we just resumed observing but we are currently recording, in which case
        // we need to refresh the recording UI.
        if (isRecording()) {
            mSensorPresenter.onRecordingStateChange(isRecording(), mRecordingStart);
        }
    }

    @VisibleForTesting
    public void setUiForConnectingNewSensor(String sensorId, String sensorDisplayName,
                                            String sensorUnits, boolean hasError) {
        mUnits = sensorUnits;
        mSensorDisplayName = sensorDisplayName;
        // Set sensorId now; if we have to load SensorChoice from database, it may not be available
        // until later.
        mSensorId = sensorId;
        SensorAppearance appearance = mAppearanceProvider.getAppearance(mSensorId);
        mNumberFormat = appearance.getNumberFormat();
        mSensorAnimationBehavior = appearance.getSensorAnimationBehavior();
        mCardStatus.setHasError(hasError);
        mCardStatus.setStatus(SensorStatusListener.STATUS_CONNECTING);
        if (mCardViewHolder != null) {
            //mCardViewHolder.headerText.setText(mSensorDisplayName);
            mCardViewHolder.headerText.setText(mSensorDisplayName);
            setMeterIcon();
            updateStatusUi();
        }
    }

    public void setViews(CardViewHolder cardViewHolder, OnCloseClickedListener closeListener) {
        mCardViewHolder = cardViewHolder;
        mCloseListener = closeListener;
        mCardTriggerPresenter.setViews(mCardViewHolder);
        //Toast.makeText(mCardViewHolder.getContext(), "sensors tab!!", Toast.LENGTH_SHORT).show();

        String formatString =
                cardViewHolder.getContext().getResources().getString(R.string.data_with_units);
        mDataFormat = getDataFormatter(formatString);

        updateRecordingUi();

        mCardViewHolder.headerText.setText(mSensorDisplayName);
        //mCardViewHolder.headerText.setText("SensorCardPresenter");

        if (mSensorAnimationBehavior != null) {
            setMeterIcon();
        }

        int color = mDataViewOptions.getGraphColor();
        int slightlyLighter = ColorUtils.getSlightlyLighterColor(color);
        mCardViewHolder.header.setBackgroundColor(color);
        mCardViewHolder.sensorSelectionArea.setBackgroundColor(slightlyLighter);
        mCardViewHolder.sensorSettingsGear.setBackground(ColorUtils.colorDrawableWithActual(
                mCardViewHolder.sensorSettingsGear.getBackground(), color));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mCardViewHolder.statusProgressBar.setIndeterminateTintList(
                    ColorStateList.valueOf(color));
        }

        if (mSensorPresenter != null) {

            mSensorPresenter.startShowing(mCardViewHolder.chartView, mInteractionListener);
        }

        updateLearnMoreButton();

        mCardViewHolder.graphStatsList.setTextBold(mLayout.showStatsOverlay);
        mCardViewHolder.graphStatsList.setOnClickListener(v -> {
            if (mSensorPresenter != null) {
                mLayout.showStatsOverlay = !mLayout.showStatsOverlay;
                mSensorPresenter.setShowStatsOverlay(mLayout.showStatsOverlay);
                mCardViewHolder.graphStatsList.setTextBold(mLayout.showStatsOverlay);
            }
        });
        updateStatusUi();
        updateAudioEnabledUi(mLayout.audioEnabled);

        // The first time a SensorCardPresenter is created, we cannot use the recycled view.
        // Exact reason unknown but this workaround fixes the bug described in b/24611618.
        // TODO: See if this bug can be resolved in a way that does not require view inflating.
        mCardViewHolder.sensorTabHolder.removeAllViews();
        LayoutInflater.from(mCardViewHolder.getContext()).inflate(
                R.layout.sensor_selector_tab_layout, mCardViewHolder.sensorTabHolder, true);
        mCardViewHolder.sensorTabLayout = (TabLayout) mCardViewHolder.sensorTabHolder.getChildAt(0);
        mCardViewHolder.sensorTabLayout.setOnTabSelectedListener(mOnTabSelectedListener);
        if (!TextUtils.isEmpty(mSensorId)) {
            initializeSensorTabs(mSensorId);
        }
        refreshTabLayout();

        RxView.clicks(mCardViewHolder.sensorSettingsGear).subscribe(click -> {

            RecorderControllerImpl.setSensorsOnDisplay(false);

            System.out.println("======================================");
            System.out.println("                  ");
            System.out.println("======================================");
            System.out.println(" ");
            System.out.println(" click in setting gear");
            System.out.println(" ");
            System.out.println("======================================");
            System.out.println("                  ");
            System.out.println("======================================");

            ManageDevicesActivity.launch(mCardViewHolder.getContext(), mExperimentId);

        });

        // Force setActive whenever the views are reset, as previously used views might be already
        // View.GONE.
        // Note: this must be done after setting up the tablayout.
        setActive(mIsActive, /* force */ true);
    }

    private ValueFormatter getDataFormatter(String formatString) {
        if (formatString.equals("%1$s %2$s")) {
            // This is, I believe, the only format currently used.
            return new ValueFormatter() {
                StringBuffer mBuffer = new StringBuffer(20);

                @Override
                public String format(String valueString, String units) {
                    mBuffer.setLength(0);
                    mBuffer.append(valueString).append(" ").append(units);
                    return mBuffer.toString();
                }
            };
        } else {
            // Just in case there are other formats, fall back to expensive String.format
            return (valueString, units) -> String.format(formatString, valueString, units);
        }
    }

    private void updateSensorTriggerUi() {
        if (mCardViewHolder == null) {
            return;
        }
        if (!mCardStatus.isConnected()) {
            mCardViewHolder.triggerSection.setVisibility(View.GONE);
            return;
        }
        mCardTriggerPresenter.updateSensorTriggerUi();
    }

    private void updateLearnMoreButton() {
        if (shouldShowInfoButton()) {
            mCardViewHolder.infoButton.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, SensorInfoActivity.class);
                intent.putExtra(SensorInfoActivity.EXTRA_SENSOR_ID, mSensorId);
                intent.putExtra(SensorInfoActivity.EXTRA_COLOR_ID,
                        mDataViewOptions.getGraphColor());
                context.startActivity(intent);
            });
            mCardViewHolder.infoButton.setVisibility(View.VISIBLE);
        } else {
            mCardViewHolder.infoButton.setOnClickListener(null);
            mCardViewHolder.infoButton.setVisibility(View.INVISIBLE);
        }
    }

    private boolean shouldShowInfoButton() {
        return mAppearanceProvider.getAppearance(mSensorId).hasLearnMore() || !TextUtils.isEmpty(
                mAppearanceProvider.getAppearance(mSensorId).getShortDescription(
                        mCardViewHolder.getContext()));
    }

    public void onViewRecycled() {
        if (mCardViewHolder != null) {
            mCardViewHolder.sensorTabLayout.setOnTabSelectedListener(null);
            mCardViewHolder.menuButton.setOnClickListener(null);
            mCardViewHolder.infoButton.setOnClickListener(null);
            mCardViewHolder.graphStatsList.setOnClickListener(null);
            mCardViewHolder.graphStatsList.clearStats();
            mCardViewHolder.meterLiveData.setText("");
            mCardViewHolder.meterLiveData.resetTextSize();
        }
        if (mSensorPresenter != null) {
            mSensorPresenter.onViewRecycled();
        }
        mCardTriggerPresenter.onViewRecycled();
        mCloseListener = null;
        mCardViewHolder = null;
    }

    private void setMeterIcon() {
        mSensorAnimationBehavior.initializeLargeIcon(mCardViewHolder.meterSensorIcon);
    }

    private void updateCardMenu() {
        if (mCardViewHolder == null || mCardViewHolder.menuButton == null) {
            return;
        }
        mCardViewHolder.menuButton.setOnClickListener(v -> openCardMenu());
    }

    private void openCardMenu() {
        if (mPopupMenu != null) {
            return;
        }
        final Context context = mCardViewHolder.getContext();
        Resources res = context.getResources();
        boolean showDevTools = DevOptionsFragment.isDevToolsEnabled(context);
        mPopupMenu = new PopupMenu(context, mCardViewHolder.menuButton);
        mPopupMenu.getMenuInflater().inflate(R.menu.menu_sensor_card, mPopupMenu.getMenu());
        final Menu menu = mPopupMenu.getMenu();
        menu.findItem(R.id.btn_sensor_card_close).setVisible(
                !mIsSingleCard && !isRecording());

        // Adjusting sensor options through the UI is only a developer option.
        menu.findItem(R.id.btn_sensor_card_settings).setVisible(showDevTools && !isRecording());

        // Don't show audio options if there is an error or bad status.
        boolean sensorConnected = mCardStatus.isConnected();
        menu.findItem(R.id.btn_sensor_card_audio_toggle).setEnabled(sensorConnected);
        menu.findItem(R.id.btn_sensor_card_audio_settings).setEnabled(sensorConnected);

        menu.findItem(R.id.btn_sensor_card_audio_toggle).setTitle(
                res.getString(mLayout.audioEnabled ?
                        R.string.graph_options_audio_feedback_disable :
                        R.string.graph_options_audio_feedback_enable));

        menu.findItem(R.id.btn_sensor_card_audio_settings).setVisible(!isRecording());

        // Disable trigger settings during recording.
        menu.findItem(R.id.btn_sensor_card_set_triggers).setEnabled(sensorConnected &&
                !isRecording() && mLayout != null);
        menu.findItem(R.id.btn_sensor_card_set_triggers).setTitle(
                res.getString(mCardTriggerPresenter.getSensorTriggers().size() == 0 ?
                        R.string.menu_item_set_triggers : R.string.menu_item_edit_triggers));

        // Show the option to disable all triggers only during recording and if triggers exist
        // on the card.
        menu.findItem(R.id.btn_disable_sensor_card_triggers).setVisible(isRecording() &&
                mLayout != null && mCardTriggerPresenter.getSensorTriggers().size() > 0);

        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.btn_sensor_card_close) {
                    if (mCloseListener != null) {
                        mCloseListener.onCloseClicked();
                    }
                    return true;
                } else if (itemId == R.id.btn_sensor_card_settings) {
                    mSensorSettingsController.launchOptionsDialog(mCurrentSource,
                            mSensorPresenter,
                            getCardOptions(mCurrentSource, context),
                            mCommitListener, new NewOptionsStorage.SnackbarFailureListener(
                                    mCardViewHolder.menuButton));
                    return true;
                } else if (itemId == R.id.btn_sensor_card_audio_toggle) {
                    mLayout.audioEnabled = !mLayout.audioEnabled;
                    updateAudio(mLayout.audioEnabled, getSonificationType(context));
                    return true;
                } else if (itemId == R.id.btn_sensor_card_audio_settings) {
                    String currentSonificationType = getCardOptions(mCurrentSource, context).load(
                            LoggingConsumer.expectSuccess(TAG, "loading card options")
                    ).getReadOnly().getString(
                            ScalarDisplayOptions.PREFS_KEY_SONIFICATION_TYPE,
                            SonificationTypeAdapterFactory.DEFAULT_SONIFICATION_TYPE);
                    AudioSettingsDialog dialog =
                            AudioSettingsDialog.newInstance(new String[]{currentSonificationType},
                                    new String[]{mSensorId}, 0);
                    dialog.show(mParentFragment.getChildFragmentManager(), AudioSettingsDialog.TAG);
                    return true;
                } else if (itemId == R.id.btn_sensor_card_set_triggers) {
                    if (mParentFragment == null) {
                        return false;
                    }

                    Intent intent = new Intent(mParentFragment.getActivity(),
                            TriggerListActivity.class);
                    intent.putExtra(TriggerListActivity.EXTRA_SENSOR_ID, mSensorId);
                    intent.putExtra(TriggerListActivity.EXTRA_EXPERIMENT_ID, mExperimentId);
                    intent.putExtra(TriggerListActivity.EXTRA_LAYOUT_POSITION,
                            mParentFragment.getPositionOfLayout(mLayout));
                    mParentFragment.getActivity().startActivity(intent);
                    return true;
                } else if (itemId == R.id.btn_sensor_card_set_triggers) {
                    return startSetTriggersActivity();
                } else if (itemId == R.id.btn_disable_sensor_card_triggers) {
                    return disableTriggers();
                }
                else if (itemId == R.id.action_set_sendData_to_db_frequency) {
                    return getUserInputForFrequency();
                }
                else if (itemId == R.id.action_BT) {
                    BleSensorManager ble = BleSensorManager.getInstance();
                    ble.scan();
                }

                return false;
            }
        });

        mPopupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                mPopupMenu = null;
            }
        });

        mPopupMenu.show();
    }

    private boolean disableTriggers() {
        // Disable all triggers on this card.
        if (mParentFragment == null) {
            return false;
        }
        mLayout.activeSensorTriggerIds = null;
        mParentFragment.disableAllTriggers(mLayout, this);
        return true;
    }

    public void onSensorTriggersCleared() {
        mCardTriggerPresenter.setSensorTriggers(Collections.<SensorTrigger>emptyList());
        mSensorPresenter.setTriggers(Collections.<SensorTrigger>emptyList());
        updateSensorTriggerUi();
    }

    //==========================================================================================
    // added: function
    private boolean getUserInputForFrequency() {
        if (mParentFragment == null) {
            return false;
        }

        Toast.makeText(mCardViewHolder.getContext(), "This is in SensorCardPresenter.java intent!!", Toast.LENGTH_SHORT).show();

        // create the intent to go to FrequencyPopup class and get the user input
        Intent frequencyIntent = new Intent(mParentFragment.getActivity(), FrequencyPopup.class);
        // add the 'sensorListAsString'
        frequencyIntent.putExtra("sensors", sensorListAsString);
        frequencyIntent.putExtra("currentSensor", mSensorId);
        // start the intent
        mParentFragment.getActivity().startActivityForResult(frequencyIntent, REQUENCY_CHANGED);
        return true;
    }
    //==========================================================================================

    // added: onResult
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUENCY_CHANGED) {

            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                //PanesActivity.toolPicker.getTabAt(1).select();
                System.out.println("======================================");
                System.out.println("                  ");
                System.out.println("======================================");
                System.out.println(" ");
                System.out.println(" ");
                System.out.println("        test ok");
                System.out.println(" ");
                System.out.println(" ");
                System.out.println("======================================");
                System.out.println("                  ");
                System.out.println("======================================");
            }
            if (resultCode == RESULT_CANCELED) {
                //PanesActivity.toolPicker.getTabAt(1).select();
                System.out.println("======================================");
                System.out.println("                  ");
                System.out.println("======================================");
                System.out.println(" ");
                System.out.println(" ");
                System.out.println("        test canceled");
                System.out.println(" ");
                System.out.println(" ");
                System.out.println("======================================");
                System.out.println("                  ");
                System.out.println("======================================");
            }
        }
    }
    private boolean startSetTriggersActivity() {
        if (mParentFragment == null) {
            return false;
        }
        Intent intent = new Intent(mParentFragment.getActivity(),
                TriggerListActivity.class);
        intent.putExtra(TriggerListActivity.EXTRA_SENSOR_ID, mSensorId);
        intent.putExtra(TriggerListActivity.EXTRA_EXPERIMENT_ID, mExperimentId);
        intent.putExtra(TriggerListActivity.EXTRA_LAYOUT_POSITION,
                mParentFragment.getPositionOfLayout(mLayout));
        mParentFragment.getActivity().startActivity(intent);
        return true;
    }

    private String getSonificationType(Context context) {
        if (context == null) {
            // Probably tearing down anyway, but return something safe
            return SonificationTypeAdapterFactory.DEFAULT_SONIFICATION_TYPE;
        }
        return getCardOptions(mCurrentSource, context).load(
                LoggingConsumer.expectSuccess(TAG, "loading card options")
        ).getReadOnly().getString(ScalarDisplayOptions.PREFS_KEY_SONIFICATION_TYPE,
                SonificationTypeAdapterFactory.DEFAULT_SONIFICATION_TYPE);
    }

    public void onAudioSettingsPreview(String previewSonificationType) {
        // Must save audio settings in the layout so that if a rotation or backgrounding occurs
        // while the dialog is active, it is saved on resume.
        updateSonificationType(previewSonificationType);
    }

    public void onAudioSettingsApplied(String newSonificationType) {
        updateSonificationType(newSonificationType);
    }

    public void onAudioSettingsCanceled(String originalSonificationType) {
        updateSonificationType(originalSonificationType);
    }

    private void updateSonificationType(String sonificationType) {
        updateAudio(mLayout.audioEnabled, sonificationType);
        getCardOptions(mCurrentSource, mParentFragment.getActivity()).load(
                LoggingConsumer.expectSuccess(TAG, "loading card options")).put(
                ScalarDisplayOptions.PREFS_KEY_SONIFICATION_TYPE,
                sonificationType);
    }

    private void initializeSensorTabs(final String sensorIdToSelect) {
        mCardViewHolder.sensorTabLayout.removeAllTabs();
        Context context = mCardViewHolder.getContext();
        int sensorsAvailable = mAvailableSensorIds.size();
        String sensorId;

        for (int i = 0; i < sensorsAvailable; i++) {
            sensorId = mAvailableSensorIds.get(i);
            addSensorTab(sensorId, i, context);

            //keep concatenating the sensors and add a ',' if there is another one
            sensorListAsString = sensorListAsString + sensorId;
            if(i < sensorsAvailable-1){
                sensorListAsString = sensorListAsString += ":";
            }

        }
        // By selecting the tab in a runnable, we also cause the SensorTabLayout to scroll
        // to the correct position.
        mCardViewHolder.sensorTabLayout.post(new Runnable() {
            @Override
            public void run() {
                if (mCardViewHolder != null) {
                    int i = mAvailableSensorIds.indexOf(sensorIdToSelect);
                    if (i < 0) {
                        i = 0;
                    }
                    TabLayout.Tab tab = mCardViewHolder.sensorTabLayout.getTabAt(i);
                    if (tab != null) {
                        tab.select();
                    }
                }
            }
        });
    }

    private void addSensorTab(String sensorId, int index, Context context) {
        final SensorAppearance appearance = mAppearanceProvider.getAppearance(sensorId);
        TabLayout.Tab tab = mCardViewHolder.sensorTabLayout.newTab();
        tab.setContentDescription(appearance.getName(context));
        tab.setIcon(appearance.getIconDrawable(context));
        tab.setTag(sensorId);
        mCardViewHolder.sensorTabLayout.addTab(tab, index, false);

        // added:
        // HACK: we need to retrieve the view using View#findViewByTag to avoid adding lots of
        // callbacks and plumbing, just for a one time use case (feature discovery).
        // We also need to set the content description on the TabView so that FeatureDiscovery can
        // retrieve it properly. This does not seem to cause a double content description in
        // TalkBack, probably because the TabView's content description is otherwise unused.
        // Finding the TabView is dependent on the current implementation of TabLayout, but since
        // it comes from the support library, not worried about it changing on different devices.
        if (mCardViewHolder.sensorTabLayout.getChildCount() > 0) {
            View tabView = ((ViewGroup) mCardViewHolder.sensorTabLayout.getChildAt(0))
                    .getChildAt(index);
            tabView.setTag(sensorId);
            tabView.setContentDescription(appearance.getName(context));
        }
    }

    public void setOnSensorSelectedListener(final OnSensorClickListener listener) {
        mOnSensorClickListener = listener;
        mOnTabSelectedListener = new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (mCardViewHolder != null) {
                    String newSensorId = (String) tab.getTag();
                    trySelectingNewSensor(newSensorId, mSensorId);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (mCardViewHolder != null) {
                    String newSensorId = (String) tab.getTag();
                    if (TextUtils.equals(mSensorId, newSensorId) && mSensorPresenter != null) {
                        mSensorPresenter.resetView();
                        // Also need to pin the graph to now again.
                        mInteractionListener.requestResetPinnedState();
                    } else {
                        trySelectingNewSensor(mSensorId, newSensorId);
                    }
                }
            }
        };
        if (mCardViewHolder != null && mIsActive) {
            mCardViewHolder.sensorTabLayout.setOnTabSelectedListener(mOnTabSelectedListener);
        }
    }


    private String getSensorName(String sensorId) {
        return mAppearanceProvider.getAppearance(sensorId).getName(mCardViewHolder.getContext());
    }

    // Selects the new sensor if it is different from the old sensor or if no sensor is currently
    // selected.
    private void trySelectingNewSensor(String newSensorId, String oldSensorId) {
        if ((mCurrentSource == null && !mCardStatus.hasError()) || !TextUtils.equals(newSensorId,
                oldSensorId)) {
            // Clear the active sensor triggers when changing sensors.
            if (!TextUtils.equals(mLayout.sensorId, newSensorId)) {
                mLayout.activeSensorTriggerIds = new String[]{};
                mCardTriggerPresenter.setSensorTriggers(Collections.<SensorTrigger>emptyList());
                updateSensorTriggerUi();
            }
            mOnSensorClickListener.onSensorClicked(newSensorId);
        }
    }

    public void setAppearanceProvider(SensorAppearanceProvider appearanceProvider) {
        mAppearanceProvider = appearanceProvider;
    }

    /**
     * @param availableSensorIds a _sorted_ list of availableSensorIds, in the order they should be
     *                           laid out in sensor tabs.
     * @param allSensorIds all of the sensors, including the currently-used ones, in order.
     */
    public void updateAvailableSensors(List<String> availableSensorIds, List<String> allSensorIds) {
        // We should never be updating the selected sensor.
        List<String> newAvailableSensorIds = new ArrayList(availableSensorIds);
        if (!TextUtils.isEmpty(mSensorId) && !availableSensorIds.contains(mSensorId)) {
            newAvailableSensorIds.add(mSensorId);
        }
        List<String> sorted = customSortSensorIds(newAvailableSensorIds, allSensorIds);
        if (!sorted.equals(mAvailableSensorIds)) {
            mAvailableSensorIds = sorted;
            if (mCardViewHolder != null && !TextUtils.isEmpty(mSensorId)) {
                initializeSensorTabs(mSensorId);
            }
        }
        refreshTabLayout();
    }

    // TODO: find a way to test without exposing this.
    @VisibleForTesting
    public ArrayList<String> getAvailableSensorIds() {
        return Lists.newArrayList(mAvailableSensorIds);
    }

    // The following is a workaround to a bug described in
    // https://code.google.com/p/android/issues/detail?id=180462.
    private void refreshTabLayout() {
        if (mCardViewHolder == null) {
            return;
        }
        mCardViewHolder.sensorTabLayout.post(new Runnable() {
            public void run() {
                if (mCardViewHolder != null) {
                    mCardViewHolder.sensorTabLayout.requestLayout();
                }
            }
        });
    }

    @VisibleForTesting
    public static List<String> customSortSensorIds(List<String> sensorIds,
                                                   List<String> allSensorIds) {
        List<String> result = new ArrayList(Arrays.asList(SENSOR_ID_ORDER));
        // Keep only the elements in result that are in the available SensorIds list.
        for (String id : SENSOR_ID_ORDER) {
            if (!sensorIds.contains(id)) {
                result.remove(id);
            } else {
                sensorIds.remove(id);
            }
        }

        for (String id : allSensorIds) {
            if (sensorIds.contains(id)) {
                result.add(id);
            }
        }

        return result;
    }

    public String getSelectedSensorId() {
        if (!TextUtils.isEmpty(mSensorId)) {
            return mSensorId;
        } else if (mCardViewHolder != null) {
            // If we are switching sensors, the TabLayout tab may already be selected even
            // though the currentSource is null (after stopObserving but before starting again).
            int position = mCardViewHolder.sensorTabLayout.getSelectedTabPosition();
            if (position >= 0) {
                return (String) mCardViewHolder.sensorTabLayout.getTabAt(position).getTag();
            }
        }
        return "";
    }

    public DataViewOptions getDataViewOptions() {
        return mDataViewOptions;
    }

    private SensorStatusListener getSensorStatusListener() {
        return mSensorStatusListener;
    }

    public void setSensorStatusListener(SensorStatusListener sensorStatusListener) {
        mSensorStatusListener = sensorStatusListener;
    }

    public void setOnRetryClickListener(View.OnClickListener retryClickListener) {
        mRetryClickListener = retryClickListener;
    }

    public SensorPresenter getSensorPresenter() {
        return mSensorPresenter;
    }

    // When the stats drawer is recycled, this can return the old drawer for a different
    // sensor, so check whether the view is recycled (unavailable) before updating.
    public void updateStats(List<StreamStat> stats) {
        if (!isRecording()) {
            return;
        }
        if (mCardViewHolder != null && mSensorPresenter != null && mTextTimeHasElapsed) {
            mCardViewHolder.graphStatsList.updateStats(stats);
            mSensorPresenter.updateStats(stats);
        }
    }

    /**
     * Updates the UI of the SensorCard to be "active" (show all buttons) or "inactive" (only show
     * header). If recording is in progress, always deactivates.
     *
     * @param isActive Whether this SensorCardPresenter should be active
     * @param force    If true, forces UI updates even if isActive is not changed from the previous
     *                 state. This is useful when a card is created for the first time or when
     *                 views
     *                 are recycled from other SensorCards and we want to make sure that they
     *                 have the
     *                 correct visibility.
     */
    public void setActive(boolean isActive, boolean force) {
        if (isRecording()) {
            isActive = false;
        }
        int expandedHeight = mCardViewHolder != null ? mCardViewHolder.getContext().getResources()
                .getDimensionPixelSize(R.dimen.sensor_tablayout_height) : 0;
        // Add animation only if "force" is false -- in other words, if this was user initiated!
        if (mIsActive != isActive && !force) {
            mIsActive = isActive;
            if (mCardViewHolder != null) {
                int startHeight = isActive ? 0 : expandedHeight;
                int endHeight = isActive ? expandedHeight : 0;
                mCardViewHolder.sensorTabLayout.setOnTabSelectedListener(isActive ?
                        mOnTabSelectedListener : null);
                final ValueAnimator animator = new ValueAnimator()
                        .ofInt(startHeight, endHeight)
                        .setDuration(ANIMATION_TIME_MS);
                animator.setTarget(mCardViewHolder.sensorSelectionArea);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (mCardViewHolder == null) {
                            return;
                        }
                        Integer value = (Integer) animation.getAnimatedValue();
                        mCardViewHolder.sensorSelectionArea.getLayoutParams().height =
                                value.intValue();
                        mCardViewHolder.sensorSelectionArea.requestLayout();
                    }
                });
                animator.start();
            }
        } else if (force) {
            mIsActive = isActive;
            if (mCardViewHolder != null) {
                mCardViewHolder.sensorSelectionArea.getLayoutParams().height = isActive ?
                        expandedHeight : 0;
                mCardViewHolder.sensorSelectionArea.requestLayout();
            }
        }
        updateButtonsVisibility(!force);
        refreshTabLayout();
    }

    public boolean isActive() {
        return mIsActive && !isRecording();
    }

    public void setIsSingleCard(boolean isSingleCard) {
        if (mCardViewHolder == null) {
            return;
        }
        int height;
        boolean alwaysUseMultiCardHeight = mCardViewHolder.getContext().getResources().getBoolean(
                R.bool.always_use_multi_card_height);
        if (alwaysUseMultiCardHeight) {
            // For extra large views, the cards are always shown at the multiple card height.
            height = Math.max((int) (MULTIPLE_CARD_HEIGHT_PERCENT * mSingleCardPresenterHeight),
                    mCardViewHolder.getContext().getResources().getDimensionPixelSize(
                            R.dimen.sensor_card_content_height_min));
        } else {
            height = isSingleCard ? mSingleCardPresenterHeight :
                    Math.max((int) (MULTIPLE_CARD_HEIGHT_PERCENT * mSingleCardPresenterHeight),
                            mCardViewHolder.getContext().getResources().getDimensionPixelSize(
                                    R.dimen.sensor_card_content_height_min));
        }
        ViewGroup.LayoutParams params = mCardViewHolder.graphViewGroup.getLayoutParams();
        params.height = height;
        mCardViewHolder.graphViewGroup.setLayoutParams(params);

        if (mIsSingleCard != isSingleCard) {
            mIsSingleCard = isSingleCard;
            updateCardMenu();
        }
    }

    public void setSingleCardPresenterHeight(int singleCardPresenterHeight) {
        mSingleCardPresenterHeight = singleCardPresenterHeight;
    }

    public void scrollToSensor(String sensorId) {
        int index = mAvailableSensorIds.indexOf(sensorId);
        if (index != -1) {
            mCardViewHolder.sensorTabLayout.setScrollPosition(index, 0, false);
        }
    }

    private void updateButtonsVisibility(boolean animate) {
        if (mCardViewHolder == null) {
            return;
        }
        mCardViewHolder.toggleButton.setActive(mIsActive, animate);
    }

    public void destroy() {
        if (!TextUtils.isEmpty(mSensorId)) {
            mRecorderController.stopObserving(mSensorId, mObserverId);
        }
        mCardTriggerPresenter.onDestroy();
        if (mCardViewHolder != null) {
            mCardViewHolder.header.setOnHeaderTouchListener(null);
        }
        onViewRecycled();

        // Close the menu, because it will reference obsolete views and
        // presenters after resume.
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
        }

        // Any other destroy code can go here.

        // TODO: Find a way to clear the ChartController from ScalarSensor when the card
        // is destroyed but still keep graph data in memory. If we called
        // lineGraphPresenter.onDestroy() here it would clear the data, which is not what we
        // want in the case of a rotation. However, destroying the data may stop the blinking bug
        // at b/28666990. Need to find a way to keep the graph most of the
        // time but not have the blinking bug happen by not trying to load too much old data.
    }

    public void onPause() {
        mPaused = true;
        if (mSensorPresenter != null) {
            mSensorPresenter.onPause();
        }
        mRecorderController.stopObserving(mSensorId, mObserverId);
    }

    public void onResume(long resetTime) {
        mPaused = false;
        updateStatusUi();
        if (mSensorPresenter != null) {
            mSensorPresenter.onResume(resetTime);
        }
    }


    public void stopObserving() {

        if (mSensorPresenter != null) {
            mSensorPresenter.onStopObserving();
        }
        mSensorPresenter = null;
        mCurrentSource = null;
        mSensorAnimationBehavior = null;
        mRecorderController.stopObserving(mSensorId, mObserverId);
        if (!mCardStatus.hasError()) {
            // Only clear the data if the disconnect didn't come from an error.
            clearSensorStreamData();
        }
    }


    public void retryConnection(Context context) {
        setConnectingUI(getSelectedSensorId(), false, context, true);
        mRecorderController.reboot(mSensorId);
    }

    private void clearSensorStreamData() {
        mSensorDisplayName = "";
        mUnits = "";
        mSensorId = "";
        if (mCardViewHolder != null) {
            mCardViewHolder.meterLiveData.setText("");
            mCardViewHolder.meterLiveData.resetTextSize();
        }
        mLastUpdatedIconTimestamp = -1;
        mLastUpdatedTextTimestamp = -1;
    }

    public void setInitialSourceTagToSelect(String sourceTag) {
        mInitialSourceTagToSelect = sourceTag;
    }

    // Selects the initial sensor source if possible, otherwise tries to select the
    // next sensor in the available list. Should be used to initialize the sensor
    // selection in this card.
    public void initializeSensorSelection() {
        if (!TextUtils.isEmpty(mInitialSourceTagToSelect)) {
            // Don't select the initial source if it isn't actually available or if it is
            // already selected.
            if (!TextUtils.equals(mSensorId, mInitialSourceTagToSelect) &&
                    mAvailableSensorIds.contains(mInitialSourceTagToSelect)) {
                trySelectingNewSensor(mInitialSourceTagToSelect, mSensorId);
            } else {
                mInitialSourceTagToSelect = null;
            }
        } else {
            trySelectingNewSensor(mAvailableSensorIds.get(0), mSensorId);
        }
    }

    public void setRecording(long recordingStart) {
        mRecordingStart = recordingStart;
        if (mSensorPresenter != null) {
            mSensorPresenter.onRecordingStateChange(isRecording(), mRecordingStart);
        }
        updateCardMenu();
        updateRecordingUi();
    }

    public void lockUiForRecording() {
        setActive(false, false);
    }

    private boolean isRecording() {
        return mRecordingStart != RecordingMetadata.NOT_RECORDING;
    }

    private void updateRecordingUi() {
        // Show the stats drawer, hide toggle button when recording.
        if (mCardViewHolder != null) {
            int toggleButtonSpacerWidth = 0;
            if (isRecording()) {
                mCardViewHolder.graphStatsList.setVisibility(View.VISIBLE);
                // TODO: Animate this change.
                mCardViewHolder.toggleButton.setVisibility(View.GONE);
                toggleButtonSpacerWidth = mCardViewHolder.getContext().getResources()
                        .getDimensionPixelSize(R.dimen.sensor_card_header_padding);
            } else {
                mCardViewHolder.graphStatsList.setVisibility(View.GONE);
                mCardViewHolder.toggleButton.setVisibility(View.VISIBLE);
            }
            ViewGroup.LayoutParams params = mCardViewHolder.toggleButtonSpacer.getLayoutParams();
            params.width = toggleButtonSpacerWidth;
            mCardViewHolder.toggleButtonSpacer.setLayoutParams(params);
            updateButtonsVisibility(true /* animate */);
            // Close the menu, because options change when recording starts.
            if (mPopupMenu != null) {
                mPopupMenu.dismiss();
            }
        }
        // Collapse the header during recording.
        if (isRecording()) {
            setActive(false, false);
        }
    }

    @NonNull
    GoosciSensorLayout.SensorLayout buildLayout() {
        // Get an updated min and max, and return mLayout.
        mLayout.sensorId = getSelectedSensorId();
        if (mSensorPresenter != null) {
            mLayout.minimumYAxisValue = mSensorPresenter.getMinY();
            mLayout.maximumYAxisValue = mSensorPresenter.getMaxY();
        }
        mLayout.extras = mCardOptions.exportAsLayoutExtras();

        // Copy layout so that future modifications don't do bad things.
        return copyLayout(mLayout);
    }

    private GoosciSensorLayout.SensorLayout copyLayout(GoosciSensorLayout.SensorLayout layout) {
        try {
            return GoosciSensorLayout.SensorLayout.parseFrom(MessageNano.toByteArray(layout));
        } catch (InvalidProtocolBufferNanoException e) {
            throw new RuntimeException("Should be impossible", e);
        }
    }

    int getColorIndex() {
        return mLayout.colorIndex;
    }

    NewOptionsStorage getCardOptions(SensorChoice sensorChoice, Context context) {
        if (sensorChoice == null) {
            return mCardOptions;
        }
        // Use card options if set, otherwise sensor defaults.
        return new OverlayOptionsStorage(mCardOptions,
                sensorChoice.getStorageForSensorDefaultOptions(context));
    }

    public void refreshLabels(List<Label> labels) {
        if (mSensorPresenter != null) {
            mSensorPresenter.onLabelsChanged(labels);
        }
    }

    void setConnectingUI(String sensorId, boolean hasError, Context context, boolean allowRetry) {
        mAllowRetry = allowRetry;
        SensorAppearance appearance = mAppearanceProvider.getAppearance(sensorId);
        setUiForConnectingNewSensor(sensorId,
                Appearances.getSensorDisplayName(appearance, context), appearance.getUnits(context),
                hasError);
    }

    public boolean isTriggerBarOnScreen() {
        if (mCardViewHolder == null || mParentFragment == null ||
                mParentFragment.getActivity() == null) {
            return false;
        }
        Resources res = mCardViewHolder.getContext().getResources();
        int[] location = new int[2];
        mCardViewHolder.triggerSection.getLocationInWindow(location);
        if (location[1] <= res.getDimensionPixelSize(R.dimen.accessibility_touch_target_min_size)) {
            return false;
        }
        DisplayMetrics metrics = new DisplayMetrics();
        mParentFragment.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        if (metrics.heightPixels < location[1]) {
            return false;
        }
        return true;
    }

    public boolean hasError() {
        return mCardStatus.hasError();
    }
}
