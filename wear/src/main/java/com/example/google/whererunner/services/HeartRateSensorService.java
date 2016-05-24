package com.example.google.whererunner.services;

import android.app.Service;
import android.content.Intent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Arrays;

public class HeartRateSensorService extends Service {

    private static final String TAG = HeartRateSensorService.class.getSimpleName();

    public static final String ACTION_HEART_RATE_CHANGED = "ACTION_HEART_RATE_CHANGED";
    public static final String HEART_RATE = "HEART_RATE";

    private SensorManager mSensorManager;
    private SensorEventListener sensorListener;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand Heart Rate Monitor Service");
        return START_NOT_STICKY; // Should this be STICKY?
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onCreate () {
        super.onCreate();
        Log.v(TAG, "OnCreate Heart Rate Monitor Service");
        // We just need to wire up the sensor manager one time; putting this in onStartCommand
        // would mean that we could end up with multiple listeners being created?
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);

        // Check that the device has a HRM
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
            Log.v(TAG, "Heart rate monitor found");
            sensorListener = new HRMSensorEventListener();
            mSensorManager.registerListener(sensorListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {
            Log.v(TAG, "No heart rate sensor on this device");
        }
    }

    /**
     * Clean up the listeners when the service is destroyed
     */
    @Override
    public void onDestroy () {
        Log.v(TAG, "OnDestroy Heart Rate Monitor Service");
        mSensorManager.unregisterListener(sensorListener);
        super.onDestroy();
    }

    /**
     * Sensor event listener for heart rate; fires an ACTION_HEART_RATE_CHANGED intent
     * whenever a new HR reading is received
     * Encapsulated in an inner class to separate concerns from the outer class
     */
    private class HRMSensorEventListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            Log.v(TAG, "Heart Rate Values: " + Arrays.toString(event.values));
            Intent intent = new Intent();
            intent.setAction(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
            intent.putExtra(HeartRateSensorService.HEART_RATE, event.values);
            LocalBroadcastManager.getInstance(HeartRateSensorService.this).sendBroadcast(intent);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.v(TAG, "Heart Rate Accuracy changed: " + accuracy);
        }

    }

}
