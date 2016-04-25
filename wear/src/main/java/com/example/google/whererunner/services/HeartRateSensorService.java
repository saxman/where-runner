package com.example.google.whererunner.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class HeartRateSensorService extends Service{

    public static final String ACTION_HEART_RATE_CHANGED = "ACTION_HEART_RATE_CHANGED";

    @Nullable
    @Override
    public IBinder onBind (Intent intent) {
        return null;
    }
}
