package com.example.google.whererunner;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;

/**
 * Quick and dirty attempt to get GMSCore permissions surfaced in Wear
 */
public class GmsCorePermissionsActivity extends FragmentActivity
        implements GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient mGoogleApiClient;

    private static final String TAG = GmsCorePermissionsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a GoogleApiClient instance
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Fitness.RECORDING_API)
                .addScope(Fitness.SCOPE_ACTIVITY_READ_WRITE)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        // We're connected, therefore we're auth'd, so return to previous activity
                        Log.i(TAG, "Successfully connected to Google Fit");
                        finish();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .build();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "Error occurred connecting to Fit API: " + result);
        finish();
    }
}