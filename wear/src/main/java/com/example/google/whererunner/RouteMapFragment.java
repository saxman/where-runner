package com.example.google.whererunner;

import android.app.Activity;
import android.content.Context;
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

    private RouteDataService mRouteDataService;

    private GoogleMap mGoogleMap;
    private MapView mMapView;

    private Marker mMapMarker;
    private Polyline mPolyline;

    private LinkedList<LatLng> mRouteCoords = new LinkedList<>();

    private int mNextRouteIndex = 0;

    private LatLng mInitialLocation = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_route, container, false);

        mMapView = (MapView) view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        return view;
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        setRouteDataService(activity);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        setRouteDataService(context);
    }

    private void setRouteDataService(Context context) {
        try {
            mRouteDataService = (RouteDataService) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement RouteDataService");
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        // If we already have route data, use it. Otherwise, use the last known location, if known.
        if (mRouteCoords.size() > 0) {
            updateUI();
        } else if (mInitialLocation != null) {
            setMapCenter(mInitialLocation);
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        mMapView.onEnterAmbient(ambientDetails);
    }

    @Override
    public void onExitAmbient()
    {
        super.onExitAmbient();

        mMapView.onExitAmbient();
    }

    @Override
    public void onUpdateAmbient() {
        updateUI();
    }

    @Override
    public void onRouteDataUpdated() {
        if (isAmbient()) {
            return;
        }

        updateUI();
    }

    private void updateUI() {
        Log.d(LOG_TAG, "Updating map");

        // Map not ready yet. Check again next UI update.
        if (mGoogleMap == null) {
            Log.d(LOG_TAG, "Deferring map UI update. Map not ready");
            return;
        }

        ArrayList<Location> route = mRouteDataService.getRoute();

        // Nothing to display or not new data
        if (route.size() == 0 || mNextRouteIndex == route.size()) {
            return;
        }

        for (int i = mNextRouteIndex; i < route.size(); i++) {
            Location loc = route.get(i);
            mRouteCoords.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
        }

        LatLng lastLatLng = mRouteCoords.get(mRouteCoords.size() - 1);

        if (mMapMarker == null) {
            mMapMarker = mGoogleMap.addMarker(new MarkerOptions().position(lastLatLng));

            mPolyline = mGoogleMap.addPolyline(new PolylineOptions()
                    .add(lastLatLng)
                    .width(5)
                    .color(Color.RED));

            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(lastLatLng));
        } else {
            mMapMarker.setPosition(lastLatLng);
            mPolyline.setPoints(mRouteCoords);
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(lastLatLng));
        }

        mNextRouteIndex = route.size();
    }

    public void setInitialLocation(Location location) {
        mInitialLocation = new LatLng(location.getLatitude(), location.getLongitude());

        if (mGoogleMap != null) {
            setMapCenter(mInitialLocation);
        }
    }

    private void setMapCenter(LatLng latLng) {
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }
}
