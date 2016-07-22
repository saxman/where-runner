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
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.google.whererunner.MainActivity;
import com.example.google.whererunner.R;
import com.example.google.whererunner.model.Workout;
import com.example.google.whererunner.persistence.WorkoutDbHelper;
import com.google.firebase.analytics.FirebaseAnalytics;

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

    /** Outgoing action reporting recording status */
    public final static String ACTION_RECORDING_STATUS_CHANGED = "RECORDING_STATUS";

    /** Extra for recording status updates */
    public final static String EXTRA_IS_RECORDING = "IS_RECORDING";

    /** Outgoing action reporting that the workout data has been updated */
    public final static String ACTION_WORKOUT_DATA_UPDATED = "WORKOUT_DATA_UPDATED";

    private FirebaseAnalytics mFirebaseAnalytics;

    private BroadcastReceiver mHeartRateReceiver;
    private ServiceConnection mLocationServiceConnection;
    private boolean mIsLocationServiceConnected = false;

    private BroadcastReceiver mLocationReceiver;
    private ServiceConnection mHeartRateServiceConnection;
    private boolean mIsHeartRateServiceConnected = false;

    private WorkoutRecordingServiceBinder mServiceBinder = new WorkoutRecordingServiceBinder();

    private int NOTIFICATION_ID = 1;
    private Notification mNotification;
    private NotificationManager mNotificationManager;

    public static boolean isRecording = false;
    public static Workout workout = new Workout();
    public static ArrayList<HeartRateSensorEvent> heartRateSamples = new ArrayList<>();
    public static ArrayList<Location> locationSamples = new ArrayList<>();

    //
    // Service class methods
    //

    @Override
    public void onCreate() {
        super.onCreate();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        CharSequence contentText = getString(R.string.notification_content_text);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        mNotification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(contentText)
                .setWhen(System.currentTimeMillis())
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .build();

        mLocationServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d(LOG_TAG, "Location service started");
                mIsLocationServiceConnected = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.w(LOG_TAG, "Location service disconnected");
                mIsLocationServiceConnected = false;
            }
        };
        
        mHeartRateServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d(LOG_TAG, "Heart rate service started");
                mIsHeartRateServiceConnected = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.w(LOG_TAG, "Heart rate service disconnected");
                mIsHeartRateServiceConnected = false;
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        startHeartRateService();
        startLocationService();

        return mServiceBinder;
    }

    @Override
    public void onDestroy() {
        mNotificationManager.cancel(NOTIFICATION_ID);

        stopLocationService();
        stopHeartRateService();

        // Reset static vars since these survive outside of the service lifecycle
        workout = new Workout();
        resetSampleCollections();

        super.onDestroy();
    }

    //
    // Private class methods
    //

    private void reportRecordingStatus() {
        Intent intent = new Intent(ACTION_RECORDING_STATUS_CHANGED);
        intent.putExtra(EXTRA_IS_RECORDING, isRecording);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Starts recording a workout session
     */
    private void startRecordingData() {
        workout = new Workout(System.currentTimeMillis());

        startHeartRateRecording();
        startLocationRecording();

        // Log in Firebase
        fbLogStartWorkout();
    }

    /**
     * Stops recording a workout session and persists data
     */
    private void stopRecordingData() {
        workout.setEndTime(System.currentTimeMillis());

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mHeartRateReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationReceiver);

        saveWorkout();
        resetSampleCollections();

        // Log in Firebase
        fbLogStopWorkout();
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
                    workout.setCurrentHeartRate(hrEvent.getHeartRate());
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

                    workout.setCurrentSpeed(location.getSpeed());

                    if (locationSamples.size() > 0) {
                        Location priorLocation = locationSamples.get(locationSamples.size() - 1);

                        float[] results = new float[1];
                        Location.distanceBetween(
                                priorLocation.getLatitude(), priorLocation.getLongitude(),
                                location.getLatitude(), location.getLongitude(),
                                results);

                        workout.setDistance(workout.getDistance() + results[0]);
                    }

                    locationSamples.add(location);

                    LocalBroadcastManager.getInstance(WorkoutRecordingService.this).sendBroadcast(new Intent(ACTION_WORKOUT_DATA_UPDATED));
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
        mDbHelper.writeWorkout(workout);
        mDbHelper.writeHeartRates(heartRateSamples);
        mDbHelper.writeLocations(locationSamples);
    }

    /**
     * Empties all the data sample collections
     */
    private void resetSampleCollections() {
        heartRateSamples.clear();
        locationSamples.clear();
    }

    /* FIREBASE METHODS */

    /**
     * Logs a user starting a workout
     */
    private void fbLogStartWorkout() {
        mFirebaseAnalytics.logEvent("workout_start", new Bundle());
    }

    /**
     * Logs a user stopping a workout
     */
    private void fbLogStopWorkout() {
        mFirebaseAnalytics.logEvent("workout_stop", new Bundle());
    }

    //
    // Public service methods
    //

    public void startRecordingWorkout() {
        Intent intent = new Intent(this, WorkoutRecordingService.class);
        startService(intent);
        startForeground(NOTIFICATION_ID, mNotification);

        startRecordingData();
        reportRecordingStatus();

        isRecording = true;
    }

    public void stopRecordingWorkout() {
        stopRecordingData();
        reportRecordingStatus();

        stopForeground(true);
        stopSelf();

        isRecording = false;
    }

    public boolean isRecordingWorkout() {
        return isRecording;
    }

    /**
     * Start the heart rate service, which reads and broadcasts heart rate samples from the corresponding sensor.
     */
    public void startHeartRateService() {
        Intent intent = new Intent(this, HeartRateSensorService.class);
        bindService(intent, mHeartRateServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Stop the heart rate service, which will stop reading and broadcasting samples from the corresponding sensor.
     */
    public void stopHeartRateService() {
        // Only unbind if the service hasn't previously been unbound
        if (HeartRateSensorService.isActive) {
            unbindService(mHeartRateServiceConnection);
        }
    }

    /**
     * @return true if the heart rate sensor is on. false if it is off
     */
    public boolean isHeartRateSensorOn() {
        return HeartRateSensorService.isActive;
    }

    // Private for now as there's no use-case for toggling sensor state
    private void startLocationService() {
        Intent intent = new Intent(this, FusedLocationService.class);
        bindService(intent, mLocationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    // Private for now as there's no use-case for toggling sensor state
    private void stopLocationService() {
        unbindService(mLocationServiceConnection);
    }

    //
    // Public inner classes
    //

    public class WorkoutRecordingServiceBinder extends Binder {
        public WorkoutRecordingService getService() {
            return WorkoutRecordingService.this;
        }
    }
}
