package info.saxman.whererunner.services;

import android.app.Service;
import android.content.Intent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class HeartRateSensorService extends Service {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = HeartRateSensorService.class.getSimpleName();

    public static final String ACTION_CONNECTIVITY_CHANGED = "HEART_RATE_SENSOR_CONNECTIVITY_CHANGED";
    public static final String EXTRA_IS_RECEIVING_SAMPLES = "IS_RECEIVING_HEART_RATE_SAMPLES";

    public static final String ACTION_HEART_RATE_CHANGED = "HEART_RATE_CHANGED";
    public static final String EXTRA_HEART_RATE = "HEART_RATE";

    private CountDownTimer mSensorSampleTimer;

    private static final int HRM_UPDATE_INTERVAL_TIMEOUT_MS = 5000;
    private static final int HRM_ACCURACY_THRESHOLD = 0;

    private SensorManager mSensorManager;
    private SensorEventListener mSensorListener;

    public static HeartRateSensorEvent lastHeartRateSample;
    public static boolean isReceivingAccurateHeartRateSamples = false;

    public static boolean isActive = false;

    @Override
    public void onCreate () {
        super.onCreate();

        isActive = true;

        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);

        // Ensure that the device has a HRM.
        // This will also return null if BODY_SENSOR permission has not been granted.
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
            mSensorListener = new HeartRateSensorEventListener();
        } else {
            Log.w(LOG_TAG, "No heart rate sensor detected, or permission not grated to access it");
        }
    }

    @Override
    public void onDestroy () {
        mSensorManager.unregisterListener(mSensorListener);

        if (mSensorSampleTimer != null) {
            mSensorSampleTimer.cancel();
        }

        isActive = false;

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE),
                SensorManager.SENSOR_DELAY_NORMAL);

        // No need to return an IBinder instance since binding is only used to controls service
        // lifecycle, and isn't used for direct access
        return null;
    }

    /**
     * Sensor event listener for heart rate; fires an ACTION_HEART_RATE_CHANGED intent whenever
     * a new HR reading is received.
     */
    private class HeartRateSensorEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Don't broadcast low quality readings
            if (event.accuracy == HRM_ACCURACY_THRESHOLD) {
                return;
            }

            // Get the last sample; however, there appears to only ever be one value...
            float value = event.values[event.values.length - 1];

            // HR sensor values appear to only ever be integers, so store as such
            HeartRateSensorEvent hrEvent = new HeartRateSensorEvent((int) value, event.timestamp, event.accuracy);
            lastHeartRateSample = hrEvent;

            Intent intent = new Intent(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
            intent.putExtra(HeartRateSensorService.EXTRA_HEART_RATE, hrEvent);
            LocalBroadcastManager.getInstance(HeartRateSensorService.this).sendBroadcast(intent);

            if (!isReceivingAccurateHeartRateSamples) {
                isReceivingAccurateHeartRateSamples = true;
                intent = new Intent(ACTION_CONNECTIVITY_CHANGED);
                intent.putExtra(EXTRA_IS_RECEIVING_SAMPLES, true);
                LocalBroadcastManager.getInstance(HeartRateSensorService.this).sendBroadcast(intent);
            }

            // Since we received an accurate heart rate sample, cancel the existing timer
            if (mSensorSampleTimer != null) {
                mSensorSampleTimer.cancel();
            }

            // Start a timer to detect if we're not receiving HR samples in regular intervals
            mSensorSampleTimer = new CountDownTimer(HRM_UPDATE_INTERVAL_TIMEOUT_MS, HRM_UPDATE_INTERVAL_TIMEOUT_MS) {
                public void onTick(long millisUntilFinished) {}

                public void onFinish() {
                    isReceivingAccurateHeartRateSamples = false;
                    Intent intent = new Intent(ACTION_CONNECTIVITY_CHANGED);
                    intent.putExtra(EXTRA_IS_RECEIVING_SAMPLES, false);
                    LocalBroadcastManager.getInstance(HeartRateSensorService.this).sendBroadcast(intent);
                }
            }.start();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    }
}
