package com.example.google.whererunner;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.FusedLocationService;
import com.example.google.whererunner.services.HeartRateSensorService;
import com.example.google.whererunner.services.LocationService;
import com.example.google.whererunner.services.WorkoutRecordingService;

public class MainActivity extends WearableActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        WearableActionDrawer.OnMenuItemClickListener {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int NAV_DRAWER_ITEMS = 2;
    private static final int NAV_DRAWER_FRAGMENT_MAIN = 0;
    private static final int NAV_DRAWER_FRAGMENT_SETTINGS = 1;

    private static final int ACTION_RECORD_INDEX = 0;
    private static final int ACTION_ACTIVITY_TYPE_INDEX = 1;

    private static final int ACTIVITY_TYPE_RUNNING = 0;
    private static final int ACTIVITY_TYPE_CYCLING = 1;

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int REQUEST_ACCESS_BODY_SENSORS = 2;

    private WearableDrawerLayout mWearableDrawerLayout;
    private WearableActionDrawer mWearableActionDrawer;
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
        mCurrentViewPagerFragment = new WorkoutMainFragment();
        fragmentManager.beginTransaction().replace(R.id.content_frame, mCurrentViewPagerFragment).commit();

        mWearableNavigationDrawerAdapter = new MyWearableNavigationDrawerAdapter();

        WearableNavigationDrawer wearableNavigationDrawer = (WearableNavigationDrawer) findViewById(R.id.nav_drawer);
        wearableNavigationDrawer.setAdapter(mWearableNavigationDrawerAdapter);

        mWearableActionDrawer = (WearableActionDrawer) findViewById(R.id.action_drawer);
        mWearableActionDrawer.setOnMenuItemClickListener(this);

        // Using a custom peek view since currently the drawer draws a white circle behind drawables
        // in the drawer, but not in the peek, which causes the drawable to look different in the
        // peek vs the drawer
        // TODO re-test and remove when SDK fixed
        View peekView = getLayoutInflater().inflate(R.layout.action_drawer_peek, null);
        mWearableActionDrawer.setPeekContent(peekView);

        mMenu = mWearableActionDrawer.getMenu();
        mWearableActionDrawer.setOnMenuItemClickListener(this);

        mWearableDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);
        mWearableDrawerLayout.peekDrawer(Gravity.TOP);
        mWearableDrawerLayout.peekDrawer(Gravity.BOTTOM);
    }

    @Override
    public void onStart() {
        super.onStart();

        startLocationService();
        startRecordingService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationService();
                }

                // TODO inform the user that they really, really need to grant permission

                break;
            case REQUEST_ACCESS_BODY_SENSORS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startHearRateSensorService();
                }

                // TODO allow the user to perpetually reject this permission

                break;
        }
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
        // Tell the location service that if can stop location updates, unless it's recording
        Intent intent = new Intent(LocationService.ACTION_STOP_LOCATION_UPDATES);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        // Tell the HR service that if can stop sampling heart rate
        intent = new Intent(HeartRateSensorService.ACTION_STOP_SENSOR_SERVICE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecordingBroadcastReceiver);

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

    private void startLocationService() {
        // If the user hasn't granted fine location permission, request it
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            // TODO if the device has a GPS sensor, use it instead of FLP, if the user prefers to do so (settings)
        }

        // Start the location service so that child fragments can receive location and recording
        // status updates via local broadcasts
        Intent intent = new Intent(this, FusedLocationService.class);
        startService(intent);

        // Once we've gotten location permission, ask for hrm sensor permission if the sensor exists
        // If these aren't asked serially in this manner, only one permission is surfaced to the user
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HEART_RATE)) {
            startHearRateSensorService();
        }
    }

    private void startHearRateSensorService() {
        // If the user hasn't granted body sensor permission, request it
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, REQUEST_ACCESS_BODY_SENSORS);
            return;
        }

        Intent intent = new Intent(this, HeartRateSensorService.class);
        startService(intent);
    }

    private void startRecordingService() {
        Intent intent = new Intent(this, WorkoutRecordingService.class);
        startService(intent);
    }

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

        // Notify the nav drawer adapter that the data has changed, to have the above icon refreshed
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
                    fragment = new WorkoutMainFragment();
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
