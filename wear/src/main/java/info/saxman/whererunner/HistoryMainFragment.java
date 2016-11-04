package info.saxman.whererunner;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.widget.LinearLayout;

import info.saxman.whererunner.framework.VerticalDotsPageIndicator;
import info.saxman.whererunner.model.Workout;
import info.saxman.whererunner.persistence.WorkoutDbHelper;

import java.util.ArrayList;
import java.util.Collection;

public class HistoryMainFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = HistoryMainFragment.class.getSimpleName();

    private static final int PAGER_ORIENTATION = LinearLayout.VERTICAL;

    private GridViewPager mViewPager;
    private WorkoutGridPagerAdapter mViewPagerAdapter;

    private VerticalDotsPageIndicator mDotsPageIndicator;

    private View mLoadingView;
    private View mNoDataView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        mDotsPageIndicator = (VerticalDotsPageIndicator) view.findViewById(R.id.page_indicator);

        mLoadingView = view.findViewById(R.id.loading);
        mNoDataView = view.findViewById(R.id.no_data);

        WorkoutDbHelper dbHelper = new WorkoutDbHelper(getActivity());
        dbHelper.readLastFiveWorkoutsAsync(new WorkoutDbHelper.ReadWorkoutsCallback() {
            @Override
            public void onRead(ArrayList<Workout> workouts) {
                mLoadingView.setVisibility(View.GONE);

                if (workouts.size() == 0) {
                    mNoDataView.setVisibility(View.VISIBLE);
                    return;
                }

                mViewPagerAdapter.addAll(workouts);
                mViewPagerAdapter.notifyDataSetChanged();

                // Must follow the mWorkouts assignment since this view access the adapter's underlying data
                mDotsPageIndicator.setPager(mViewPager);
            }
        });

        return view;
    }

    private class WorkoutGridPagerAdapter extends FragmentGridPagerAdapter {

        private ArrayList<Workout> mWorkouts = new ArrayList<>();

        WorkoutGridPagerAdapter(FragmentManager fm) {
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

        public void addAll(Collection<Workout> workouts) {
            mWorkouts.addAll(workouts);
        }
    }
}
