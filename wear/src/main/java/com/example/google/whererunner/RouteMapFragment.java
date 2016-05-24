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

    private LinkedList<LatLng> mRouteCoords = new LinkedList<>();

    private int mNextRouteLocationIndex = 0;

    private GoogleApiClient mGoogleApiClient;

    private Location mLastLocation = null;

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1;

    private BroadcastReceiver mLocationChangedReceiver;

    private List<Location> mPathLocations = new ArrayList<>();

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
                    Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);
                    mPathLocations.add(location);
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
        Log.d(LOG_TAG, "Location permission granted"); //xxx
        switch (requestCode) {
            case REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation();
                }
                break;
        }
    }

    // OnMapReadyCallback method

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(LOG_TAG, "Map ready");
        mGoogleMap = googleMap;
        updateUI();
    }

    // WearableFragment methods

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

    // GoogleApiClient methods

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getLastLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    // Class methods

    private void updateUI() {
        // Map not ready yet. Once it is, updateUi() will be called again
        if (mGoogleMap == null) {
            Log.d(LOG_TAG, "Deferring map UI update. Map not ready");
            return;
        }

        List<Location> route = mPathLocations; // TODO XXX

        if (route.size() == 0 && mLastLocation != null) {
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            setMapCenter(latLng);
            return;
        }

        // No new data
        if (mNextRouteLocationIndex == route.size()) {
            Log.d(LOG_TAG, "Deferring map UI update. No new data");
            return;
        }

        // Append the coords for the new location samples to the route (store as LatLngs for polyline rendering
        for (int i = mNextRouteLocationIndex; i < route.size(); i++) {
            Location loc = route.get(i);
            mRouteCoords.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
        }

        LatLng lastLatLng = mRouteCoords.get(mRouteCoords.size() - 1);

        // If we haven't displayed the user's current location yet, as a marker, set that up
        if (mMapMarker == null) {
            mMapMarker = mGoogleMap.addMarker(new MarkerOptions().position(lastLatLng));
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(lastLatLng));
        } else {
            mMapMarker.setPosition(lastLatLng);
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(lastLatLng));
        }

        // If we haven't rendered the user's route yet, set that up
        if (mPolyline == null) {
            mPolyline = mGoogleMap.addPolyline(new PolylineOptions()
                    .add(lastLatLng)
                    .width(5)
                    .color(Color.RED));
        }

        mPolyline.setPoints(mRouteCoords);

        mNextRouteLocationIndex = route.size();
    }

    private void setMapCenter(LatLng latLng) {
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    private void getLastLocation() {
        Log.d(LOG_TAG, "Retrieving last know location");

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (location != null) {
            Log.d(LOG_TAG, "Last known location: " + location.toString());
            mLastLocation = location;
            updateUI();
        } else {
            Log.w(LOG_TAG, "Unable to retrieve user's last known location");
        }
    }
}
