package com.example.google.whererunner;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.FusedLocationService;
import com.example.google.whererunner.services.HeartRateSensorService;
import com.example.google.whererunner.services.LocationService;

public class MainActivity extends WearableActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        WearableActionDrawer.OnMenuItemClickListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int DRAWER_PEEK_TIME_MS = 2500;

    private static final int NAV_DRAWER_ITEMS = 2;
    private static final int NAV_DRAWER_FRAGMENT_MAIN = 0;
    private static final int NAV_DRAWER_FRAGMENT_SETTINGS = 1;

    private static final int ACTION_RECORD_INDEX = 0;
    private static final int ACTION_ACTIVITY_TYPE_INDEX = 1;

    private static final int ACTIVITY_TYPE_RUNNING = 0;
    private static final int ACTIVITY_TYPE_CYCLING = 1;

    private WearableDrawerLayout mWearableDrawerLayout;
    private WearableActionDrawer mWearableActionDrawer;
    private WearableNavigationDrawer mWearableNavigationDrawer;
    private WearableNavigationDrawer.WearableNavigationDrawerAdapter mWearableNavigationDrawerAdapter;

    private Fragment mCurrentViewPagerFragment;

    private Menu mMenu;

    private BroadcastReceiver mRecordingBroadcastReceiver;

    private boolean mIsRecording = false;

    private int mActivityType = ACTIVITY_TYPE_RUNNING;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        FragmentManager fragmentManager = getFragmentManager();
        mCurrentViewPagerFragment = new ActivityMainFragment();
        fragmentManager.beginTransaction().replace(R.id.content_frame, mCurrentViewPagerFragment).commit();

        mWearableNavigationDrawerAdapter = new MyWearableNavigationDrawerAdapter();

        mWearableNavigationDrawer = (WearableNavigationDrawer) findViewById(R.id.nav_drawer);
        mWearableNavigationDrawer.setAdapter(mWearableNavigationDrawerAdapter);

        mWearableActionDrawer = (WearableActionDrawer) findViewById(R.id.action_drawer);
        mWearableActionDrawer.setOnMenuItemClickListener(this);

        View peekView = getLayoutInflater().inflate(R.layout.action_drawer_peek, null);
        mWearableActionDrawer.setPeekContent(peekView);

        mMenu = mWearableActionDrawer.getMenu();
        mWearableActionDrawer.setOnMenuItemClickListener(this);

        mWearableDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);

        // Wait until the drawer layout has been laid out, then peek its drawers
        mWearableDrawerLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mWearableDrawerLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                mWearableDrawerLayout.peekDrawer(Gravity.TOP);
                mWearableDrawerLayout.peekDrawer(Gravity.BOTTOM);

                // Hide the peeking drawers after a few seconds
                new CountDownTimer(DRAWER_PEEK_TIME_MS, DRAWER_PEEK_TIME_MS) {
                    public void onTick(long millisUntilFinished) {}

                    public void onFinish() {
                        if (mWearableDrawerLayout.isPeeking(Gravity.TOP)) {
                            mWearableDrawerLayout.closeDrawer(Gravity.TOP);
                        }

                        if (mWearableDrawerLayout.isPeeking(Gravity.BOTTOM)) {
                            mWearableDrawerLayout.closeDrawer(Gravity.BOTTOM);
                        }
                    }
                }.start();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        // Start the location service so that child fragments can receive location and recording
        // status updates via local broadcasts.
        Intent  intent = new Intent(this, FusedLocationService.class);
        startService(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register a receiver to listen for the recording status of the location service.
        if (mRecordingBroadcastReceiver == null) {
            mRecordingBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case LocationService.ACTION_RECORDING_STATUS:
                        case LocationService.ACTION_RECORDING_STATUS_CHANGED:
                            mIsRecording = intent.getBooleanExtra(LocationService.EXTRA_IS_RECORDING, false);
                            setRecordingButtonUiState();
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_RECORDING_STATUS);
        intentFilter.addAction(LocationService.ACTION_RECORDING_STATUS_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRecordingBroadcastReceiver, intentFilter);

        // Request the recording status from the location service.
        Intent intent = new Intent(LocationService.ACTION_REPORT_RECORDING_STATUS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onStop() {
        // Tell the location service that if can stop location updates, unless it's recording.
        Intent intent = new Intent(LocationService.ACTION_STOP_LOCATION_UPDATES);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecordingBroadcastReceiver);

        // TODO: Implement better HRM service management!
        // HR service is started in HeartRateFragment; stopped here to kill when activity stops
        stopService(new Intent(this, HeartRateSensorService.class));

        super.onStop();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.record_button:
                if (mIsRecording) {
                    // Tell the location service to stop recording
                    Intent intent = new Intent(LocationService.ACTION_STOP_RECORDING);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                } else {
                    // Tell the location service to start recording
                    Intent intent = new Intent(LocationService.ACTION_START_RECORDING);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                }

                mWearableDrawerLayout.closeDrawer(Gravity.BOTTOM);

                break;
            case R.id.activity_type_button:
                toggleActivityTypeUiState();
                break;
        }

        return true;
    }

    //
    // WearableActivity methods
    //

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        if (mCurrentViewPagerFragment instanceof WearableFragment) {
            ((WearableFragment) mCurrentViewPagerFragment).onEnterAmbient(ambientDetails);
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        if (mCurrentViewPagerFragment instanceof WearableFragment) {
            ((WearableFragment) mCurrentViewPagerFragment).onExitAmbient();
        }
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();

        if (mCurrentViewPagerFragment instanceof WearableFragment) {
            ((WearableFragment) mCurrentViewPagerFragment).onUpdateAmbient();
        }
    }

    //
    // Class methods
    //

    private void setRecordingButtonUiState() {
        MenuItem menuItem = mMenu.getItem(ACTION_RECORD_INDEX);

        if (mIsRecording) {
            menuItem.setIcon(getDrawable(R.drawable.ic_stop));
            menuItem.setTitle(getString(R.string.stop_recording));
        } else {
            menuItem.setIcon(getDrawable(R.drawable.ic_record));
            menuItem.setTitle(getString(R.string.record));
        }
    }

    private void toggleActivityTypeUiState() {
        MenuItem menuItem = mMenu.getItem(ACTION_ACTIVITY_TYPE_INDEX);

        if (mActivityType == ACTIVITY_TYPE_CYCLING) {
            menuItem.setIcon(getDrawable(R.drawable.ic_running));
            menuItem.setTitle(getString(R.string.activity_running));
            mActivityType = ACTIVITY_TYPE_RUNNING;
        } else {
            menuItem.setIcon(getDrawable(R.drawable.ic_cycling));
            menuItem.setTitle(getString(R.string.activity_cycling));
            mActivityType = ACTIVITY_TYPE_CYCLING;
        }

        // TODO does not seem to force the nav drawer to refresh icons...
        mWearableNavigationDrawerAdapter.notifyDataSetChanged();
    }

    //
    // Inner classes
    //

    class MyWearableNavigationDrawerAdapter extends WearableNavigationDrawer.WearableNavigationDrawerAdapter {
        @Override
        public String getItemText(int pos) {
            switch (pos) {
                case NAV_DRAWER_FRAGMENT_MAIN:
                    return getString(R.string.recording);
                case NAV_DRAWER_FRAGMENT_SETTINGS:
                    return getString(R.string.settings);
            }

            return null;
        }

        @Override
        public Drawable getItemDrawable(int pos) {
            switch (pos) {
                case NAV_DRAWER_FRAGMENT_MAIN:
                    int id;

                    if (mActivityType == ACTIVITY_TYPE_RUNNING) {
                        id = R.drawable.ic_running_white;
                    } else {
                        id = R.drawable.ic_cycling_white;
                    }

                    return getDrawable(id);
                case NAV_DRAWER_FRAGMENT_SETTINGS:
                    return getDrawable(R.drawable.ic_settings);
            }

            return null;
        }

        @Override
        public void onItemSelected(int pos) {
            Fragment fragment = null;

            switch (pos) {
                case NAV_DRAWER_FRAGMENT_MAIN:
                    fragment = new ActivityMainFragment();
                    // Ensure the action drawer is visible, since other nav drawer pages could have hidden it
                    mWearableActionDrawer.setVisibility(View.VISIBLE);
                    break;
                case NAV_DRAWER_FRAGMENT_SETTINGS:
                    fragment = new SettingsFragment();
                    // Hide the action drawer since we don't need its actions in the settings page
                    mWearableActionDrawer.setVisibility(View.GONE);
                    break;
            }

            mCurrentViewPagerFragment = fragment;

            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }

        @Override
        public int getCount() {
            return NAV_DRAWER_ITEMS;
        }
    }
}
