package info.saxman.android.whererunner.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;

import info.saxman.android.whererunner.MainActivity;
import info.saxman.android.whererunner.R;
import info.saxman.android.whererunner.model.HeartRateSample;
import info.saxman.android.whererunner.model.LocationSample;
import info.saxman.android.whererunner.model.Workout;
import info.saxman.android.whererunner.model.WorkoutType;
import info.saxman.android.whererunner.persistence.WorkoutRepository;

/**
 * Listens for incoming local broadcast intents for starting and stopping a session
 * and all the data associated with it.
 *
 * This service should be spun up when the app is started and should be foregrounded during an
 * active workout to ensure that all data is captured and recorded.
 */
public class WorkoutRecordingService extends Service {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutRecordingService.class.getSimpleName();

    /** Outgoing broadcast action reporting workout recording status */
    public final static String ACTION_SERVICE_STATE_CHANGED = "WorkoutRecordingService.RECORDING_STATE_CHANGED";
    public final static String EXTRA_IS_RECORDING = "IS_RECORDING";
    public final static String EXTRA_IS_HRM_ON = "IS_HRM_ON";
    public final static String EXTRA_WORKOUT_TYPE = "WORKOUT_TYPE";

    /** Outgoing broadcast action reporting that the is updated workout data available */
    public final static String ACTION_WORKOUT_DATA_UPDATED = "WORKOUT_DATA_UPDATED";

    private final BroadcastReceiver mHeartRateBroadcastReceiver = new HeartRateBroadcastReceiver();
    private final ServiceConnection mHeartRateServiceConnection = new HeartRateServiceConnection();

    private final ServiceConnection mLocationServiceConnection = new MyServiceConnection();
    private final BroadcastReceiver mLocationBroadcastReceiver = new LocationBroadcastReceiver();

    private final ServiceConnection mPhoneConnectivityServiceConnection = new MyServiceConnection();

    private WorkoutRecordingServiceBinder mServiceBinder = new WorkoutRecordingServiceBinder();

    private static final int NOTIFICATION_ID = 1;
    private NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;

    // TODO make private and non-static, and rely on broadcasts to setup UI
    public static boolean isRecording = false;
    public static Workout workout = new Workout();
    public static ArrayList<HeartRateSample> heartRateSamples = new ArrayList<>();
    public static ArrayList<LocationSample> locationSamples = new ArrayList<>();

    /** The last known location of the user during an active workout session. */
    private Location mLastKnownWorkoutLocation;

    private boolean mIsHrmActive = false;

    //
    // Service class methods
    //

    @Override
    public void onCreate() {
        super.onCreate();

        Intent stopIntent =
                new Intent(MainActivity.ACTION_STOP_WORKOUT_CONFIRM, null, this, MainActivity.class);

        PendingIntent pi = PendingIntent.getActivity(this, 0, stopIntent, 0);

        NotificationCompat.Action.Builder actionBuilder =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_stop_white, getString(R.string.stop_recording), pi);

        NotificationCompat.Action.WearableExtender actionExtender =
                new NotificationCompat.Action.WearableExtender()
                        .setHintLaunchesActivity(true)
                        .setHintDisplayActionInline(true);

        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .addAction(actionBuilder.extend(actionExtender).build());

        Intent contentIntent = new Intent(this, MainActivity.class);

        String channelId = createNotificationChannel();

        mNotificationManager = NotificationManagerCompat.from(this);
        mNotificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setLocalOnly(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(
                        PendingIntent.getActivity(this, 0, contentIntent, 0))
                .extend(wearableExtender);

        // Set the workout type to the last used workout type
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int workoutTypeId = sharedPrefs.getInt(getString(R.string.pref_workout_type), WorkoutType.RUNNING.preferencesId);

        // TODO somewhat inefficient since setWorkoutType writes to shared prefs
        setWorkoutType(WorkoutType.getWorkoutType(workoutTypeId));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        startLocationService();

        boolean autoStartHrm = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_auto_start_hrm), true);

        if (autoStartHrm) {
            startHeartRateService();
        }

        bindService(new Intent(this, PhoneConnectivityService.class),
                mPhoneConnectivityServiceConnection, Context.BIND_AUTO_CREATE);

        return mServiceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);

        // The activity has re-connected; re-broadcast the service state to set up the UI.
        intent = new Intent(ACTION_SERVICE_STATE_CHANGED);
        intent.putExtra(EXTRA_IS_RECORDING, isRecording);
        intent.putExtra(EXTRA_IS_HRM_ON, mIsHrmActive);
        intent.putExtra(EXTRA_WORKOUT_TYPE, getWorkoutType().preferencesId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        // TODO re-evaluate where we should be removing the notification
        mNotificationManager.cancel(NOTIFICATION_ID);

        unbindService(mPhoneConnectivityServiceConnection);
        unbindService(mLocationServiceConnection);
        stopHeartRateService();

        // Reset static vars since these survive outside of the service lifecycle.
        workout = new Workout();
        heartRateSamples.clear();
        locationSamples.clear();

        super.onDestroy();
    }

    //
    // Private class methods
    //

    /**
     * Starts recording a workout session
     */
    private void startRecordingData() {
        int type = workout.getType();

        workout = new Workout();
        workout.setStartTime(System.currentTimeMillis());
        workout.setType(type);

        heartRateSamples.clear();
        locationSamples.clear();

        mLastKnownWorkoutLocation = null;

        IntentFilter filter = new IntentFilter();
        filter.addAction(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mHeartRateBroadcastReceiver, filter);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationBroadcastReceiver, intentFilter);
    }

    /**
     * Stops recording a workout session and persists data
     */
    private void stopRecordingData() {
        workout.setEndTime(System.currentTimeMillis());

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mHeartRateBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationBroadcastReceiver);

        WorkoutRepository repo = new WorkoutRepository(this);
        repo.storeWorkout(workout);
        repo.storeHeartRateSamples(heartRateSamples);
        repo.storeLocationSamples(locationSamples);
    }

    /**
     * @return The channel id, if the device is running Android O or later. null otherwise.
     */
    private String createNotificationChannel() {
        // NotificationChannels are required for Notifications on O (API 26) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "where_runner_workout_channel";

            NotificationChannel notificationChannel =
                    new NotificationChannel(channelId, "Where Runner", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Where Runner Workout Status Updates");
            notificationChannel.enableVibration(false);
            notificationChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            // Adds NotificationChannel to system. Attempting to create an existing notification
            // channel with its original values performs no operation, so it's safe to perform the
            // below sequence.
            NotificationManager notificationManager =
                    (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);

            return channelId;
        } else {
            // Returns empty string for pre-O (26) devices.
            return null;
        }
    }

    //
    // Public service methods
    //

    public void startRecordingWorkout() {
        Intent intent = new Intent(this, WorkoutRecordingService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        mNotificationBuilder.setWhen(System.currentTimeMillis()).setUsesChronometer(true).setShowWhen(true);
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());

        startRecordingData();
        isRecording = true;

        Intent intent2 = new Intent(ACTION_SERVICE_STATE_CHANGED);
        intent2.putExtra(EXTRA_IS_RECORDING, isRecording);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent2);
    }

    public void stopRecordingWorkout() {
        stopRecordingData();
        isRecording = false;

        Intent intent = new Intent(ACTION_SERVICE_STATE_CHANGED);
        intent.putExtra(EXTRA_IS_RECORDING, isRecording);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        stopForeground(true);
        stopSelf();
    }

    public void startLocationService() {
        // Only attempt to unbind if the service hasn't previously been unbound
        if (LocationService.isActive) {
            unbindService(mLocationServiceConnection);
        }

        boolean useWatchGps = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_use_watch_gps), false);

        // Use the FusedLocationService for watch and/or phone GPS, or the GpsLocationService
        // for watch only.
        if (useWatchGps) {
            bindService(new Intent(this, GpsLocationService.class),
                    mLocationServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            bindService(new Intent(this, FusedLocationService.class),
                    mLocationServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void startHeartRateService() {
        if (!HeartRateSensorService.isActive) {
            Intent intent = new Intent(this, HeartRateSensorService.class);
            bindService(intent, mHeartRateServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Stop the heart rate service, which will stop reading and broadcasting samples from the corresponding sensor.
     */
    public void stopHeartRateService() {
        // Only unbind if the service hasn't previously been unbound
        if (HeartRateSensorService.isActive) {
            unbindService(mHeartRateServiceConnection);
            mIsHrmActive = false;

            Intent intent = new Intent(ACTION_SERVICE_STATE_CHANGED);
            intent.putExtra(EXTRA_IS_HRM_ON, mIsHrmActive);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    public void setWorkoutType(WorkoutType workoutType) {
        workout.setType(workoutType.preferencesId);

        // Store the workout type in shared prefs so it can be the default when the app is run next.
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(getString(R.string.pref_workout_type), workout.getType());
        editor.apply();

        Intent intent = new Intent(ACTION_SERVICE_STATE_CHANGED);
        intent.putExtra(EXTRA_WORKOUT_TYPE, workout.getType());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Static since UI components need this info w/o having to bind to the service.
    public WorkoutType getWorkoutType() {
        return WorkoutType.getWorkoutType(workout.getType());
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

    private class HeartRateServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // Once we know that the HRM service is active, let the UI components know.
            mIsHrmActive = true;

            Intent intent = new Intent(ACTION_SERVICE_STATE_CHANGED);
            intent.putExtra(EXTRA_IS_HRM_ON, mIsHrmActive);
            LocalBroadcastManager.getInstance(WorkoutRecordingService.this).sendBroadcast(intent);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {}
    }

    private class LocationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (intent != null && LocationService.ACTION_LOCATION_CHANGED.equals(intent.getAction())) {
                Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);

                // If we have at least 2 samples, calculate distance and speed
                if (mLastKnownWorkoutLocation != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            mLastKnownWorkoutLocation.getLatitude(), mLastKnownWorkoutLocation.getLongitude(),
                            location.getLatitude(), location.getLongitude(),
                            results);
                    float dist = results[0];

                    workout.setDistance(workout.getDistance() + dist);
                    workout.setSpeedCurrent(dist / (location.getTime() - mLastKnownWorkoutLocation.getTime()));
                    workout.setSpeedAverage(workout.getDistance() / (location.getTime() - workout.getStartTime()));
                }

                mLastKnownWorkoutLocation = location;

                LocationSample s = new LocationSample();
                s.lat = location.getLatitude();
                s.lng = location.getLongitude();
                s.timestamp = location.getTime();
                locationSamples.add(s);

                // Notify listeners the the workout data has changed.
                LocalBroadcastManager.getInstance(WorkoutRecordingService.this).sendBroadcast(
                        new Intent(ACTION_WORKOUT_DATA_UPDATED));
            }
        }
    }

    private class HeartRateBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (intent != null && HeartRateSensorService.ACTION_HEART_RATE_CHANGED.equals(intent.getAction())) {
                HeartRateSample hrEvent = intent.getParcelableExtra(HeartRateSensorService.EXTRA_HEART_RATE);

                // Calculate new heart rate average
                float avg = workout.getHeartRateAverage();
                avg = (avg * heartRateSamples.size() + hrEvent.heartRate) / (heartRateSamples.size() + 1);

                workout.setCurrentHeartRate(hrEvent.heartRate);
                workout.setHeartRateAverage(avg);

                heartRateSamples.add(hrEvent);
            }
        }
    }
}
