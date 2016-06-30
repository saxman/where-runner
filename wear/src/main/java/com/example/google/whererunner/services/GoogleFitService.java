package com.example.google.whererunner.services;

import android.app.Fragment;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;

public class GoogleFitService extends Service {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = HeartRateSensorService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    private BroadcastReceiver mRecordingReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        setupRecordingListener();
        setupGoogleApiClient();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // No need to return an IBinder instance since binding is only used to controls service
        // lifecycle, and isn't used for direct access
        return null;
    }

    private void setupGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.RECORDING_API)
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.i(LOG_TAG, "Connected!!1");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.i(LOG_TAG, "Connection suspended :(");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.i(LOG_TAG, "Unable to connect to Google Fit - shutting down service");
                        // TODO: Shut down the service here; check what this does to the binding!!
                    }
                })
                .build();
    }

    private void setupRecordingListener() {
        // Set up the recording listener
        mRecordingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case WorkoutRecordingService.ACTION_START_RECORDING:
                        Log.i(LOG_TAG, "Recording start event received in Google Fit service");
                        break;

                    case WorkoutRecordingService.ACTION_STOP_RECORDING:
                        Log.i(LOG_TAG, "Recording stop event received in Google Fit service");
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(WorkoutRecordingService.ACTION_START_RECORDING);
        filter.addAction(WorkoutRecordingService.ACTION_STOP_RECORDING);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRecordingReceiver, filter);
    }

}
