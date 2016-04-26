package com.example.google.whererunner.services;

import android.app.IntentService;
import android.content.Intent;
import android.hardware.*;
import android.util.Log;

import java.util.Arrays;

public class HeartRateSensorService extends IntentService implements SensorEventListener {

    private static final String TAG = HeartRateSensorService.class.getSimpleName();

    public static final String ACTION_HEART_RATE_CHANGED = "ACTION_HEART_RATE_CHANGED";
    public static final String ACTION_HEART_RATE_START = "ACTION_HEART_RATE_START";
    public static final String ACTION_HEART_RATE_STOP = "ACTION_HEART_RATE_STOP";

    public static final String HEART_RATE = "HEART_RATE";

    private final SensorManager mSensorManager;
    private final Sensor mHeartRateMonitorSensor;

    public HeartRateSensorService(){
        super(HeartRateSensorService.class.getSimpleName());
        mSensorManager = (SensorManager)getApplicationContext().getSystemService(SENSOR_SERVICE);
        mHeartRateMonitorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
    }

    @Override
    protected void onHandleIntent (Intent intent) {
        Sensor mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onCreate () {
        super.onCreate();
        mSensorManager.registerListener(this, mHeartRateMonitorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Log.v(TAG, "OnCreate Heart Rate Monitor Service");
    }

    @Override
    public void onDestroy () {
        mSensorManager.unregisterListener(this);
        Log.v(TAG, "OnDestroy Heart Rate Monitor Service");
        super.onDestroy();
    }

    @Override
    public void onSensorChanged (SensorEvent event) {
        Log.v(TAG, "Heart Rate Values: " + Arrays.toString(event.values));
        Intent intent = new Intent();
        intent.setAction(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        intent.putExtra(HeartRateSensorService.HEART_RATE, event.values);
        sendBroadcast(intent);
    }

    @Override
    public void onAccuracyChanged (Sensor sensor, int accuracy) {
        Log.v(TAG, "Heart Rate Accuracy "+accuracy);
    }
}
