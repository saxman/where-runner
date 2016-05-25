package com.example.google.whererunner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.LocationService;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RouteMapFragment extends WearableFragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks {

    private static final String LOG_TAG = RouteMapFragment.class.getSimpleName();

    private GoogleMap mGoogleMap;
    private MapView mMapView;

    private Marker mMapMarker;
    private Polyline mPolyline;

    private GoogleApiClient mGoogleApiClient;

    private Location mInitialLocation = null;
    private Location mLastLocation = null;
    private boolean mIsRecording = false;

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1;

    private BroadcastReceiver mLocationChangedReceiver;

    private List<Location> mPathLocations = new ArrayList<>();
    private LinkedList<LatLng> mPathCoords;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_route, container, false);

        mMapView = (MapView) view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
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
                    mIsRecording = intent.getBooleanExtra(LocationService.EXTRA_IS_RECORDING, false);
                    Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);

                    if (mIsRecording) {
                        mPathLocations.add(location);
                    }

                    mLastLocation = location;
                    updateUI();
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter(LocationService.ACTION_LOCATION_CHANGED);
        getActivity().registerReceiver(mLocationChangedReceiver, intentFilter);
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
        getActivity().unregisterReceiver(mLocationChangedReceiver);
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation();
                }
                break;
        }
    }

    //
    // OnMapReadyCallback method
    //

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
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
    }

    @Override
    public void onUpdateAmbient() {
        updateUI();
    }

    //
    // GoogleApiClient methods
    //

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getLastLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // TODO
    }

    //
    // Class methods
    //

    private void updateUI() {
        // Map not ready yet. Once it is, updateUI() will be called again
        if (mGoogleMap == null) {
            Log.d(LOG_TAG, "Deferring map UI update. Map not ready");
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
            if (mMapMarker == null) {
                mMapMarker = mGoogleMap.addMarker(new MarkerOptions().position(lastLatLng));
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(lastLatLng));
            } else {
                mMapMarker.setPosition(lastLatLng);
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(lastLatLng));
            }
        }

        // If we're recording and we have path data, render it
        if (mIsRecording && mPathLocations.size() > 0) {
            // If we haven't rendered the path polyline yet, create the polyline and re-create
            // the collection for it's coordinates
            // TODO move to onmapready
            if (mPolyline == null) {
                mPolyline = mGoogleMap.addPolyline(new PolylineOptions()
                        .width(5)
                        .color(Color.RED));

                mPathCoords = new LinkedList<>();
            }

            // Update the polyline if we have new location samples
            if (mPathLocations.size() > mPathCoords.size()) {
                // Add the new location samples from mPathLocations to the polyline
                for (int i = mPathCoords.size(); i < mPathLocations.size(); i++) {
                    Location location = mPathLocations.get(i);
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mPathCoords.add(latLng);
                }

                mPolyline.setPoints(mPathCoords);
            }
        }

        // If we're no longer recording, ensure the path polyline has been removed
        if (!mIsRecording && mPolyline != null) {
            mPolyline.remove();
            mPolyline = null;
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (location != null) {
            Log.d(LOG_TAG, "Last known location: " + location.toString());
            mInitialLocation = location;
            updateUI();
        } else {
            Log.w(LOG_TAG, "Unable to retrieve user's last known location");
        }
    }
}
