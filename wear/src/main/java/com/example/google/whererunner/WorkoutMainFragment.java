package com.example.google.whererunner;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
    private static final int FRAGMENT_GPS = 3;

    private static final int PAGER_ORIENTATION = LinearLayout.VERTICAL;

    private static final int VIBRATOR_DURATION_MS = 200;

    private BroadcastReceiver mLocationChangedReceiver;

    private GridViewPager mViewPager;
    private FragmentGridPagerAdapter mViewPagerAdapter;
    private int mViewPagerItems = 2;

    private TextClock mTextClock;
    private ViewGroup mPagerPagePips;

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
        mPagerPagePips = (ViewGroup) view.findViewById(R.id.pager_page_pips);

        // If the device has a heart rate monitor, add the HRM view to the view pager
        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HEART_RATE)) {
            mHrmConnectivityImageView.setVisibility(View.VISIBLE);
            mViewPagerItems += 1;
        }

        mViewPagerAdapter = new MyFragmentGridPagerAdapter(getChildFragmentManager());

        mViewPager = (GridViewPager) view.findViewById(R.id.pager);
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setOnPageChangeListener(new GridViewPagerChangeListener(mPagerPagePips));

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
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mLocationChangedReceiver);

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mLocationChangedReceiver == null) {
            mLocationChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case LocationService.ACTION_LOCATION_CHANGED:
                            mIsLocationServiceConnected = true;
                            break;

                        case LocationService.ACTION_CONNECTIVITY_LOST:
                            // TODO UI should be updated even if in ambient mode
                            mIsLocationServiceConnected = false;

                            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(VIBRATOR_DURATION_MS);

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

                            // TODO animate the heart to beat at the current rate

                            break;
                    }

                    if (!isAmbient()) {
                        updateUI();
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);
        intentFilter.addAction(LocationService.ACTION_CONNECTIVITY_LOST);
        intentFilter.addAction(HeartRateSensorService.ACTION_HEART_RATE_SENSOR_TIMEOUT);
        intentFilter.addAction(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mLocationChangedReceiver, intentFilter);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        mTextClock.setFormat12Hour("h:mm");
        mTextClock.setFormat24Hour("H:mm");

        mPagerPagePips.setVisibility(View.INVISIBLE);

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

        mPagerPagePips.setVisibility(View.VISIBLE);

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

    @Override
    public void onWatchButtonPressed(int keyCode) {
        // TODO add support for horizontal paging?
        switch (keyCode) {
            // top button on LG Watch Urbane 2nd Edition
            case KeyEvent.KEYCODE_STEM_1:
                Point p1 = mViewPager.getCurrentItem();
                mViewPager.setCurrentItem(p1.y - 1, p1.x);
                break;

            // bottom button on LG Watch Urbane 2nd Edition
            case KeyEvent.KEYCODE_STEM_2:
                Point p2 = mViewPager.getCurrentItem();
                mViewPager.setCurrentItem(p2.y + 1, p2.x);
                break;

            case KeyEvent.KEYCODE_STEM_3:
            case KeyEvent.KEYCODE_STEM_PRIMARY:
                Log.d(LOG_TAG, "Watch button pressed event received, but not handled");
                break;
        }
    }

    private void updateUI() {
        if (mIsLocationServiceConnected) {
            mGpsConnectivityImageView.setImageResource(R.drawable.ic_gps_fixed);
        } else {
            mGpsConnectivityImageView.setImageResource(R.drawable.ic_gps_not_fixed);
        }

        if (mIsPhoneConnected) {
            mPhoneConnectivityImageView.setVisibility(View.VISIBLE);
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
                    fragment = new HeartRateFragment();
                    break;
                case FRAGMENT_GPS:
                    fragment = new GpsStatusFragment();
                    break;
            }

            return fragment;
        }

        @Override
        public int getRowCount() {
            return PAGER_ORIENTATION == LinearLayout.VERTICAL ? mViewPagerItems : 1;
        }

        @Override
        public int getColumnCount(int i)
        {
            return PAGER_ORIENTATION == LinearLayout.HORIZONTAL ? mViewPagerItems : 1;
        }
    }

    private class GridViewPagerChangeListener implements GridViewPager.OnPageChangeListener {
        private ImageView[] mPipImageViews;

        public GridViewPagerChangeListener(ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mPipImageViews = new ImageView[mViewPagerItems];

            int spacing = (int) getResources().getDimension(R.dimen.map_overlay_spacing);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, spacing, 0, spacing);

            for (int i = 0; i < mViewPagerItems; i++) {
                ImageView view = (ImageView) inflater.inflate(R.layout.pager_pip, null);
                parent.addView(view, layoutParams);
                mPipImageViews[i] = view;
            }
        }

        @Override
        public void onPageSelected(int row, int col) {
            int i = PAGER_ORIENTATION == LinearLayout.VERTICAL ? row : col;

            for (int j = 0; j < mPipImageViews.length; j++) {
                if (i != j) {
                    mPipImageViews[j].setImageResource(R.drawable.ic_more_circle);
                } else {
                    mPipImageViews[j].setImageResource(R.drawable.ic_more_circle_opaque);
                }
            }
        }

        @Override
        public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {}

        @Override
        public void onPageScrollStateChanged(int i) {}
    }

    private class GoogleApiClientCallbacks implements
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnected(@NonNull Bundle connectionHint) {
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
