package com.vorsk.minimalin.config;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.support.wearable.complications.ProviderInfoRetriever.OnProviderInfoReceivedCallback;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;

import com.vorsk.minimalin.R;
import com.vorsk.minimalin.config.color.ColorSelectionActivity;
import com.vorsk.minimalin.model.ConfigData.BackgroundComplicationConfigItem;
import com.vorsk.minimalin.model.ConfigData.ColorConfigItem;
import com.vorsk.minimalin.model.ConfigData.ComplicationsConfigItem;
import com.vorsk.minimalin.model.ConfigData.ConfigItemType;
import com.vorsk.minimalin.model.ConfigData.SwitchConfigItem;
import com.vorsk.minimalin.watchface.MinimalinWatchFaceService;

import java.util.ArrayList;
import java.util.concurrent.Executors;

import static com.vorsk.minimalin.config.color.ColorSelectionActivity.EXTRA_SHARED_PREF;

/**
 * Displays different layouts for configuring watch face's complications and appearance settings
 * (highlight color [second arm], background color, unread notifications, etc.).
 *
 * <p>All appearance settings are saved via {@link SharedPreferences}.
 *
 * <p>Layouts provided by this adapter are split into 5 main view types.
 *
 * <p>A watch face preview including complications. Allows user to tap on the complications to
 * change the complication data and see a live preview of the watch face.
 *
 * <p>Simple arrow to indicate there are more options below the fold.
 *
 * <p>Color configuration options for both highlight (seconds marker) and background color.
 *
 * <p>Toggle for unread notifications.
 *
 * <p>Background image complication configuration for changing background image of watch face.
 */
public class ConfigRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_COMPLICATIONS_CONFIG = 0;
    public static final int TYPE_COLOR_CONFIG = 1;
    public static final int TYPE_SWITCH_CONFIG = 2;
    public static final int TYPE_BACKGROUND_COMPLICATION_IMAGE_CONFIG = 3;
    private static final String TAG = "CompConfigAdapter";
    // ComponentName associated with watch face service (service that renders watch face). Used
    // to retrieve complication information.
    private ComponentName mWatchFaceComponentName;
    private ArrayList<ConfigItemType> mSettingsDataSet;
    private Context mContext;
    private SharedPreferences mSharedPref;
    // Selected complication id by user.
    private int mSelectedComplicationId;
    private int mBackgroundComplicationId;
    private int mLeftComplicationId;
    private int mRightComplicationId;
    private int mTopComplicationId;
    private int mBottomComplicationId;
    // Required to retrieve complication data from watch face for preview.
    private ProviderInfoRetriever mProviderInfoRetriever;
    // Maintains reference view holder to dynamically update watch face preview. Used instead of
    // notifyItemChanged(int position) to avoid flicker and re-inflating the view.
    private ComplicationsViewHolder mComplicationsViewHolder;

    ConfigRecyclerViewAdapter(
            Context context,
            Class watchFaceServiceClass,
            ArrayList<ConfigItemType> settingsDataSet) {

        mContext = context;
        mWatchFaceComponentName = new ComponentName(mContext, watchFaceServiceClass);
        mSettingsDataSet = settingsDataSet;

        // Default value is invalid (only changed when user taps to change complication).
        mSelectedComplicationId = -1;

        mBackgroundComplicationId =
                MinimalinWatchFaceService.getComplicationId(
                        ComplicationLocation.BACKGROUND);

        mLeftComplicationId =
                MinimalinWatchFaceService.getComplicationId(ComplicationLocation.LEFT);
        mRightComplicationId =
                MinimalinWatchFaceService.getComplicationId(ComplicationLocation.RIGHT);
        mTopComplicationId =
                MinimalinWatchFaceService.getComplicationId(ComplicationLocation.TOP);
        mBottomComplicationId =
                MinimalinWatchFaceService.getComplicationId(ComplicationLocation.BOTTOM);

        mSharedPref =
                context.getSharedPreferences(
                        context.getString(R.string.preference_file_key),
                        Context.MODE_PRIVATE);

        // Initialization of code to retrieve active complication data for the watch face.
        mProviderInfoRetriever =
                new ProviderInfoRetriever(mContext, Executors.newCachedThreadPool());
        mProviderInfoRetriever.init();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder(): viewType: " + viewType);

        RecyclerView.ViewHolder viewHolder = null;

        switch (viewType) {
            case TYPE_COMPLICATIONS_CONFIG:
                // Need direct reference to watch face preview view holder to update watch face
                mComplicationsViewHolder =
                        new ComplicationsViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_item_complications,
                                                parent,
                                                false));
                viewHolder = mComplicationsViewHolder;
                break;

            case TYPE_COLOR_CONFIG:
                viewHolder =
                        new ColorPickerViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.config_item_button, parent, false));
                break;

            case TYPE_SWITCH_CONFIG:
                viewHolder =
                        new SwitchViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_item_switch,
                                                parent,
                                                false));
                break;

            case TYPE_BACKGROUND_COMPLICATION_IMAGE_CONFIG:
                viewHolder =
                        new BackgroundComplicationViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_item_button,
                                                parent,
                                                false));
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Log.d(TAG, "Element " + position + " set.");

        // Pulls all data required for creating the UX for the specific setting option.
        ConfigItemType configItemType = mSettingsDataSet.get(position);

        switch (viewHolder.getItemViewType()) {
            case TYPE_COMPLICATIONS_CONFIG:
                ComplicationsViewHolder complicationsViewHolder =
                        (ComplicationsViewHolder) viewHolder;

                ComplicationsConfigItem complicationsConfigItem =
                        (ComplicationsConfigItem) configItemType;

                int defaultComplicationResourceId =
                        complicationsConfigItem.getDefaultComplicationResourceId();
                int defaultComplicationLongResourceId =
                        complicationsConfigItem.getDefaultComplicationLongResourceId();
                int defaultAddedComplicationResourceId =
                        complicationsConfigItem.getDefaultAddedComplicationResourceId();
                int defaultAddedComplicationLongResourceId =
                        complicationsConfigItem.getDefaultAddedComplicationLongResourceId();
                complicationsViewHolder.setDefaultComplicationDrawable(
                        defaultComplicationResourceId, defaultComplicationLongResourceId, defaultAddedComplicationResourceId, defaultAddedComplicationLongResourceId);

                complicationsViewHolder.initializesColorsAndComplications();
                break;


            case TYPE_COLOR_CONFIG:
                ColorPickerViewHolder colorPickerViewHolder = (ColorPickerViewHolder) viewHolder;
                ColorConfigItem colorConfigItem = (ColorConfigItem) configItemType;

                int iconResourceId = colorConfigItem.getIconResourceId();
                String name = colorConfigItem.getName();
                String sharedPrefString = colorConfigItem.getSharedPrefString();
                Class<ColorSelectionActivity> activity =
                        colorConfigItem.getActivityToChoosePreference();

                colorPickerViewHolder.setIcon(iconResourceId);
                colorPickerViewHolder.setName(name);
                colorPickerViewHolder.setSharedPrefString(sharedPrefString);
                colorPickerViewHolder.setLaunchActivityToSelectColor(activity);
                break;

            case TYPE_SWITCH_CONFIG:
                SwitchViewHolder switchViewHolder =
                        (SwitchViewHolder) viewHolder;

                SwitchConfigItem switchConfigItem =
                        (SwitchConfigItem) configItemType;

                int switchEnabledIconResourceId = switchConfigItem.getIconEnabledResourceId();
                int switchDisabledIconResourceId = switchConfigItem.getIconDisabledResourceId();

                String switchName = switchConfigItem.getName();
                int SharedPrefId = switchConfigItem.getSharedPrefId();

                switchViewHolder.setIcons(
                        switchEnabledIconResourceId, switchDisabledIconResourceId);
                switchViewHolder.setName(switchName);
                switchViewHolder.setSharedPrefId(SharedPrefId);
                break;

            case TYPE_BACKGROUND_COMPLICATION_IMAGE_CONFIG:
                BackgroundComplicationViewHolder backgroundComplicationViewHolder =
                        (BackgroundComplicationViewHolder) viewHolder;

                BackgroundComplicationConfigItem backgroundComplicationConfigItem =
                        (BackgroundComplicationConfigItem) configItemType;

                int backgroundIconResourceId = backgroundComplicationConfigItem.getIconResourceId();
                String backgroundName = backgroundComplicationConfigItem.getName();

                backgroundComplicationViewHolder.setIcon(backgroundIconResourceId);
                backgroundComplicationViewHolder.setName(backgroundName);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        ConfigItemType configItemType = mSettingsDataSet.get(position);
        return configItemType.getConfigType();
    }

    @Override
    public int getItemCount() {
        return mSettingsDataSet.size();
    }

    /**
     * Updates the selected complication id saved earlier with the new information.
     */
    void updateSelectedComplication(ComplicationProviderInfo complicationProviderInfo) {
        Log.d(TAG, "updateSelectedComplication: " + mComplicationsViewHolder);

        // Checks if view is inflated and complication id is valid.
        if (mComplicationsViewHolder != null && mSelectedComplicationId >= 0) {
            mComplicationsViewHolder.updateComplicationViews(
                    mSelectedComplicationId, complicationProviderInfo);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // Required to release retriever for active complication data on detach.
        mProviderInfoRetriever.release();
    }

    /**
     * Used by associated watch face ({@link MinimalinWatchFaceService}) to let this
     * adapter know which complication locations are supported, their ids, and supported
     * complication data types.
     */
    public enum ComplicationLocation {
        BACKGROUND,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    /**
     * Displays watch face complication locations. Allows user to tap on the
     * complication they want to change and preview updates dynamically.
     */
    public class ComplicationsViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private ImageButton mLeftComplication;
        private ImageButton mRightComplication;
        private ImageButton mTopComplication;
        private ImageButton mBottomComplication;

        private Drawable mDefaultComplicationDrawable;
        private Drawable mDefaultComplicationLongDrawable;
        private Drawable mDefaultAddedComplicationDrawable;
        private Drawable mDefaultAddedComplicationLongDrawable;

        ComplicationsViewHolder(final View view) {
            super(view);

            // Sets up left complication preview.
            mLeftComplication = view.findViewById(R.id.left_complication);
            mLeftComplication.setOnClickListener(this);

            // Sets up right complication preview.
            mRightComplication = view.findViewById(R.id.right_complication);
            mRightComplication.setOnClickListener(this);

            // Sets up top complication preview.
            mTopComplication = view.findViewById(R.id.top_complication);
            mTopComplication.setOnClickListener(this);

            // Sets up bottom complication preview.
            mBottomComplication = view.findViewById(R.id.bottom_complication);
            mBottomComplication.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (view.equals(mLeftComplication)) {
                Log.d(TAG, "Left Complication click()");

                Activity currentActivity = (Activity) view.getContext();
                launchComplicationHelperActivity(currentActivity, ComplicationLocation.LEFT);

            } else if (view.equals(mRightComplication)) {
                Log.d(TAG, "Right Complication click()");

                Activity currentActivity = (Activity) view.getContext();
                launchComplicationHelperActivity(currentActivity, ComplicationLocation.RIGHT);
            } else if (view.equals(mTopComplication)) {
                Log.d(TAG, "Top Complication click()");

                Activity currentActivity = (Activity) view.getContext();
                launchComplicationHelperActivity(currentActivity, ComplicationLocation.TOP);
            } else if (view.equals(mBottomComplication)) {
                Log.d(TAG, "Bottom Complication click()");

                Activity currentActivity = (Activity) view.getContext();
                launchComplicationHelperActivity(currentActivity, ComplicationLocation.BOTTOM);
            }
        }


        // Verifies the watch face supports the complication location, then launches the helper
        // class, so user can choose their complication data provider.
        private void launchComplicationHelperActivity(
                Activity currentActivity, ComplicationLocation complicationLocation) {

            mSelectedComplicationId =
                    MinimalinWatchFaceService.getComplicationId(complicationLocation);

            if (mSelectedComplicationId >= 0) {

                int[] supportedTypes =
                        MinimalinWatchFaceService.getSupportedComplicationTypes(
                                complicationLocation);

                ComponentName watchFace =
                        new ComponentName(
                                currentActivity, MinimalinWatchFaceService.class);

                currentActivity.startActivityForResult(
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                                currentActivity,
                                watchFace,
                                mSelectedComplicationId,
                                supportedTypes),
                        ConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE);

            } else {
                Log.d(TAG, "Complication not supported by watch face.");
            }
        }

        void setDefaultComplicationDrawable(int resourceId, int longResourceId, int addedResourceId, int addedLongResourceId) {
            mDefaultComplicationDrawable = mContext.getDrawable(resourceId);
            mDefaultComplicationLongDrawable = mContext.getDrawable(longResourceId);
            mDefaultAddedComplicationDrawable = mContext.getDrawable(addedResourceId);
            mDefaultAddedComplicationLongDrawable = mContext.getDrawable(addedLongResourceId);
        }

        void updateComplicationViews(
                int watchFaceComplicationId, ComplicationProviderInfo complicationProviderInfo) {
            Log.d(TAG, "updateComplicationViews(): id: " + watchFaceComplicationId);
            Log.d(TAG, "\tinfo: " + complicationProviderInfo);

            if (watchFaceComplicationId != mBackgroundComplicationId) {
                if (watchFaceComplicationId == mLeftComplicationId) {
                    updateComplicationView(complicationProviderInfo, mLeftComplication, false);
                } else if (watchFaceComplicationId == mRightComplicationId) {
                    updateComplicationView(complicationProviderInfo, mRightComplication, false);
                } else if (watchFaceComplicationId == mTopComplicationId) {
                    updateComplicationView(complicationProviderInfo, mTopComplication, false);
                } else if (watchFaceComplicationId == mBottomComplicationId) {
                    updateComplicationView(complicationProviderInfo, mBottomComplication, true);
                }
            } // Currently I don't preview the background complication in the preview
        }

        private void updateComplicationView(ComplicationProviderInfo complicationProviderInfo,
                                            ImageButton button, boolean big) {
            if (complicationProviderInfo != null) {
                button.setImageIcon(complicationProviderInfo.providerIcon);
                button.setContentDescription(
                        mContext.getString(R.string.edit_complication,
                                complicationProviderInfo.appName + " " +
                                        complicationProviderInfo.providerName));
                if (big) {
                    button.setBackground(mDefaultAddedComplicationLongDrawable);
                } else {
                    button.setBackground(mDefaultAddedComplicationDrawable);
                }
            } else {
                if (big) {
                    button.setImageDrawable(mDefaultComplicationLongDrawable);
                } else {
                    button.setImageDrawable(mDefaultComplicationDrawable);
                }
                button.setBackgroundResource(android.R.color.transparent);
                button.setContentDescription(mContext.getString(R.string.add_complication));
            }
        }

        void initializesColorsAndComplications() {
            final int[] complicationIds = MinimalinWatchFaceService.getComplicationIds();

            mProviderInfoRetriever.retrieveProviderInfo(
                    new OnProviderInfoReceivedCallback() {
                        @Override
                        public void onProviderInfoReceived(
                                int watchFaceComplicationId,
                                @Nullable ComplicationProviderInfo complicationProviderInfo) {

                            Log.d(TAG, "onProviderInfoReceived: " + complicationProviderInfo);

                            updateComplicationViews(
                                    watchFaceComplicationId, complicationProviderInfo);
                        }
                    },
                    mWatchFaceComponentName,
                    complicationIds);
        }
    }


    /**
     * Displays color options for the an item on the watch face. These could include marker color,
     * background color, etc.
     */
    public class ColorPickerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private Button mAppearanceButton;

        private String mSharedPrefResourceString;

        private Class<ColorSelectionActivity> mLaunchActivityToSelectColor;

        ColorPickerViewHolder(View view) {
            super(view);

            mAppearanceButton = view.findViewById(R.id.item_button);
            view.setOnClickListener(this);
        }

        public void setName(String name) {
            mAppearanceButton.setText(name);
        }

        public void setIcon(int resourceId) {
            Context context = mAppearanceButton.getContext();
            mAppearanceButton.setCompoundDrawablesWithIntrinsicBounds(
                    context.getDrawable(resourceId), null, null, null);
        }

        void setSharedPrefString(String sharedPrefString) {
            mSharedPrefResourceString = sharedPrefString;
        }

        void setLaunchActivityToSelectColor(Class<ColorSelectionActivity> activity) {
            mLaunchActivityToSelectColor = activity;
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            Log.d(TAG, "Complication onClick() position: " + position);

            if (mLaunchActivityToSelectColor != null) {
                Intent launchIntent = new Intent(view.getContext(), mLaunchActivityToSelectColor);

                // Pass shared preference name to save color value to.
                launchIntent.putExtra(EXTRA_SHARED_PREF, mSharedPrefResourceString);

                Activity activity = (Activity) view.getContext();
                activity.startActivityForResult(
                        launchIntent,
                        ConfigActivity.UPDATE_COLORS_CONFIG_REQUEST_CODE);
            }
        }
    }

    /**
     * Displays switch for toggle settings. User can
     * toggle on/off.
     */
    public class SwitchViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private Switch mSwitch;

        private int mEnabledIconResourceId;
        private int mDisabledIconResourceId;

        private int mSharedPrefResourceId;

        SwitchViewHolder(View view) {
            super(view);

            mSwitch = view.findViewById(R.id.item_switch);
            view.setOnClickListener(this);
        }

        public void setName(String name) {
            mSwitch.setText(name);
        }

        void setIcons(int enabledIconResourceId, int disabledIconResourceId) {

            mEnabledIconResourceId = enabledIconResourceId;
            mDisabledIconResourceId = disabledIconResourceId;

            Context context = mSwitch.getContext();

            // Set default to enabled.
            mSwitch.setCompoundDrawablesWithIntrinsicBounds(
                    context.getDrawable(mEnabledIconResourceId), null, null, null);
        }

        void setSharedPrefId(int sharedPrefId) {
            mSharedPrefResourceId = sharedPrefId;

            if (mSwitch != null) {

                Context context = mSwitch.getContext();
                String sharedPreferenceString = context.getString(mSharedPrefResourceId);
                Boolean currentState = mSharedPref.getBoolean(sharedPreferenceString, true);

                updateIcon(context, currentState);
            }
        }

        private void updateIcon(Context context, Boolean currentState) {
            int currentIconResourceId;

            if (currentState) {
                currentIconResourceId = mEnabledIconResourceId;
            } else {
                currentIconResourceId = mDisabledIconResourceId;
            }

            mSwitch.setChecked(currentState);
            mSwitch.setCompoundDrawablesWithIntrinsicBounds(
                    context.getDrawable(currentIconResourceId), null, null, null);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            Log.d(TAG, "Complication onClick() position: " + position);

            Context context = view.getContext();
            String sharedPreferenceString = context.getString(mSharedPrefResourceId);

            // Since user clicked on a switch, new state should be opposite of current state.
            Boolean newState = !mSharedPref.getBoolean(sharedPreferenceString, true);

            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putBoolean(sharedPreferenceString, newState);
            editor.apply();

            updateIcon(context, newState);
        }
    }

    /**
     * Displays button to trigger background image complication selector.
     */
    public class BackgroundComplicationViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private Button mBackgroundComplicationButton;

        BackgroundComplicationViewHolder(View view) {
            super(view);

            mBackgroundComplicationButton = view.findViewById(R.id.item_button);
            view.setOnClickListener(this);
        }

        public void setName(String name) {
            mBackgroundComplicationButton.setText(name);
        }

        public void setIcon(int resourceId) {
            Context context = mBackgroundComplicationButton.getContext();
            mBackgroundComplicationButton.setCompoundDrawablesWithIntrinsicBounds(
                    context.getDrawable(resourceId), null, null, null);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            Log.d(TAG, "Background Complication onClick() position: " + position);

            Activity currentActivity = (Activity) view.getContext();

            mSelectedComplicationId =
                    MinimalinWatchFaceService.getComplicationId(
                            ComplicationLocation.BACKGROUND);

            if (mSelectedComplicationId >= 0) {

                int[] supportedTypes =
                        MinimalinWatchFaceService.getSupportedComplicationTypes(
                                ComplicationLocation.BACKGROUND);

                ComponentName watchFace =
                        new ComponentName(
                                currentActivity, MinimalinWatchFaceService.class);

                currentActivity.startActivityForResult(
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                                currentActivity,
                                watchFace,
                                mSelectedComplicationId,
                                supportedTypes),
                        ConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE);

            } else {
                Log.d(TAG, "Complication not supported by watch face.");
            }
        }
    }
}
