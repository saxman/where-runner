package com.example.google.whererunner.services;

import android.app.Notification;
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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.example.google.whererunner.MainActivity;
import com.example.google.whererunner.R;
import com.example.google.whererunner.WhereRunnerApp;
import com.example.google.whererunner.model.Workout;
import com.example.google.whererunner.model.WorkoutType;
import com.example.google.whererunner.persistence.WorkoutDbHelper;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

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
    public final static String ACTION_RECORDING_STATUS_CHANGED = "RECORDING_STATUS_CHANGED";

    /** Extra for recording status updates */
    public final static String EXTRA_IS_RECORDING = "IS_RECORDING";

    /** Outgoing action reporting that the workout data has been updated */
    public final static String ACTION_WORKOUT_DATA_UPDATED = "WORKOUT_DATA_UPDATED";

    private FirebaseAnalytics mFirebaseAnalytics;

    private final BroadcastReceiver mHeartRateBroadcastReceiver = new HeartRateBroadcastReceiver();
    private final ServiceConnection mLocationServiceConnection = new MyServiceConnection();

    private final BroadcastReceiver mLocationBroadcastReceiver = new LocationBroadcastReceiver();
    private final ServiceConnection mHeartRateServiceConnection = new MyServiceConnection();

    private WorkoutRecordingServiceBinder mServiceBinder = new WorkoutRecordingServiceBinder();

    private static final int NOTIFICATION_ID = 1;
    private NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;

    public static boolean isRecording = false;
    public static Workout workout = new Workout();
    public static ArrayList<HeartRateSensorEvent> heartRateSamples = new ArrayList<>();
    public static ArrayList<Location> locationSamples = new ArrayList<>();

    private WorkoutType mWorkoutType = WorkoutType.RUNNING;

    //
    // Service class methods
    //

    @Override
    public void onCreate() {
        super.onCreate();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);


        Intent stopIntent = new Intent(this, WorkoutRecordingService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, stopIntent, 0);
        NotificationCompat.Action.Builder actionBuilder = new NotificationCompat.Action.Builder( R.drawable.ic_stop, "stop", pi);

        NotificationCompat.Action.WearableExtender actionExtender = new NotificationCompat.Action.WearableExtender().setHintLaunchesActivity(false).setHintDisplayActionInline(true);
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
        wearableExtender.addAction(actionBuilder.extend(actionExtender).build());
        wearableExtender.setHintContentIntentLaunchesActivity(false);

        // orig

        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.setAction(MainActivity.ACTION_SHOW_WORKOUT);

        mNotificationManager = NotificationManagerCompat.from(this);
        mNotificationBuilder = new NotificationCompat.Builder(this)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setLocalOnly(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(
                        PendingIntent.getActivity(this, 0, contentIntent, 0))
                .extend(wearableExtender);
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
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        // Allow re-binding. Otherwise, the service will remain running if it is bound, started, unbound,
        // re-bound (attempted; will fail), stopped, and unbound (attempted; will fail since not re-bound).
        return true;
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

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mHeartRateBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationBroadcastReceiver);

        saveWorkout();
        resetSampleCollections();

        // Log in Firebase
        fbLogStopWorkout();
    }

    /**
     * Starts listening for HR notifications and records values
     */
    private void startHeartRateRecording() {
        IntentFilter filter = new IntentFilter(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mHeartRateBroadcastReceiver, filter);
    }

    /**
     * Starts listening for GPS notifications and records values
     */
    private void startLocationRecording() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationBroadcastReceiver, intentFilter);
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

    private void startLocationService() {
        Intent intent = new Intent(this, FusedLocationService.class);
        bindService(intent, mLocationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopLocationService() {
        unbindService(mLocationServiceConnection);
    }

    //
    // Public service methods
    //

    public void startRecordingWorkout() {
        startService(new Intent(this, WorkoutRecordingService.class));
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());

        startRecordingData();
        isRecording = true;

        reportRecordingStatus();

        mNotificationBuilder.setWhen(System.currentTimeMillis()).setUsesChronometer(true).setShowWhen(true);
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    public void stopRecordingWorkout() {
        stopRecordingData();
        isRecording = false;

        reportRecordingStatus();

        stopForeground(true);
        stopSelf();
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

    // TODO should be moved to workout class? maybe these wrap accessors on workout class?
    public void setActivityType(WorkoutType workoutType) {
        mWorkoutType = workoutType;
    }

    public WorkoutType getActivityType() {
        return mWorkoutType;
    }

    //
    // Inner classes
    //

    public class WorkoutRecordingServiceBinder extends Binder {
        public WorkoutRecordingService getService() {
            return WorkoutRecordingService.this;
        }
    }

    private class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {}

        @Override
        public void onServiceDisconnected(ComponentName componentName) {}
    }

    private class LocationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);

                    // If we have at least 2 samples, calculate distance and speed
                    if (locationSamples.size() > 0) {
                        Location priorLocation = locationSamples.get(locationSamples.size() - 1);

                        float[] results = new float[1];
                        Location.distanceBetween(
                                priorLocation.getLatitude(), priorLocation.getLongitude(),
                                location.getLatitude(), location.getLongitude(),
                                results);
                        float dist = results[0];

                        workout.setDistance(workout.getDistance() + dist);
                        workout.setCurrentSpeed(dist / (location.getTime() - priorLocation.getTime()));
                        workout.setAverageSpeed(workout.getDistance() / (location.getTime() - workout.getStartTime()));
                    }

                    locationSamples.add(location);

                    LocalBroadcastManager.getInstance(WorkoutRecordingService.this).sendBroadcast(new Intent(ACTION_WORKOUT_DATA_UPDATED));
                }
            }).run();
        }
    }

    private class HeartRateBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    HeartRateSensorEvent hrEvent = intent.getParcelableExtra(HeartRateSensorService.EXTRA_HEART_RATE);

                    // Calculate new heart rate average
                    float avg = workout.getHeartRateAverage();
                    avg = (avg * heartRateSamples.size() + hrEvent.getHeartRate()) / (heartRateSamples.size() + 1);

                    workout.setCurrentHeartRate(hrEvent.getHeartRate());
                    workout.setHeartRateAverage(avg);

                    heartRateSamples.add(hrEvent);
                }
            }).run();
        }
    }
}
