package com.example.google.whererunner.services;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;

public abstract class LocationService extends Service {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = LocationService.class.getSimpleName();

    public final static String ACTION_LOCATION_CHANGED = "LOCATION_CHANGED";
    public final static String EXTRA_LOCATION = "LOCATION";
    public final static String ACTION_CONNECTIVITY_LOST = "CONNECTIVITY_LOST";

    protected static final int LOCATION_UPDATE_INTERVAL_MS = 1000;
    private static final int LOCATION_UPDATE_INTERVAL_TIMEOUT_MS = 10000;
    private static final int LOCATION_ACCURACY_MAX_METERS = 25;

    protected boolean mIsLocationUpdating = false;

    private CountDownTimer mLocationSampleTimer;

    @Override
    public void onDestroy() {
        stopLocationUpdates();

        if (mLocationSampleTimer != null) {
            mLocationSampleTimer.cancel();
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        startLocationUpdates();

        return null;
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
}
