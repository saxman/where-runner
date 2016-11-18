package info.saxman.whererunner;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Point;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

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

    private static final int PAGER_ORIENTATION = LinearLayout.HORIZONTAL;

    private static final int VIBRATOR_DURATION_MS = 200;

    private final BroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();

    private GridViewPager mViewPager;
    private FragmentGridPagerAdapter mViewPagerAdapter;
    private static final int VIEW_PAGER_ITEMS = 3;

    private TextClock mTextClock;

    private ImageView mLocationButton;
    private ImageView mHeartButton;

    private View mAlertView;
    private TextView mAlertTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_workout_main, container, false);

        mTextClock = (TextClock) view.findViewById(R.id.time);
        mAlertView = view.findViewById(R.id.alert_connectivity);
        mAlertTextView = (TextView) mAlertView.findViewById(R.id.text2);

        final GestureDetector locationButtonGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown (MotionEvent event){
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent event) {
                if (PAGER_ORIENTATION == LinearLayout.VERTICAL) {
                    mViewPager.setCurrentItem(FRAGMENT_MAP, 0);
                } else {
                    mViewPager.setCurrentItem(0, FRAGMENT_MAP);
                }

                return super.onSingleTapConfirmed(event);
            }
        });

        mLocationButton = (ImageView) view.findViewById(R.id.map_button);
        mLocationButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                // Dispatch all events to the underlying viewpager so that it can handle dragging
                // and flinging events
                mViewPager.dispatchTouchEvent(event);
                return locationButtonGestureDetector.onTouchEvent(event);
            }
        });

        final GestureDetector heartButtonGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown (MotionEvent event){
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent event) {
                if (PAGER_ORIENTATION == LinearLayout.VERTICAL) {
                    mViewPager.setCurrentItem(FRAGMENT_HEART, 0);
                } else {
                    mViewPager.setCurrentItem(0, FRAGMENT_HEART);
                }

                return super.onSingleTapConfirmed(event);
            }
        });

        mHeartButton = (ImageView) view.findViewById(R.id.heart_button);
        mHeartButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                // Dispatch all events to the underlying viewpager so that it can handle dragging
                // and flinging events
                mViewPager.dispatchTouchEvent(event);
                return heartButtonGestureDetector.onTouchEvent(event);
            }
        });

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

        // If a different initial view has been requested, display it first.
        // mViewPager.setCurrentItem() doesn't appear to queue the initial page change, even after
        // mViewPager.setAdapter() is called, as is expected (ref http://stackoverflow.com/a/29136603/763176).
        // Instead, we need to tell the view pager to change the current item, but we need to wait until it has been laid out.
        final int currentItem;
        if (getArguments() != null) {
            currentItem = getArguments().getInt(ARGUMENT_INITIAL_FRAGMENT, FRAGMENT_DATA);
        } else {
            currentItem = FRAGMENT_DATA;
        }

        ViewTreeObserver observer = mViewPager.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mViewPager.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                if (PAGER_ORIENTATION == LinearLayout.VERTICAL) {
                    mViewPager.setCurrentItem(currentItem, 0, false);
                } else {
                    mViewPager.setCurrentItem(0, currentItem, false);
                }
            }
        });

        updateUI();

        return view;
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
        mTextClock.setBackgroundResource(R.drawable.bg_text_overlay_ambient);
        mTextClock.getPaint().setAntiAlias(false);

        mLocationButton.setVisibility(View.INVISIBLE);
        mHeartButton.setVisibility(View.INVISIBLE);

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
        mTextClock.setBackgroundResource(R.drawable.bg_text_overlay);
        mTextClock.setTextColor(getResources().getColor(R.color.text_dark, null));
        mTextClock.getPaint().setAntiAlias(true);

        mLocationButton.setVisibility(View.VISIBLE);
        mHeartButton.setVisibility(View.VISIBLE);

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

    //
    // Private class methods
    //

    private void updateUI() {
        if (LocationService.isReceivingAccurateLocationSamples) {
            mLocationButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_gps_fixed));
        } else {
            mLocationButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_gps_not_fixed));
        }

        if (HeartRateSensorService.isReceivingAccurateHeartRateSamples) {
            mHeartButton.setColorFilter(getContext().getColor(R.color.highlight_dark));
        } else {
            mHeartButton.setColorFilter(getContext().getColor(R.color.black_86p));
        }
    }

    private Fragment getCurrentViewPagerFragment() {
        Point p = mViewPager.getCurrentItem();
        return mViewPagerAdapter.findExistingFragment(p.y, p.x);
    }

    private void notifyUserOfConnectivityIssue() {
        final LocationManager manager = (LocationManager) getActivity().getSystemService( Context.LOCATION_SERVICE );

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mAlertTextView.setText(getString(R.string.location_unavailable));
        } else {
            mAlertTextView.setText(getString(R.string.reacquiring_location));
        }

        mAlertView.animate().translationY(0);
    }

    //
    // Public class methods
    //

    public int getCurrentFragment() {
        Point p = mViewPager.getCurrentItem();
        if (PAGER_ORIENTATION == LinearLayout.VERTICAL) {
            return p.y;
        }

        return p.x;
    }

    public void setCurrentFragment(int fragmentIndex) {
        if (PAGER_ORIENTATION == LinearLayout.VERTICAL) {
            mViewPager.setCurrentItem(fragmentIndex, 0, true);
        } else {
            mViewPager.setCurrentItem(0, fragmentIndex, true);
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

                        if (!PhoneConnectivityService.isPhoneConnected) {
                            // TODO shows the dialog even if the phone disconnected a long time ago...
                            notifyUserOfConnectivityIssue();
                        }
                    } else {
                        // Ensure that the connectivity alert overlay is closed, as we're receiving GPS samples
                        float x = WorkoutMainFragment.this.getResources().getDimension(R.dimen.alert_overlay_height_neg);
                        if (mAlertTextView.getTranslationY() != x) {
                            mAlertView.animate().translationY(x);
                        }
                    }

                    break;

                case HeartRateSensorService.ACTION_CONNECTIVITY_CHANGED:
                    break;

                case PhoneConnectivityService.ACTION_PHONE_CONNECTIVITY_CHANGED:
                    boolean isPhoneConnected = intent.getBooleanExtra(PhoneConnectivityService.EXTRA_IS_PHONE_CONNECTED, false);

                    if (!isPhoneConnected && !LocationService.isReceivingAccurateLocationSamples) {
                        notifyUserOfConnectivityIssue();
                    }

                    break;
            }

            if (!isAmbient()) {
                updateUI();
            }
        }
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
            return PAGER_ORIENTATION == LinearLayout.VERTICAL ? VIEW_PAGER_ITEMS : 1;
        }

        @Override
        public int getColumnCount(int i) {
            return PAGER_ORIENTATION == LinearLayout.HORIZONTAL ? VIEW_PAGER_ITEMS : 1;
        }
    }
}
