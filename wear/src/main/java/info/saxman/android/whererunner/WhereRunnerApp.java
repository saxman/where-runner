package info.saxman.android.whererunner;

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WhereRunnerApp extends Application {
    public static final String LOG_TAG = WhereRunnerApp.class.getSimpleName();

    private static WhereRunnerApp sInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Locale.getDefault().equals(Locale.US)) {
            PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.prefs_settings_us, false);
        }

        sInstance = this;
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

        String metric = sInstance.getString(R.string.pref_units_metric);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                sInstance.getApplicationContext());
        String units = sharedPrefs.getString(sInstance.getString(R.string.pref_units), metric);

        if (units.equals(metric)) {
            if (distance < 1000) {
                String meters = sInstance.getString(R.string.format_distance_meters);
                string = String.format(Locale.getDefault(), meters, distance);
            } else {
                String kms = sInstance.getString(R.string.format_distance_kms);
                string = String.format(Locale.getDefault(), kms, distance / 1000);
            }
        } else {
            String miles = sInstance.getString(R.string.format_distance_miles);
            string = String.format(Locale.getDefault(), miles, distance * 0.000621371192);
        }

        return string;
    }

    /** Convert speed in m/mAnimationDurationMs as km/h or mi/h, per the user's preferred units */
    private static double getLocalizedSpeed(float speed) {
        // kph = m/mAnimationDurationMs * km/m * mAnimationDurationMs/s * s/hr = speed * 1/1000 * 1000/1 * 3600/1
        double s = speed * 3600;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                sInstance.getApplicationContext());
        String units = sharedPrefs.getString(sInstance.getString(R.string.pref_units),
                sInstance.getString(R.string.pref_units_metric));

        if (units.equals("us")) {
            s *= 0.621371192;
        }

        return s;
    }

    public static String getLocalizedDistanceLabel() {
        String metric = sInstance.getString(R.string.pref_units_metric);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                sInstance.getApplicationContext());
        String units = sharedPrefs.getString(sInstance.getString(R.string.pref_units),
                sInstance.getString(R.string.pref_units_metric));

        if (units.equals(metric)) {
            return sInstance.getString(R.string.distance_metric);
        } else {
            return sInstance.getString(R.string.distance_us_customary);
        }
    }

    public static String getLocalizedSpeedLabel() {
        String metric = sInstance.getString(R.string.pref_units_metric);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                sInstance.getApplicationContext());
        String units = sharedPrefs.getString(sInstance.getString(R.string.pref_units),
                sInstance.getString(R.string.pref_units_metric));

        if (units.equals(metric)) {
            return sInstance.getString(R.string.speed_metric);
        } else {
            return sInstance.getString(R.string.speed_us_customary);
        }
    }

    public static String getLocalizedPaceLabel() {
        String metric = sInstance.getString(R.string.pref_units_metric);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                sInstance.getApplicationContext());
        String units = sharedPrefs.getString(sInstance.getString(R.string.pref_units),
                sInstance.getString(R.string.pref_units_metric));

        if (units.equals(metric)) {
            return sInstance.getString(R.string.pace_metric);
        } else {
            return sInstance.getString(R.string.pace_us_customary);
        }
    }

    public static String formatSpeed(float speed) {
        if (speed >= 100) {
            return DecimalFormatSymbols.getInstance().getInfinity();
        }

        String format = sInstance.getString(R.string.format_speed);
        return String.format(Locale.getDefault(), format, getLocalizedSpeed(speed));
    }

    public static String formatPace(float speed) {
        // convert to mins / km or mins / mile
        double pace;
        if (speed == 0) {
            pace = 0; // avoid divide by zero
        } else {
            pace = 60 / getLocalizedSpeed(speed);
        }

        if (pace >= 100) {
            return DecimalFormatSymbols.getInstance().getInfinity();
        }

        int mins = (int) pace;
        int secs = (int) (60 * (pace - mins));

        String format = sInstance.getString(R.string.format_pace);
        return String.format(Locale.getDefault(), format, mins, secs);
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
