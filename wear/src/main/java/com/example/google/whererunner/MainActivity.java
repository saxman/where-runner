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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.google.whererunner.model.Workout;
import com.example.google.whererunner.model.WorkoutType;
import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.persistence.WorkoutDbHelper;
import com.example.google.whererunner.services.WorkoutRecordingService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends WearableActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        WearableActionDrawer.OnMenuItemClickListener {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int NAV_DRAWER_ITEMS = 3;
    private static final int NAV_DRAWER_FRAGMENT_MAIN = 0;
    private static final int NAV_DRAWER_FRAGMENT_SETTINGS = 2;
    private static final int NAV_DRAWER_FRAGMENT_HISTORY = 1;

    private static final int ACTION_RECORD_INDEX = 0;
    private static final int ACTION_ACTIVITY_TYPE_INDEX = 1;

    private static final int ACTIVITY_TYPE_RUNNING = WorkoutType.RUNNING;
    private static final int ACTIVITY_TYPE_CYCLING = WorkoutType.CYCLING;

    private static final int REQUEST_PERMISSIONS = 1;

    private WearableDrawerLayout mWearableDrawerLayout;
    private WearableActionDrawer mWearableActionDrawer;
    private WearableNavigationDrawer.WearableNavigationDrawerAdapter mWearableNavigationDrawerAdapter;

    private Fragment mCurrentViewPagerFragment;

    private Menu mMenu;

    private BroadcastReceiver mRecordingBroadcastReceiver;

    private int mActivityType = ACTIVITY_TYPE_RUNNING;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        setRecordingButtonUiState(WorkoutRecordingService.isRecording);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Check for the requisite permissions and, if met, start the recording service
        ArrayList<String> permissions = new ArrayList<>(2);

        // If not already granted, ask for fine location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // If the device has a HRM, also request body sensor permission, if not already granted
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HEART_RATE)
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {

            permissions.add(Manifest.permission.BODY_SENSORS);
        }

        // If there are permissions that haven't been granted yet, request them
        if (permissions.size() > 0) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), REQUEST_PERMISSIONS);
        } else {
            startRecordingService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                // Start the recording service as long as the location permission (first permission) has been granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startRecordingService();
                } else {
                    // TODO notify the user that that the app can't function w/o location permission
                    finish();
                }

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
                        case WorkoutRecordingService.ACTION_RECORDING_STATUS_CHANGED:
                            boolean isRecording = intent.getBooleanExtra(WorkoutRecordingService.EXTRA_IS_RECORDING, false);
                            setRecordingButtonUiState(isRecording);
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WorkoutRecordingService.ACTION_RECORDING_STATUS_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRecordingBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        // Tell the location service that it can stop location updates, unless it's recording
        Intent intent = new Intent(WorkoutRecordingService.ACTION_STOP_SERVICES);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecordingBroadcastReceiver);

        super.onStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Pass watch key events the current child fragment to handle
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_STEM_1:
            case KeyEvent.KEYCODE_STEM_2:
            case KeyEvent.KEYCODE_STEM_3:
            case KeyEvent.KEYCODE_STEM_PRIMARY:
            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
            case KeyEvent.KEYCODE_NAVIGATE_IN:
            case KeyEvent.KEYCODE_NAVIGATE_OUT:
                if (mCurrentViewPagerFragment instanceof WearableFragment) {
                    ((WearableFragment) mCurrentViewPagerFragment).onWearableKeyEvent(event);
                }
                return true;
            default:
                return super.onKeyDown(event.getKeyCode(), event);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.record_button:
                toggleRecording();
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

    private void startRecordingService() {
        Intent intent = new Intent(this, WorkoutRecordingService.class);
        startService(intent);
    }

    private void setRecordingButtonUiState(boolean isRecording) {
        MenuItem menuItem = mMenu.getItem(ACTION_RECORD_INDEX);

        if (isRecording) {
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

    private void toggleRecording() {
        if (WorkoutRecordingService.isRecording) {
            // Tell the location service to stop recording
            Intent intent = new Intent(WorkoutRecordingService.ACTION_STOP_RECORDING);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else {
            // Tell the location service to start recording
            Intent intent = new Intent(WorkoutRecordingService.ACTION_START_RECORDING);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
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
                case NAV_DRAWER_FRAGMENT_HISTORY:
                    return getString(R.string.history);
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
                case NAV_DRAWER_FRAGMENT_HISTORY:
                    return getDrawable(R.drawable.ic_view_day);
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
                case NAV_DRAWER_FRAGMENT_HISTORY:
                    WorkoutDbHelper dbHelper = new WorkoutDbHelper(MainActivity.this);
                    ArrayList<Workout> workouts = dbHelper.readLastFiveWorkouts();

                    fragment = HistoryMainFragment.newInstance(workouts);

                    // Hide the action drawer since we don't need its actions in the history page
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
