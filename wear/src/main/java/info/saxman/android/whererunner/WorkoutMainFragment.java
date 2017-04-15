package info.saxman.android.whererunner;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentTransaction;
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

public class WorkoutMainFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutMainFragment.class.getSimpleName();

    public static final String ARGUMENT_WORKOUT_VIEW = "WORKOUT_VIEW";

    public static final int VALUE_WORKOUT_VIEW_MAP = 0;
    public static final int VALUE_WORKOUT_VIEW_DATA = 1;
    public static final int VALUE_WORKOUT_VIEW_HEART_RATE = 2;

    private static final int VIBRATOR_DURATION_MS = 200;

    private final BroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();

    private TextClock mTextClock;

    private ImageView mGpsStatus;
    private ImageView mPhoneStatus;

    private Fragment mContentFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_workout_main, container, false);

        mTextClock = (TextClock) rootView.findViewById(R.id.time);

        mPhoneStatus = (ImageView) rootView.findViewById(R.id.phone_status);
        mGpsStatus = (ImageView) rootView.findViewById(R.id.gps_status);

        int initialWorkoutView = VALUE_WORKOUT_VIEW_DATA;
        if (getArguments() != null) {
            initialWorkoutView = getArguments().getInt(ARGUMENT_WORKOUT_VIEW, VALUE_WORKOUT_VIEW_DATA);
        }

        if (initialWorkoutView == VALUE_WORKOUT_VIEW_MAP) {
            mContentFragment = new WorkoutMapFragment();

            mTextClock.setTextColor(getContext().getColor(R.color.text_dark));
            mTextClock.setBackgroundResource(R.drawable.bg_text_clock_map);

            mGpsStatus.setColorFilter(getContext().getColor(R.color.text_dark));
            mPhoneStatus.setColorFilter(getContext().getColor(R.color.text_dark));
        } else {
            mContentFragment = new WorkoutDataFragment();

            mTextClock.setTextColor(getContext().getColor(R.color.text_primary));
            mTextClock.setBackgroundResource(R.drawable.bg_text_clock);

            mGpsStatus.setColorFilter(getContext().getColor(R.color.text_primary));
            mPhoneStatus.setColorFilter(getContext().getColor(R.color.text_primary));
        }

        // Pass this fragment's arguments to the child fragment, in case there's an argument
        // being passed from the activity (e.g. show heart rate).
        mContentFragment.setArguments(getArguments());

        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.replace(R.id.workout_content_view, mContentFragment);
        ft.commit();

        updateUI();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

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

        if (mContentFragment instanceof WearableFragment) {
            ((WearableFragment) mContentFragment).onEnterAmbient(ambientDetails);
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        mTextClock.setFormat12Hour("h:mm:ss");
        mTextClock.setFormat24Hour("H:mm:ss");
        mTextClock.getPaint().setAntiAlias(true);

        if (mContentFragment instanceof WorkoutMapFragment) {
            mTextClock.setBackgroundResource(R.drawable.bg_text_clock_map);
            mTextClock.setTextColor(getContext().getColor(R.color.text_dark));
        } else {
            mTextClock.setBackgroundResource(R.drawable.bg_text_clock);
            mTextClock.setTextAppearance(R.style.text_clock);
        }

        mGpsStatus.setVisibility(View.VISIBLE);
        mPhoneStatus.setVisibility(View.VISIBLE);

        if (mContentFragment instanceof WearableFragment) {
            ((WearableFragment) mContentFragment).onExitAmbient();
        }
    }

    @Override
    public void onUpdateAmbient() {
        if (mContentFragment instanceof WearableFragment) {
            ((WearableFragment) mContentFragment).onUpdateAmbient();
        }

        updateUI();
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
