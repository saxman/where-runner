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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.LocationService;
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
import java.util.List;

public class WorkoutMapFragment extends WearableFragment implements OnMapReadyCallback {

    private static final String LOG_TAG = WorkoutMapFragment.class.getSimpleName();

    private GoogleMap mGoogleMap;
    private MapView mMapView;

    private Marker mMapMarker;
    private Polyline mPolyline;
    private Circle mAccuracyCircle;

    private GoogleApiClient mGoogleApiClient;

    private Location mInitialLocation = null;
    private Location mLastLocation = null;
    private boolean mIsRecording = false;
    private boolean mIsLocationFixed = false;

    private BroadcastReceiver mLocationChangedReceiver;

    private List<Location> mPathLocations = new ArrayList<>();
    private LinkedList<LatLng> mPathPolylineLatLngs = new LinkedList<>();

    private BitmapDescriptor mRecordingMapMarkerIcon;
    private BitmapDescriptor mDefaultMapMarkerIcon;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_workout_map, container, false);

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
                            Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);
                            mLastLocation = location;
                            mIsLocationFixed = true;

                            if (mIsRecording) {
                                mPathLocations.add(location);
                            }

                            break;
                        case LocationService.ACTION_RECORDING_STATUS_CHANGED:
                            mIsRecording = intent.getBooleanExtra(LocationService.EXTRA_IS_RECORDING, false);
                            setMapMarkerIcon();

                            if (!mIsRecording) {
                                // Ensure that the prior location samples are cleared out
                                mPathLocations.clear();
                            }

                            break;
                        case LocationService.ACTION_RECORDING_STATUS:
                            mIsRecording = intent.getBooleanExtra(LocationService.EXTRA_IS_RECORDING, false);
                            setMapMarkerIcon();
                            break;
                        case LocationService.ACTION_CONNECTIVITY_LOST:
                            // We haven't received a location sample in too long, so hide the accuracy circle
                            mIsLocationFixed = false;
                            break;
                    }

                    if (!isAmbient()) {
                        updateUI();
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);
        intentFilter.addAction(LocationService.ACTION_RECORDING_STATUS);
        intentFilter.addAction(LocationService.ACTION_RECORDING_STATUS_CHANGED);
        intentFilter.addAction(LocationService.ACTION_CONNECTIVITY_LOST);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mLocationChangedReceiver, intentFilter);

        Intent intent = new Intent(LocationService.ACTION_REPORT_RECORDING_STATUS);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
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

        mRecordingMapMarkerIcon = loadDrawable(R.drawable.map_marker_recording);
        mDefaultMapMarkerIcon = loadDrawable(R.drawable.map_marker);

        mMapMarker = mGoogleMap.addMarker(
                new MarkerOptions()
                        .visible(false)
                        .position(new LatLng(0, 0))
                        .icon(mDefaultMapMarkerIcon)
                        .anchor(0.5f, 0.5f));

        setMapMarkerIcon();
        
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

    // TODO break this method into multiple UI update methods. e.g. updatePath, updateMapLocation, etc
    private void updateUI() {
        // Map not ready yet. Once it is, updateUI() will be called again
        if (mGoogleMap == null) {
            return;
        }

        // If we have the user's last known location, but not their precise location,
        // re-center the map on their last know location
        if (mInitialLocation != null && mLastLocation == null) {
            LatLng latLng = new LatLng(mInitialLocation.getLatitude(), mInitialLocation.getLongitude());
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            return;
        }

        // If the location service has given us the user's precise location,
        // place a marker there and re-center the map
        if (mLastLocation != null) {
            // If we haven't displayed the user's current location yet, create the marker.
            // Otherwise, move the pre-existing marker
            LatLng lastLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mMapMarker.setPosition(lastLatLng);

            // If we haven't displayed the marker yet (i.e. we didn't have precise location
            // previously, move the map to it's proper place and display the marker
            if (!mMapMarker.isVisible()) {
                mMapMarker.setVisible(true);
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(lastLatLng));
            } else {
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(lastLatLng));
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

        // If we're recording and we have new path data, update the path polyline
        if (mIsRecording && mPathLocations.size() > mPathPolylineLatLngs.size()) {
            // Add the new location samples from the location service to the polyline
            for (int i = mPathPolylineLatLngs.size(); i < mPathLocations.size(); i++) {
                Location location = mPathLocations.get(i);
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mPathPolylineLatLngs.add(latLng);
            }

            mPolyline.setPoints(mPathPolylineLatLngs);

            // If we haven't
            if (!mPolyline.isVisible()) {
                mPolyline.setVisible(true);
            }
        }

        // If we're no longer recording, hide the path polyline and clear out its path data
        if (!mIsRecording && mPolyline.isVisible()) {
            mPolyline.setVisible(false);
            mPathPolylineLatLngs.clear();
            mPolyline.setPoints(mPathPolylineLatLngs);
        }
    }

    private void setMapMarkerIcon() {
        // Map marker not ready yet. setMapMarkerIcon will be called again once the marker is ready.
        if (mMapMarker == null) {
            return;
        }

        if (mIsRecording) {
            mMapMarker.setIcon(mRecordingMapMarkerIcon);
        } else {
            mMapMarker.setIcon(mDefaultMapMarkerIcon);
        }
    }

    private void getLastLocation() {
        // If the user hasn't granted fine location permission (handled by MainActivity), don't bother asking for their last location
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (location != null) {
            mInitialLocation = location;

            if (!isAmbient()) {
                updateUI();
            }
        }
    }

    // TODO move to util class? run in background thread?
    private BitmapDescriptor loadDrawable(int id) {
        Drawable circle = getResources().getDrawable(id, null);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(circle.getIntrinsicWidth(), circle.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        circle.setBounds(0, 0, circle.getIntrinsicWidth(), circle.getIntrinsicHeight());
        circle.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private class GoogleApiClientCallbacks implements
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnected(@NonNull Bundle connectionHint) {
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
