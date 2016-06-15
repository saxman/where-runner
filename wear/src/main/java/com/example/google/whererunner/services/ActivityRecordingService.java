/**
 * Listens for incoming local broadcast intents for starting and stopping a session
 * and all the data associated with it.
 *
 * This service should be spun up when the app is started and should be foregrounded to ensure
 * that all data is captured and recorded.
 */
package com.example.google.whererunner.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class ActivityRecordingService extends Service {

    @SuppressWarnings("unused")
    private static final String TAG = ActivityRecordingService.class.getSimpleName();

    // Receivers
    private BroadcastReceiver recordingReceiver;
    private BroadcastReceiver hrReceiver;
    private BroadcastReceiver locationReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand Activity Recording Service; this does nothing!");
        return START_NOT_STICKY; // TODO: Should this be STICKY?
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Set up the recording broadcast receiver
        recordingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isRecording = intent.getBooleanExtra(LocationService.EXTRA_IS_RECORDING, false);
                // TODO: Handle what to do when recording message received
            }
        };
        IntentFilter filter = new IntentFilter(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(recordingReceiver, filter);
    }

    /**
     * Manages the recording state of the service
     *
     * Why the enum for such just two states? Well, if we introduce pausing, etc.
     * later on, this could get complicated, and using enums now makes it nice and readable
     */
    private enum RecordingTransition {START_RECORDING, STOP_RECORDING}
    private enum RecordingState {RECORDING, NOT_RECORDING}
    private RecordingState mRecordingState;
    private long startTime, stopTime;

    /**
     * Update the recording state from a recording transition
     * @param transition the transition to be applied to the current state
     */
    private void recordingStateManager(RecordingTransition transition) {
        if (mRecordingState == RecordingState.RECORDING) {
            if (transition == RecordingTransition.START_RECORDING) {
                // Already recording, do nothing, but log a warning as the UI should
                // not enable starting recording multiple times
                Log.w(TAG, "Recording start requested when service already recording");
            }
            else if (transition == RecordingTransition.STOP_RECORDING) {
                stopRecording();
                mRecordingState = RecordingState.NOT_RECORDING;
            }
        }
        else if (mRecordingState == RecordingState.NOT_RECORDING) {
            if (transition == RecordingTransition.START_RECORDING) {
                startRecording();
                mRecordingState = RecordingState.RECORDING;
            }
            else if (transition == RecordingTransition.STOP_RECORDING) {
                Log.w(TAG, "Recording stop requested when service is not recording");
            }
        }
    }

    private void startRecording() {
        startTime =  System.currentTimeMillis();
    }

    private void stopRecording() {
        stopTime = System.currentTimeMillis();
        // TODO: Save the session at this point!
    }

    private void startRecordingHR() {
        if (hrReceiver ==null) {
            hrReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Show the last reading
                    float[] hrValues = intent.getFloatArrayExtra(HeartRateSensorService.HEART_RATE);
                    for (float hr : hrValues) {
                        // TODO: Save the HR value here!
                    }
                }
            };
        }
            IntentFilter filter = new IntentFilter(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
            LocalBroadcastManager.getInstance(this).registerReceiver(hrReceiver, filter);
    }

    private void stopRecordingHR() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hrReceiver);
    }

    private void startGPSRecording() {

        if (locationReceiver == null) {
            locationReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);
                    // TODO: Record the location here!
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, intentFilter);
    }

    private void stopGPSRecording() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver);
    }

}
