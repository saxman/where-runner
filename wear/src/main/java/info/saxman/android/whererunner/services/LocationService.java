package info.saxman.android.whererunner.services;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public abstract class LocationService extends Service {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = LocationService.class.getSimpleName();

    public final static String ACTION_LOCATION_CHANGED = "LOCATION_CHANGED";
    public final static String EXTRA_LOCATION = "LOCATION";

    public final static String ACTION_CONNECTIVITY_CHANGED = "LOCATION_SENOR_CONNECTIVITY_CHANGED";
    public final static String EXTRA_IS_RECEIVING_SAMPLES = "IS_RECEIVING_LOCATION_SAMPLES";

    protected static final int LOCATION_UPDATE_INTERVAL_MS = 1000;
    private static final int LOCATION_UPDATE_INTERVAL_TIMEOUT_MS = 10000;
    private static final int LOCATION_ACCURACY_MAX_METERS = 25;

    private CountDownTimer mLocationSampleTimer;

    public static Location lastKnownLocation;
    public static boolean isReceivingAccurateLocationSamples = false;

    public static boolean isActive = false;

    @Override
    public void onCreate() {
        super.onCreate();

        isActive = true;
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();

        if (mLocationSampleTimer != null) {
            mLocationSampleTimer.cancel();
        }

        isActive = false;

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        startLocationUpdates();

        // No need to return an IBinder instance since binding is only used to controls service
        // lifecycle, and isn't used for direct access.
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

        lastKnownLocation = location;

        Intent intent = new Intent(ACTION_LOCATION_CHANGED).putExtra(EXTRA_LOCATION, location);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        if (!isReceivingAccurateLocationSamples) {
            isReceivingAccurateLocationSamples = true;

            intent = new Intent(ACTION_CONNECTIVITY_CHANGED);
            intent.putExtra(EXTRA_IS_RECEIVING_SAMPLES, true);

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }

        // Since we received an accurate location samples, cancel the existing timer
        if (mLocationSampleTimer != null) {
            mLocationSampleTimer.cancel();
        }

        // Start a timer to detect if we're not receiving location samples in regular intervals
        mLocationSampleTimer = new CountDownTimer(
                LocationService.LOCATION_UPDATE_INTERVAL_TIMEOUT_MS,
                LocationService.LOCATION_UPDATE_INTERVAL_TIMEOUT_MS) {

            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                isReceivingAccurateLocationSamples = false;

                Intent intent = new Intent(ACTION_CONNECTIVITY_CHANGED);
                intent.putExtra(EXTRA_IS_RECEIVING_SAMPLES, false);

                LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(intent);
            }
        }.start();
    }

    protected abstract void startLocationUpdates();
    protected abstract void stopLocationUpdates();

    protected boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
