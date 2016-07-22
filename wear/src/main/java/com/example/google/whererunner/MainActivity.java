package com.example.google.whererunner;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
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

import com.google.firebase.analytics.FirebaseAnalytics;

import com.example.google.whererunner.model.Workout;
import com.example.google.whererunner.model.WorkoutType;
import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.persistence.WorkoutDbHelper;
import com.example.google.whererunner.services.WorkoutRecordingService;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        WearableActionDrawer.OnMenuItemClickListener {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // Firebase Analytics
    private FirebaseAnalytics mFirebaseAnalytics;

    private static final int NAV_DRAWER_ITEMS = 3;
    private static final int NAV_DRAWER_FRAGMENT_MAIN = 0;
    private static final int NAV_DRAWER_FRAGMENT_HISTORY = 1;
    private static final int NAV_DRAWER_FRAGMENT_SETTINGS = 2;

    private static final int ACTION_TOGGLE_RECORDING = 0;
    private static final int ACTION_TOGGLE_ACTIVITY_TYPE = 1;
    private static final int ACTION_TOGGLE_HEART_RATE = 2;

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

    private static final long AMBIENT_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10);

    private AlarmManager mAmbientStateAlarmManager;
    private PendingIntent mAmbientStatePendingIntent;


    private WorkoutRecordingService mWorkoutRecordingService;

    private ServiceConnection mWorkoutRecordingServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mWorkoutRecordingService = ((WorkoutRecordingService.WorkoutRecordingServiceBinder) binder).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mWorkoutRecordingService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        mAmbientStateAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent ambientStateIntent = new Intent(getApplicationContext(), MainActivity.class);

        mAmbientStatePendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0 /* requestCode */,
                ambientStateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

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

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HEART_RATE)) {
            MenuItem menuItem = mMenu.getItem(ACTION_TOGGLE_HEART_RATE);
            menuItem.setVisible(true);
        }

        mWearableDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);
        mWearableDrawerLayout.peekDrawer(Gravity.TOP);
        mWearableDrawerLayout.peekDrawer(Gravity.BOTTOM);

        setRecordingButtonUiState(WorkoutRecordingService.isRecording);

        // Initialize Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecordingBroadcastReceiver);
        unbindService(mWorkoutRecordingServiceConnection);

        super.onStop();
    }

    /**
     * Called by the AlarmManager that is active while the app is in ambient mode, to update the UI
     * at a more frequent rate than is standard for ambient mode.
     *
     * @param intent
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        if (mCurrentViewPagerFragment instanceof WearableFragment) {
            ((WearableFragment) mCurrentViewPagerFragment).onUpdateAmbient();
        }

        scheduleAmbientUpdate();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.record_menu_item:
                toggleWorkoutRecording();
                mWearableDrawerLayout.closeDrawer(Gravity.BOTTOM);
                break;

            case R.id.activity_type_menu_item:
                toggleWorkoutActivityType();
                break;

            case R.id.heart_rate_menu_item:
                toggleHearRateSensor();
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

        scheduleAmbientUpdate();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        if (mCurrentViewPagerFragment instanceof WearableFragment) {
            ((WearableFragment) mCurrentViewPagerFragment).onExitAmbient();
        }

        mAmbientStateAlarmManager.cancel(mAmbientStatePendingIntent);
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

    private void scheduleAmbientUpdate() {
        long timeMs = System.currentTimeMillis();
        long delayMs = AMBIENT_INTERVAL_MS - (timeMs % AMBIENT_INTERVAL_MS);
        long triggerTimeMs = timeMs + delayMs;

        mAmbientStateAlarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                mAmbientStatePendingIntent);
    }

    private void startRecordingService() {
        Intent intent = new Intent(this, WorkoutRecordingService.class);
        bindService(intent, mWorkoutRecordingServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setRecordingButtonUiState(boolean isRecording) {
        MenuItem menuItem = mMenu.getItem(ACTION_TOGGLE_RECORDING);

        if (isRecording) {
            menuItem.setIcon(getDrawable(R.drawable.ic_stop));
            menuItem.setTitle(getString(R.string.stop_recording));
        } else {
            menuItem.setIcon(getDrawable(R.drawable.ic_record));
            menuItem.setTitle(getString(R.string.record));
        }
    }

    private void toggleWorkoutActivityType() {
        MenuItem menuItem = mMenu.getItem(ACTION_TOGGLE_ACTIVITY_TYPE);

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

    private void toggleWorkoutRecording() {
        if (mWorkoutRecordingService.isRecordingWorkout()) {
            mWorkoutRecordingService.stopRecordingWorkout();
        } else {
            mWorkoutRecordingService.startRecordingWorkout();
        }
    }

    private void toggleHearRateSensor() {
        MenuItem menuItem = mMenu.getItem(ACTION_TOGGLE_HEART_RATE);

        if (mWorkoutRecordingService.isHeartRateSensorOn()) {
            mWorkoutRecordingService.stopHeartRateService();
            menuItem.setTitle(getString(R.string.hrm_turn_on));
        } else {
            mWorkoutRecordingService.startHeartRateService();
            menuItem.setTitle(getString(R.string.hrm_turn_off));
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
                    // Ensure the action drawer is visible, since it could have been hidden for other nav drawer pages
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
