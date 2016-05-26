package com.example.google.whererunner.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.google.whererunner.MainActivity;
import com.example.google.whererunner.R;

public abstract class LocationService extends Service {

    private static final String LOG_TAG = LocationService.class.getSimpleName();

    public final static String ACTION_LOCATION_CHANGED = "LOCATION_CHANGED";
    public final static String ACTION_STATUS_CHANGED = "STATUS_CHANGED";

    public final static String EXTRA_LOCATION = "LOCATION";
    public final static String EXTRA_IS_RECORDING = "IS_RECORDING";
    public final static String EXTRA_STATUS = "STATUS";

    protected static final int LOCATION_UPDATE_INTERVAL_MS = 1000;

    protected int NOTIFICATION_ID = 1;
    protected Notification mNotification;
    private NotificationManager mNotificationManager;

    private LocationServiceBinder mBinder;

    protected boolean mIsLocationUpdating = false;

    private boolean mIsRecording = false;

    public LocationService() {
        mBinder = new LocationServiceBinder(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

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
    }

    @Override
    public void onDestroy() {
        mNotificationManager.cancel(NOTIFICATION_ID);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!mIsRecording) {
            stopLocationUpdates();
            stopSelf();
        }

        // Must allow re-binding, otherwise there are no future calls to onUnbind when the parent,
        // binding activity is re-started, which means that location updates aren't later stopped
        // here when the re-stated activity is again stopped.
        return true;
    }

    /**
     * Callback method that sends location change events to ACTION_LOCATION_CHANGED local broadcast
     * receivers. This method is automatically called by subclasses of this class that implement
     * either com.google.android.gms.location.LocationListener or android.location.LocationListener
     * and that register themselves as listeners via FusedLocationApi.requestLocationUpdates(...)
     * or LocationManager.requestLocationUpdates(...).
     *
     * @param location The new location, as a Location object.
     */
    public void onLocationChanged(Location location) {
        Intent intent = new Intent()
                .setAction(ACTION_LOCATION_CHANGED)
                .putExtra(EXTRA_LOCATION, location);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    //
    // Service interface methods (protected, to be implemented or used by subclasses)
    //

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
    // Service interface methods (public, for objects binding this service)
    //

    public void startRecording() {
        startForeground(NOTIFICATION_ID, mNotification);
        mIsRecording = true;

        Intent intent = new Intent()
                .setAction(ACTION_STATUS_CHANGED)
                .putExtra(EXTRA_IS_RECORDING, mIsRecording);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void stopRecording() {
        stopForeground(true);
        mIsRecording = false;

        Intent intent = new Intent()
                .setAction(ACTION_STATUS_CHANGED)
                .putExtra(EXTRA_IS_RECORDING, mIsRecording);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    //
    // Nested classes (public)
    //

    public class LocationServiceBinder extends Binder {
        private Service mService;

        public LocationServiceBinder(Service service) {
            mService = service;
        }

        public Service getService() {
            return mService;
        }
    }
}
