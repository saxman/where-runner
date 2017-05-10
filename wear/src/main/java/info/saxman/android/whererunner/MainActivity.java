package info.saxman.android.whererunner;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.wearable.view.ConfirmationOverlay;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.google.android.gms.maps.MapView;

import info.saxman.android.whererunner.model.WorkoutType;
import info.saxman.android.whererunner.framework.WearableFragment;
import info.saxman.android.whererunner.services.HeartRateSensorService;
import info.saxman.android.whererunner.services.WorkoutRecordingService;

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

        setTheme(R.style.AppTheme);
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
        // Ensures the drawer remains in peek mode.
        mWearableActionDrawer.setCanAutoPeek(false);

        mMenu = mWearableActionDrawer.getMenu();

        // If the device has a heart rate monitor, un-hide the menu item for controlling it
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HEART_RATE)) {
            MenuItem menuItem = mMenu.findItem(R.id.heart_rate_menu_item);
            menuItem.setVisible(true);
        } else {
            // The Wear SDK displays invisible menu items. So, delete the unused/invisible item to
            // ensure it isn't displayed
            // TODO bug?
            mMenu.removeItem(R.id.heart_rate_menu_item);
        }

        mWearableDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);
        mWearableDrawerLayout.peekDrawer(Gravity.BOTTOM);

        mCurrentViewPagerFragment = new WorkoutMainFragment();

        // Set up the UI based on intent action. Starting and stopping the recording is handled once
        // the recording service has been connected to.
        String action = getIntent().getAction();
        if (ACTION_SHOW_HEART_RATE.equals(action)) {
            // Pass an attribute to the main workout fragment to tell it to display the heart rate.
            Bundle bundle = new Bundle();
            bundle.putInt(WorkoutMainFragment.ARGUMENT_WORKOUT_VIEW, WorkoutMainFragment.VALUE_WORKOUT_VIEW_HEART_RATE);
            mCurrentViewPagerFragment.setArguments(bundle);
        }

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.main_content_view, mCurrentViewPagerFragment);
        ft.commit();

        // Temporarily peek the nav drawer to help ensure the user is aware of it
        ViewTreeObserver observer = mWearableDrawerLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mWearableDrawerLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mWearableDrawerLayout.peekDrawer(Gravity.TOP);
            }
        });

        // Initialize the MapView in a background thread so that it loads faster when needed
        // Reduces MapView.onCreate() time from 900 to 100 mAnimationDurationMs on an LG Urban 2nd Edition
        // ref: http://stackoverflow.com/questions/26265526/what-makes-my-map-fragment-loading-slow
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MapView mv = new MapView(getApplicationContext());
                    mv.onCreate(null);
                    mv.onPause();
                    mv.onDestroy();
                } catch (Exception e) {}
            }
        }).start();
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
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                    != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.BODY_SENSORS);
        }

        // If there are permissions that haven't been granted yet, request them. Else, start the
        // recording service.
        if (permissions.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    permissions.toArray(new String[permissions.size()]), REQUEST_PERMISSIONS);
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
    public void onDestroy() {
        mAmbientStateAlarmManager.cancel(mAmbientStatePendingIntent);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                // Start the recording service as long as the location permission (first permission)
                // has been granted.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (event.getRepeatCount() == 0) {
            if (keyCode == KeyEvent.KEYCODE_STEM_1) {
                toggleWorkoutRecording();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_STEM_2) {
                // do something
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_STEM_3) {
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }


    //
    // WearableActivity methods
    //

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        mWearableDrawerLayout.closeDrawer(Gravity.TOP);
        mWearableDrawerLayout.closeDrawer(Gravity.BOTTOM);

        // If the current fragment supports ambient mode, then enter ambient mode.
        if (mCurrentViewPagerFragment instanceof WearableFragment) {
            ((WearableFragment) mCurrentViewPagerFragment).onEnterAmbient(ambientDetails);
            scheduleAmbientUpdate();
        } else {
            finish();

            // TODO find some way to switch to the main workout fragment in ambient mode
            // The following call will switch to the main workout fragment; however, the fragment
            // doesn't know that it's in ambient mode
            // mWearableNavigationDrawer.setCurrentItem(0, false)
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        mWearableDrawerLayout.peekDrawer(Gravity.BOTTOM);

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

        mAmbientStateAlarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs,
                mAmbientStatePendingIntent);
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
        // Must call setWorkoutType() on the service first since setWorkoutActivityTypeUiState()
        // causes both drawers to refresh. The nav drawer UI refresh is handled automatically by
        // its adapter, based on the activity type of the service.
        switch (mWorkoutRecordingService.getWorkoutType()) {
            case RUNNING:
                mWorkoutRecordingService.setWorkoutType(WorkoutType.CYCLING);
                setWorkoutActivityTypeUiState(WorkoutType.CYCLING);
                break;
            case CYCLING:
                mWorkoutRecordingService.setWorkoutType(WorkoutType.RUNNING);
                setWorkoutActivityTypeUiState(WorkoutType.RUNNING);
                break;
        }
    }

    private void setWorkoutActivityTypeUiState(WorkoutType workoutType) {
        MenuItem menuItem = mMenu.findItem(R.id.activity_type_menu_item);
        menuItem.setIcon(getDrawable(workoutType.drawableId));
        menuItem.setTitle(getString(R.string.activity_type) + ": " + getString(workoutType.titleId));

        // Notify the adapter that the data has changed, so that the workout type icon is refreshed.
        mWearableNavigationDrawerAdapter.notifyDataSetChanged();
    }

    private void toggleWorkoutRecording() {
        if (WorkoutRecordingService.isRecording) {
            DialogFragment dialogFragment = new MyDialogFragment();
            dialogFragment.show(getFragmentManager(), "");
        } else {
            mWorkoutRecordingService.startRecordingWorkout();
        }
    }

    private void toggleHearRateSensor() {
        MenuItem menuItem = mMenu.findItem(R.id.heart_rate_menu_item);

        if (HeartRateSensorService.isActive) {
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

    private class MyWearableNavigationDrawerAdapter
            extends WearableNavigationDrawer.WearableNavigationDrawerAdapter {

        private static final int NAV_DRAWER_ITEMS = 4;

        private static final int NAV_DRAWER_FRAGMENT_MAIN = 0;
        private static final int NAV_DRAWER_FRAGMENT_MAP = 1;
        private static final int NAV_DRAWER_FRAGMENT_HISTORY = 2;
        private static final int NAV_DRAWER_FRAGMENT_SETTINGS = 3;

        @Override
        public String getItemText(int pos) {
            switch (pos) {
                case NAV_DRAWER_FRAGMENT_MAIN:
                    return getString(R.string.recording);
                case NAV_DRAWER_FRAGMENT_MAP:
                    return getString(R.string.map);
                case NAV_DRAWER_FRAGMENT_HISTORY:
                    return getString(R.string.history);
                case NAV_DRAWER_FRAGMENT_SETTINGS:
                    return getString(R.string.settings);
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
                        id = mWorkoutRecordingService.getWorkoutType().invertedDrawableId;
                    }

                    return getDrawable(id);
                case NAV_DRAWER_FRAGMENT_MAP:
                    return getDrawable(R.drawable.ic_place);
                case NAV_DRAWER_FRAGMENT_HISTORY:
                    return getDrawable(R.drawable.ic_history);
                case NAV_DRAWER_FRAGMENT_SETTINGS:
                    return getDrawable(R.drawable.ic_settings);
            }

            return null;
        }

        @Override
        public void onItemSelected(int pos) {
            switch (pos) {
                case NAV_DRAWER_FRAGMENT_MAIN:
                    mCurrentViewPagerFragment = new WorkoutMainFragment();

                    // Unlock and re-peek the action drawer as it may have been locked closed for
                    // other views.
                    mWearableActionDrawer.unlockDrawer();
                    mWearableDrawerLayout.peekDrawer(Gravity.BOTTOM);

                    break;

                case NAV_DRAWER_FRAGMENT_MAP:
                    Bundle bundle = new Bundle();
                    bundle.putInt(WorkoutMainFragment.ARGUMENT_WORKOUT_VIEW,
                            WorkoutMainFragment.VALUE_WORKOUT_VIEW_MAP);

                    mCurrentViewPagerFragment = new WorkoutMainFragment();
                    mCurrentViewPagerFragment.setArguments(bundle);

                    mWearableActionDrawer.lockDrawerClosed();

                    break;

                case NAV_DRAWER_FRAGMENT_HISTORY:
                    mCurrentViewPagerFragment = new HistoryMainFragment();
                    mWearableActionDrawer.lockDrawerClosed();
                    break;

                case NAV_DRAWER_FRAGMENT_SETTINGS:
                    mCurrentViewPagerFragment = new SettingsFragment();
                    mWearableActionDrawer.lockDrawerClosed();
                    break;
            }

            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_content_view, mCurrentViewPagerFragment)
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
                    boolean isRecording = intent.getBooleanExtra(
                            WorkoutRecordingService.EXTRA_IS_RECORDING, false);
                    setRecordingButtonState(isRecording);
                    break;
            }
        }
    }

    private class MyServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mWorkoutRecordingService =
                    ((WorkoutRecordingService.WorkoutRecordingServiceBinder) binder).getService();

            if (ACTION_START_WORKOUT.equals(getIntent().getAction())) {
                switch (getIntent().getType()) {
                    case MIME_TYPE_WORKOUT_BIKING:
                        mWorkoutRecordingService.setWorkoutType(WorkoutType.CYCLING);
                        break;
                    case MIME_TYPE_WORKOUT_RUNNING:
                        mWorkoutRecordingService.setWorkoutType(WorkoutType.RUNNING);
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
            if (!HeartRateSensorService.isActive) {
                menuItem.setTitle(getString(R.string.hrm_turn_on));
            }

            setWorkoutActivityTypeUiState(mWorkoutRecordingService.getWorkoutType());
            setRecordingButtonState(WorkoutRecordingService.isRecording);
        }

        public void onServiceDisconnected(ComponentName className) {
            mWorkoutRecordingService = null;
        }
    }

    public static class MyDialogFragment extends DialogFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.alert_workout_end, container, false);

            View confirmButton = view.findViewById(R.id.confirm_button);
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity) getActivity()).mWorkoutRecordingService.stopRecordingWorkout();
                    dismiss();
                }
            });

            View cancelButton = view.findViewById(R.id.cancel_button);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });

            return view;
        }
    }
}
