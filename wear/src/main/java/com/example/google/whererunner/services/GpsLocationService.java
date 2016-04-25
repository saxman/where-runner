package com.example.google.whererunner.services;

import android.content.Intent;
import android.location.GpsStatus;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GpsLocationService extends LocationService implements GpsStatus.Listener, LocationListener {

    private GpsStatus mGpsStatus;
    private LocationManager mLocationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        if (checkPermission()) {
            //noinspection ResourceType
            mLocationManager.removeUpdates(this);
            mLocationManager.removeGpsStatusListener(this);
        }

        super.onDestroy();
    }

    @Override
    public void onGpsStatusChanged(int i) {
        mGpsStatus = mLocationManager.getGpsStatus(mGpsStatus);

        Intent intent = new Intent();
        intent.setAction(ACTION_STATUS_CHANGED);

        intent.putExtra(EXTRA_STATUS, i);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    @Override
    protected void startLocationUpdates() {
        if (checkPermission()) {
            //noinspection ResourceType
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, this);
            mLocationManager.removeGpsStatusListener(this);
            startForeground(NOTIFICATION_ID, mNotification);
            mIsLocationUpdating = true;
        }
    }

    @Override
    protected void stopLocationUpdates() {
        if (checkPermission()) {
            //noinspection ResourceType
            mLocationManager.removeUpdates(this);
            mLocationManager.removeGpsStatusListener(this);
            stopForeground(true);
            mIsLocationUpdating = false;
        }
    }

    @Override
    public int toggleLocationUpdates() {
        if (mIsLocationUpdating) {
            stopLocationUpdates();
        } else {
            startLocationUpdates();
        }

        return mIsLocationUpdating ? LOCATION_UPDATING : LOCATION_UPDATES_STOPPED;
    }
}