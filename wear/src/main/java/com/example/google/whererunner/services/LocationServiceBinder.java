package com.example.google.whererunner.services;

import android.app.Service;
import android.os.Binder;

public class LocationServiceBinder extends Binder {
    private Service mService;

    public LocationServiceBinder(Service service) {
        mService = service;
    }

    public Service getService() {
        return mService;
    }
}
