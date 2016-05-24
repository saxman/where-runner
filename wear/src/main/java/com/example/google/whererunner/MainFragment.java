package com.example.google.whererunner;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Point;
import android.os.Bundle;
import android.support.wearable.view.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.google.whererunner.framework.WearableFragment;

public class MainFragment extends WearableFragment {
    private static final String LOG_TAG = MainFragment.class.getSimpleName();

    private GridViewPager mViewPager;
    private FragmentGridPagerAdapter mViewPagerAdapter;

    private static final int FRAGMENT_ROUTE = 0;
    private static final int FRAGMENT_DATA = 1;
    private static final int FRAGMENT_HEART = 2;

    private static final int PAGER_ORIENTATION = LinearLayout.VERTICAL;
    private static final int PAGER_ITEMS = 3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        mViewPager = (GridViewPager) view.findViewById(R.id.pager);
        mViewPagerAdapter = new MyFragmentGridPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);

        return view;
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        Fragment fragment = getCurrentViewPagerFragment();
        if (fragment instanceof WearableFragment) {
            ((WearableFragment) fragment).onEnterAmbient(ambientDetails);
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

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

    public void setHeartRate(float heartRate){
//        if(getCurrentViewPagerFragment() instanceof HeartFragment){
//            ((HeartFragment)getCurrentViewPagerFragment()).setHeartRate(heartRate);
//        }
    }

    public void disableHeartRate(){
//        if(getCurrentViewPagerFragment() instanceof HeartFragment){
//            ((HeartFragment)getCurrentViewPagerFragment()).disableHeartRate();
//        }
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

    private Fragment getCurrentViewPagerFragment() {
        Point p = mViewPager.getCurrentItem();
        return mViewPagerAdapter.findExistingFragment(p.y, p.x);
    }
}
