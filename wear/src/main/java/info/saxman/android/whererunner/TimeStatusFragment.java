package info.saxman.android.whererunner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextClock;

import info.saxman.android.whererunner.framework.WearableFragment;
import info.saxman.android.whererunner.services.HeartRateSensorService;
import info.saxman.android.whererunner.services.LocationService;
import info.saxman.android.whererunner.services.PhoneConnectivityService;

public class TimeStatusFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = TimeStatusFragment.class.getSimpleName();

    private static final int VIBRATOR_DURATION_MS = 200;

    private final BroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();

    private TextClock mTextClock;

    private ImageView mGpsStatus;
    private ImageView mPhoneStatus;

    private int mDisplayMode = DISPLAY_MODE_DARK;

    public static final int DISPLAY_MODE_DARK = 1;
    public static final int DISPLAY_MODE_LIGHT = 2;

    public static final String EXTRA_DISPLAY_MODE = "display_mode";

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_time_status, container, false);

        mTextClock = rootView.findViewById(R.id.time);
        mPhoneStatus = rootView.findViewById(R.id.phone_status);
        mGpsStatus = rootView.findViewById(R.id.gps_status);

        if (getArguments() != null) {
            mDisplayMode = getArguments().getInt(EXTRA_DISPLAY_MODE, DISPLAY_MODE_DARK);
        }

        setDisplayMode(mDisplayMode);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        updateUI();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_CONNECTIVITY_CHANGED);
        intentFilter.addAction(HeartRateSensorService.ACTION_CONNECTIVITY_CHANGED);
        intentFilter.addAction(PhoneConnectivityService.ACTION_PHONE_CONNECTIVITY_CHANGED);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    //
    // WearableFragment methods
    //

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        mTextClock.setFormat12Hour("h:mm");
        mTextClock.setFormat24Hour("H:mm");
        mTextClock.setTextColor(Color.WHITE);
        mTextClock.setBackgroundResource(R.drawable.bg_text_clock_ambient);
        mTextClock.getPaint().setAntiAlias(false);

        mGpsStatus.setVisibility(View.INVISIBLE);
        mPhoneStatus.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        mTextClock.setFormat12Hour("h:mm:ss");
        mTextClock.setFormat24Hour("H:mm:ss");
        mTextClock.getPaint().setAntiAlias(true);

        setDisplayMode(mDisplayMode);

        mGpsStatus.setVisibility(View.VISIBLE);
        mPhoneStatus.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUpdateAmbient() {
        updateUI();
    }

    public void setDisplayMode(int displayMode) {
        mDisplayMode = displayMode;

        if (mDisplayMode == DISPLAY_MODE_LIGHT) {
            mTextClock.setTextColor(getContext().getColor(R.color.text_dark));
            mTextClock.setBackgroundResource(R.drawable.bg_text_clock_map);

            mGpsStatus.setColorFilter(getContext().getColor(R.color.text_dark));
            mPhoneStatus.setColorFilter(getContext().getColor(R.color.text_dark));
        } else if (mDisplayMode == DISPLAY_MODE_DARK) {
            mTextClock.setTextColor(getContext().getColor(R.color.text_primary));
            mTextClock.setBackgroundResource(R.drawable.bg_text_clock);

            mGpsStatus.setColorFilter(getContext().getColor(R.color.text_primary));
            mPhoneStatus.setColorFilter(getContext().getColor(R.color.text_primary));
        } else {
            throw new IllegalArgumentException(
                    "Invalid display mode. Must be DISPLAY_MODE_LIGHT or DISPLAY_MODE_DARK");
        }
    }

    //
    // Private class methods
    //

    private void updateUI() {
        if (LocationService.isReceivingAccurateLocationSamples) {
            mGpsStatus.setImageDrawable(getContext().getDrawable(R.drawable.ic_gps_fixed));
        } else {
            PackageManager packageManager = getContext().getPackageManager();
            boolean hasGPS = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);

            // if we can (eventually) get GPS data, show that it's not fixed
            if (hasGPS || PhoneConnectivityService.isPhoneConnected) {
                mGpsStatus.setImageDrawable(getContext().getDrawable(R.drawable.ic_gps_not_fixed));
            } else {
                mGpsStatus.setImageDrawable(getContext().getDrawable(R.drawable.ic_gps_off));
            }
        }

        if (PhoneConnectivityService.isPhoneConnected) {
            mPhoneStatus.setImageDrawable(getContext().getDrawable(R.drawable.ic_phone_connected));
        } else {
            mPhoneStatus.setImageDrawable(getContext().getDrawable(R.drawable.ic_phone_disconnected));
        }
    }

    //
    // Private inner classes
    //

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case LocationService.ACTION_CONNECTIVITY_CHANGED:
                    boolean accurateSamples = intent.getBooleanExtra(LocationService.EXTRA_IS_RECEIVING_SAMPLES, false);

                    if (!accurateSamples) {
                        Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(VIBRATOR_DURATION_MS);
                    }

                    break;

                case HeartRateSensorService.ACTION_CONNECTIVITY_CHANGED:
                    break;

                case PhoneConnectivityService.ACTION_PHONE_CONNECTIVITY_CHANGED:
                    break;
            }

            if (!isAmbient()) {
                updateUI();
            }
        }
    }
}
