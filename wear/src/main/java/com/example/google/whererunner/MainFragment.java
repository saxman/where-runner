package com.example.google.whererunner;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Point;
import android.os.Bundle;
import android.support.wearable.view.*;
import android.view.*;
import android.widget.TextClock;

import com.example.google.whererunner.framework.RouteDataService;
import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.LocationService;

public class MainFragment extends WearableFragment implements RouteDataService.RouteDataUpdateListener {
    private static final String LOG_TAG = MainFragment.class.getSimpleName();

    private GridViewPager mViewPager;
    private FragmentGridPagerAdapter mViewPagerAdapter;

    private CircularButton mRecordButton;
    private CircularButton mActivityTypeButton;

    private TextClock mTextClock;

    private static final int FRAGMENT_ROUTE = 0;
    private static final int FRAGMENT_DATA = 1;
    private static final int FRAGMENT_HEART = 2;

    private static final int PAGER_ROWS = 1;
    private static final int PAGER_COLS = 3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        mViewPager = (GridViewPager) view.findViewById(R.id.pager);
        mViewPagerAdapter = new MyFragmentGridPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);

        mTextClock = (TextClock) view.findViewById(R.id.time);

        mRecordButton = (CircularButton) view.findViewById(R.id.record_button);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleRecording();
            }
        });

        mActivityTypeButton = (CircularButton) view.findViewById(R.id.activity_type_button);
        mActivityTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleActivityType();
            }
        });

        return view;
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
    }

    @Override
    public void onRouteDataUpdated(RouteDataService routeDataService) {
        Fragment fragment = getCurrentViewPagerFragment();
        if (fragment instanceof RouteDataService.RouteDataUpdateListener) {
            ((RouteDataService.RouteDataUpdateListener) fragment).onRouteDataUpdated(routeDataService);
        }
    }

    public void setHeartRate(float heartRate){
        if(getCurrentViewPagerFragment() instanceof HeartFragment){
            ((HeartFragment)getCurrentViewPagerFragment()).setHeartRate(heartRate);
        }
    }

    public void disableHeartRate(){
        if(getCurrentViewPagerFragment() instanceof HeartFragment){
            ((HeartFragment)getCurrentViewPagerFragment()).disableHeartRate();
        }
    }

    private void toggleRecording() {
        switch (((MainActivity) getActivity()).toggleRecording()) {
            case LocationService.LOCATION_UPDATES_STOPPED:
                mRecordButton.setImageResource(R.drawable.ic_record);
                break;
            case LocationService.LOCATION_UPDATING:
                mRecordButton.setImageResource(R.drawable.ic_stop);
                break;
        }
    }

    private void toggleActivityType() {
        // TODO
        mActivityTypeButton.setImageResource(R.drawable.ic_cycling);
    }

    private class MyFragmentGridPagerAdapter extends FragmentGridPagerAdapter {
        public MyFragmentGridPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getFragment(int row, int col) {
            Fragment fragment = null;

            switch (col) {
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
            return PAGER_ROWS;
        }

        @Override
        public int getColumnCount(int i)
        {
            return PAGER_COLS;
        }
    }

    private Fragment getCurrentViewPagerFragment() {
        Point p = mViewPager.getCurrentItem();
        return mViewPagerAdapter.findExistingFragment(p.y, p.x);
    }
}
