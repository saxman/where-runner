package com.example.google.whererunner;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.LocationService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

public class ActivityMainFragment extends WearableFragment {
    private static final String LOG_TAG = ActivityMainFragment.class.getSimpleName();

    private static final int FRAGMENT_ROUTE = 0;
    private static final int FRAGMENT_DATA = 1;
    private static final int FRAGMENT_HEART = 2;
    private static final int FRAGMENT_GPS = 3;

    private static final int PAGER_ORIENTATION = LinearLayout.VERTICAL;
    private static final int PAGER_ITEMS = 3;

    private static final int VIBRATOR_DURATION_MS = 200;

    private BroadcastReceiver mLocationChangedReceiver;

    private GridViewPager mViewPager;
    private FragmentGridPagerAdapter mViewPagerAdapter;

    private TextClock mTextClock;
    private ImageView mPhoneConnectedImageView;
    private ImageView mGpsConnectivityImageView;
    private ViewGroup mPagerPagePips;

    private Location mLastLocation;

    private GoogleApiClient mGoogleApiClient;

    private CountDownTimer mLocationSampleTimer;

    private int mConnectedNodes = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_activity_main, container, false);

        mViewPagerAdapter = new MyFragmentGridPagerAdapter(getChildFragmentManager());

        mPagerPagePips = (ViewGroup) view.findViewById(R.id.pager_page_pips);

        mViewPager = (GridViewPager) view.findViewById(R.id.pager);
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setOnPageChangeListener(new GridViewPagerChangeListener(mPagerPagePips));

        mTextClock = (TextClock) view.findViewById(R.id.time);
        mPhoneConnectedImageView = (ImageView) view.findViewById(R.id.phone_connectivity);
        mGpsConnectivityImageView = (ImageView) view.findViewById(R.id.gps_connectivity);

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

        if (mLocationSampleTimer != null) {
            mLocationSampleTimer.cancel();
            mLocationSampleTimer = null;
        }

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
                            mLastLocation = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);

                            // Reset (cancel) pre-existing timer
                            if (mLocationSampleTimer != null) {
                                mLocationSampleTimer.cancel();
                            }

                            // Start a timer to detect if we're not receiving location samples in regular intervals
                            // TODO move timer to location service and broadcast timeout
                            mLocationSampleTimer = new CountDownTimer(LocationService.LOCATION_UPDATE_INTERVAL_TIMEOUT_MS, LocationService.LOCATION_UPDATE_INTERVAL_TIMEOUT_MS) {
                                public void onTick(long millisUntilFinished) {}

                                public void onFinish() {
                                    // We haven't received a location sample in too long. Set the last location to null so updateUI() shows a disconnected icon next refresh cycle
                                    mLastLocation = null;

                                    // TODO should be updated even if in ambient mode
                                    if (!isAmbient()) {
                                        updateUI();
                                    }

                                    Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                                    vibrator.vibrate(VIBRATOR_DURATION_MS);
                                }
                            }.start();

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

    private void updateUI() {
        if (mLastLocation != null) {
            mGpsConnectivityImageView.setImageResource(R.drawable.ic_gps_fixed);
        } else {
            mGpsConnectivityImageView.setImageResource(R.drawable.ic_gps_not_fixed);
        }

        if (mConnectedNodes != -1) {
            mPhoneConnectedImageView.setVisibility(View.VISIBLE);

            if (mConnectedNodes == 0) {
                mPhoneConnectedImageView.setImageResource(R.drawable.ic_phone_disconnected);
            } else {
                mPhoneConnectedImageView.setImageResource(R.drawable.ic_phone_connected);
            }
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
                case FRAGMENT_ROUTE:
                    fragment = new ActivityMapFragment();
                    break;
                case FRAGMENT_DATA:
                    fragment = new ActivityDataFragment();
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
            return PAGER_ORIENTATION == LinearLayout.VERTICAL ? PAGER_ITEMS : 1;
        }

        @Override
        public int getColumnCount(int i)
        {
            return PAGER_ORIENTATION == LinearLayout.HORIZONTAL ? PAGER_ITEMS : 1;
        }
    }

    private class GridViewPagerChangeListener implements GridViewPager.OnPageChangeListener {
        private ImageView[] mPipImageViews;

        public GridViewPagerChangeListener(ViewGroup rootView) {
            LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mPipImageViews = new ImageView[PAGER_ITEMS];

            int spacing = (int) getResources().getDimension(R.dimen.map_overlay_spacing);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, spacing, 0, spacing);

            for (int i = 0; i < PAGER_ITEMS; i++) {
                ImageView view = (ImageView) inflater.inflate(R.layout.pager_pip, null);
                rootView.addView(view, layoutParams);
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
            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                @Override
                public void onResult(@NonNull NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                    mConnectedNodes = getConnectedNodesResult.getNodes().size();

                    if (!isAmbient()) {
                        updateUI();
                    }
                }
            }, 1000, TimeUnit.MILLISECONDS);

            // TODO use non-deprecated API
            Wearable.NodeApi.addListener(mGoogleApiClient, new NodeApi.NodeListener() {
                @Override
                public void onPeerConnected(Node node) {
                    mConnectedNodes = 1;

                    if (!isAmbient()) {
                        updateUI();
                    }
                }

                @Override
                public void onPeerDisconnected(Node node) {
                    mConnectedNodes = 0;

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
