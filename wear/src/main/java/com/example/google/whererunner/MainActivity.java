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

import android.util.Log;
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

    @SuppressWarnings("unused")
    private FirebaseAnalytics mFirebaseAnalytics;

    public static final String ACTION_SHOW_WORKOUT = "com.example.google.whererunner.SHOW_WORKOUT";
    public static final String ACTION_START_WORKOUT = "vnd.google.fitness.TRACK";

    private static final int REQUEST_PERMISSIONS = 1;

    private Menu mMenu;

    private WearableDrawerLayout mWearableDrawerLayout;
    private WearableActionDrawer mWearableActionDrawer;
    private WearableNavigationDrawer.WearableNavigationDrawerAdapter mWearableNavigationDrawerAdapter;
    private Fragment mCurrentViewPagerFragment;

    private WorkoutRecordingService mWorkoutRecordingService;
    private final BroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();
    private final ServiceConnection mWorkoutRecordingServiceConnection = new MyServiceConnection();

    private static final long AMBIENT_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10);

    private AlarmManager mAmbientStateAlarmManager;
    private PendingIntent mAmbientStatePendingIntent;

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

        mWearableNavigationDrawerAdapter = new MyWearableNavigationDrawerAdapter();
        WearableNavigationDrawer wearableNavigationDrawer = (WearableNavigationDrawer) findViewById(R.id.nav_drawer);
        wearableNavigationDrawer.setAdapter(mWearableNavigationDrawerAdapter);

        mWearableActionDrawer = (WearableActionDrawer) findViewById(R.id.action_drawer);
        mWearableActionDrawer.setOnMenuItemClickListener(this);

        // Using a custom peek view since currently the drawer draws a white circle behind drawables
        // in the drawer, but not in the peek, which causes the drawable to look different in the
        // peek vs the drawer
        // TODO re-test and remove when SDK fixed to render default action icons appropriately
        mWearableActionDrawer.setPeekContent(
                getLayoutInflater().inflate(R.layout.action_drawer_peek, null));

        mMenu = mWearableActionDrawer.getMenu();

        // If the device has a heart rate monitor, show the menu item for controlling it
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HEART_RATE)) {
            MenuItem menuItem = mMenu.findItem(R.id.heart_rate_menu_item);
            menuItem.setVisible(true);
        } else {
            // The Wear SDK displays invisible menu items. So, delete the unused/invisible item to
            // ensure it isn't displayed
            mMenu.removeItem(R.id.heart_rate_menu_item);
        }

        mWearableDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);
        mCurrentViewPagerFragment = new WorkoutMainFragment();

        if (ACTION_SHOW_WORKOUT.equals(getIntent().getAction())) {
                // Pass an attribute to the main workout fragment to tell it to display the workout data rather than the map
                Bundle bundle = new Bundle();
                bundle.putInt(WorkoutMainFragment.ARGUMENT_INITIAL_FRAGMENT, WorkoutMainFragment.FRAGMENT_DATA);
                mCurrentViewPagerFragment.setArguments(bundle);
        } else if (ACTION_START_WORKOUT.equals(getIntent().getAction())) {
            // TODO start the workout automatically with the correct workout type. Need to handle the state if a workout is already active
            String type = getIntent().getType();
            switch (type) {
                case "vnd.google.fitness.activity/biking":
                case "vnd.google.fitness.activity/running":
                case "vnd.google.fitness.activity/other":
            }
        } else {
            // Peek the drawers to remind the user that they're there
            mWearableDrawerLayout.peekDrawer(Gravity.TOP);
            mWearableDrawerLayout.peekDrawer(Gravity.BOTTOM);
        }

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, mCurrentViewPagerFragment).commit();

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

        // If there are permissions that haven't been granted yet, request them. Else, start the recording service
        if (permissions.size() > 0) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), REQUEST_PERMISSIONS);
        } else {
            bindService(new Intent(this, WorkoutRecordingService.class),
                    mWorkoutRecordingServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mBroadcastReceiver,
                new IntentFilter(WorkoutRecordingService.ACTION_RECORDING_STATUS_CHANGED));
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        unbindService(mWorkoutRecordingServiceConnection);

        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                // Start the recording service as long as the location permission (first permission) has been granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bindService(new Intent(this, WorkoutRecordingService.class),
                            mWorkoutRecordingServiceConnection, Context.BIND_AUTO_CREATE);
                } else {
                    // TODO notify the user that that the app can't function w/o location permission
                    finish();
                }

                break;
        }
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

        // If the current fragment supports ambient mode (it should, per onEnterAmbient()), have
        // it update its UI
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

        // If the current fragment supports ambient mode, then enter ambient mode. Else close the app.
        if (mCurrentViewPagerFragment instanceof WearableFragment) {
            mWearableDrawerLayout.closeDrawer(Gravity.TOP);
            mWearableDrawerLayout.closeDrawer(Gravity.BOTTOM);

            ((WearableFragment) mCurrentViewPagerFragment).onEnterAmbient(ambientDetails);

            scheduleAmbientUpdate();
        } else {
            // TODO if there's a recording in progress, we could instead go back to the data fragment
            finish();
        }
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

        mAmbientStateAlarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, mAmbientStatePendingIntent);
    }

    /**
     * Sets the state (record, stop) of the recording action button. This method is used when the
     * activity is created, and when the recording state changes.
     *
     * @param isRecording
     */
    private void setRecordingButtonState(boolean isRecording) {
        MenuItem menuItem = mMenu.findItem(R.id.record_menu_item);

        if (isRecording) {
            menuItem.setIcon(getDrawable(R.drawable.ic_stop));
            menuItem.setTitle(getString(R.string.stop_recording));
        } else {
            menuItem.setIcon(getDrawable(R.drawable.ic_record));
            menuItem.setTitle(getString(R.string.record));
        }
    }

    private void toggleWorkoutActivityType() {
        // Must call setActivityType() on the service first since setWorkoutActivityTypeUiState()
        // causes both drawers to refresh. The nav drawer UI refresh is handled automatically by
        // its adapter.
        switch (mWorkoutRecordingService.getActivityType()) {
            case RUNNING:
                mWorkoutRecordingService.setActivityType(WorkoutType.CYCLING);
                setWorkoutActivityTypeUiState(WorkoutType.CYCLING);
                break;
            case CYCLING:
                mWorkoutRecordingService.setActivityType(WorkoutType.RUNNING);
                setWorkoutActivityTypeUiState(WorkoutType.RUNNING);
                break;
        }
    }

    private void setWorkoutActivityTypeUiState(WorkoutType workoutType) {
        MenuItem menuItem = mMenu.findItem(R.id.activity_type_menu_item);
        menuItem.setIcon(getDrawable(workoutType.drawableId));
        menuItem.setTitle(getString(workoutType.titleId));

        // Notify the nav drawer adapter that the data has changed, so that the record icon is refreshed
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
        MenuItem menuItem = mMenu.findItem(R.id.heart_rate_menu_item);

        if (mWorkoutRecordingService.isHeartRateSensorOn()) {
            mWorkoutRecordingService.stopHeartRateService();
            menuItem.setTitle(getString(R.string.hrm_turn_on));
        } else {
            mWorkoutRecordingService.startHeartRateService();
            menuItem.setTitle(getString(R.string.hrm_turn_off));
        }
    }

    //
    // Inner classes
    //

    private class MyWearableNavigationDrawerAdapter extends WearableNavigationDrawer.WearableNavigationDrawerAdapter {
        private static final int NAV_DRAWER_ITEMS = 3;
        private static final int NAV_DRAWER_FRAGMENT_MAIN = 0;
        private static final int NAV_DRAWER_FRAGMENT_HISTORY = 1;

        @Override
        public String getItemText(int pos) {
            switch (pos) {
                case NAV_DRAWER_FRAGMENT_MAIN:
                    return getString(R.string.recording);
                case NAV_DRAWER_FRAGMENT_HISTORY:
                    return getString(R.string.history);
            }

            return null;
        }

        @Override
        public Drawable getItemDrawable(int pos) {
            switch (pos) {
                case NAV_DRAWER_FRAGMENT_MAIN:
                    int id = WorkoutType.RUNNING.invertedDrawableId;

                    // Once we have a connection to the workout recording service, the drawer is
                    // notified of a data change to refresh, to synchronize the icon
                    if (mWorkoutRecordingService != null) {
                        id = mWorkoutRecordingService.getActivityType().invertedDrawableId;
                    }

                    return getDrawable(id);
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

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case WorkoutRecordingService.ACTION_RECORDING_STATUS_CHANGED:
                    boolean isRecording = intent.getBooleanExtra(WorkoutRecordingService.EXTRA_IS_RECORDING, false);
                    setRecordingButtonState(isRecording);
            }
        }
    }

    private class MyServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mWorkoutRecordingService = ((WorkoutRecordingService.WorkoutRecordingServiceBinder) binder).getService();

            // Update the activity's actions to reflect the state from the workout service
            MenuItem menuItem = mMenu.findItem(R.id.heart_rate_menu_item);
            if (!mWorkoutRecordingService.isHeartRateSensorOn()) {
                menuItem.setTitle(getString(R.string.hrm_turn_on));
            }

            setWorkoutActivityTypeUiState(mWorkoutRecordingService.getActivityType());
            setRecordingButtonState(mWorkoutRecordingService.isRecordingWorkout());
        }

        public void onServiceDisconnected(ComponentName className) {
            mWorkoutRecordingService = null;
        }
    }
}
