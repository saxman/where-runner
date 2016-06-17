package com.example.google.whererunner;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

public class WhereRunnerApp extends Application {
    public static final String ACTION_SETTINGS_CHANGED = "SETTINGS_CHANGED";

    private static final String SHARED_PREFS = "com.example.google.whererunner.SHARED_PREFS";

    public static final String PREF_MAP_ACCURACY_CIRCLE = "MAP_ACCURACY_CIRCLE";
    public static final String PREF_WEAR_GPS_ONLY = "WEAR_GPS_ONLY";
    public static final String PREF_VIBRATE_NO_SIGNAL = "VIBRATE_NO_SIGNAL";
    public static final String PREF_NO_SIGNAL_TIMEOUT = "NO_SIGNAL_TIMEOUT";
    public static final String PREF_LOCATION_ACCURACY_TIMEOUT = "LOCATION_ACCURACY_TIMEOUT";
    public static final String PREF_PHONE_CONNECTIVITY_STATUS = "PHONE_CONNECTIVITY_STATUS";

    private SharedPreferences mSharedPrefs;

    private static WhereRunnerApp sInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;
        mSharedPrefs = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
    }

    public static void storeUserPreference(String pref, String value) {
        SharedPreferences.Editor editor = sInstance.mSharedPrefs.edit();
        editor.putString(pref, value);
        editor.commit();

        // Notify listeners that that the user preferences have changed
        Intent intent = new Intent(ACTION_SETTINGS_CHANGED);
        LocalBroadcastManager.getInstance(sInstance).sendBroadcast(intent);
    }

    public static String retrieveUserPreference(String pref) {
        return sInstance.mSharedPrefs.getString(pref, "");
    }
}
