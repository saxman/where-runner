package com.example.google.whererunner;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.concurrent.TimeUnit;

public class WhereRunnerApp extends Application {
    public static final String LOG_TAG = WhereRunnerApp.class.getSimpleName();

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
        editor.apply();

        // Notify listeners that that the user preferences have changed
        Intent intent = new Intent(ACTION_SETTINGS_CHANGED);
        LocalBroadcastManager.getInstance(sInstance).sendBroadcast(intent);
    }

    public static String retrieveUserPreference(String pref) {
        return sInstance.mSharedPrefs.getString(pref, "");
    }

    public static BitmapDescriptor loadDrawable(int id) {
        Drawable circle = sInstance.getResources().getDrawable(id, null);

        int width = 24;
        int height = 24;

        try {
            width = circle.getIntrinsicWidth();
            height = circle.getIntrinsicHeight();
        } catch (NullPointerException e) {
            Log.w(LOG_TAG, "Drawable must have intrinsic width and height. Using arbitrary default of 24");
        }

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        circle.setBounds(0, 0, width, height);
        circle.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public static long[] millisToHoursMinsSecs(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);

        return new long[]{hours, minutes, seconds, millis};
    }
}
