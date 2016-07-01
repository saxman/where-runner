package com.example.google.whererunner;

import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.app.Fragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.model.Workout;
import com.example.google.whererunner.persistence.WorkoutDbHelper;

import java.util.ArrayList;

public class HistoryMainFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = HistoryMainFragment.class.getSimpleName();

    private static final int PAGER_ORIENTATION = LinearLayout.VERTICAL;

    private int mViewPagerItems;
    private GridViewPager mViewPager;
    private FragmentGridPagerAdapter mViewPagerAdapter;
    private ViewGroup mPagerPagePips;

    private ArrayList<Workout> mWorkouts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        WorkoutDbHelper mDbHelper = new WorkoutDbHelper(getContext());
        mWorkouts = mDbHelper.readLastFiveWorkouts();

        mViewPagerItems = mWorkouts.size();

        mPagerPagePips = (ViewGroup) view.findViewById(R.id.pager_pips);

        mViewPagerAdapter = new MyFragmentGridPagerAdapter(getChildFragmentManager());

        mViewPager = (GridViewPager) view.findViewById(R.id.pager);
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setOnPageChangeListener(new GridViewPagerChangeListener(mPagerPagePips));

        return view;
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
    }

    @Override
    public void onUpdateAmbient() {}

    @Override
    public void onWearableKeyEvent(KeyEvent event) {
        // TODO add support for horizontal paging?
        switch (event.getKeyCode()) {
            // top button on LG Watch Urbane 2nd Edition
            case KeyEvent.KEYCODE_STEM_1:
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                Point p1 = mViewPager.getCurrentItem();
                mViewPager.setCurrentItem(p1.y - 1, p1.x);
                break;

            // bottom button on LG Watch Urbane 2nd Edition
            case KeyEvent.KEYCODE_STEM_2:
            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                Point p2 = mViewPager.getCurrentItem();
                mViewPager.setCurrentItem(p2.y + 1, p2.x);
                break;

            case KeyEvent.KEYCODE_STEM_3:
            case KeyEvent.KEYCODE_STEM_PRIMARY:
            case KeyEvent.KEYCODE_NAVIGATE_IN:
            case KeyEvent.KEYCODE_NAVIGATE_OUT:
                Log.d(LOG_TAG, "Key event received, but not handled: keycode=" + event.getKeyCode());
                break;
        }
    }

    private class MyFragmentGridPagerAdapter extends FragmentGridPagerAdapter {
        public MyFragmentGridPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getFragment(int row, int col) {
            int x = PAGER_ORIENTATION == LinearLayout.HORIZONTAL ? col : row;
            return HistoryDataFragment.newInstance(mWorkouts.get(x));
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

            if (mPipImageViews.length > 0) {
                mPipImageViews[0].setImageResource(R.drawable.ic_pager_pip_selected);
            }
        }

        @Override
        public void onPageSelected(int row, int col) {
            int i = PAGER_ORIENTATION == LinearLayout.VERTICAL ? row : col;

            for (int j = 0; j < mPipImageViews.length; j++) {
                if (i != j) {
                    mPipImageViews[j].setImageResource(R.drawable.ic_pager_pip);
                } else {
                    mPipImageViews[j].setImageResource(R.drawable.ic_pager_pip_selected);
                }
            }
        }

        @Override
        public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {}

        @Override
        public void onPageScrollStateChanged(int i) {}
    }

}
