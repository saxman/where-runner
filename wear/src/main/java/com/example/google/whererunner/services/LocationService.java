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
import android.util.Log;

import com.example.google.whererunner.MainActivity;
import com.example.google.whererunner.MainFragment;
import com.example.google.whererunner.R;

public abstract class LocationService extends Service {

    private static final String LOG_TAG = LocationService.class.getSimpleName();

    public final static String ACTION_LOCATION_CHANGED = "LOCATION_CHANGED";
    public final static String ACTION_STATUS_CHANGED = "STATUS_CHANGED";

    public final static String EXTRA_LOCATION = "LOCATION";
    public final static String EXTRA_STATUS = "STATUS";

    public final static int LOCATION_UPDATING = 1;
    public final static int LOCATION_UPDATES_STOPPED = 0;

    protected static final int LOCATION_UPDATE_INTERVAL_MS = 1000;

    protected int NOTIFICATION_ID = 1;
    protected Notification mNotification;
    private NotificationManager mNotificationManager;

    private LocationServiceBinder mBinder;

    protected boolean mIsLocationUpdating = false;

    public LocationService() {
        mBinder = new LocationServiceBinder(this);
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
                .setAutoCancel(true) // close the notification when the user triggers an action
                .build();

        // TODO add action to stop location recording
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "Start command received");

        if (!mIsLocationUpdating) {
            startLocationUpdates();
        } else {
            // Notification is set to auto cancel. Otherwise, it remains open after the user
            // opens the app via the notification. Re-starting service in foreground will
            // re-display the notification.
            Log.d(LOG_TAG, "Service already receiving location updates. Re-starting service in foreground");
            startForeground(NOTIFICATION_ID, mNotification);
        }

        return START_STICKY;
    }

    public void onLocationChanged(Location location) {
        Log.d(LOG_TAG, "Location update received: " + location.toString());

        Intent intent = new Intent()
                .setAction(ACTION_LOCATION_CHANGED)
                .putExtra(EXTRA_LOCATION, location);

        sendBroadcast(intent);
    }

    public abstract int toggleLocationUpdates();

    protected abstract void startLocationUpdates();
    protected abstract void stopLocationUpdates();

    protected boolean checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "Location permission not granted");
            // TODO (re)ask the user for permission
            return false;
        }

        return true;
    }

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
