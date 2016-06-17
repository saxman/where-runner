package com.example.google.whererunner.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.example.google.whererunner.MainActivity;
import com.example.google.whererunner.R;

public abstract class LocationService extends Service {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = LocationService.class.getSimpleName();

    public final static String ACTION_LOCATION_CHANGED = "LOCATION_CHANGED";
    public final static String ACTION_RECORDING_STATUS_CHANGED = "RECORDING_STATUS_CHANGED";

    public final static String ACTION_REPORT_RECORDING_STATUS = "REPORT_RECORDING_STATUS";
    public final static String ACTION_RECORDING_STATUS = "RECORDING_STATUS";

    public final static String ACTION_STOP_LOCATION_UPDATES = "STOP_LOCATION_UPDATES";
    public final static String ACTION_START_RECORDING = "START_RECORDING";
    public final static String ACTION_STOP_RECORDING = "STOP_RECORDING";

    public final static String EXTRA_LOCATION = "LOCATION";
    public final static String EXTRA_IS_RECORDING = "IS_RECORDING";

    public final static String ACTION_CONNECTIVITY_LOST = "CONNECTIVITY_LOST";

    protected static final int LOCATION_UPDATE_INTERVAL_MS = 1000;
    private static final int LOCATION_UPDATE_INTERVAL_TIMEOUT_MS = 10000;
    private static final int LOCATION_ACCURACY_MAX_METERS = 25;

    private int NOTIFICATION_ID = 1;
    private Notification mNotification;
    private NotificationManager mNotificationManager;

    protected boolean mIsLocationUpdating = false;
    private boolean mIsRecording = false;

    private BroadcastReceiver mBroadcastReceiver;

    private CountDownTimer mLocationSampleTimer;

    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        startLocationUpdates();

        return START_NOT_STICKY;
    }

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

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case ACTION_STOP_LOCATION_UPDATES:
                        if (!mIsRecording) {
                            stopLocationUpdates();
                            stopSelf();
                        }
                        break;
                    case ACTION_START_RECORDING:
                        startRecording();
                        break;
                    case ACTION_STOP_RECORDING:
                        stopRecording();
                        break;
                    case ACTION_REPORT_RECORDING_STATUS:
                        Intent intentOut = new Intent()
                                .setAction(ACTION_RECORDING_STATUS)
                                .putExtra(EXTRA_IS_RECORDING, mIsRecording);

                        LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(intentOut);
                }

            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_STOP_LOCATION_UPDATES);
        intentFilter.addAction(ACTION_START_RECORDING);
        intentFilter.addAction(ACTION_STOP_RECORDING);
        intentFilter.addAction(ACTION_REPORT_RECORDING_STATUS);

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        mNotificationManager.cancel(NOTIFICATION_ID);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        if (mLocationSampleTimer != null) {
            mLocationSampleTimer.cancel();
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new IllegalAccessError("Don't bind me, bro!");
    }

    //
    // Protected service interface methods (to be implemented or used by subclasses)
    //

    /**
     * Callback method that sends location change events to ACTION_LOCATION_CHANGED local broadcast
     * receivers. This method should be called by subclasses of this class as they receive location
     * updates.
     *
     * @param location The new location, as a Location object.
     */
    protected void onLocationChanged(Location location) {
        // Drop location samples that are below accuracy threshold
        if (location.getAccuracy() > LOCATION_ACCURACY_MAX_METERS) {
            return;
        }

        Intent intent = new Intent()
                .setAction(ACTION_LOCATION_CHANGED)
                .putExtra(EXTRA_LOCATION, location);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        // Reset (cancel) pre-existing timer
        if (mLocationSampleTimer != null) {
            mLocationSampleTimer.cancel();
        }

        // Start a timer to detect if we're not receiving location samples in regular intervals
        mLocationSampleTimer = new CountDownTimer(LocationService.LOCATION_UPDATE_INTERVAL_TIMEOUT_MS, LocationService.LOCATION_UPDATE_INTERVAL_TIMEOUT_MS) {
            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                Intent intent = new Intent(ACTION_CONNECTIVITY_LOST);
                LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(intent);
            }
        }.start();
    }

    protected abstract void startLocationUpdates();
    protected abstract void stopLocationUpdates();

    protected boolean checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO (re)ask the user for permission
            return false;
        }

        return true;
    }

    //
    // Private methods
    //

    private void startRecording() {
        startForeground(NOTIFICATION_ID, mNotification);
        mIsRecording = true;

        Intent intent = new Intent()
                .setAction(ACTION_RECORDING_STATUS_CHANGED)
                .putExtra(EXTRA_IS_RECORDING, mIsRecording);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void stopRecording() {
        stopForeground(true);
        mIsRecording = false;

        Intent intent = new Intent()
                .setAction(ACTION_RECORDING_STATUS_CHANGED)
                .putExtra(EXTRA_IS_RECORDING, mIsRecording);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
