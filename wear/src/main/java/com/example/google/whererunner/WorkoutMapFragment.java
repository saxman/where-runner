package com.example.google.whererunner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.LocationService;
import com.example.google.whererunner.services.WorkoutRecordingService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.LinkedList;

public class WorkoutMapFragment extends WearableFragment implements OnMapReadyCallback {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutMapFragment.class.getSimpleName();

    private GoogleMap mGoogleMap;
    private MapView mMapView;

    private Marker mMapMarker;
    private Polyline mPolyline;
    private Circle mAccuracyCircle;

    private GoogleApiClient mGoogleApiClient;

    private boolean mIsMapInitialized = false;

    private BroadcastReceiver mBroadcastReceiver;

    private LinkedList<LatLng> mPathLatLngs = new LinkedList<>();

    private BitmapDescriptor mRecordingMapMarkerIcon;
    private BitmapDescriptor mDefaultMapMarkerIcon;
    private BitmapDescriptor mDisconnectedMapMarkerIcon;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Only attempt to get the user's last location from the FLP if we don't already know it
        if (LocationService.lastKnownLocation == null && mGoogleApiClient == null) {
            GoogleApiClientCallbacks callbacks = new GoogleApiClientCallbacks();

            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(callbacks)
                    .addOnConnectionFailedListener(callbacks)
                    .build();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_workout_map, container, false);

        mMapView = (MapView) view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (LocationService.lastKnownLocation == null && mGoogleApiClient == null) {
            Log.d(LOG_TAG, "Using LocationServices to get user's last known location");
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mMapView.onResume();

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case LocationService.ACTION_LOCATION_CHANGED:
                        Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);

                        if (!isAmbient()) {
                            updateMapMarkerLocation(location);
                            updateMapMarkerAccuracy(location);
                            updateMapCenter(location);

                            // If the location was previously not-fixed, change the marker icon
                            if (!LocationService.isLocationUpdating) {
                                updateMapMarkerIcon();
                            }
                        }

                        break;

                    case LocationService.ACTION_CONNECTIVITY_CHANGED:
                        if (!isAmbient()) {
                            updateMapMarkerAccuracy(LocationService.lastKnownLocation);
                            updateMapMarkerIcon();
                        }

                        break;

                    case WorkoutRecordingService.ACTION_WORKOUT_DATA_UPDATED:
                        if (!isAmbient()) {
                            updateMapPolyline();
                        }

                        break;

                    case WorkoutRecordingService.ACTION_RECORDING_STATUS_CHANGED:
                        if (WorkoutRecordingService.isRecording) {
                            mPathLatLngs.clear();
                            mPolyline.setPoints(mPathLatLngs);

                            if (!isAmbient()) {
                                updateMapPolyline();
                            }
                        }

                        if (!isAmbient()) {
                            updateMapMarkerIcon();
                        }

                        break;
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);
        intentFilter.addAction(LocationService.ACTION_CONNECTIVITY_CHANGED);
        intentFilter.addAction(WorkoutRecordingService.ACTION_RECORDING_STATUS_CHANGED);
        intentFilter.addAction(WorkoutRecordingService.ACTION_WORKOUT_DATA_UPDATED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onLowMemory() {
        mMapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);

        super.onStop();
    }

    //
    // OnMapReadyCallback method
    //

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        mRecordingMapMarkerIcon = WhereRunnerApp.loadDrawable(R.drawable.map_marker_recording);
        mDefaultMapMarkerIcon = WhereRunnerApp.loadDrawable(R.drawable.map_marker);
        mDisconnectedMapMarkerIcon = WhereRunnerApp.loadDrawable(R.drawable.map_marker_disconnected);

        mMapMarker = mGoogleMap.addMarker(
                new MarkerOptions()
                        .visible(false)
                        .position(new LatLng(0, 0))
                        .icon(mDefaultMapMarkerIcon)
                        .anchor(0.5f, 0.5f));

        mPolyline = mGoogleMap.addPolyline(new PolylineOptions()
                .width(5)
                .color(getActivity().getColor(R.color.highlight)));

        mAccuracyCircle = mGoogleMap.addCircle(new CircleOptions()
                .center(new LatLng(0, 0))
                .radius(0)
                .strokeWidth(0)
                .fillColor(getResources().getColor(R.color.location_accuracy_circle, null))
                .visible(false));

        updateUI();
    }

    //
    // WearableFragment methods
    //

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        mMapView.onEnterAmbient(ambientDetails);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        mMapView.onExitAmbient();
        updateUI();
    }

    @Override
    public void onUpdateAmbient() {
        updateUI();
    }

    //
    // Class methods
    //

    /**
     * Update all map facets, including the marker, polyline, and center
     */
    private void updateUI() {
        // Map not ready yet. Once it is, updateUI() will be called again
        if (mGoogleMap == null) {
            return;
        }

        Location location = LocationService.lastKnownLocation;

        updateMapCenter(location);
        updateMapMarkerLocation(location);
        updateMapMarkerAccuracy(location);
        updateMapMarkerIcon();
        updateMapPolyline();
    }

    /**
     * Update the map marker icon, position, and accuracy circle
     */
    private void updateMapMarkerLocation(Location location) {
        if (mMapMarker == null || location == null) {
            return;
        }

        if (location != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMapMarker.setPosition(latLng);

            if (!mMapMarker.isVisible()) {
                mMapMarker.setVisible(true);
            }
        }
    }

    private void updateMapMarkerAccuracy(Location location) {
        if (mAccuracyCircle == null || location == null) {
            return;
        }

        if (LocationService.isLocationUpdating) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mAccuracyCircle.setCenter(latLng);
            mAccuracyCircle.setRadius(location.getAccuracy());

            if (!mAccuracyCircle.isVisible()) {
                mAccuracyCircle.setVisible(true);
            }
        } else {
            if (mAccuracyCircle.isVisible()) {
                mAccuracyCircle.setVisible(false);
            }

        }
    }

    private void updateMapMarkerIcon() {
        if (mAccuracyCircle == null) {
            return;
        }

        if (LocationService.isLocationUpdating) {
            if (WorkoutRecordingService.isRecording) {
                mMapMarker.setIcon(mRecordingMapMarkerIcon);
            } else {
                mMapMarker.setIcon(mDefaultMapMarkerIcon);
            }
        } else {
            mMapMarker.setIcon(mDisconnectedMapMarkerIcon);
        }
    }

    /**
     * Update the map polyline with new location samples
     */
    private void updateMapPolyline() {
        if (mPolyline == null || mPathLatLngs.size() == WorkoutRecordingService.locationSamples.size()) {
            return;
        }

        // Add any new location samples to the polyline
        for (int i = mPathLatLngs.size(); i < WorkoutRecordingService.locationSamples.size(); i++) {
            Location location = WorkoutRecordingService.locationSamples.get(i);
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mPathLatLngs.add(latLng);
        }

        mPolyline.setPoints(mPathLatLngs);
    }

    /**
     * Re-center the map to the appropriate location. Will animate the move if we're actively receiving location samples
     */
    private void updateMapCenter(Location location) {
        // Map not ready yet. This method will be called again once the map is ready.
        if (mGoogleMap == null) {
            return;
        }

        if (!mIsMapInitialized) {
            // Choose the map's starting position
            LatLng latLng;

            if (location == null) {
                // If we don't have a decent location sample yet, move to some place interesting
                latLng = new LatLng(37.422393, -122.083964);
            } else {
                latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mIsMapInitialized = true;
            }

            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        } else {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    private void getLastLocation() {
        // If the user hasn't granted fine location permission (handled by MainActivity), don't bother asking for their last location
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (location != null) {
            if (!isAmbient()) {
                updateMapCenter(location);
                updateMapMarkerLocation(location);
            }
        }
    }

    private class GoogleApiClientCallbacks implements
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnected(Bundle connectionHint) {
            getLastLocation();
        }

        @Override
        public void onConnectionSuspended(int i) {
            // TODO
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            // TODO
        }
    }
}
