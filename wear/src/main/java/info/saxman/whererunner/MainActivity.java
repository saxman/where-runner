package info.saxman.whererunner;

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

import info.saxman.whererunner.model.Workout;
import info.saxman.whererunner.model.WorkoutType;
import info.saxman.whererunner.framework.WearableFragment;
import info.saxman.whererunner.persistence.WorkoutDbHelper;
import info.saxman.whererunner.services.HeartRateSensorService;
import info.saxman.whererunner.services.LocationService;
import info.saxman.whererunner.services.WorkoutRecordingService;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        WearableActionDrawer.OnMenuItemClickListener {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    public static final String ACTION_SHOW_WORKOUT = MainActivity.class.getPackage() + ".SHOW_WORKOUT";
    public static final String ACTION_START_WORKOUT = "vnd.google.fitness.TRACK";
    public static final String ACTION_STOP_WORKOUT = MainActivity.class.getPackage() + ".STOP_WORKOUT";
    public static final String ACTION_SHOW_HEART_RATE = "vnd.google.fitness.VIEW";

    private static final String MIME_TYPE_WORKOUT_BIKING = "vnd.google.fitness.activity/biking";
    private static final String MIME_TYPE_WORKOUT_RUNNING =  "vnd.google.fitness.activity/running";
    private static final String MIME_TYPE_WORKOUT_OTHER = "vnd.google.fitness.activity/other";

    private static final int REQUEST_PERMISSIONS = 1;

    private Menu mMenu;

    private WearableDrawerLayout mWearableDrawerLayout;
    private WearableActionDrawer mWearableActionDrawer;
    private WearableNavigationDrawer mWearableNavigationDrawer;
    private WearableNavigationDrawer.WearableNavigationDrawerAdapter mWearableNavigationDrawerAdapter;
    private Fragment mCurrentViewPagerFragment;

    private WorkoutRecordingService mWorkoutRecordingService;
    private final ServiceConnection mWorkoutRecordingServiceConnection = new MyServiceConnection();
    private final BroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();

    private static final long AMBIENT_UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10);

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
        mWearableNavigationDrawer = (WearableNavigationDrawer) findViewById(R.id.nav_drawer);
        mWearableNavigationDrawer.setAdapter(mWearableNavigationDrawerAdapter);

        mWearableActionDrawer = (WearableActionDrawer) findViewById(R.id.action_drawer);
        mWearableActionDrawer.setOnMenuItemClickListener(this);
        mWearableActionDrawer.setShouldLockWhenNotOpenOrPeeking(false);

        // Using a custom peek view since currently the drawer draws a white circle behind drawables
        // in the drawer, but not in the peek, which causes the drawable to look different in the
        // peek vs the drawer
        // TODO re-test and remove when SDK fixed to render default action icons appropriately
        View peekLayout = getLayoutInflater().inflate(R.layout.action_drawer_peek, null);
        mWearableActionDrawer.setPeekContent(peekLayout);

        mMenu = mWearableActionDrawer.getMenu();

        // If the device has a heart rate monitor, un-hide the menu item for controlling it
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

        // Set up the UI based on intent action. Starting and stopping the recording is handled once
        // the recording service has been connected to.
        String action = getIntent().getAction();
        if (Intent.ACTION_MAIN.equals(action)) {
            // If starting fresh, peek the drawers to remind the user that they're there
            mWearableDrawerLayout.peekDrawer(Gravity.TOP);
            mWearableDrawerLayout.peekDrawer(Gravity.BOTTOM);
        } else if (ACTION_SHOW_HEART_RATE.equals(action)) {
            // Pass an attribute to the main workout fragment to tell it to display the workout data
            Bundle bundle = new Bundle();
            bundle.putInt(WorkoutMainFragment.ARGUMENT_INITIAL_FRAGMENT, WorkoutMainFragment.FRAGMENT_HEART);
            mCurrentViewPagerFragment.setArguments(bundle);
        }

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, mCurrentViewPagerFragment).commit();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Check for the requisite permissions and, if met, start the recording service
        ArrayList<String> permissions = new ArrayList<>(2);

        // If not already granted, ask for fine location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
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
            startWorkoutRecordingService();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WorkoutRecordingService.ACTION_RECORDING_STATUS_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        if (mWorkoutRecordingService != null) {
            unbindService(mWorkoutRecordingServiceConnection);
        }

        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                // Start the recording service as long as the location permission (first permission) has been granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startWorkoutRecordingService();
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

    @Override
    public void onBackPressed() {
        // Bring the user back to the home view (workout data) if they're elsewhere in the app.
        // If they're already on the home view, exit the app.
        if (mCurrentViewPagerFragment instanceof WorkoutMainFragment) {
            WorkoutMainFragment wmf = (WorkoutMainFragment) mCurrentViewPagerFragment;
            if (wmf.getCurrentFragment() != WorkoutMainFragment.FRAGMENT_DATA) {
                wmf.setCurrentFragment(WorkoutMainFragment.FRAGMENT_DATA);
                return;
            }
        } else {
            mWearableNavigationDrawer.setCurrentItem(0, true);
            return;
        }

        super.onBackPressed();
    }

    //
    // WearableActivity methods
    //

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        // If the current fragment supports ambient mode, then enter ambient mode.
        if (mCurrentViewPagerFragment instanceof WearableFragment) {
            mWearableDrawerLayout.closeDrawer(Gravity.TOP);
            mWearableDrawerLayout.closeDrawer(Gravity.BOTTOM);

            ((WearableFragment) mCurrentViewPagerFragment).onEnterAmbient(ambientDetails);

            scheduleAmbientUpdate();
        } else {
            // TODO instead of exiting the app, return to the home view (workout data) and the enter ambient mode
            finish();

            // For manually switching back to the main fragment for ambient mode, there's an issue
            // where the current nav drawer fragment isn't initialized directly after
            // mWearableNavigationDrawer.setCurrentItem() returns. Therefore, we can't call
            // onEnterAmbient() on the fragment, since it's views aren't ready.
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
    // Class methods (private)
    //

    private void startWorkoutRecordingService() {
        bindService(new Intent(this, WorkoutRecordingService.class),
                mWorkoutRecordingServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void scheduleAmbientUpdate() {
        long timeMs = System.currentTimeMillis();
        long delayMs = AMBIENT_UPDATE_INTERVAL_MS - (timeMs % AMBIENT_UPDATE_INTERVAL_MS);
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
            menuItem.setTitle(getString(R.string.record_workout));
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
        menuItem.setTitle(getString(R.string.activity_type) + ": " + getString(workoutType.titleId));

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
    // Inner classes (private)
    //

    private class MyWearableNavigationDrawerAdapter extends WearableNavigationDrawer.WearableNavigationDrawerAdapter {
        private static final int NAV_DRAWER_ITEMS = 2;

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
            switch (pos) {
                case NAV_DRAWER_FRAGMENT_MAIN:
                    mCurrentViewPagerFragment = new WorkoutMainFragment();

                    // Ensure the action drawer is visible, since it could have been hidden for other nav drawer pages
                    mWearableActionDrawer.setVisibility(View.VISIBLE);
                    break;
                case NAV_DRAWER_FRAGMENT_HISTORY:
                    mCurrentViewPagerFragment = new HistoryMainFragment();

                    // Hide the action drawer since we don't need its actions in the history page
                    mWearableActionDrawer.setVisibility(View.GONE);
                    break;
            }

            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, mCurrentViewPagerFragment)
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
                    break;
            }
        }
    }

    private class MyServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mWorkoutRecordingService = ((WorkoutRecordingService.WorkoutRecordingServiceBinder) binder).getService();

            if (ACTION_START_WORKOUT.equals(getIntent().getAction())) {
                switch (getIntent().getType()) {
                    case MIME_TYPE_WORKOUT_BIKING:
                        mWorkoutRecordingService.setActivityType(WorkoutType.CYCLING);
                        break;
                    case MIME_TYPE_WORKOUT_RUNNING:
                        mWorkoutRecordingService.setActivityType(WorkoutType.RUNNING);
                        break;
                    case MIME_TYPE_WORKOUT_OTHER:
                        // Use default if starting new workout
                        break;
                }

                // TODO move constants elsewhere
                String status = getIntent().getExtras().getString("actionStatus");
                if ("ActiveActionStatus".equals(status)) {
                    mWorkoutRecordingService.startRecordingWorkout();
                } else if ("CompletedActionStatus".equals(status)) {
                    mWorkoutRecordingService.stopRecordingWorkout();
                }
            } else if (ACTION_STOP_WORKOUT.equals(getIntent().getAction())) {
                mWorkoutRecordingService.stopRecordingWorkout();
            }

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
