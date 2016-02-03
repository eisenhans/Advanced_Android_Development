/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face for the Sunshine app.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final String LOG_TAG = SunshineWatchFace.class.getSimpleName();
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private LocalBroadcastManager broadcastManager;
    private int resourceId = R.drawable.art_clear;
    private String maxTemp = "";
    private String minTemp = "";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredReceivers = false;
        Paint mBackgroundPaint;
        Paint mTimeTextPaint;
        Paint mDateTextPaint;
        Paint mMaxTempTextPaint;
        Paint mMinTempTextPaint;
        boolean mAmbient;

        Calendar mCalendar;
        Date mDate;
        SimpleDateFormat mDayOfWeekFormat;
        java.text.DateFormat mDateFormat;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                initFormats();
                invalidate();
            }
        };

        final BroadcastReceiver mWeatherReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(LOG_TAG, "received weather intent: " + intent);
                String data = intent.getStringExtra(SunshineListenerService.WEATHER_CHANGED_KEY);
                Log.i(LOG_TAG, "weather data: " + data);
                if (data == null || data.length() == 0) {
                    return;
                }
                String[] values = data.split(",");
                if (values.length < 3) {
                    return;
                }
                int weatherId = Integer.valueOf(values[0]);
                resourceId = WatchUtility.getArtResourceForWeatherCondition(weatherId);
                maxTemp = values[1];
                minTemp = values[2];
                Log.i(LOG_TAG, "updated weather: maxTemp=" + maxTemp + ", minTemp=" + minTemp);
                invalidate();
            }
        };

        int mTapCount;

        float mXOffset;
        float mYOffset;
        float mLineHeight;

        float mXDividerStart;
        float mXDividerEnd;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Log.i(LOG_TAG, "creating watch face");

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = SunshineWatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTimeTextPaint = new Paint();
            mTimeTextPaint = createTextPaint(resources.getColor(R.color.primary_text));

            mDateTextPaint = new Paint();
            mDateTextPaint = createTextPaint(resources.getColor(R.color.secondary_text));

            mMaxTempTextPaint = new Paint();
            mMaxTempTextPaint = createTextPaint(resources.getColor(R.color.primary_text));

            mMinTempTextPaint = new Paint();
            mMinTempTextPaint = createTextPaint(resources.getColor(R.color.secondary_text));

            mCalendar = Calendar.getInstance();
            mDate = new Date();
            initFormats();

            broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceivers();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                initFormats();
            } else {
                unregisterReceivers();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void initFormats() {
            mDayOfWeekFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            mDayOfWeekFormat.setCalendar(mCalendar);
            mDateFormat = DateFormat.getDateFormat(SunshineWatchFace.this);
            mDateFormat.setCalendar(mCalendar);
        }

        private void registerReceivers() {
            if (mRegisteredReceivers) {
                return;
            }
            mRegisteredReceivers = true;
            IntentFilter timeZoneFilter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, timeZoneFilter);

            IntentFilter weatherFilter = new IntentFilter(SunshineListenerService.WEATHER_CHANGED_INTENT);
            broadcastManager.registerReceiver(mWeatherReceiver, weatherFilter);
            Log.i(LOG_TAG, "registered weather broadcast receiver");
        }

        private void unregisterReceivers() {
            if (!mRegisteredReceivers) {
                return;
            }
            mRegisteredReceivers = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
            broadcastManager.unregisterReceiver(mWeatherReceiver);
            Log.i(LOG_TAG, "unregistered receivers");
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();

            float timeTextSize = resources.getDimension(isRound
                    ? R.dimen.time_text_size_round : R.dimen.time_text_size);
            mTimeTextPaint.setTextSize(timeTextSize);

            float dateTextSize = resources.getDimension(isRound
                    ? R.dimen.date_text_size_round : R.dimen.date_text_size);
            mDateTextPaint.setTextSize(dateTextSize);

            float tempTextSize = resources.getDimension(isRound
                    ? R.dimen.temp_text_size_round : R.dimen.temp_text_size);

            mMaxTempTextPaint.setTextSize(tempTextSize);
            mMinTempTextPaint.setTextSize(tempTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTimeTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = SunshineWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            drawDateAndTime(canvas, bounds);
            drawDivider(canvas, bounds);
            drawWeather(canvas, bounds);
        }

        private void drawDateAndTime(Canvas canvas, Rect bounds) {
            WatchData watchData = new WatchData();
            int width = bounds.width();
            int height = bounds.height();

            float xTime = width * 0.2f;
            float yTime = height * 0.3f;
            canvas.drawText(watchData.time, xTime, yTime, mTimeTextPaint);

            float xDate = width * 0.1f;
            float yDate = height * 0.45f;
            canvas.drawText(watchData.date, xDate, yDate, mDateTextPaint);
        }

        private void drawDivider(Canvas canvas, Rect bounds) {
            float y = bounds.height() * 0.5f;
            float width = bounds.width() * 0.25f;
            float startX = bounds.width() * 0.375f;
            canvas.drawLine(startX, y, startX + width, y, mDateTextPaint);
        }

        private void drawWeather(Canvas canvas, Rect bounds) {
            float xIcon = bounds.width() * 0.05f;
            float yIcon = bounds.height() * 0.6f;

            if (!isInAmbientMode()) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resourceId);
                canvas.drawBitmap(bitmap, xIcon, yIcon, mBackgroundPaint);
            }

            float y = bounds.height() * 0.75f;
            float xMaxTemp = bounds.width() * 0.4f;
            canvas.drawText(maxTemp, xMaxTemp, y, mMaxTempTextPaint);

            float xMinTemp = bounds.width() * 0.7f;
            canvas.drawText(minTemp, xMinTemp, y, mMinTempTextPaint);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    private static void logInfo(String message) {
        Log.i(LOG_TAG, message);
    }
}
