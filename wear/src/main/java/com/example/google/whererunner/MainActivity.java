package com.example.google.whererunner;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
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

import com.firebase.client.Firebase;

public class MainActivity extends WearableActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback, WearableActionDrawer.OnMenuItemClickListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private LocationService mLocationService;
    boolean mLocationServiceBound = false;

    private Fragment mCurrentViewPagerFragment;

    private WearableDrawerLayout mWearableDrawerLayout;
    private WearableActionDrawer mWearableActionDrawer;
    private WearableNavigationDrawer mWearableNavigationDrawer;

    private Menu mMenu;

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

        Firebase.setAndroidContext(this);

        setAmbientEnabled();
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent intent = new Intent(this, FusedLocationService.class);
        startService(intent);
        bindService(intent, mLocationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        if (mLocationServiceBound) {
            unbindService(mLocationServiceConnection);
        }

        // TODO: Implement better HRM service management!
        // HR service is started in HeartFragment; stopped here to kill when activity stops
        stopService(new Intent(this, HeartRateSensorService.class));

        super.onStop();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.record_button:
                toggleRecording();
                return true;
            case R.id.activity_type_button:
                // TODO
                return true;
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

    private void toggleRecording() {
        if (mLocationService.isRecording()) {
            mLocationService.stopRecording();
        } else {
            mLocationService.startRecording();
        }

        setRecordingActionButtonState();
    }

    private void setRecordingActionButtonState() {
        MenuItem menuItem = mMenu.getItem(0);

        if (mLocationService.isRecording()) {
            menuItem.setIcon(getDrawable(R.drawable.ic_stop));
            menuItem.setTitle(getString(R.string.stop_recording));
        } else {
            menuItem.setIcon(getDrawable(R.drawable.ic_record));
            menuItem.setTitle(getString(R.string.record));
        }
    }

    //
    // Inner classes
    //

    private ServiceConnection mLocationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            LocationService.LocationServiceBinder binder = (LocationService.LocationServiceBinder) service;
            mLocationService = (LocationService) binder.getService();
            mLocationServiceBound = true;

            // Set the initial state of the recording action button
            setRecordingActionButtonState();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mLocationServiceBound = false;
        }
    };

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
