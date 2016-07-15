package com.example.google.whererunner;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.*;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;

import com.example.google.whererunner.framework.VerticalDotsPageIndicator;
import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.HeartRateSensorService;
import com.example.google.whererunner.services.LocationService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

public class WorkoutMainFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutMainFragment.class.getSimpleName();

    private static final int FRAGMENT_MAP = 0;
    private static final int FRAGMENT_DATA = 1;
    private static final int FRAGMENT_HEART = 2;

    private static final int PAGER_ORIENTATION = LinearLayout.VERTICAL;

    private static final int VIBRATOR_DURATION_MS = 200;

    private BroadcastReceiver mBroadcastReceiver;

    private GridViewPager mViewPager;
    private FragmentGridPagerAdapter mViewPagerAdapter;
    private int mViewPagerItems = 2;

    private TextClock mTextClock;
    private VerticalDotsPageIndicator mDotsPageIndicator;

    private ImageView mPhoneConnectivityImageView;
    private ImageView mGpsConnectivityImageView;
    private ImageView mHrmConnectivityImageView;

    private boolean mIsHrmConnected = false;
    private boolean mIsPhoneConnected = false;
    private boolean mIsLocationServiceConnected = false;

    private GoogleApiClient mGoogleApiClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_workout_main, container, false);

        // Status overlays
        mTextClock = (TextClock) view.findViewById(R.id.time);
        mPhoneConnectivityImageView = (ImageView) view.findViewById(R.id.phone_connectivity);
        mGpsConnectivityImageView = (ImageView) view.findViewById(R.id.gps_connectivity);
        mHrmConnectivityImageView = (ImageView) view.findViewById(R.id.hrm_connectivity);

        // If the device has a heart rate monitor, add the HRM view to the view pager
        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HEART_RATE)) {
            mHrmConnectivityImageView.setVisibility(View.VISIBLE);
            mViewPagerItems += 1;
        }

        mViewPagerAdapter = new MyFragmentGridPagerAdapter(getChildFragmentManager());

        mViewPager = (GridViewPager) view.findViewById(R.id.pager);
        mViewPager.setAdapter(mViewPagerAdapter);

        mViewPager.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }

                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.KEYCODE_STEM_1:
                    case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                        Point p1 = mViewPager.getCurrentItem();

                        if (PAGER_ORIENTATION == LinearLayout.VERTICAL) {
                            mViewPager.setCurrentItem(p1.y - 1, p1.x);
                        } else {
                            mViewPager.setCurrentItem(p1.y, p1.x - 1);
                        }

                        return true;

                    case KeyEvent.KEYCODE_STEM_2:
                    case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                        Point p2 = mViewPager.getCurrentItem();

                        if (PAGER_ORIENTATION == LinearLayout.VERTICAL) {
                            mViewPager.setCurrentItem(p2.y + 1, p2.x);
                        } else {
                            mViewPager.setCurrentItem(p2.y, p2.x + 1);
                        }

                        return true;

                    case KeyEvent.KEYCODE_STEM_3:
                    case KeyEvent.KEYCODE_STEM_PRIMARY:
                    case KeyEvent.KEYCODE_NAVIGATE_IN:
                    case KeyEvent.KEYCODE_NAVIGATE_OUT:
                        Log.d(LOG_TAG, "Key event received, but not handled: keycode=" + keyEvent.getKeyCode());
                        break;
                }

                return false;
            }
        });

        mDotsPageIndicator = (VerticalDotsPageIndicator) view.findViewById(R.id.page_indicator);
        mDotsPageIndicator.setPager(mViewPager);

        if (mGoogleApiClient == null) {
            GoogleApiClientCallbacks callbacks = new GoogleApiClientCallbacks();

            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(callbacks)
                    .addOnConnectionFailedListener(callbacks)
                    .addApi(Wearable.API)
                    .build();
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case LocationService.ACTION_CONNECTIVITY_CHANGED:
                        mIsLocationServiceConnected = intent.getBooleanExtra(LocationService.EXTRA_IS_LOCATION_UPDATING, false);

                        if (!mIsLocationServiceConnected) {
                            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(VIBRATOR_DURATION_MS);
                        }

                        break;

                    case HeartRateSensorService.ACTION_HEART_RATE_SENSOR_TIMEOUT:
                        mIsHrmConnected = false;
                        break;

                    case HeartRateSensorService.ACTION_HEART_RATE_CHANGED:
                        if (mIsHrmConnected) {
                            // No need to update the UI if the status hasn't changed
                            return;
                        } else {
                            mIsHrmConnected = true;
                        }

                        break;
                }

                if (!isAmbient()) {
                    updateUI();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_CONNECTIVITY_CHANGED);
        intentFilter.addAction(HeartRateSensorService.ACTION_HEART_RATE_SENSOR_TIMEOUT);
        intentFilter.addAction(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        mTextClock.setFormat12Hour("h:mm");
        mTextClock.setFormat24Hour("H:mm");
        mTextClock.setTextColor(Color.WHITE);
        mTextClock.setBackgroundResource(R.drawable.bg_map_overlay_ambient);
        mTextClock.getPaint().setAntiAlias(false);

        mDotsPageIndicator.setVisibility(View.INVISIBLE);

        // TODO instead of just hiding the status indicator, create b&w modes for them
        mPhoneConnectivityImageView.setVisibility(View.INVISIBLE);
        mGpsConnectivityImageView.setVisibility(View.INVISIBLE);
        mHrmConnectivityImageView.setVisibility(View.INVISIBLE);

        Fragment fragment = getCurrentViewPagerFragment();
        if (fragment instanceof WearableFragment) {
            ((WearableFragment) fragment).onEnterAmbient(ambientDetails);
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        mTextClock.setFormat12Hour("h:mm:ss");
        mTextClock.setFormat24Hour("H:mm:ss");
        mTextClock.setBackgroundResource(R.drawable.bg_map_overlay);
        mTextClock.setTextColor(getResources().getColor(R.color.text_dark, null));
        mTextClock.getPaint().setAntiAlias(true);

        mPhoneConnectivityImageView.setVisibility(View.VISIBLE);
        mGpsConnectivityImageView.setVisibility(View.VISIBLE);
        mHrmConnectivityImageView.setVisibility(View.VISIBLE);

        mDotsPageIndicator.setVisibility(View.VISIBLE);

        Fragment fragment = getCurrentViewPagerFragment();
        if (fragment instanceof WearableFragment) {
            ((WearableFragment) fragment).onExitAmbient();
        }
    }

    @Override
    public void onUpdateAmbient() {
        Fragment fragment = getCurrentViewPagerFragment();
        if (fragment instanceof WearableFragment) {
            ((WearableFragment) fragment).onUpdateAmbient();
        }

        updateUI();
    }

    private void updateUI() {
        if (mIsLocationServiceConnected) {
            mGpsConnectivityImageView.setImageResource(R.drawable.ic_gps_fixed);
        } else {
            mGpsConnectivityImageView.setImageResource(R.drawable.ic_gps_not_fixed);
        }

        if (mIsPhoneConnected) {
            mPhoneConnectivityImageView.setImageResource(R.drawable.ic_phone_connected);
        } else {
            mPhoneConnectivityImageView.setImageResource(R.drawable.ic_phone_disconnected);
        }

        // TODO can set image tint instead of changing icon
        // TODO determine how this vector image is scale and make it larger
        if (mIsHrmConnected) {
            mHrmConnectivityImageView.setImageResource(R.drawable.ic_heart_red_12);
        } else {
            mHrmConnectivityImageView.setImageResource(R.drawable.ic_heart_12);
        }
    }

    private Fragment getCurrentViewPagerFragment() {
        Point p = mViewPager.getCurrentItem();
        return mViewPagerAdapter.findExistingFragment(p.y, p.x);
    }

    private class MyFragmentGridPagerAdapter extends FragmentGridPagerAdapter {
        public MyFragmentGridPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getFragment(int row, int col) {
            Fragment fragment = null;

            int x = PAGER_ORIENTATION == LinearLayout.HORIZONTAL ? col : row;

            switch (x) {
                case FRAGMENT_MAP:
                    fragment = new WorkoutMapFragment();
                    break;
                case FRAGMENT_DATA:
                    fragment = new WorkoutDataFragment();
                    break;
                case FRAGMENT_HEART:
                    fragment = new WorkoutHeartRateFragment();
                    break;
            }

            return fragment;
        }

        @Override
        public int getRowCount() {
            return PAGER_ORIENTATION == LinearLayout.VERTICAL ? mViewPagerItems : 1;
        }

        @Override
        public int getColumnCount(int i) {
            return PAGER_ORIENTATION == LinearLayout.HORIZONTAL ? mViewPagerItems : 1;
        }
    }

    private class GoogleApiClientCallbacks implements
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnected(Bundle connectionHint) {
            // Get the initial connectivity state of the phone
            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                @Override
                public void onResult(@NonNull NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                    mIsPhoneConnected = getConnectedNodesResult.getNodes().size() > 0;

                    if (!isAmbient()) {
                        updateUI();
                    }
                }
            }, 1000, TimeUnit.MILLISECONDS);

            // Get updates on the connectivity state of the phone
            // TODO use non-deprecated API
            Wearable.NodeApi.addListener(mGoogleApiClient, new NodeApi.NodeListener() {
                @Override
                public void onPeerConnected(Node node) {
                    mIsPhoneConnected = true;

                    if (!isAmbient()) {
                        updateUI();
                    }
                }

                @Override
                public void onPeerDisconnected(Node node) {
                    mIsPhoneConnected = false;

                    if (!isAmbient()) {
                        updateUI();
                    }
                }
            });
        }

        @Override
        public void onConnectionSuspended(int i) {
            // TODO
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            // TODO
        }
    }
}
