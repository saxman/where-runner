package com.example.google.whererunner.services;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class FusedLocationService extends LocationService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String LOG_TAG = FusedLocationService.class.getSimpleName();

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();

        mLocationRequest = new LocationRequest()
                .setInterval(LOCATION_UPDATE_INTERVAL_MS)
                .setFastestInterval(LOCATION_UPDATE_INTERVAL_MS)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        startLocationUpdates();
    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        super.onDestroy();
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        // TODO
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // TODO
    }

    protected void startLocationUpdates() {
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
            return;
        }

        if (checkPermission()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mIsLocationUpdating = true;
        }
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mIsLocationUpdating = false;
        }
    }
}
