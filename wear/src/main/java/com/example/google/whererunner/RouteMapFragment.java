package com.example.google.whererunner;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.google.whererunner.framework.RouteDataService;
import com.example.google.whererunner.framework.WearableFragment;
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

public class RouteMapFragment extends WearableFragment
        implements OnMapReadyCallback, RouteDataService.RouteDataUpdateListener {

    private static final String LOG_TAG = RouteMapFragment.class.getSimpleName();

    private GoogleMap mGoogleMap;
    private MapView mMapView;

    private Marker mMapMarker;
    private Polyline mPolyline;

    private LinkedList<LatLng> mRouteCoords = new LinkedList<>();

    private int mNextRouteLocationIndex = 0;

    private LatLng mLastLocation = null;
    private RouteDataService mRouteDataService;

    private static final String PARAM_LAST_LOCATION = "lastLocation";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_route, container, false);

        if (savedInstanceState != null) {
            mLastLocation = savedInstanceState.getParcelable(PARAM_LAST_LOCATION);
        }

        mMapView = (MapView) view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelable(PARAM_LAST_LOCATION, mLastLocation);
    }

    @Override
    public void onResume() {
        mMapView.onResume();
        super.onResume();
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
    public void onMapReady(GoogleMap googleMap) {
        Log.d(LOG_TAG, "Map ready");
        mGoogleMap = googleMap;
        updateUI();
    }

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

    @Override
    public void onRouteDataUpdated(RouteDataService routeDataService) {
        mRouteDataService = routeDataService;

        if (!isAmbient()) {
            updateUI();
        }
    }

    private void updateUI() {
        // Map not ready yet. Once it is, updateuI() will be called again
        if (mGoogleMap == null) {
            Log.d(LOG_TAG, "Deferring map UI update. Map not ready");
            return;
        }

        // No route data yet...
        if (mRouteDataService == null) {
            Log.d(LOG_TAG, "Deferring map UI update. No route data");
            return;
        }

        ArrayList<Location> route = mRouteDataService.getRoute();

        // Nothing route to display yet
        if (route.size() == 0) {
            // If we have last known location, center map there instead of the user's current location
            Location loc = mRouteDataService.getInitialLocation();
            if (loc != null) {
                mLastLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
                setMapCenter(mLastLocation);
            }

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
}
