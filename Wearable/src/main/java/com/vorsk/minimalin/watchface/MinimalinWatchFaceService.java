package com.vorsk.minimalin.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;

import com.vorsk.minimalin.R;
import com.vorsk.minimalin.config.ConfigRecyclerViewAdapter;

import org.w3c.dom.Text;

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

    // Background, Left and right complication IDs as array for Complication API.
    private static final int[] COMPLICATION_IDS = {
            BACKGROUND_COMPLICATION_ID, LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID, TOP_COMPLICATION_ID, BOTTOM_COMPLICATION_ID
    };

    // Left and right dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {ComplicationData.TYPE_LARGE_IMAGE},
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            }
    };

    // Used by {@link ConfigRecyclerViewAdapter} to check if complication location
    // is supported in settings config activity.
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
            default:
                return -1;
        }
    }

    // Used by {@link ConfigRecyclerViewAdapter} to retrieve all complication ids.
    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    // Used by {@link ConfigRecyclerViewAdapter} to see which complication types
    // are supported in the settings config activity.
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
            default:
                return new int[]{};
        }
    }

    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final int MSG_UPDATE_TIME = 0;

        private static final float HOUR_STROKE_WIDTH = 5f;
        private static final float MINUTE_STROKE_WIDTH = 3f;
        private static final float SECOND_TICK_STROKE_WIDTH = 2f;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;

        private static final int SHADOW_RADIUS = 6;

        private Calendar mCalendar;
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;

        private float mCenterX;
        private float mCenterY;

        private float mSecondHandLength;
        private float mMinuteHandLength;
        private float mHourHandLength;
        private float mTickLength;
        private float mMinimalinTextRadiusLength;

        // Colors for all hands (hour, minute, seconds, ticks) based on photo loaded.
        private int mWatchComplicationsColor;
        private int mWatchSecondHandHighlightColor;
        private int mWatchMinuteHandHighlightColor;
        private int mWatchHourHandHighlightColor;
        private int mWatchHandShadowColor;

        private int mBackgroundColor;

        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondAndHighlightPaint;
        private Paint mTickAndCirclePaint;
        private TextPaint mMinimalinTimePaint;

        private Paint mBackgroundPaint;

        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         */
        private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        // Used to pull user's preferences for background color, highlight color, and visual
        // indicating there are unread notifications.
        SharedPreferences mSharedPref;

        // User's preference for if they want visual shown to indicate unread notifications.
        private boolean mUnreadNotificationsPreference;
        private int mNumberOfUnreadNotifications = 0;

        private final BroadcastReceiver mTimeZoneReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        mCalendar.setTimeZone(TimeZone.getDefault());
                        invalidate();
                    }
                };

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
                            .setHideNotificationIndicator(true)
                            .build());

            loadSavedPreferences();
            initializeComplicationsAndBackground();
            initializeWatchFace();
        }

        // Pulls all user's preferences for watch face appearance.
        private void loadSavedPreferences() {

            String backgroundColorResourceName =
                    getApplicationContext().getString(R.string.saved_background_color);

            mBackgroundColor = mSharedPref.getInt(backgroundColorResourceName, Color.BLACK);

            String markerSecondColorResourceName =
                    getApplicationContext().getString(R.string.saved_marker_color_second);
            String markerMinuteColorResourceName =
                    getApplicationContext().getString(R.string.saved_marker_color_minute);
            String markerHourColorResourceName =
                    getApplicationContext().getString(R.string.saved_marker_color_hour);
            String markerComplicationsColorResourceName =
                    getApplicationContext().getString(R.string.saved_complications_color);

            // Set defaults for colors
            mWatchComplicationsColor = mSharedPref.getInt(markerComplicationsColorResourceName, Color.WHITE);
            mWatchSecondHandHighlightColor = mSharedPref.getInt(markerSecondColorResourceName, Color.RED);
            mWatchMinuteHandHighlightColor = mSharedPref.getInt(markerMinuteColorResourceName, Color.WHITE);
            mWatchHourHandHighlightColor = mSharedPref.getInt(markerHourColorResourceName, Color.WHITE);


            if (mBackgroundColor == Color.WHITE) {
                //mWatchComplicationsColor = Color.BLACK;
                mWatchHandShadowColor = Color.WHITE;
            } else {
                //mWatchComplicationsColor = Color.WHITE;
                mWatchHandShadowColor = Color.BLACK;
            }

            String unreadNotificationPreferenceResourceName =
                    getApplicationContext().getString(R.string.saved_unread_notifications_pref);

            mUnreadNotificationsPreference =
                    mSharedPref.getBoolean(unreadNotificationPreferenceResourceName, true);
        }

        private void initializeComplicationsAndBackground() {
            Log.d(TAG, "initializeComplications()");

            // Initialize background color (in case background complication is inactive).
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mBackgroundColor);

            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            // Creates a ComplicationDrawable for each location where the user can render a
            // complication on the watch face. In this watch face, we create one for left, right,
            // and background, but you could add many more.
            ComplicationDrawable leftComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            ComplicationDrawable rightComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());
            ComplicationDrawable topComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());
            ComplicationDrawable bottomComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            ComplicationDrawable backgroundComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            // Adds new complications to a SparseArray to simplify setting styles and ambient
            // properties for all complications, i.e., iterate over them all.
            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);
            mComplicationDrawableSparseArray.put(TOP_COMPLICATION_ID, topComplicationDrawable);
            mComplicationDrawableSparseArray.put(BOTTOM_COMPLICATION_ID, bottomComplicationDrawable);
            mComplicationDrawableSparseArray.put(
                    BACKGROUND_COMPLICATION_ID, backgroundComplicationDrawable);

            setComplicationsActiveAndAmbientColors(mWatchComplicationsColor);
            setActiveComplications(COMPLICATION_IDS);
        }

        private void initializeWatchFace() {

            mHourPaint = new Paint();
            //mHourPaint.setColor(mWatchHandAndComplicationsColor);
            mHourPaint.setColor(mWatchHourHandHighlightColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mMinutePaint = new Paint();
            //mMinutePaint.setColor(mWatchHandAndComplicationsColor);
            mMinutePaint.setColor(mWatchMinuteHandHighlightColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mSecondAndHighlightPaint = new Paint();
            mSecondAndHighlightPaint.setColor(mWatchMinuteHandHighlightColor);
            mSecondAndHighlightPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mSecondAndHighlightPaint.setAntiAlias(true);
            mSecondAndHighlightPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecondAndHighlightPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mTickAndCirclePaint = new Paint();
            mTickAndCirclePaint.setColor(mWatchComplicationsColor);
            mTickAndCirclePaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mTickAndCirclePaint.setAntiAlias(true);
            mTickAndCirclePaint.setStyle(Paint.Style.STROKE);
            mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            //mMinimalinTimePaint = new TextPaint(mTickAndCirclePaint);
            mMinimalinTimePaint = new TextPaint();
            // TODO add more stlying options here like shadow and stroke/color
            //Typeface custom_font = Typeface.createFromAsset(getAssets(),  "fonts/AtomicAge-Regular.ttf");
            //Typeface custom_font = Typeface.createFromAsset(getAssets(),  "fonts/Baumans-Regular.ttf");
            Typeface custom_font = Typeface.createFromAsset(getAssets(),  "fonts/Comfortaa-Regular.ttf"); // I like this one
            //--Typeface custom_font = Typeface.createFromAsset(getAssets(),  "fonts/Voces-Regular.ttf");
            //--Typeface custom_font = Typeface.createFromAsset(getAssets(),  "fonts/NovaSquare.ttf");
            //--Typeface custom_font = Typeface.createFromAsset(getAssets(),  "fonts/nupe.ttf");
            mMinimalinTimePaint.setTypeface(custom_font);
            mMinimalinTimePaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.minimalin_font_size));
//            mMinimalinTimePaint.setLetterSpacing(getResources().getDimensionPixelSize(R.dimen.minimalin_font_spacing));
            // TODO stylize this
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

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                if (complicationId == BACKGROUND_COMPLICATION_ID) {
                    // It helps for the background color to be black in case the image used for the
                    // watch face's background takes some time to load.
                    complicationDrawable.setBackgroundColorActive(Color.BLACK);
                } else {
                    // Active mode colors.
                    complicationDrawable.setBorderColorActive(primaryComplicationColor);
                    complicationDrawable.setRangedValuePrimaryColorActive(primaryComplicationColor);

                    // Ambient mode colors.
                    complicationDrawable.setBorderColorAmbient(Color.WHITE);
                    complicationDrawable.setRangedValuePrimaryColorAmbient(Color.WHITE);
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

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);

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

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);
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
                mSecondAndHighlightPaint.setColor(Color.WHITE);
                mTickAndCirclePaint.setColor(Color.WHITE);
                mMinimalinTimePaint.setColor(Color.WHITE);

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                mSecondAndHighlightPaint.setAntiAlias(false);
                mTickAndCirclePaint.setAntiAlias(false);
                mMinimalinTimePaint.setAntiAlias(false);

                mHourPaint.clearShadowLayer();
                mMinutePaint.clearShadowLayer();
                mSecondAndHighlightPaint.clearShadowLayer();
                mTickAndCirclePaint.clearShadowLayer();
                mMinimalinTimePaint.clearShadowLayer();

            } else {

                mBackgroundPaint.setColor(mBackgroundColor);

                mHourPaint.setColor(mWatchHourHandHighlightColor);
                mMinutePaint.setColor(mWatchMinuteHandHighlightColor);
                mTickAndCirclePaint.setColor(mWatchComplicationsColor);
                mMinimalinTimePaint.setColor(mWatchComplicationsColor); // TODO change font color?

                mSecondAndHighlightPaint.setColor(mWatchSecondHandHighlightColor);

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                mSecondAndHighlightPaint.setAntiAlias(true);
                mTickAndCirclePaint.setAntiAlias(true);
                mMinimalinTimePaint.setAntiAlias(true);

                mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mSecondAndHighlightPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinimalinTimePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinimalinTimePaint.setTextAlign(Paint.Align.LEFT); // don't use this when using StaticLayout
                mMinimalinTimePaint.bgColor = Color.GREEN; // TODO testing, this does nothg?
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
                mSecondAndHighlightPaint.setAlpha(inMuteMode ? 80 : 255);
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
            mMinimalinTextRadiusLength = (float) (mCenterX * 0.2);

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
            int sizeOfLongComplication = width/2; //(width * 2)/3;

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
                            midpointOfScreen - (sizeOfComplication / 2),
                            midpointOfScreen - ((midpointOfScreen - sizeOfComplication)/2) - sizeOfComplication,
                            midpointOfScreen + (sizeOfComplication / 2),
                            midpointOfScreen - (midpointOfScreen - sizeOfComplication)/2);

            ComplicationDrawable topComplicationDrawable =
                    mComplicationDrawableSparseArray.get(TOP_COMPLICATION_ID);
            topComplicationDrawable.setBounds(topBounds);

            Rect bottomBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            midpointOfScreen - (sizeOfLongComplication / 2),
                            midpointOfScreen + ((midpointOfScreen - sizeOfComplication)/2),
                            midpointOfScreen + (sizeOfLongComplication / 2),
                            midpointOfScreen + ((midpointOfScreen - sizeOfComplication)/2) + sizeOfComplication);

            ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID);
            bottomComplicationDrawable.setBounds(bottomBounds);

            Rect screenForBackgroundBound =
                    // Left, Top, Right, Bottom
                    new Rect(0, 0, width, height);

            ComplicationDrawable backgroundComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BACKGROUND_COMPLICATION_ID);
            backgroundComplicationDrawable.setBounds(screenForBackgroundBound);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawComplications(canvas, now);
            drawUnreadNotificationIcon(canvas);
            drawWatchFace(canvas);
        }

        private void drawUnreadNotificationIcon(Canvas canvas) {

            if (mUnreadNotificationsPreference && (mNumberOfUnreadNotifications > 0)) {

                int width = canvas.getWidth();
                int height = canvas.getHeight();

                canvas.drawCircle(width / 2, height - 40, 10, mTickAndCirclePaint);

                /*
                 * Ensure center highlight circle is only drawn in interactive mode. This ensures
                 * we don't burn the screen with a solid circle in ambient mode.
                 */
                if (!mAmbient) {
                    canvas.drawCircle(width / 2, height - 40, 4, mSecondAndHighlightPaint);
                }
            }
        }

        private void drawBackground(Canvas canvas) {

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);

            } else {
                canvas.drawColor(mBackgroundColor);
            }
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }




        private void drawTextCentered(Canvas canvas, String text, float x, float y, TextPaint textPaint) {
            Paint testPaint = new Paint(mSecondAndHighlightPaint);
            testPaint.setColor(Color.GREEN);

            canvas.drawCircle(
                    x, y, CENTER_GAP_AND_CIRCLE_RADIUS, testPaint);


            Rect r2 = new Rect(); // make static
            Paint paint2 = new Paint(textPaint);
            paint2.setColor(Color.YELLOW);
            paint2.setTextAlign(Paint.Align.LEFT);
            paint2.getTextBounds(text, 0, text.length(), r2);
            float x2 = x - r2.width() / 2f - r2.left;
            float y2 = y + r2.height() / 2f - r2.bottom;
            canvas.drawText(text, x2, y2, paint2);
        }


        private boolean minimalinTimesConflicting(int hour, int minute) {
            return hour % 12 == minute / 5;
            //return false;
        }

        private boolean minimalinTimesConflictingNorthOrSouth(int hour, int minute) {
            if (!minimalinTimesConflicting(hour, minute)) {
                return false;
            }
            final int hourMod = hour % 12;
            return hourMod <= 1 || hourMod >= 11 || (hourMod >= 5 && hourMod <= 7);
            //return false;
        }

        private void drawWatchFace(Canvas canvas) {
            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             */
            float innerTickRadius = mCenterX - mTickLength;
            float outerTickRadius = mCenterX;
            float minimalinTextCenterRadius = mCenterX - mMinimalinTextRadiusLength;

            int tickIndexHour = mCalendar.get(Calendar.SECOND) % 12; //9;//mCalendar.get(Calendar.HOUR);
            int tickIndexMinute = 45;//mCalendar.get(Calendar.MINUTE);

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
                    mTickAndCirclePaint);

            if (minimalinTimesConflicting(tickIndexHour,tickIndexMinute)) {
                if (minimalinTimesConflictingNorthOrSouth(tickIndexHour, tickIndexMinute)) {
                    // minimalin horizontal time
                    float hourTextInnerX = (float) Math.sin(hourTickRot) * minimalinTextCenterRadius;
                    float hourTextInnerY = (float) -Math.cos(hourTickRot) * minimalinTextCenterRadius;
                    drawTextCentered(canvas, String.format("%2d:%02d", tickIndexHour, tickIndexMinute), mCenterX + hourTextInnerX, mCenterY + hourTextInnerY, mMinimalinTimePaint);
                    drawVertAlignedText(canvas, mCenterX + hourTextInnerX, mCenterY + hourTextInnerY, String.format("%2d:%02d", tickIndexHour, tickIndexMinute), mMinimalinTimePaint, TextVertAlign.Middle);
                } else {
                    // minimalin vertical time
                    float hourTextInnerX = (float) Math.sin(hourTickRot) * minimalinTextCenterRadius;
                    float hourTextInnerY = (float) -Math.cos(hourTickRot) * minimalinTextCenterRadius;
                    drawVertAlignedText(canvas, mCenterX + hourTextInnerX, mCenterY + hourTextInnerY, String.format("%2d", tickIndexHour), mMinimalinTimePaint, TextVertAlign.Baseline);
                    drawVertAlignedText(canvas, mCenterX + hourTextInnerX, mCenterY + hourTextInnerY, String.format("%02d", tickIndexMinute), mMinimalinTimePaint, TextVertAlign.Top);
                    drawTextCentered(canvas, String.format("%2d\n%02d", tickIndexHour, tickIndexMinute), mCenterX + hourTextInnerX, mCenterY + hourTextInnerY, mMinimalinTimePaint);
                }
            } else {
                // Minimalin Hour text
                float hourTextInnerX = (float) Math.sin(hourTickRot) * minimalinTextCenterRadius;
                float hourTextInnerY = (float) -Math.cos(hourTickRot) * minimalinTextCenterRadius;
                drawTextCentered(canvas, String.format("%2d", tickIndexHour), mCenterX + hourTextInnerX, mCenterY + hourTextInnerY, mMinimalinTimePaint);
                drawVertAlignedText(canvas, mCenterX + hourTextInnerX, mCenterY + hourTextInnerY, String.format("%2d", tickIndexHour), mMinimalinTimePaint, TextVertAlign.Middle);
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
                        mTickAndCirclePaint);

                // Minimalin Minute text
                float minuteTextInnerX = (float) Math.sin(minuteTickRot) * minimalinTextCenterRadius;
                float minuteTextInnerY = (float) -Math.cos(minuteTickRot) * minimalinTextCenterRadius;
                drawTextCentered(canvas, String.format("%02d", tickIndexMinute), mCenterX + minuteTextInnerX, mCenterY + minuteTextInnerY, mMinimalinTimePaint);
                drawVertAlignedText(canvas, mCenterX + minuteTextInnerX, mCenterY + minuteTextInnerY, String.format("%02d", tickIndexMinute), mMinimalinTimePaint, TextVertAlign.Middle);

            }

            /* calculate the angle between the hour and minutes hand
             * if under threshold, display time at hour hand
             */


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
                        mSecondAndHighlightPaint);
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

    public enum TextVertAlign {
        Top,
        Middle,
        Baseline,
        Bottom
    }

    // see https://www.slideshare.net/rtc1/intro-todrawingtextandroid
    private void drawVertAlignedText(Canvas canvas, float x, float y, String s, Paint p, TextVertAlign vertAlign ) {
        Rect r = new Rect();
        p.getTextBounds(s, 0, s.length(), r); //Note r.top will be negative
        float textX = x - r.width() / 2f - r.left;
        float textY = y;
        switch ( vertAlign) {
            case Top:
                textY = y - r.top;
                break;
            case Middle:
                textY = y - r.top - r.height()/2;
                break;
            case Baseline:
                break;
            case Bottom:
                textY = y - (r.height() + r.top);
                break;
        }
        canvas.drawText(s, textX, textY, p);
    }
}
