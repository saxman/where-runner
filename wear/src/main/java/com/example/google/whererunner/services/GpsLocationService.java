package com.example.google.whererunner.services;

import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

public class GpsLocationService extends LocationService {

    private static final String LOG_TAG = GpsLocationService.class.getSimpleName();

    public final static String ACTION_GPS_STATUS_CHANGED = "GPS_STATUS_CHANGED";

    public final static String EXTRA_GPS_STATUS = "GPS_STATUS";
    public final static String EXTRA_GPS_TTFF = "GPS_TTFF";
    public final static String EXTRA_GPS_SATELLITES = "GPS_SATELLITES";

    private static final float GPS_MIN_DISTANCE = 0;

    private GpsStatus mGpsStatus;
    private LocationManager mLocationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        startLocationUpdates();
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }

    @Override
    protected void startLocationUpdates() {
        if (checkPermission()) {
            mLocationManager.addGpsStatusListener(mGpsStatusListener);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL_MS, GPS_MIN_DISTANCE, mLocationListener);
            mIsLocationUpdating = true;
        }
    }

    @Override
    protected void stopLocationUpdates() {
        if (checkPermission()) {
            mLocationManager.removeUpdates(mLocationListener);
            mLocationManager.removeGpsStatusListener(mGpsStatusListener);
            mIsLocationUpdating = false;
        }
    }

    private GpsStatus.Listener mGpsStatusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int status) {
            mGpsStatus = mLocationManager.getGpsStatus(mGpsStatus);

            Intent intent = new Intent(ACTION_GPS_STATUS_CHANGED);
            intent.putExtra(EXTRA_GPS_STATUS, status);

            switch (status) {
                case GpsStatus.GPS_EVENT_STARTED:
                case GpsStatus.GPS_EVENT_STOPPED:
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    int i = mGpsStatus.getTimeToFirstFix();
                    intent.putExtra(EXTRA_GPS_TTFF, i);
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    int s = 0;

                    for (GpsSatellite satellite : mGpsStatus.getSatellites()) {
                        s++;
                    }

                    intent.putExtra(EXTRA_GPS_SATELLITES, s);
                    break;
            }

            LocalBroadcastManager.getInstance(GpsLocationService.this).sendBroadcast(intent);
        }
    };

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            GpsLocationService.this.onLocationChanged(location);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {}

        @Override
        public void onProviderEnabled(String s) {}

        @Override
        public void onProviderDisabled(String s) {}
    };
}
