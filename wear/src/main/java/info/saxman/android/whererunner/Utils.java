package info.saxman.android.whererunner;

import android.content.Context;
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

public class Utils {
    public static final String LOG_TAG = Utils.class.getSimpleName();

    private static Utils sInstance;
    private Context mContext;

    public static Utils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Utils(context);
        }

        return sInstance;
    }

    private Utils(Context context) {
        mContext = context.getApplicationContext();
    }

    public BitmapDescriptor bitmapDescriptorForDrawable(int id) {
        Drawable circle = mContext.getResources().getDrawable(id, null);

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

    public long[] millisToHoursMinsSecs(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);

        return new long[]{hours, minutes, seconds, millis};
    }

    public String formatDistance(float distance) {
        String string;

        String metric = mContext.getString(R.string.pref_units_metric);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                mContext.getApplicationContext());
        String units = sharedPrefs.getString(mContext.getString(R.string.pref_units), metric);

        if (units.equals(metric)) {
            if (distance < 1000) {
                String meters = mContext.getString(R.string.format_distance_meters);
                string = String.format(Locale.getDefault(), meters, distance);
            } else {
                String kms = mContext.getString(R.string.format_distance_kms);
                string = String.format(Locale.getDefault(), kms, distance / 1000);
            }
        } else {
            String miles = mContext.getString(R.string.format_distance_miles);
            string = String.format(Locale.getDefault(), miles, distance * 0.000621371192);
        }

        return string;
    }

    /**
     * Convert speed in m/s to km/h or mi/h, per the user's preferred units.
     *
     * @param speed Speed in meters / millisecond
     * @return Speed in km/h or mi/h, depending on the user's preferred units
     */
    private double getLocalizedSpeed(float speed) {
        // kph = m/ms * km/m * s/hr * ms/s = speed * 1/1000 * 3600/1 * 1000
        double s = speed * 3600;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                mContext.getApplicationContext());
        String units = sharedPrefs.getString(mContext.getString(R.string.pref_units),
                mContext.getString(R.string.pref_units_metric));

        if (units.equals(mContext.getString(R.string.pref_units_us))) {
            s *= 0.621371192;
        }

        return s;
    }

    public String getLocalizedDistanceLabel() {
        String metric = mContext.getString(R.string.pref_units_metric);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String units = sharedPrefs.getString(mContext.getString(R.string.pref_units),
                mContext.getString(R.string.pref_units_metric));

        if (units.equals(metric)) {
            return mContext.getString(R.string.distance_metric);
        } else {
            return mContext.getString(R.string.distance_us_customary);
        }
    }

    public String getLocalizedSpeedLabel() {
        String metric = mContext.getString(R.string.pref_units_metric);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                mContext.getApplicationContext());
        String units = sharedPrefs.getString(mContext.getString(R.string.pref_units),
                mContext.getString(R.string.pref_units_metric));

        if (units.equals(metric)) {
            return mContext.getString(R.string.speed_metric);
        } else {
            return mContext.getString(R.string.speed_us_customary);
        }
    }

    public String getLocalizedPaceLabel() {
        String metric = mContext.getString(R.string.pref_units_metric);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                mContext.getApplicationContext());
        String units = sharedPrefs.getString(mContext.getString(R.string.pref_units),
                mContext.getString(R.string.pref_units_metric));

        if (units.equals(metric)) {
            return mContext.getString(R.string.pace_metric);
        } else {
            return mContext.getString(R.string.pace_us_customary);
        }
    }

    public String formatSpeed(float speed) {
        if (speed >= 100) {
            return DecimalFormatSymbols.getInstance().getInfinity();
        }

        String format = mContext.getString(R.string.format_speed);
        return String.format(Locale.getDefault(), format, getLocalizedSpeed(speed));
    }

    /**
     * Get displayable pace.
     *
     * @param speed Speed in meters/second.
     * @return Speed in mins/km or mins/mile, depending on the user's unit preferences, or infinity
     * if the speed indicates that the user is stationary.
     */
    public String formatPace(float speed) {
        // Convert to mins / km or mins / mile
        double pace;
        if (speed == 0) {
            pace = 0; // Avoid divide by zero
        } else {
            pace = 60 / getLocalizedSpeed(speed);

            // If the pace is too high, e.g. the user is standing still, show infinity.
            if (pace >= 100) {
                return DecimalFormatSymbols.getInstance().getInfinity();
            }
        }

        int mins = (int) pace;
        int secs = (int) (60 * (pace - mins));

        String format = mContext.getString(R.string.format_pace);
        return String.format(Locale.getDefault(), format, mins, secs);
    }

    public String formatDuration(long duration) {
        long[] hms = millisToHoursMinsSecs(duration);
        long hours = hms[0];
        long minutes = hms[1];
        long seconds = hms[2];
        long millis = hms[3];

        // TODO formats should be in strings.xml
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d.%1d", minutes, seconds, millis / 100);
        }
    }

    public String formatDateTime(long time) {
        return DateUtils.formatDateTime(mContext, time,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME);
    }
}
