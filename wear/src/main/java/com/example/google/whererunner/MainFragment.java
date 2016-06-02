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
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.LocationService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

public class MainFragment extends WearableFragment {
    private static final String LOG_TAG = MainFragment.class.getSimpleName();

    private static final int FRAGMENT_ROUTE = 0;
    private static final int FRAGMENT_DATA = 1;
    private static final int FRAGMENT_HEART = 2;

    private static final int PAGER_ORIENTATION = LinearLayout.VERTICAL;
    private static final int PAGER_ITEMS = 3;

    private BroadcastReceiver mLocationChangedReceiver;

    private GridViewPager mViewPager;
    private FragmentGridPagerAdapter mViewPagerAdapter;

    private TextClock mTextClock;
    private TextView mLocationAccuracyTextView;
    private ImageView mPhoneConnectedImageView;

    private Location mLastLocation;

    private GoogleApiClient mGoogleApiClient;

    private int mConnectedNodes = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        mViewPager = (GridViewPager) view.findViewById(R.id.pager);
        mViewPagerAdapter = new MyFragmentGridPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);

        mTextClock = (TextClock) view.findViewById(R.id.time);
        mLocationAccuracyTextView = (TextView) view.findViewById(R.id.location_accuracy);
        mPhoneConnectedImageView = (ImageView) view.findViewById(R.id.phone_connectivity);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle connectionHint) {
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
                        public void onConnectionSuspended(int cause) {
                            Log.d(LOG_TAG, "onConnectionSuspended: " + cause);
                            // TODO
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.d(LOG_TAG, "onConnectionFailed: " + result);
                            // TODO
                        }
                    })
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
                            mLastLocation = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);

                            if (!isAmbient()) {
                                updateUI();
                            }

                            break;
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
            // Ensure that the view is visible, as it's invisible before data is available
            mLocationAccuracyTextView.setVisibility(View.VISIBLE);
            mLocationAccuracyTextView.setText(Float.toString(mLastLocation.getAccuracy()));
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
                    fragment = new RouteMapFragment();
                    break;
                case FRAGMENT_DATA:
                    fragment = new DataFragment();
                    break;
                case FRAGMENT_HEART:
                    fragment = new HeartFragment();
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
}
