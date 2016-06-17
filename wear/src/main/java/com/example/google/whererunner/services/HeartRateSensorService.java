package com.example.google.whererunner.services;

import android.app.Service;
import android.content.Intent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class HeartRateSensorService extends Service {

    private static final String LOG_TAG = HeartRateSensorService.class.getSimpleName();

    public static final String ACTION_HEART_RATE_SENSOR_TIMEOUT = "HEART_RATE_SENSOR_TIMEOUT";

    public static final String ACTION_HEART_RATE_CHANGED = "HEART_RATE_CHANGED";
    public static final String EXTRA_HEART_RATE = "HEART_RATE";

    private CountDownTimer mSensorSampleTimer;

    private static final int HRM_UPDATE_INTERVAL_TIMEOUT_MS = 5000;
    private static final int HRM_ACCURACY_LOW = 0;

    private SensorManager mSensorManager;
    private SensorEventListener mSensorListener;

    private float mHeartRateMin = Float.MAX_VALUE;
    private float mHeartRateMax = Float.MIN_VALUE;

    @Override
    public void onCreate () {
        super.onCreate();

        // We just need to wire up the sensor manager one time; putting this in onStartCommand
        // would mean that we could end up with multiple listeners being created?
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);

        // Ensure that the device has a HRM
        // This will also return null if BODY permissions have not been granted
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
            mSensorListener = new HRMSensorEventListener();

            mSensorManager.registerListener(mSensorListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {
            Log.v(LOG_TAG, "No heart rate sensor (or permission to access) detected");
        }
    }

    @Override
    public void onDestroy () {
        mSensorManager.unregisterListener(mSensorListener);

        if (mSensorSampleTimer != null) {
            mSensorSampleTimer.cancel();
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    /**
     * Sensor event listener for heart rate; fires an ACTION_HEART_RATE_CHANGED intent whenever
     * a new HR reading is received.
     */
    private class HRMSensorEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Don't broadcast low quality readings
            if (event.accuracy == HRM_ACCURACY_LOW) {
                return;
            }

            if (mSensorSampleTimer != null) {
                mSensorSampleTimer.cancel();
            }

            // Start a timer to detect if we're not receiving location samples in regular intervals
            mSensorSampleTimer = new CountDownTimer(HRM_UPDATE_INTERVAL_TIMEOUT_MS, HRM_UPDATE_INTERVAL_TIMEOUT_MS) {
                public void onTick(long millisUntilFinished) {}

                public void onFinish() {
                    Intent intent = new Intent(ACTION_HEART_RATE_SENSOR_TIMEOUT);
                    LocalBroadcastManager.getInstance(HeartRateSensorService.this).sendBroadcast(intent);
                }
            }.start();

            // Get the last sample; however, there appears to only ever be one value...
            float val = event.values[event.values.length - 1];

            mHeartRateMin = (val < mHeartRateMin) ? val : mHeartRateMin;
            mHeartRateMax = (val > mHeartRateMax) ? val : mHeartRateMax;

            HeartRateSensorEvent evt = new HeartRateSensorEvent(val,
                    mHeartRateMin, mHeartRateMax, event.timestamp);

            Intent intent = new Intent(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
            intent.putExtra(HeartRateSensorService.EXTRA_HEART_RATE, evt);

            LocalBroadcastManager.getInstance(HeartRateSensorService.this).sendBroadcast(intent);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    }

}
