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

import com.example.google.whererunner.datatypes.HeartRate;
import com.example.google.whererunner.sql.WorkoutContract;
import com.example.google.whererunner.sql.WorkoutDbHelper;

import java.util.ArrayList;

public class WorkoutRecordingService extends Service {

    @SuppressWarnings("unused")
    private static final String TAG = WorkoutRecordingService.class.getSimpleName();

    // Receivers
    private BroadcastReceiver recordingReceiver;
    private BroadcastReceiver hrReceiver;
    private BroadcastReceiver locationReceiver;


    // Data caches
    private ArrayList<HeartRate> hrCache = new ArrayList<>();
    private ArrayList<Location> locationCache = new ArrayList<>();

    //
    // Service override methods
    //

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
        // Set up the recording broadcast receiver listener
        recordingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "Received ACTION_RECORDING_STATUS_CHANGED");
                boolean isRecording = intent.getBooleanExtra(LocationService.EXTRA_IS_RECORDING, false);
                if (isRecording) {
                    Log.v(TAG, "Received isRecording");
                    recordingStateManager(RecordingTransition.START_RECORDING);
                } else {
                    Log.v(TAG, "Received not isRecording");
                    recordingStateManager(RecordingTransition.STOP_RECORDING);
                }
            }
        };
        IntentFilter filter = new IntentFilter(LocationService.ACTION_RECORDING_STATUS_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(recordingReceiver, filter);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(recordingReceiver);
        super.onDestroy();
    }

    //
    // Service implementation methods
    //

    /**
     * Manages the recording state of the service
     *
     * Why the enum for such just two states? Well, if we introduce pausing, etc.
     * later on, this could get complicated, and using enums now makes it nice and readable
     */
    private enum RecordingTransition {START_RECORDING, STOP_RECORDING}
    private enum RecordingState {RECORDING, NOT_RECORDING}
    private RecordingState mRecordingState = RecordingState.NOT_RECORDING;
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

    /**
     * Starts recording a workout session
     */
    private void startRecording() {
        Log.v(TAG, "In startRecording");
        startTime =  System.currentTimeMillis();
        startHRRecording();
        startGPSRecording();
    }

    /**
     * Stops recording a workout session and persists data
     */
    private void stopRecording() {
        Log.v(TAG, "In stopRecording");
        stopTime = System.currentTimeMillis();
        stopHRRecording();
        stopGPSRecording();
        saveWorkout();
        emptyCaches();
    }

    /**
     * Starts listening for HR notifications and records values
     */
    private void startHRRecording() {
        if (hrReceiver ==null) {
            hrReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    float val = intent.getFloatExtra(HeartRateSensorService.EXTRA_HEART_RATE, -1);
                    long timestamp = intent.getLongExtra(HeartRateSensorService.EXTRA_TIMESTAMP, -1);
                    hrCache.add(new HeartRate(timestamp, val));
                }
            };
        }
            IntentFilter filter = new IntentFilter(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
            LocalBroadcastManager.getInstance(this).registerReceiver(hrReceiver, filter);
    }

    /**
     * Stops listening for HR notification
     */
    private void stopHRRecording() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hrReceiver);
    }

    /**
     * Starts listening for GPS notifications and records values
     */
    private void startGPSRecording() {

        if (locationReceiver == null) {
            locationReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);
                    locationCache.add(location);
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, intentFilter);
    }

    /**
     * Stops listening for GPS notifications
     */
    private void stopGPSRecording() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver);
    }

    /**
     * Saves the workout session data
     */
    private void saveWorkout() {
        Log.i(TAG, "Start time: " + new java.util.Date(this.startTime));
        Log.i(TAG, "End time: " + new java.util.Date(this.stopTime));
        Log.i(TAG, "Nr. HR values: " + this.hrCache.size());
        Log.i(TAG, "Nr. location values: " + this.locationCache.size());

        WorkoutDbHelper mDbHelper = new WorkoutDbHelper(this);
        // TODO: write in correct workout type
        mDbHelper.writeWorkout(WorkoutContract.WorkoutType.RUNNING, startTime, stopTime);
        mDbHelper.writeHeartRates(hrCache);
        mDbHelper.writeLocations(locationCache);
    }

    /**
     * Empties all the data caches
     */
    private void emptyCaches() {
        this.hrCache.clear();
        this.locationCache.clear();
    }

}
