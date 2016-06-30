package com.example.google.whererunner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
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

    private Location mLastLocation = null;

    private boolean mIsRecording = false;
    private boolean mIsLocationFixed = false;

    private BroadcastReceiver mLocationChangedReceiver;

    private ArrayList<Location> mPathLocations;
    private LinkedList<LatLng> mPathPolylineLatLngs = new LinkedList<>();

    private BitmapDescriptor mRecordingMapMarkerIcon;
    private BitmapDescriptor mDefaultMapMarkerIcon;
    private BitmapDescriptor mDisconnectedMapMarkerIcon;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_workout_map, container, false);

        mIsRecording = WorkoutRecordingService.isRecording;
        mPathLocations = WorkoutRecordingService.locationSamples;

        // If we have a historic location sample, use it to center the map, but don't assume we have a location fix
        if (mPathLocations.size() > 0) {
            // TODO if the sample isn't too old, we can assume we have a location fix?
            mIsLocationFixed = false;
            mLastLocation = mPathLocations.get(mPathLocations.size() - 1);
        }

        mMapView = (MapView) view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        if (mGoogleApiClient == null) {
            GoogleApiClientCallbacks callbacks = new GoogleApiClientCallbacks();

            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(callbacks)
                    .addOnConnectionFailedListener(callbacks)
                    .build();
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();

        mMapView.onResume();

        if (mLocationChangedReceiver == null) {
            mLocationChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case LocationService.ACTION_LOCATION_CHANGED:
                            mIsLocationFixed = true;
                            mLastLocation = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);

                            if (!isAmbient()) {
                                updateMapMarker();
                                updateMapCenter();
                            }

                            break;

                        case LocationService.ACTION_CONNECTIVITY_LOST:
                            // We haven't received a location sample in too long, so update the UI
                            // to reflect poor connectivity
                            mIsLocationFixed = false;

                            if (!isAmbient()) {
                                updateMapMarker();
                            }

                            break;

                        case WorkoutRecordingService.ACTION_WORKOUT_DATA_UPDATED:
                            if (!isAmbient()) {
                                updateMapPolyline();
                            }

                            break;

                        case WorkoutRecordingService.ACTION_RECORDING_STATUS:
                            mIsRecording = intent.getBooleanExtra(WorkoutRecordingService.EXTRA_IS_RECORDING, false);

                            if (!isAmbient()) {
                                updateMapMarkerIcon();
                            }

                            break;
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);
        intentFilter.addAction(LocationService.ACTION_CONNECTIVITY_LOST);
        intentFilter.addAction(WorkoutRecordingService.ACTION_RECORDING_STATUS);
        intentFilter.addAction(WorkoutRecordingService.ACTION_WORKOUT_DATA_UPDATED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mLocationChangedReceiver, intentFilter);
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
        mGoogleApiClient.disconnect();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mLocationChangedReceiver);

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
                .color(getActivity().getColor(R.color.highlight))
                .visible(false));

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

        updateMapMarker();
        updateMapPolyline();
        updateMapCenter();
    }

    /**
     * Update the map marker icon, position, and accuracy circle
     */
    private void updateMapMarker() {
        // Map (and marker) not ready yet. updateMapMarker() will be called again once the map is ready.
        if (mGoogleMap == null) {
            return;
        }

        updateMapMarkerIcon();

        if (mLastLocation != null) {
            LatLng lastLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mMapMarker.setPosition(lastLatLng);

            if (!mMapMarker.isVisible()) {
                mMapMarker.setVisible(true);
            }

            if (mIsLocationFixed) {
                // Update the location accuracy circle and ensure that's it's visible, as it's initially
                // hidden, and is hidden when the accuracy falls below the threshold
                mAccuracyCircle.setCenter(lastLatLng);
                mAccuracyCircle.setRadius(mLastLocation.getAccuracy());
                mAccuracyCircle.setVisible(true);
            } else {
                mAccuracyCircle.setVisible(false);
            }
        }
    }

    private void updateMapMarkerIcon() {
        if (mIsRecording) {
            mMapMarker.setIcon(mRecordingMapMarkerIcon);
        } else if (mIsLocationFixed) {
            mMapMarker.setIcon(mDefaultMapMarkerIcon);
        } else {
            mMapMarker.setIcon(mDisconnectedMapMarkerIcon);
        }
    }

    /**
     * Update the map polyline with new location samples
     */
    private void updateMapPolyline() {
        // Map (and polyline) not ready yet. updateMapPolyline() will be called again once the map is ready.
        if (mGoogleMap == null) {
            return;
        }

        for (int i = mPathPolylineLatLngs.size(); i < mPathLocations.size(); i++) {
            Location location = mPathLocations.get(i);
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mPathPolylineLatLngs.add(latLng);
        }

        mPolyline.setPoints(mPathPolylineLatLngs);

        // If the polyline is still invisible, display it
        if (!mPolyline.isVisible()) {
            mPolyline.setVisible(true);
        }
    }

    /**
     * Re-center the map to the appropriate location. Will animate the move if we're actively receiving location samples
     */
    private void updateMapCenter() {
        // Map not ready yet. updateMapCenter() will be called again once the map is ready.
        if (mGoogleMap == null) {
            return;
        }

        // If we don't have a location sample, move the camera to someplace interesting
        if (mLastLocation == null) {
            LatLng latLng = new LatLng(37.422393, -122.083964);
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            return;
        }

        if (!mIsLocationFixed) {
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        } else {
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
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
            mLastLocation = location;
            mIsLocationFixed = false;

            if (!isAmbient()) {
                updateUI();
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
