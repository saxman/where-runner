package com.example.google.whererunner.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.google.whererunner.MainActivity;
import com.example.google.whererunner.R;
import com.example.google.whererunner.sql.WorkoutContract;
import com.example.google.whererunner.sql.WorkoutDbHelper;

import java.util.ArrayList;

/**
 * Listens for incoming local broadcast intents for starting and stopping a session
 * and all the data associated with it.
 *
 * This service should be spun up when the app is started and should be foregrounded to ensure
 * that all data is captured and recorded.
 */
public class WorkoutRecordingService extends Service {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutRecordingService.class.getSimpleName();

    /** Incoming action to stop services if not recording */
    public final static String ACTION_STOP_SERVICES = "STOP_SERVICES";

    /** Incoming actions to start and stop recording */
    public final static String ACTION_START_RECORDING = "START_RECORDING";
    public final static String ACTION_STOP_RECORDING = "STOP_RECORDING";

    /** Outgoing action reporting recording status */
    public final static String ACTION_RECORDING_STATUS = "RECORDING_STATUS";

    /** Extra for recording status updates */
    public final static String EXTRA_IS_RECORDING = "IS_RECORDING";

    /** Outgoing action reporting a workout data update */
    public final static String ACTION_RECORDING_DATA = "RECORDING_DATA";

    /** Extra for reporting workout distance */
    public final static String EXTRA_DISTANCE = "DISTANCE";

    private BroadcastReceiver mRecordingReceiver;
    private BroadcastReceiver mHeartRateReceiver;
    private BroadcastReceiver mLocationReceiver;

    // Data caches
    private static ArrayList<HeartRateSensorEvent> heartRateSamples = new ArrayList<>();
    private static ArrayList<Location> locationSamples = new ArrayList<>();

    private int NOTIFICATION_ID = 1;
    private Notification mNotification;
    private NotificationManager mNotificationManager;

    public static boolean isRecording = false;
    public static long startTime;
    public static long stopTime;
    public static double distance;

    private ServiceConnection mLocationServiceConnection;
    private ServiceConnection mHeartRateServiceConnection;

    //
    // Service override methods
    //

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        CharSequence contentText = getString(R.string.notification_content_text);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        mNotification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(contentText)
                .setWhen(System.currentTimeMillis())
//                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .build();

        mRecordingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case ACTION_STOP_SERVICES:
                        if (!isRecording) {
                            // child service unbinding handled in service onDestroy
                            stopSelf();
                        }

                        break;

                    case ACTION_START_RECORDING:
                        startForeground(NOTIFICATION_ID, mNotification);
                        startRecordingData();
                        isRecording = true;
                        reportRecordingStatus();

                        break;

                    case ACTION_STOP_RECORDING:
                        stopForeground(true);
                        stopRecordingData();
                        isRecording = false;
                        reportRecordingStatus();
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STOP_SERVICES);
        filter.addAction(ACTION_START_RECORDING);
        filter.addAction(ACTION_STOP_RECORDING);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRecordingReceiver, filter);

        mLocationServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {}

            @Override
            public void onServiceDisconnected(ComponentName componentName) {}
        };
        
        mHeartRateServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {}

            @Override
            public void onServiceDisconnected(ComponentName componentName) {}
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        intent = new Intent(this, FusedLocationService.class);
        bindService(intent, mLocationServiceConnection, Context.BIND_AUTO_CREATE);

        intent = new Intent(this, HeartRateSensorService.class);
        bindService(intent, mHeartRateServiceConnection, Context.BIND_AUTO_CREATE);

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        mNotificationManager.cancel(NOTIFICATION_ID);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecordingReceiver);

        unbindService(mLocationServiceConnection);
        unbindService(mHeartRateServiceConnection);

        // Reset static vars since these survive outside of the service lifecycle
        distance = 0;
        startTime = 0;
        stopTime = 0;
        resetSampleCollections();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //
    // Service implementation methods
    //

    private void reportRecordingStatus() {
        Intent intent = new Intent(ACTION_RECORDING_STATUS);
        intent.putExtra(EXTRA_IS_RECORDING, isRecording);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Starts recording a workout session
     */
    private void startRecordingData() {
        startTime =  System.currentTimeMillis();
        distance = 0;

        startHeartRateRecording();
        startLocationRecording();
    }

    /**
     * Stops recording a workout session and persists data
     */
    private void stopRecordingData() {
        stopTime = System.currentTimeMillis();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mHeartRateReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationReceiver);

        saveWorkout();
        resetSampleCollections();
    }

    /**
     * Starts listening for HR notifications and records values
     */
    private void startHeartRateRecording() {
        if (mHeartRateReceiver == null) {
            mHeartRateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    HeartRateSensorEvent hrEvent =
                            intent.getParcelableExtra(HeartRateSensorService.EXTRA_HEART_RATE);
                    heartRateSamples.add(hrEvent);
                }
            };
        }

        IntentFilter filter = new IntentFilter(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mHeartRateReceiver, filter);
    }

    /**
     * Starts listening for GPS notifications and records values
     */
    private void startLocationRecording() {
        if (mLocationReceiver == null) {
            mLocationReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);

                    if (locationSamples.size() > 0) {
                        Location priorLocation = locationSamples.get(locationSamples.size() - 1);

                        float[] results = new float[1];
                        Location.distanceBetween(
                                priorLocation.getLatitude(), priorLocation.getLongitude(),
                                location.getLatitude(), location.getLongitude(),
                                results);

                        distance += results[0];
                    }

                    locationSamples.add(location);

                    Intent intent2 = new Intent(ACTION_RECORDING_DATA);
                    intent2.putExtra(EXTRA_DISTANCE, distance);
                    LocalBroadcastManager.getInstance(WorkoutRecordingService.this).sendBroadcast(intent2);
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationReceiver, intentFilter);
    }

    /**
     * Saves the workout session data
     */
    private void saveWorkout() {
        WorkoutDbHelper mDbHelper = new WorkoutDbHelper(this);
        // TODO: write in correct workout type
        mDbHelper.writeWorkout(WorkoutContract.WorkoutType.RUNNING, startTime, stopTime);
        mDbHelper.writeHeartRates(heartRateSamples);
        mDbHelper.writeLocations(locationSamples);
        // TODO: Remove the following test code for testing db reads
        Log.i(LOG_TAG, "Nr workouts in db: " + mDbHelper.readNrWorkouts());
        for (long startTime : mDbHelper.readLastFiveWorkouts()) {
            Log.i(LOG_TAG, "Start time: " + new java.util.Date(startTime));
        }
    }

    /**
     * Empties all the data caches
     */
    private void resetSampleCollections() {
        heartRateSamples.clear();
        locationSamples.clear();
    }

}
