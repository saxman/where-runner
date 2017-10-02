package info.saxman.android.whererunner;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.wear.widget.SwipeDismissFrameLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingsFragment extends PreferenceFragment {

    private static final String LOG_TAG = SettingsFragment.class.getSimpleName();

    private String mPrefUseGps;
    private String mPrefAutoStartHrm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_settings);

        mPrefUseGps = getString(R.string.pref_use_watch_gps);
        mPrefAutoStartHrm = getString(R.string.pref_auto_start_hrm);

        // If the device lacks a GPS sensor, disable the setting to force the sensor's use.
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            getPreferenceManager().findPreference(mPrefUseGps).setEnabled(false);
        }

        // If the device lacks a GPS sensor, disable the setting to force the sensor's use.
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HEART_RATE)) {
            getPreferenceManager().findPreference(mPrefAutoStartHrm).setEnabled(false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Add the preferences view to a swipe dismiss layout so that we can pop the back stack
        // instead of dismissing the app.
        SwipeDismissFrameLayout swipeDismissLayout = new SwipeDismissFrameLayout(getActivity());
        // TODO use the theme background instead
        swipeDismissLayout.setBackgroundResource(R.color.indigo_15b);
        swipeDismissLayout.addView(view);
        swipeDismissLayout.addCallback(new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                ((MainActivity) getActivity()).navigateBack();
            }
        });

        return swipeDismissLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(
                (SharedPreferences.OnSharedPreferenceChangeListener) getActivity());
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                (SharedPreferences.OnSharedPreferenceChangeListener) getActivity());

        super.onPause();
    }
}
