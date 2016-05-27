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
        ActivityCompat.OnRequestPermissionsResultCallback, WearableActionDrawer.OnMenuItemClickListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private Fragment mCurrentViewPagerFragment;

    private WearableDrawerLayout mWearableDrawerLayout;
    private WearableActionDrawer mWearableActionDrawer;
    private WearableNavigationDrawer mWearableNavigationDrawer;

    private Menu mMenu;

    private BroadcastReceiver mRecodringBroadcastRecevier;

    private boolean mIsRecording = false;

    private static final int DRAWER_PEEK_TIME_MS = 2500;

    private static final int NAV_DRAWER_ITEMS = 2;
    private static final int NAV_DRAWER_FRAGMENT_MAIN = 0;
    private static final int NAV_DRAWER_FRAGMENT_SETTINGS = 1;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, new MainFragment()).commit();

        mWearableNavigationDrawer = (WearableNavigationDrawer) findViewById(R.id.nav_drawer);
        mWearableNavigationDrawer.setAdapter(new MyWearableNavigationDrawerAdapter());

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

        setAmbientEnabled();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Start the location service so that child fragments can receive location and recording
        // status updates via local broadcasts.
        Intent intent = new Intent(this, FusedLocationService.class);
        startService(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register a receiver to listen for the recording status of the location service.
        if (mRecodringBroadcastRecevier == null) {
            mRecodringBroadcastRecevier = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mIsRecording = intent.getBooleanExtra(LocationService.EXTRA_IS_RECORDING, false);
                    setRecordingActionButtonState();
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_RECORDING_STATUS);
        intentFilter.addAction(LocationService.ACTION_RECORDING_STATUS_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRecodringBroadcastRecevier, intentFilter);

        // Request the recording status from the location service.
        Intent intent = new Intent(LocationService.ACTION_REPORT_RECORDING_STATUS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onStop() {
        // Tell the location service that if can stop location updates, unless it's recording.
        Intent intent = new Intent(LocationService.ACTION_STOP_LOCATION_UPDATES);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecodringBroadcastRecevier);

        // TODO: Implement better HRM service management!
        // HR service is started in HeartFragment; stopped here to kill when activity stops
        stopService(new Intent(this, HeartRateSensorService.class));

        super.onStop();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.record_button:
                if (mIsRecording) {
                    stopRecording();
                } else {
                    startRecording();
                }

                mWearableDrawerLayout.closeDrawer(Gravity.BOTTOM);

                break;
            case R.id.activity_type_button:
                // TODO
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

    private void setRecordingActionButtonState() {
        MenuItem menuItem = mMenu.getItem(0);

        if (mIsRecording) {
            menuItem.setIcon(getDrawable(R.drawable.ic_stop));
            menuItem.setTitle(getString(R.string.stop_recording));
        } else {
            menuItem.setIcon(getDrawable(R.drawable.ic_record));
            menuItem.setTitle(getString(R.string.record));
        }
    }

    private void stopRecording() {
        // Tell the location service to stop recording
        Intent intent = new Intent(LocationService.ACTION_STOP_RECORDING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void startRecording() {
        // Tell the location service to start recording
        Intent intent = new Intent(LocationService.ACTION_START_RECORDING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
                    return getDrawable(R.drawable.ic_running_white);
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
                    fragment = new MainFragment();
                    break;
                case NAV_DRAWER_FRAGMENT_SETTINGS:
                    fragment = new SettingsFragment();
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
