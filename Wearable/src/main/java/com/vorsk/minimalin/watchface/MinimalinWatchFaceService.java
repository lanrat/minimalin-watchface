package com.vorsk.minimalin.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;

import com.vorsk.minimalin.MaterialColors;
import com.vorsk.minimalin.R;
import com.vorsk.minimalin.config.ConfigRecyclerViewAdapter;
import com.vorsk.minimalin.model.ConfigData;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class MinimalinWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "MinimalinWatchFaceService";

    // Unique IDs for each complication. The settings activity that supports allowing users
    // to select their complication data provider requires numbers to be >= 0.
    private static final int BACKGROUND_COMPLICATION_ID = 0;

    private static final int LEFT_COMPLICATION_ID = 100;
    private static final int RIGHT_COMPLICATION_ID = 101;
    private static final int TOP_COMPLICATION_ID = 102;
    private static final int BOTTOM_COMPLICATION_ID = 103;
    private static final int NOTIFICATION_COMPLICATION_ID = 104;

    // Background, Left and right complication IDs as array for Complication API.
    private static final int[] COMPLICATION_IDS = {
            BACKGROUND_COMPLICATION_ID, LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID, TOP_COMPLICATION_ID, BOTTOM_COMPLICATION_ID, NOTIFICATION_COMPLICATION_ID
    };

    // Left and right dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            // background
            {ComplicationData.TYPE_LARGE_IMAGE},
            {
                    // left
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_NO_PERMISSION
            },
            {
                    // right
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_NO_PERMISSION
            },
            {
                    // top
                    ComplicationData.TYPE_LONG_TEXT,
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_NO_PERMISSION
            },
            {
                    // bottom
                    ComplicationData.TYPE_LONG_TEXT,
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_NO_PERMISSION
            },
            {
                    // notification
                    ComplicationData.TYPE_LONG_TEXT,
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_NO_PERMISSION
            }
    };

    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private Rect mTextRect = new Rect();

    // Used by {@link ConfigRecyclerViewAdapter} to check if complication location
    // is supported in settings config_list activity.
    public static int getComplicationId(
            ConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case BACKGROUND:
                return BACKGROUND_COMPLICATION_ID;
            case LEFT:
                return LEFT_COMPLICATION_ID;
            case RIGHT:
                return RIGHT_COMPLICATION_ID;
            case TOP:
                return TOP_COMPLICATION_ID;
            case BOTTOM:
                return BOTTOM_COMPLICATION_ID;
            case NOTIFICATION:
                return NOTIFICATION_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    // Used by {@link ConfigRecyclerViewAdapter} to retrieve all complication ids.
    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    // Used by {@link ConfigRecyclerViewAdapter} to see which complication types
    // are supported in the settings config_list activity.
    public static int[] getSupportedComplicationTypes(
            ConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case BACKGROUND:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case LEFT:
                return COMPLICATION_SUPPORTED_TYPES[1];
            case RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[2];
            case TOP:
                return COMPLICATION_SUPPORTED_TYPES[3];
            case BOTTOM:
                return COMPLICATION_SUPPORTED_TYPES[4];
            case NOTIFICATION:
                return COMPLICATION_SUPPORTED_TYPES[5];
            default:
                return new int[]{};
        }
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    // see https://www.slideshare.net/rtc1/intro-todrawingtextandroid
    private void drawVertAlignedText(Canvas canvas, float x, float y, String s, Paint p, TextVerticalAlign vertAlign) {
        //Rect r = new Rect();
        p.getTextBounds(s, 0, s.length(), mTextRect); //Note r.top will be negative
        float textX = x - mTextRect.width() / 2f - mTextRect.left;
        float textY = y;
        switch (vertAlign) {
            case Top:
                textY = y - mTextRect.top;
                break;
            case Middle:
                textY = y - mTextRect.top - mTextRect.height() / 2;
                break;
            case Baseline:
                break;
            case Bottom:
                textY = y - (mTextRect.height() + mTextRect.top);
                break;
        }
        canvas.drawText(s, textX, textY, p);
    }

    public enum TextVerticalAlign {
        Top,
        Middle,
        Baseline,
        Bottom
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final int MSG_UPDATE_TIME = 0;

        private static final float HOUR_STROKE_WIDTH = 6f;
        private static final float MINUTE_STROKE_WIDTH = 4f;
        private static final float SECOND_TICK_STROKE_WIDTH = 3f;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 6f;
        private static final float NOTIFICATION_OUTLINE_STROKE_WIDTH = 2f;

        private static final int SHADOW_RADIUS = 3;
        // Used to pull user's preferences for background color, highlight color, and visual
        // indicating there are unread notifications.
        SharedPreferences mSharedPref;
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        mCalendar.setTimeZone(TimeZone.getDefault());
                        invalidate();
                    }
                };
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;
        private float mCenterX;
        private float mCenterY;
        private float mSecondHandLength;
        private float mMinuteHandLength;
        private float mHourHandLength;
        private float mTickLength;
        private float mMinimalinTextRadiusLength;
        private float mMinimalinVerticalTimeGap;
        // Colors for all hands (hour, minute, seconds, ticks) based on photo loaded.
        private int mWatchSecondHandHighlightColor;
        private int mWatchMinuteHandHighlightColor;
        private int mWatchHourHandHighlightColor;
        private int mWatchHandShadowColor;
        private int mBackgroundColor;
        private int mWatchComplicationsColor; // TODO change or remove this, currently just used for notification dot
        private boolean mIsBackgroundDark;
        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mNotificationCirclePaint;
        private Paint mTicksPaint;
        private Paint mBackgroundCirclePaint;
        private TextPaint mMinimalinTimePaint;
        private TextPaint mNotificationCountPaint;
        private Paint mBackgroundPaint;
        private MaterialColors.Color mPrimaryMaterialColor;
        private MaterialColors.Color mSecondaryMaterialColor;
        private MaterialColors.Color mBackgroundMaterialColor;

        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        // User's preference for if they want visual shown to indicate unread notifications.
        private boolean mUnreadNotificationsPreference;
        private int mNumberOfUnreadNotifications = 0;
        private boolean mMilitaryTimePreference;
        private boolean mBackgroundGradient;
        private boolean mComplicationBackgrounds;
        private Bitmap mBackgroundGradientBitmap;


        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         */
        private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;
        private boolean mAmbient;
        // Handler to update the time once a second in interactive mode.
        private final Handler mUpdateTimeHandler =
                new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs =
                                    INTERACTIVE_UPDATE_RATE_MS
                                            - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                    }
                };


        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(TAG, "onCreate");

            super.onCreate(holder);

            // Used throughout watch face to pull user's preferences.
            Context context = getApplicationContext();
            mSharedPref =
                    context.getSharedPreferences(
                            getString(R.string.preference_file_key),
                            Context.MODE_PRIVATE);

            mCalendar = Calendar.getInstance();

            setWatchFaceStyle(
                    new WatchFaceStyle.Builder(MinimalinWatchFaceService.this)
                            .setAcceptsTapEvents(true)
                            .setHideNotificationIndicator(true) // unread top indicator
                            .setShowUnreadCountIndicator(false) // unread count #
                            //.setStatusBarGravity(Gravity.CENTER_HORIZONTAL)
                            .setViewProtectionMode(WatchFaceStyle.PROTECT_STATUS_BAR)
                            .build());

            loadSavedPreferences();
            initializeComplications();
            initializeWatchFace();
        }

        // Pulls all user's preferences for watch face appearance.
        private void loadSavedPreferences() {
            //mSharedPref.edit().clear().commit(); // used for testing, resets all settings to default
            String primaryColorResourceName =
                    getApplicationContext().getString(R.string.saved_primary_color);
            String primaryColorName = mSharedPref.getString(primaryColorResourceName, ConfigData.DEFAULT_PRIMARY_COLOR);
            mPrimaryMaterialColor = MaterialColors.Get(primaryColorName);

            String secondaryColorResourceName =
                    getApplicationContext().getString(R.string.saved_secondary_color);
            String secondaryColorName = mSharedPref.getString(secondaryColorResourceName, ConfigData.DEFAULT_SECONDARY_COLOR);
            mSecondaryMaterialColor = MaterialColors.Get(secondaryColorName);

            String backgroundGradientPreferenceResourceName =
                    getApplicationContext().getString(R.string.saved_background_gradient);
            mBackgroundGradient =
                    mSharedPref.getBoolean(backgroundGradientPreferenceResourceName, ConfigData.DEFAULT_BACKGROUND_GRADIENT);

            String backgroundColorResourceName =
                    getApplicationContext().getString(R.string.saved_background_color);
            String backgroundColorName = mSharedPref.getString(backgroundColorResourceName, ConfigData.DEFAULT_BACKGROUND_COLOR);
            mBackgroundMaterialColor = MaterialColors.Get(backgroundColorName);

            mBackgroundColor = mBackgroundMaterialColor.Color(500);
            mWatchComplicationsColor = mSecondaryMaterialColor.Color(800);
            mWatchSecondHandHighlightColor = mPrimaryMaterialColor.Color(500);
            mWatchMinuteHandHighlightColor = mPrimaryMaterialColor.Color(300);
            mWatchHourHandHighlightColor = mPrimaryMaterialColor.Color(200);

            // Initialize background color (in case background complication is inactive).
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mBackgroundColor);


            // invalidate cache of mBackgroundGradient in case color changed
            if (mBackgroundGradient) {
                mBackgroundGradientBitmap = null;
            }

            mIsBackgroundDark = MaterialColors.isColorDark(mBackgroundColor);

            // this is not ideal with black hands on dark background, but changing it to white looks worse
            mWatchHandShadowColor = Color.BLACK;

            String unreadNotificationPreferenceResourceName =
                    getApplicationContext().getString(R.string.saved_unread_notifications_pref);
            mUnreadNotificationsPreference =
                    mSharedPref.getBoolean(unreadNotificationPreferenceResourceName, ConfigData.DEFAULT_UNREAD_NOTIFICATION);

            String complicationBackgroundPreferenceResourceName =
                    getApplicationContext().getString(R.string.saved_complication_background);
            mComplicationBackgrounds =
                    mSharedPref.getBoolean(complicationBackgroundPreferenceResourceName, ConfigData.DEFAULT_COMPLICATION_BACKGROUND);

            String militaryTimePreferenceResourceName =
                    getApplicationContext().getString(R.string.saved_24h_pref);
            mMilitaryTimePreference =
                    mSharedPref.getBoolean(militaryTimePreferenceResourceName, ConfigData.DEFAULT_24_HOUR_TIME);
        }

        private void initializeComplications() {
            Log.d(TAG, "initializeComplications()");

            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            // Creates a ComplicationDrawable for each location where the user can render a
            // complication on the watch face.
            // All styles for the complications are defined in
            // drawable/custom_complication_styles.xml.
            ComplicationDrawable leftComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            leftComplicationDrawable.setContext(getApplicationContext());
            ComplicationDrawable rightComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            rightComplicationDrawable.setContext(getApplicationContext());
            ComplicationDrawable topComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            topComplicationDrawable.setContext(getApplicationContext());
            ComplicationDrawable bottomComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            bottomComplicationDrawable.setContext(getApplicationContext());
            ComplicationDrawable notificationComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            notificationComplicationDrawable.setContext(getApplicationContext());

            ComplicationDrawable backgroundComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            // Adds new complications to a SparseArray to simplify setting styles and ambient
            // properties for all complications, i.e., iterate over them all.
            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);
            mComplicationDrawableSparseArray.put(TOP_COMPLICATION_ID, topComplicationDrawable);
            mComplicationDrawableSparseArray.put(BOTTOM_COMPLICATION_ID, bottomComplicationDrawable);
            mComplicationDrawableSparseArray.put(NOTIFICATION_COMPLICATION_ID, notificationComplicationDrawable);
            mComplicationDrawableSparseArray.put(
                    BACKGROUND_COMPLICATION_ID, backgroundComplicationDrawable);

            // set default values
            setDefaultSystemComplicationProvider(TOP_COMPLICATION_ID, ConfigData.DEFAULT_TOP_COMPLICATION[0], ConfigData.DEFAULT_TOP_COMPLICATION[1]);
            setDefaultSystemComplicationProvider(LEFT_COMPLICATION_ID, ConfigData.DEFAULT_LEFT_COMPLICATION[0], ConfigData.DEFAULT_LEFT_COMPLICATION[1]);
            setDefaultSystemComplicationProvider(RIGHT_COMPLICATION_ID, ConfigData.DEFAULT_RIGHT_COMPLICATION[0], ConfigData.DEFAULT_RIGHT_COMPLICATION[1]);
            setDefaultSystemComplicationProvider(BOTTOM_COMPLICATION_ID, ConfigData.DEFAULT_BOTTOM_COMPLICATION[0], ConfigData.DEFAULT_BOTTOM_COMPLICATION[1]);
            // TODO notification complication

            setComplicationsActiveAndAmbientColors(mWatchComplicationsColor);
            setActiveComplications(COMPLICATION_IDS);
        }

        private void initializeWatchFace() {
            mHourPaint = new Paint();
            mHourPaint.setColor(mWatchHourHandHighlightColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchMinuteHandHighlightColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mWatchMinuteHandHighlightColor);
            mSecondPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mNotificationCirclePaint = new Paint();
            mNotificationCirclePaint.setColor(mWatchComplicationsColor); // TODO change this? (especially if we allow the border to be hidden)
            mNotificationCirclePaint.setAntiAlias(true);
            mNotificationCirclePaint.setStyle(Paint.Style.FILL);
            mNotificationCirclePaint.setStrokeWidth(NOTIFICATION_OUTLINE_STROKE_WIDTH);

            mBackgroundCirclePaint = new Paint();
            mBackgroundCirclePaint.setColor(getApplicationContext().getColor(R.color.color_complication_background_dark));
            mBackgroundCirclePaint.setStyle(Paint.Style.STROKE);
            mBackgroundCirclePaint.setAntiAlias(true);

            // https://fonts.google.com/specimen/Comfortaa?selection.family=Comfortaa
            Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/Comfortaa/Comfortaa-Bold.ttf");

            mNotificationCountPaint = new TextPaint();
            mNotificationCountPaint.setColor(mBackgroundColor);
            mNotificationCountPaint.setStrokeWidth(1.5f);
            mNotificationCountPaint.setAntiAlias(true);
            mNotificationCountPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mNotificationCountPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.minimalin_notification_font_size));
            mNotificationCountPaint.setTypeface(custom_font);
            mNotificationCountPaint.setTextAlign(Paint.Align.LEFT);

            mTicksPaint = new Paint();
            //mTicksPaint.setColor(mWatchTicksColor); // TODO remove
            mTicksPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mTicksPaint.setAntiAlias(true);
            mTicksPaint.setStrokeCap(Paint.Cap.ROUND);
            mTicksPaint.setStyle(Paint.Style.STROKE);

            mMinimalinTimePaint = new TextPaint();
            //mMinimalinTimePaint.setColor(mWatchTimeColor); // TODO remove
            mMinimalinTimePaint.setTypeface(custom_font);
            mMinimalinTimePaint.setAntiAlias(true);
            mMinimalinTimePaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.minimalin_font_size));
            mMinimalinTimePaint.setTextAlign(Paint.Align.LEFT);
            mMinimalinVerticalTimeGap = getResources().getDimensionPixelOffset(R.dimen.minimalin_vertical_font_gap);

            if (mIsBackgroundDark) {
                mTicksPaint.setColor(Color.WHITE);
                mMinimalinTimePaint.setColor(Color.WHITE);
            } else {
                mTicksPaint.setColor(Color.BLACK);
                mMinimalinTimePaint.setColor(Color.BLACK);
            }

        }

        /* Sets active/ambient mode colors for all complications.
         *
         * Note: With the rest of the watch face, we update the paint colors based on
         * ambient/active mode callbacks, but because the ComplicationDrawable handles
         * the active/ambient colors, we only set the colors twice. Once at initialization and
         * again if the user changes the highlight color via ConfigActivity.
         */
        private void setComplicationsActiveAndAmbientColors(int primaryComplicationColor) {
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int COMPLICATION_ID : COMPLICATION_IDS) {
                complicationId = COMPLICATION_ID;
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                if (complicationId == BACKGROUND_COMPLICATION_ID) {
                    // It helps for the background color to be black in case the image used for the
                    // watch face's background takes some time to load.
                    complicationDrawable.setBackgroundColorActive(Color.BLACK);
                } else {
                    complicationDrawable.setIconColorActive(primaryComplicationColor);
                    complicationDrawable.setRangedValuePrimaryColorActive(primaryComplicationColor);
                    if (mIsBackgroundDark) {
                        complicationDrawable.setTextColorActive(Color.WHITE);
                        if (mComplicationBackgrounds) {
                            complicationDrawable.setBackgroundColorActive(ContextCompat.getColor(getApplicationContext(), R.color.color_complication_background_light));
                        } else {
                            complicationDrawable.setBackgroundColorActive(Color.TRANSPARENT);
                        }
                        //complicationDrawable.setTitleColorActive(Color.GRAY);
                        complicationDrawable.setTitleColorActive(mSecondaryMaterialColor.Color(200));
                    } else {
                        complicationDrawable.setTextColorActive(Color.BLACK);
                        if (mComplicationBackgrounds) {
                            complicationDrawable.setBackgroundColorActive(ContextCompat.getColor(getApplicationContext(), R.color.color_complication_background_dark));
                        } else {
                            complicationDrawable.setBackgroundColorActive(Color.TRANSPARENT);
                        }
                        //complicationDrawable.setTitleColorActive(Color.DKGRAY);
                        complicationDrawable.setTitleColorActive(mSecondaryMaterialColor.Color(800));
                    }
                }
            }
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);

            // Updates complications to properly render in ambient mode based on the
            // screen's capabilities.
            ComplicationDrawable complicationDrawable;

            for (int COMPLICATION_ID : COMPLICATION_IDS) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_ID);

                complicationDrawable.setLowBitAmbient(mLowBitAmbient);
                complicationDrawable.setBurnInProtection(mBurnInProtection);
            }
        }

        /*
         * Called when there is updated data for a complication id.
         */
        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            Log.d(TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(TAG, "OnTapCommand()");
            switch (tapType) {
                case TAP_TYPE_TAP:

                    // If your background complication is the first item in your array, you need
                    // to walk backward through the array to make sure the tap isn't for a
                    // complication above the background complication.
                    for (int i = COMPLICATION_IDS.length - 1; i >= 0; i--) {
                        int complicationId = COMPLICATION_IDS[i];
                        ComplicationDrawable complicationDrawable =
                                mComplicationDrawableSparseArray.get(complicationId);

                        boolean successfulTap = complicationDrawable.onTap(x, y);

                        if (successfulTap) {
                            return;
                        }
                    }
                    break;
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);

            mAmbient = inAmbientMode;

            updateWatchPaintStyles();

            // Update drawable complications' ambient state.
            // Note: ComplicationDrawable handles switching between active/ambient colors, we just
            // have to inform it to enter ambient mode.
            ComplicationDrawable complicationDrawable;

            for (int COMPLICATION_ID : COMPLICATION_IDS) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_ID);
                complicationDrawable.setInAmbientMode(mAmbient);
            }

            // Check and trigger whether or not timer should be running (only in active mode).
            updateTimer();
        }

        private void updateWatchPaintStyles() {
            if (mAmbient) {
                mBackgroundPaint.setColor(Color.BLACK);
                mHourPaint.setColor(Color.WHITE);
                mMinutePaint.setColor(Color.WHITE);
                mSecondPaint.setColor(Color.WHITE);
                mNotificationCirclePaint.setColor(Color.GRAY);
                mMinimalinTimePaint.setColor(Color.WHITE);
                mNotificationCountPaint.setColor(Color.GRAY);
                mTicksPaint.setColor(Color.WHITE);

                mHourPaint.setAntiAlias(!mLowBitAmbient);
                mMinutePaint.setAntiAlias(!mLowBitAmbient);
                mSecondPaint.setAntiAlias(!mLowBitAmbient);
                mNotificationCirclePaint.setAntiAlias(!mLowBitAmbient);
                mMinimalinTimePaint.setAntiAlias(!mLowBitAmbient);
                mTicksPaint.setAntiAlias(!mLowBitAmbient);
                mNotificationCountPaint.setAntiAlias(!mLowBitAmbient);

                mNotificationCirclePaint.setStyle(Paint.Style.STROKE);


                mHourPaint.clearShadowLayer();
                mMinutePaint.clearShadowLayer();
                mSecondPaint.clearShadowLayer();
            } else {
                mNotificationCountPaint.setColor(mBackgroundColor);
                mBackgroundPaint.setColor(mBackgroundColor);
                //mTicksPaint.setColor(mBackgroundColor);
                mHourPaint.setColor(mWatchHourHandHighlightColor);
                mMinutePaint.setColor(mWatchMinuteHandHighlightColor);
                mNotificationCirclePaint.setColor(mWatchComplicationsColor);
                //mMinimalinTimePaint.setColor(mWatchTimeColor);
                //mTicksPaint.setColor(mWatchTicksColor);
                mSecondPaint.setColor(mWatchSecondHandHighlightColor);

                if (mIsBackgroundDark) {
                    mTicksPaint.setColor(Color.WHITE);
                    mMinimalinTimePaint.setColor(Color.WHITE);
                } else {
                    mTicksPaint.setColor(Color.BLACK);
                    mMinimalinTimePaint.setColor(Color.BLACK);
                }

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                mSecondPaint.setAntiAlias(true);
                mNotificationCirclePaint.setAntiAlias(true);
                mMinimalinTimePaint.setAntiAlias(true);
                mTicksPaint.setAntiAlias(true);
                mNotificationCountPaint.setAntiAlias(true);

                mNotificationCirclePaint.setStyle(Paint.Style.FILL);

                mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            /*
             * Calculate lengths of different hands based on watch screen size.
             */
            mSecondHandLength = (float) (mCenterX * 0.875);
            mMinuteHandLength = (float) (mCenterX * 0.75);
            mHourHandLength = (float) (mCenterX * 0.5);
            mTickLength = (float) (mCenterX * 0.1);
            mMinimalinTextRadiusLength = (float) (mCenterX * 0.2); // TODO this should be a function of the font height

            mBackgroundCirclePaint.setStrokeWidth( (float)(mCenterX * 0.3));

            /*
             * Calculates location bounds for right and left circular complications. Please note,
             * we are not demonstrating a long text complication in this watch face.
             *
             * We suggest using at least 1/4 of the screen width for circular (or squared)
             * complications and 2/3 of the screen width for wide rectangular complications for
             * better readability.
             */

            // For most Wear devices, width and height are the same, so we just chose one (width).
            int sizeOfComplication = width / 5;
            int sizeOfLongComplicationWidth = width / 2;
            int sizeOfLongComplicationHeight = height / 6;

            int midpointOfScreen = width / 2;

            int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

            Rect leftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            horizontalOffset,
                            verticalOffset,
                            (horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable leftComplicationDrawable =
                    mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
            leftComplicationDrawable.setBounds(leftBounds);

            Rect rightBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (midpointOfScreen + horizontalOffset),
                            verticalOffset,
                            (midpointOfScreen + horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable rightComplicationDrawable =
                    mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID);
            rightComplicationDrawable.setBounds(rightBounds);

            Rect topBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            midpointOfScreen - (sizeOfLongComplicationWidth / 2),
                            midpointOfScreen - ((midpointOfScreen - sizeOfLongComplicationHeight) / 2) - sizeOfLongComplicationHeight,
                            midpointOfScreen + (sizeOfLongComplicationWidth / 2),
                            midpointOfScreen - (midpointOfScreen - sizeOfLongComplicationHeight) / 2);

            ComplicationDrawable topComplicationDrawable =
                    mComplicationDrawableSparseArray.get(TOP_COMPLICATION_ID);
            topComplicationDrawable.setBounds(topBounds);

            Rect bottomBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            midpointOfScreen - (sizeOfLongComplicationWidth / 2),
                            midpointOfScreen + ((midpointOfScreen - sizeOfLongComplicationHeight) / 2),
                            midpointOfScreen + (sizeOfLongComplicationWidth / 2),
                            midpointOfScreen + ((midpointOfScreen - sizeOfLongComplicationHeight) / 2) + sizeOfLongComplicationHeight);

            ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID);
            bottomComplicationDrawable.setBounds(bottomBounds);

            Rect topBounds2 =
                    // Left, Top, Right, Bottom
                    new Rect(
                            midpointOfScreen - (sizeOfLongComplicationWidth / 2),
                            midpointOfScreen - ((midpointOfScreen - sizeOfLongComplicationHeight) / 2) - sizeOfLongComplicationHeight,
                            midpointOfScreen + (sizeOfLongComplicationWidth / 2),
                            midpointOfScreen - (midpointOfScreen - sizeOfLongComplicationHeight) / 2);

            ComplicationDrawable notificationComplicationDrawable =
                    mComplicationDrawableSparseArray.get(NOTIFICATION_COMPLICATION_ID);
            notificationComplicationDrawable.setBounds(topBounds2);

            Rect screenForBackgroundBound =
                    // Left, Top, Right, Bottom
                    new Rect(0, 0, width, height);

            ComplicationDrawable backgroundComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BACKGROUND_COMPLICATION_ID);
            backgroundComplicationDrawable.setBounds(screenForBackgroundBound);

            // invalidate cache of mBackgroundGradientBitmap in case bounds changed
            if (mBackgroundGradient) {
                mBackgroundGradientBitmap = null;
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawComplications(canvas, now);
            drawNotificationCount(canvas);
            drawWatchFace(canvas);
        }


        /**
         * Handles drawing the notification count
         *
         * @param canvas to draw to
         */
        private void drawNotificationCount(Canvas canvas) {
            /*if (mNotificationIndicatorUnread) {
                count = getUnreadCount();
            } else if (mNotificationIndicatorAll) {
                count = getNotificationCount();
            }*/

            if (mUnreadNotificationsPreference && (mNumberOfUnreadNotifications > 0)) {
                int count = mNumberOfUnreadNotifications;
                String countStr = "+";

                if (count <= 9) {
                    countStr = String.valueOf(count);
                }
                //(x,y) coordinates for where to draw the notification indicator
                float xPos = mCenterX;
                float yPos = mCenterY + (mCenterY / 4);

                canvas.drawCircle(xPos, yPos, mCenterX * 0.05f, mNotificationCirclePaint);
                /*canvas.drawText(String.valueOf(count), xPos,
                        yPos - (mNotificationCountPaint.descent()
                                + mNotificationCountPaint.ascent()) / 2, mNotificationCountPaint);*/
                drawVertAlignedText(canvas, xPos, yPos, countStr, mNotificationCountPaint, TextVerticalAlign.Middle);
            }
        }

        private void drawBackground(Canvas canvas) {
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else {
                if (mBackgroundGradient) {
                    // if cache for mBackgroundGradientBitmap is null, rebuild
                    if (mBackgroundGradientBitmap == null) {
                        mBackgroundGradientBitmap = generateBackgroundGradient(mBackgroundMaterialColor);
                    }
                    canvas.drawBitmap(mBackgroundGradientBitmap, 0,0, mBackgroundPaint);
                } else {
                    canvas.drawColor(mBackgroundColor);
                }
                //  TODO circle outline testing
                canvas.drawCircle(mCenterX, mCenterY, mSecondHandLength, mBackgroundCirclePaint);
            }
        }

        private Bitmap generateBackgroundGradient(MaterialColors.Color color) {
            LinearGradient gradient = new LinearGradient(0f, mCenterX * 2f, mCenterY * 2f, 0f,
                    color.Color(400),
                    color.Color(800),
                    Shader.TileMode.CLAMP);
            mBackgroundPaint.setShader(gradient);
            mBackgroundPaint.setDither(true);
            Bitmap bitmap = Bitmap.createBitmap((int)mCenterX * 2, (int)mCenterY * 2, Bitmap.Config.ARGB_8888);
            Canvas c1 = new Canvas(bitmap);
            c1.drawRect(0, 0, (int)mCenterX * 2, (int)mCenterY * 2, mBackgroundPaint);
            return bitmap;
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            ComplicationDrawable complicationDrawable;

            int skipComplication = NOTIFICATION_COMPLICATION_ID;
            if (getNotificationCount() > 0) {
                skipComplication = TOP_COMPLICATION_ID;
            }

            for (int complicationId : COMPLICATION_IDS) {
                if (complicationId == skipComplication) {
                    continue;
                }
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }


        private boolean minimalinTimesConflicting(int hour, int minute) {
            // Need to add 2 to minutes to shift center of combined time to overlap point
            // need to % 60 on minutes to cover the :58, and :59 minutes back to the correct numbers when adding 2
            return hour % 12 == ((minute + 2) % 60) / 5;
        }

        private boolean minimalinTimesConflictingNorthOrSouth(int hour, int minute) {
            if (!minimalinTimesConflicting(hour, minute)) {
                return false;
            }
            final int hourMod = hour % 12;
            return hourMod <= 1 || hourMod >= 11 || (hourMod >= 5 && hourMod <= 7);
        }

        private void drawWatchFace(Canvas canvas) {
            drawMinimalinTime(canvas);
            drawWatchHands(canvas);
        }

        private void drawMinimalinTime(Canvas canvas) {
            float innerTickRadius = mCenterX - mTickLength;
            float outerTickRadius = mCenterX;
            float minimalinTextCenterRadius = mCenterX - mMinimalinTextRadiusLength;

            int tickIndexHour = mCalendar.get(Calendar.HOUR);
            int tickIndexMinute = mCalendar.get(Calendar.MINUTE);
            int printedHour = mMilitaryTimePreference ? mCalendar.get(Calendar.HOUR_OF_DAY) : tickIndexHour == 0 ? 12 : tickIndexHour;

            // Hour Tick for Minimalin
            float hourTickRot = (float) (tickIndexHour * Math.PI * 2 / 12);
            float hourInnerX = (float) Math.sin(hourTickRot) * innerTickRadius;
            float hourInnerY = (float) -Math.cos(hourTickRot) * innerTickRadius;
            float hourOuterX = (float) Math.sin(hourTickRot) * outerTickRadius;
            float hourOuterY = (float) -Math.cos(hourTickRot) * outerTickRadius;

            canvas.drawLine(
                    mCenterX + hourInnerX,
                    mCenterY + hourInnerY,
                    mCenterX + hourOuterX,
                    mCenterY + hourOuterY,
                    mTicksPaint);

            if (minimalinTimesConflicting(tickIndexHour, tickIndexMinute)) {
                if (minimalinTimesConflictingNorthOrSouth(tickIndexHour, tickIndexMinute)) {
                    // minimalin horizontal time
                    float hourTextInnerX = (float) Math.sin(hourTickRot) * minimalinTextCenterRadius;
                    float hourTextInnerY = (float) -Math.cos(hourTickRot) * minimalinTextCenterRadius;
                    drawVertAlignedText(canvas, mCenterX + hourTextInnerX, mCenterY + hourTextInnerY, String.format("%2d:%02d", printedHour, tickIndexMinute), mMinimalinTimePaint, TextVerticalAlign.Middle);
                } else {
                    // minimalin vertical time
                    float hourTextInnerX = (float) Math.sin(hourTickRot) * minimalinTextCenterRadius;
                    float hourTextInnerY = (float) -Math.cos(hourTickRot) * minimalinTextCenterRadius;
                    drawVertAlignedText(canvas, mCenterX + hourTextInnerX, mCenterY + hourTextInnerY - mMinimalinVerticalTimeGap / 2, String.format("%2d", printedHour), mMinimalinTimePaint, TextVerticalAlign.Baseline);
                    drawVertAlignedText(canvas, mCenterX + hourTextInnerX, mCenterY + hourTextInnerY + mMinimalinVerticalTimeGap / 2, String.format("%02d", tickIndexMinute), mMinimalinTimePaint, TextVerticalAlign.Top);
                }
            } else {
                // Minimalin Hour text
                float hourTextInnerX = (float) Math.sin(hourTickRot) * minimalinTextCenterRadius;
                float hourTextInnerY = (float) -Math.cos(hourTickRot) * minimalinTextCenterRadius;
                drawVertAlignedText(canvas, mCenterX + hourTextInnerX, mCenterY + hourTextInnerY, String.format("%2d", printedHour), mMinimalinTimePaint, TextVerticalAlign.Middle);

                // Minute Tick for Minimalin
                float minuteTickRot = (float) (tickIndexMinute * Math.PI * 2 / 60);
                float minuteInnerX = (float) Math.sin(minuteTickRot) * innerTickRadius;
                float minuteInnerY = (float) -Math.cos(minuteTickRot) * innerTickRadius;
                float minuteOuterX = (float) Math.sin(minuteTickRot) * outerTickRadius;
                float minuteOuterY = (float) -Math.cos(minuteTickRot) * outerTickRadius;

                canvas.drawLine(
                        mCenterX + minuteInnerX,
                        mCenterY + minuteInnerY,
                        mCenterX + minuteOuterX,
                        mCenterY + minuteOuterY,
                        mTicksPaint);

                // Minimalin Minute text
                float minuteTextInnerX = (float) Math.sin(minuteTickRot) * minimalinTextCenterRadius;
                float minuteTextInnerY = (float) -Math.cos(minuteTickRot) * minimalinTextCenterRadius;
                drawVertAlignedText(canvas, mCenterX + minuteTextInnerX, mCenterY + minuteTextInnerY, String.format("%02d", tickIndexMinute), mMinimalinTimePaint, TextVerticalAlign.Middle);
            }
        }

        private void drawWatchHands(Canvas canvas) {
            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minuteHandOffset = mCalendar.get(Calendar.SECOND) / 10f;
            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f + minuteHandOffset;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mMinuteHandLength,
                    mMinutePaint);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondPaint);
            }
            canvas.drawCircle(
                    mCenterX, mCenterY, CENTER_GAP_AND_CIRCLE_RADIUS, mHourPaint);

            /* Restore the canvas' original orientation. */
            canvas.restore();

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {

                // Preferences might have changed since last time watch face was visible.
                loadSavedPreferences();

                // With the rest of the watch face, we update the paint colors based on
                // ambient/active mode callbacks, but because the ComplicationDrawable handles
                // the active/ambient colors, we only need to update the complications' colors when
                // the user actually makes a change to the highlight color, not when the watch goes
                // in and out of ambient mode.
                setComplicationsActiveAndAmbientColors(mWatchComplicationsColor);
                updateWatchPaintStyles();

                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        @Override
        public void onUnreadCountChanged(int count) {
            Log.d(TAG, "onUnreadCountChanged(): " + count);
            super.onUnreadCountChanged(count);

            if (mUnreadNotificationsPreference) {

                if (mNumberOfUnreadNotifications != count) {
                    mNumberOfUnreadNotifications = count;
                    invalidate();
                }
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MinimalinWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MinimalinWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }
    }
}
