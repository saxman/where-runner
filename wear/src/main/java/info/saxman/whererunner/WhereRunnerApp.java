package info.saxman.whererunner;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.util.Log;

import info.saxman.whererunner.services.WorkoutRecordingService;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WhereRunnerApp extends Application {
    public static final String LOG_TAG = WhereRunnerApp.class.getSimpleName();

    public static final String ACTION_SETTINGS_CHANGED = WhereRunnerApp.class.getPackage() + ".SETTINGS_CHANGED";

    private static final String SHARED_PREFS = WhereRunnerApp.class.getPackage() + ".SHARED_PREFS";

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

    public static String formatDistance(float distance) {
        String string;

        if (WorkoutRecordingService.workout.getDistance() < 1000) {
            String meters = sInstance.getString(R.string.format_distance_meters);
            string = String.format(Locale.getDefault(), meters, distance);
        } else {
            String kms = sInstance.getString(R.string.format_distance_kms);
            string = String.format(Locale.getDefault(), kms, distance / 1000);
        }

        return string;
    }

    public static String formatSpeed(float speed) {
        // kph = m/ms * km/m * ms/s * s/hr = speed * 1/1000 * 1000/1 * 3600/1
        String format = sInstance.getString(R.string.format_speed);
        return String.format(Locale.getDefault(), format, speed * 3600);
    }

    public static String formatDuration(long duration) {
        long[] hms = WhereRunnerApp.millisToHoursMinsSecs(duration);
        long hours = hms[0];
        long minutes = hms[1];
        long seconds = hms[2];
        long millis = hms[3];

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d.%1d", minutes, seconds, millis / 100);
        }
    }

    public static String formatDateTime(long time) {
        return DateUtils.formatDateTime(sInstance, time, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME);
    }
}
