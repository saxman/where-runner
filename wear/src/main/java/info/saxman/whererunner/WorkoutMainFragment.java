package info.saxman.whererunner;

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

import info.saxman.whererunner.framework.WearableFragment;
import info.saxman.whererunner.services.HeartRateSensorService;
import info.saxman.whererunner.services.LocationService;
import info.saxman.whererunner.services.PhoneConnectivityService;

public class WorkoutMainFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutMainFragment.class.getSimpleName();

    public static final String ARGUMENT_INITIAL_FRAGMENT = "INITIAL_FRAGMENT";

    public static final int FRAGMENT_MAP = 0;
    public static final int FRAGMENT_DATA = 1;
    public static final int FRAGMENT_HEART = 2;

    private static final int VIBRATOR_DURATION_MS = 200;

    private final BroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();

    private TextClock mTextClock;

    private ImageView mMapButton;
    private ImageView mHeartButton;

    private ImageView mGpsStatus;
    private ImageView mPhoneStatus;

    private Fragment mContentFragment;

    private boolean mIsImmersiveMode = false;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_workout_main, container, false);

        mTextClock = (TextClock) rootView.findViewById(R.id.time);

        final String TAG_MAP = getString(R.string.tag_map);
        final String TAG_HEART_RATE = getString(R.string.tag_heart_rate);
        final String TAG_DATA = getString(R.string.tag_data);

        mMapButton = (ImageView) rootView.findViewById(R.id.map_button);
        mMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TAG_MAP.equals(view.getTag())) {
                    mContentFragment = new WorkoutMapFragment();

                    mMapButton.setImageResource(R.drawable.ic_menu);
                    mMapButton.setTag(TAG_DATA);

                    if (TAG_DATA.equals(mHeartButton.getTag())) {
                        mHeartButton.setTag(TAG_HEART_RATE);
                        updateUI();
                    }

                    mTextClock.setTextColor(getContext().getColor(R.color.text_dark));
                    mTextClock.setBackgroundResource(R.drawable.bg_text_clock_map);

                    int c1 = getContext().getColor(R.color.text_primary);
                    int c2 = getContext().getColor(R.color.text_dark);

                    ObjectAnimator.ofArgb(mGpsStatus, "colorFilter", c1, c2).start();
                    ObjectAnimator.ofArgb(mPhoneStatus, "colorFilter", c1, c2).start();
                } else {
                    mContentFragment = new WorkoutDataFragment();

                    mMapButton.setImageResource(R.drawable.ic_place);
                    mMapButton.setTag(TAG_MAP);

                    mTextClock.setTextColor(getContext().getColor(R.color.text_primary));
                    mTextClock.setBackgroundResource(R.drawable.bg_text_clock);

                    int c1 = getContext().getColor(R.color.text_dark);
                    int c2 = getContext().getColor(R.color.text_primary);

                    ObjectAnimator.ofArgb(mGpsStatus, "colorFilter", c1, c2).start();
                    ObjectAnimator.ofArgb(mPhoneStatus, "colorFilter", c1, c2).start();
                }

                FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                ft.replace(R.id.workout_content_view, mContentFragment);
                ft.commit();
            }
        });

        mHeartButton = (ImageView) rootView.findViewById(R.id.heart_button);
        mHeartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTextClock.setTextColor(getContext().getColor(R.color.text_primary));
                mTextClock.setBackgroundResource(R.drawable.bg_text_clock);

                mGpsStatus.setColorFilter(getContext().getColor(R.color.text_primary));
                mPhoneStatus.setColorFilter(getContext().getColor(R.color.text_primary));

                if (TAG_HEART_RATE.equals(view.getTag())) {
                    mContentFragment = new WorkoutHeartRateFragment();

                    mHeartButton.setImageResource(R.drawable.ic_menu);
                    mHeartButton.setColorFilter(getContext().getColor(R.color.button_tint));
                    mHeartButton.setTag(TAG_DATA);

                    if (TAG_DATA.equals(mMapButton.getTag())) {
                        mMapButton.setImageResource(R.drawable.ic_place);
                        mMapButton.setTag(TAG_MAP);
                    }
                } else {
                    mContentFragment = new WorkoutDataFragment();

                    mHeartButton.setTag(TAG_HEART_RATE);
                    updateUI();
                }

                FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                ft.replace(R.id.workout_content_view, mContentFragment);
                ft.commit();
            }
        });

        mPhoneStatus = (ImageView) rootView.findViewById(R.id.phone_status);
        mGpsStatus = (ImageView) rootView.findViewById(R.id.gps_status);

        int initialFragment;
        if (getArguments() != null) {
            initialFragment = getArguments().getInt(ARGUMENT_INITIAL_FRAGMENT, FRAGMENT_DATA);
        } else {
            initialFragment = FRAGMENT_DATA;
        }

        switch (initialFragment) {
            case FRAGMENT_DATA:
                mContentFragment = new WorkoutDataFragment();
                break;
            case FRAGMENT_HEART:
                mContentFragment = new WorkoutHeartRateFragment();
                break;
            case FRAGMENT_MAP:
                mContentFragment = new WorkoutMapFragment();
        }

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

        if (!mIsImmersiveMode) {
            hideUiControls();
        }

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

        if (!mIsImmersiveMode) {
            showUiControls();
        }

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
    // Public, non-inherited class methods
    //

    public void toggleImmersiveMode() {
        if (mIsImmersiveMode) {
            showUiControls();
            mIsImmersiveMode = false;
        } else {
            hideUiControls();
            mIsImmersiveMode = true;
        }
    }

    //
    // Private class methods
    //

    private void hideUiControls() {
        float offset = getResources().getDimensionPixelOffset(R.dimen.fab_screen_offset);
        float diameter = getResources().getDimensionPixelOffset(R.dimen.fab_diameter);

        ObjectAnimator.ofFloat(mMapButton, "translationX", -offset, -diameter).start();
        ObjectAnimator.ofFloat(mHeartButton, "translationX", offset, diameter).start();
    }

    private void showUiControls() {
        float offset = getResources().getDimensionPixelOffset(R.dimen.fab_screen_offset);
        float diameter = getResources().getDimensionPixelOffset(R.dimen.fab_diameter);

        ObjectAnimator.ofFloat(mMapButton, "translationX", -diameter, -offset).start();
        ObjectAnimator.ofFloat(mHeartButton, "translationX", diameter, offset).start();
    }

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

        if (getString(R.string.tag_heart_rate).equals(mHeartButton.getTag())) {
            if (HeartRateSensorService.isReceivingAccurateHeartRateSamples) {
                mHeartButton.setImageResource(R.drawable.ic_heart);
                mHeartButton.setColorFilter(getContext().getColor(R.color.mediumorchid));
            } else {
                mHeartButton.setImageResource(R.drawable.ic_heart_outline);
                mHeartButton.setColorFilter(getContext().getColor(R.color.button_tint));
            }
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
