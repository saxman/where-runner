package com.example.google.whererunner.framework;

import android.location.Location;

import java.util.ArrayList;

public interface RouteDataService {
    double getDistance();
    double getDuration();
    ArrayList<Location> getRoute();

    interface RouteDataUpdateListener {
        void onRouteDataUpdated(RouteDataService routeDataService);
    }
}
