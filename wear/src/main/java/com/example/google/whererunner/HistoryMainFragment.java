package com.example.google.whererunner;

import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.app.Fragment;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.google.whererunner.framework.VerticalDotsPageIndicator;
import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.model.Workout;

import java.util.ArrayList;

public class HistoryMainFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = HistoryMainFragment.class.getSimpleName();

    public static final String EXTRA_WORKOUTS = "WORKOUTS";

    private static final int PAGER_ORIENTATION = LinearLayout.VERTICAL;

    private GridViewPager mViewPager;
    private FragmentGridPagerAdapter mViewPagerAdapter;

    private ArrayList<Workout> mWorkouts;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWorkouts = getArguments().getParcelableArrayList(EXTRA_WORKOUTS);
    }

    public static HistoryMainFragment newInstance(ArrayList<Workout> workouts)
    {
        HistoryMainFragment fragment = new HistoryMainFragment();

        Bundle bundle = new Bundle(1);
        bundle.putParcelableArrayList(EXTRA_WORKOUTS, workouts);

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mWorkouts.size() == 0) {
            return inflater.inflate(R.layout.fragment_history_empty, container, false);
        }

        final View view = inflater.inflate(R.layout.fragment_history_main, container, false);

        mViewPagerAdapter = new WorkoutGridPagerAdapter(getChildFragmentManager());

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

        VerticalDotsPageIndicator dotsPageIndicator = (VerticalDotsPageIndicator) view.findViewById(R.id.page_indicator);
        dotsPageIndicator.setPager(mViewPager);

        return view;
    }

    private class WorkoutGridPagerAdapter extends FragmentGridPagerAdapter {
        public WorkoutGridPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getFragment(int row, int col) {
            int x = PAGER_ORIENTATION == LinearLayout.HORIZONTAL ? col : row;
            return HistoryDataFragment.newInstance(mWorkouts.get(x));
        }

        @Override
        public int getRowCount() {
            return PAGER_ORIENTATION == LinearLayout.VERTICAL ? mWorkouts.size() : 1;
        }

        @Override
        public int getColumnCount(int i) {
            return PAGER_ORIENTATION == LinearLayout.HORIZONTAL ? mWorkouts.size() : 1;
        }
    }
}
