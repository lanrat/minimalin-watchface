package com.vorsk.minimalin.model;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.SystemProviders;

import com.vorsk.minimalin.MaterialColors;
import com.vorsk.minimalin.R;
import com.vorsk.minimalin.config.ConfigActivity;
import com.vorsk.minimalin.config.ConfigRecyclerViewAdapter;
import com.vorsk.minimalin.config.color.ColorSelectionActivity;
import com.vorsk.minimalin.watchface.MinimalinWatchFaceService;

import java.util.ArrayList;

/**
 * Data represents different views for configuring the
 * {@link MinimalinWatchFaceService} watch face's appearance and complications
 * via {@link ConfigActivity}.
 */
public class ConfigData {

    // default setting for booleans
    public static final boolean DEFAULT_BACKGROUND_GRADIENT = true;
    public static final boolean DEFAULT_COMPLICATION_BACKGROUND = false;
    public static final boolean DEFAULT_UNREAD_NOTIFICATION = true;
    public static final boolean DEFAULT_24_HOUR_TIME = false;
    public static final boolean DEFAULT_NOTIFICATION_COMPLICATION = false;
    public static final String DEFAULT_BACKGROUND_COLOR = MaterialColors.Color.BLUE_GRAY.name();
    public static final String DEFAULT_PRIMARY_COLOR = MaterialColors.Color.BLUE.name();
    public static final String DEFAULT_SECONDARY_COLOR = MaterialColors.Color.ORANGE.name();
    // best to choose complications that do not require the RECEIVE_COMPLICATION_DATA permission so they render on first load
    // https://developer.android.com/reference/android/support/wearable/complications/SystemProviders
    public static final int[] DEFAULT_LEFT_COMPLICATION = {SystemProviders.WATCH_BATTERY,  ComplicationData.TYPE_RANGED_VALUE};
    public static final int[] DEFAULT_RIGHT_COMPLICATION = {SystemProviders.STEP_COUNT,  ComplicationData.TYPE_SHORT_TEXT};
    public static final int[] DEFAULT_TOP_COMPLICATION = {SystemProviders.DATE,  ComplicationData.TYPE_SHORT_TEXT};
    public static final int[] DEFAULT_BOTTOM_COMPLICATION = {SystemProviders.NEXT_EVENT,  ComplicationData.TYPE_LONG_TEXT};
    //public static final int[] DEFAULT_NOTIFICATION_COMPLICATION = {SystemProviders.UNREAD_NOTIFICATION_COUNT,  ComplicationData.TYPE_LONG_TEXT};

    /**
     * Returns Watch Face Service class associated with configuration Activity.
     */
    public static Class getWatchFaceServiceClass() {
        return MinimalinWatchFaceService.class;
    }

    /**
     * Includes all data to populate each of the 5 different custom
     * {@link ViewHolder} types in {@link ConfigRecyclerViewAdapter}.
     */
    public static ArrayList<ConfigItemType> getDataToPopulateAdapter(Context context) {

        ArrayList<ConfigItemType> settingsConfigData = new ArrayList<>();

        // Data for watch face complications UX in settings Activity.
        ConfigItemType complicationConfigItem =
                new ComplicationsConfigItem(R.drawable.add_complication, R.drawable.add_big_complication, R.drawable.added_complication, R.drawable.added_big_complication);
        settingsConfigData.add(complicationConfigItem);

        ConfigItemType primaryColorConfigItem =
                new ColorConfigItem(
                        context.getString(R.string.config_primary_color_label),
                        R.drawable.ic_color_lens,
                        context.getString(R.string.saved_primary_color),
                        ColorSelectionActivity.class);
        settingsConfigData.add(primaryColorConfigItem);

        ConfigItemType accentColorConfigItem =
                new ColorConfigItem(
                        context.getString(R.string.config_secondary_color_label),
                        R.drawable.ic_color_lens,
                        context.getString(R.string.saved_secondary_color),
                        ColorSelectionActivity.class);
        settingsConfigData.add(accentColorConfigItem);

        // Data for Background color UX in settings Activity.
        ConfigItemType backgroundColorConfigItem =
                new ColorConfigItem(
                        context.getString(R.string.config_background_color_label),
                        R.drawable.ic_color_lens,
                        context.getString(R.string.saved_background_color),
                        ColorSelectionActivity.class);
        settingsConfigData.add(backgroundColorConfigItem);

        // Data for background ic_gradient
        ConfigItemType gradientConfigItem =
                new SwitchConfigItem(
                        context.getString(R.string.config_background_gradient_label),
                        R.drawable.ic_gradient,
                        R.drawable.ic_square,
                        R.string.saved_background_gradient,
                        DEFAULT_BACKGROUND_GRADIENT);
        settingsConfigData.add(gradientConfigItem);

        // Data notification complication
        ConfigItemType notificationComplicationToggle =
                new ComplicationSwitchConfigItem(
                        context.getString(R.string.config_notification_complication_label),
                        R.drawable.ic_notifications,
                        R.drawable.ic_notification_outline,
                        R.string.saved_notification_complication,
                        ConfigRecyclerViewAdapter.ComplicationLocation.NOTIFICATION,
                        R.string.notification_complication_instruction_toast,
                        DEFAULT_NOTIFICATION_COMPLICATION);
        settingsConfigData.add(notificationComplicationToggle);

        // Data for complication background
        ConfigItemType complicationBackgroundConfigItem =
                new SwitchConfigItem(
                        context.getString(R.string.config_complication_background_label),
                        R.drawable.ic_plus_circle,
                        R.drawable.ic_plus,
                        R.string.saved_complication_background,
                        DEFAULT_COMPLICATION_BACKGROUND);
        settingsConfigData.add(complicationBackgroundConfigItem);

        // Data for 'Unread Notifications' UX (toggle) in settings Activity.
        ConfigItemType unreadNotificationsConfigItem =
                new SwitchConfigItem(
                        context.getString(R.string.config_unread_notifications_label),
                        R.drawable.ic_notifications,
                        R.drawable.ic_notifications_off,
                        R.string.saved_unread_notifications_pref,
                        DEFAULT_UNREAD_NOTIFICATION);
        settingsConfigData.add(unreadNotificationsConfigItem);

        // Data for 24 hour mode UX (toggle) in settings Activity.
        ConfigItemType militaryTimeConfigItem =
                new SwitchConfigItem(
                        context.getString(R.string.config_24_hour_label),
                        R.drawable.time_24h,
                        R.drawable.time_12h,
                        R.string.saved_24h_pref,
                        DEFAULT_24_HOUR_TIME);
        settingsConfigData.add(militaryTimeConfigItem);

        // Data for background complications UX in settings Activity.
        ConfigItemType backgroundImageComplicationConfigItem =
                // TODO move color and complication selector to same select activity
                new BackgroundComplicationConfigItem(
                        context.getString(R.string.config_background_image_complication_label),
                        R.drawable.ic_landscape);
        settingsConfigData.add(backgroundImageComplicationConfigItem);

        return settingsConfigData;
    }

    /**
     * Interface all ConfigItems must implement so the {@link RecyclerView}'s Adapter associated
     * with the configuration activity knows what type of ViewHolder to inflate.
     */
    public interface ConfigItemType {
        int getConfigType();
    }

    /**
     * Data for Watch Face Complications Preview item in RecyclerView.
     */
    public static class ComplicationsConfigItem implements ConfigItemType {

        private int defaultComplicationResourceId;
        private int defaultComplicationLongResourceId;
        private int defaultAddedComplicationResourceId;
        private int defaultAddedComplicationLongResourceId;

        ComplicationsConfigItem(int defaultComplicationResourceId, int defaultComplicationLongResourceId,
                                int defaultAddedComplicationResourceId, int defaultAddedComplicationLongResourceId) {
            this.defaultComplicationResourceId = defaultComplicationResourceId;
            this.defaultComplicationLongResourceId = defaultComplicationLongResourceId;
            this.defaultAddedComplicationResourceId = defaultAddedComplicationResourceId;
            this.defaultAddedComplicationLongResourceId = defaultAddedComplicationLongResourceId;
        }

        public int getDefaultComplicationResourceId() {
            return defaultComplicationResourceId;
        }

        public int getDefaultComplicationLongResourceId() {
            return defaultComplicationLongResourceId;
        }

        public int getDefaultAddedComplicationResourceId() {
            return defaultAddedComplicationResourceId;
        }

        public int getDefaultAddedComplicationLongResourceId() {
            return defaultAddedComplicationLongResourceId;
        }

        @Override
        public int getConfigType() {
            return ConfigRecyclerViewAdapter.TYPE_COMPLICATIONS_CONFIG;
        }
    }

    /**
     * Data for color picker item in RecyclerView.
     */
    public static class ColorConfigItem implements ConfigItemType {

        private String name;
        private int iconResourceId;
        private String sharedPrefString;
        private Class<ColorSelectionActivity> activityToChoosePreference;

        ColorConfigItem(
                String name,
                int iconResourceId,
                String sharedPrefString,
                Class<ColorSelectionActivity> activity) {
            this.name = name;
            this.iconResourceId = iconResourceId;
            this.sharedPrefString = sharedPrefString;
            this.activityToChoosePreference = activity;
        }

        public String getName() {
            return name;
        }

        public int getIconResourceId() {
            return iconResourceId;
        }

        public String getSharedPrefString() {
            return sharedPrefString;
        }

        public Class<ColorSelectionActivity> getActivityToChoosePreference() {
            return activityToChoosePreference;
        }

        @Override
        public int getConfigType() {
            return ConfigRecyclerViewAdapter.TYPE_COLOR_CONFIG;
        }
    }

    /**
     * Data for switch preference picker item in RecyclerView.
     */
    public static class SwitchConfigItem implements ConfigItemType {

        private String name;
        private int iconEnabledResourceId;
        private int iconDisabledResourceId;
        private int sharedPrefId;
        private boolean switchDefault;

        SwitchConfigItem(
                String name,
                int iconEnabledResourceId,
                int iconDisabledResourceId,
                int sharedPrefId,
                boolean switchDefault) {
            this.name = name;
            this.iconEnabledResourceId = iconEnabledResourceId;
            this.iconDisabledResourceId = iconDisabledResourceId;
            this.sharedPrefId = sharedPrefId;
            this.switchDefault = switchDefault;
        }

        public String getName() {
            return name;
        }

        public boolean getDefault() {
            return switchDefault;
        }

        public int getIconEnabledResourceId() {
            return iconEnabledResourceId;
        }

        public int getIconDisabledResourceId() {
            return iconDisabledResourceId;
        }

        public int getSharedPrefId() {
            return sharedPrefId;
        }

        @Override
        public int getConfigType() {
            return ConfigRecyclerViewAdapter.TYPE_SWITCH_CONFIG;
        }
    }

    public static class ComplicationSwitchConfigItem extends SwitchConfigItem {

        private ConfigRecyclerViewAdapter.ComplicationLocation complicationLocation;
        private int instructionToastTextID;

        ComplicationSwitchConfigItem(
                String name,
                int iconEnabledResourceId,
                int iconDisabledResourceId,
                int sharedPrefId,
                ConfigRecyclerViewAdapter.ComplicationLocation complicationLocation,
                int instructionToastTextID,
                boolean switchDefault) {
            super(name, iconEnabledResourceId, iconDisabledResourceId, sharedPrefId, switchDefault);
            this.complicationLocation = complicationLocation;
            this.instructionToastTextID = instructionToastTextID;
        }

        public ConfigRecyclerViewAdapter.ComplicationLocation getComplicationLocation() {
            return this.complicationLocation;
        }

        public int getInstructionToastTextID() {
            return instructionToastTextID;
        }

        @Override
        public int getConfigType() {
            return ConfigRecyclerViewAdapter.TYPE_COMPLICATION_SWITCH_CONFIG;
        }
    }

    /**
     * Data for background image complication picker item in RecyclerView.
     */
    public static class BackgroundComplicationConfigItem implements ConfigItemType {

        private String name;
        private int iconResourceId;

        BackgroundComplicationConfigItem(
                String name,
                int iconResourceId) {

            this.name = name;
            this.iconResourceId = iconResourceId;
        }

        public String getName() {
            return name;
        }

        public int getIconResourceId() {
            return iconResourceId;
        }

        @Override
        public int getConfigType() {
            return ConfigRecyclerViewAdapter.TYPE_BACKGROUND_COMPLICATION_IMAGE_CONFIG;
        }
    }
}
