package info.saxman.android.whererunner;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wear.ambient.AmbientMode;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.MapView;

import java.util.ArrayList;
import java.util.Locale;

import info.saxman.android.whererunner.framework.WearableFragment;
import info.saxman.android.whererunner.model.WorkoutType;
import info.saxman.android.whererunner.services.HeartRateSensorService;
import info.saxman.android.whererunner.services.WorkoutRecordingService;

public class MainActivity extends Activity
        implements ActivityCompat.OnRequestPermissionsResultCallback,
        AmbientMode.AmbientCallbackProvider,
        SharedPreferences.OnSharedPreferenceChangeListener {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    public static final String ACTION_SHOW_WORKOUT = "SHOW_WORKOUT";
    public static final String ACTION_STOP_WORKOUT = "STOP_WORKOUT";
    public static final String ACTION_STOP_WORKOUT_CONFIRM = "STOP_WORKOUT_CONFIRM";

    private static final String ACTION_START_WORKOUT = "vnd.google.fitness.TRACK";
    private static final String EXTRA_START_WORKOUT_STATUS = "actionStatus";
    private static final String EXTRA_START_WORKOUT_STATUS_START = "ActiveActionStatus";
    private static final String EXTRA_START_WORKOUT_STATUS_STOP = "CompletedActionStatus";

    private static final String ACTION_SHOW_HEART_RATE = "vnd.google.fitness.VIEW";

    private static final String MIME_TYPE_WORKOUT_BIKING = "vnd.google.fitness.activity/biking";
    private static final String MIME_TYPE_WORKOUT_RUNNING =  "vnd.google.fitness.activity/running";
    private static final String MIME_TYPE_WORKOUT_OTHER = "vnd.google.fitness.activity/other";

    private static final int REQUEST_PERMISSIONS = 1;
    private final static int REQUEST_GOOGLE_PLAY_SERVICES_RESOLUTION = 10;

    private Menu mMenu;

    private WearableActionDrawerView mWearableActionDrawer;
    private WearableNavigationDrawerView mWearableNavigationDrawer;
    private MyWearableNavigationDrawerAdapter mWearableNavigationDrawerAdapter;

    private Fragment mContentFragment;
    private TimeStatusFragment mStatusFragment;

    private WorkoutRecordingService mWorkoutRecordingService;
    private final ServiceConnection mWorkoutRecordingServiceConnection = new MyServiceConnection();
    private final BroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the theme to the app's standard theme, which replaces the theme which includes a
        // background image, for start-up (i.e. creating a splash screen).
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);

        // If Google Play services is not installed or needs updating, forgo any other app
        // initialization.
        if (!isGooglePlayServicesAvailable()) {
            return;
        }

        AmbientMode.attachAmbientSupport(this);

        if (Locale.getDefault().equals(Locale.US)) {
            PreferenceManager.setDefaultValues(this, R.xml.prefs_settings_us, false);
        } else {
            PreferenceManager.setDefaultValues(this, R.xml.prefs_settings, false);
        }

        mWearableNavigationDrawerAdapter = new MyWearableNavigationDrawerAdapter();

        mWearableNavigationDrawer = findViewById(R.id.nav_drawer);
        mWearableNavigationDrawer.setAdapter(mWearableNavigationDrawerAdapter);
        mWearableNavigationDrawer.addOnItemSelectedListener(mWearableNavigationDrawerAdapter);
        // Ensures the drawer doesn't peek on child view scrolling.
        mWearableNavigationDrawer.setIsAutoPeekEnabled(false);

        // Temporarily peek the nav drawer to help ensure the user is aware of it
        ViewTreeObserver observer = mWearableNavigationDrawer.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mWearableNavigationDrawer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mWearableNavigationDrawer.getController().peekDrawer();
            }
        });

        mWearableActionDrawer = findViewById(R.id.action_drawer);
        // Ensures the drawer remains in peek mode.
        mWearableActionDrawer.setIsAutoPeekEnabled(false);
        mWearableActionDrawer.getController().peekDrawer();
        mWearableActionDrawer.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
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
        });

        mMenu = mWearableActionDrawer.getMenu();

        // If the device has a heart rate monitor, un-hide the menu item for controlling it.
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HEART_RATE)) {
            MenuItem menuItem = mMenu.findItem(R.id.heart_rate_menu_item);
            menuItem.setVisible(true);
        } else {
            // The Wear SDK displays invisible menu items. So, delete the unused/invisible item to
            // ensure it isn't displayed
            // TODO bug?
            mMenu.removeItem(R.id.heart_rate_menu_item);
        }

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

        // Ensure the nav drawer shows the workout data screen as selected.
        handleIntent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If we've requested to resolve a Google Play Services issue, close the app.
        if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES_RESOLUTION) {
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!isGooglePlayServicesAvailable()) {
            return;
        }

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

        if (!isGooglePlayServicesAvailable()) {
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WorkoutRecordingService.ACTION_SERVICE_STATE_CHANGED);

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (!isGooglePlayServicesAvailable()) {
            return;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        if (mWorkoutRecordingService != null) {
            unbindService(mWorkoutRecordingServiceConnection);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                // Start the recording service as long as the location permission (firstChange permission)
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

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // App could have been re-launched with an intent to display specific data (e.g. heart rate).
        handleIntent();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (event.getRepeatCount() == 0) {
            if (keyCode == KeyEvent.KEYCODE_STEM_1) {
                toggleWorkoutRecording();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_STEM_2) {
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_STEM_3) {
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    /*
     * SharedPreferences.OnSharedPreferenceChangeListener interface method
     */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_use_watch_gps))) {
            mWorkoutRecordingService.startLocationService();
        }
    }

    /*
     * AmbientMode.AmbientCallbackProvider interface method (and ambient mode support)
     */

    @Override
    public AmbientMode.AmbientCallback getAmbientCallback() {
        return new AmbientMode.AmbientCallback() {
            @Override
            public void onEnterAmbient(Bundle ambientDetails) {
                super.onEnterAmbient(ambientDetails);

                mWearableActionDrawer.getController().closeDrawer();
                mWearableNavigationDrawer.getController().closeDrawer();

                mStatusFragment.onEnterAmbient(ambientDetails);

                // If the current fragment does not support ambient mode, exit the app.
                if (mContentFragment instanceof WearableFragment) {
                    ((WearableFragment) mContentFragment).onEnterAmbient(ambientDetails);
                } else {
                    // mWearableNavigationDrawer.setCurrentItem(0, false);
                    finish();
                }
            }

            @Override
            public void onExitAmbient() {
                super.onExitAmbient();

                mWearableActionDrawer.getController().peekDrawer();

                mStatusFragment.onExitAmbient();

                if (mContentFragment instanceof WearableFragment) {
                    ((WearableFragment) mContentFragment).onExitAmbient();
                }
            }

            @Override
            public void onUpdateAmbient() {
                super.onUpdateAmbient();

                if (mContentFragment instanceof WearableFragment) {
                    ((WearableFragment) mContentFragment).onUpdateAmbient();
                }
            }
        };
    }

    /*
     * Class methods (public)
     */

    public void navigateBack() {
        getFragmentManager().popBackStackImmediate();
        mWearableNavigationDrawerAdapter.navigateBack();
    }

    /*
     * Class methods (private)
     */

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, REQUEST_GOOGLE_PLAY_SERVICES_RESOLUTION).show();
            }

            return false;
        }

        return true;
    }

    private void handleIntent() {
        // TODO if we're already on the data screen, then should just re-position
        // TODO should likely have a navigateToScreen method to avoid duplicate content in the nav drawer adapter

        // Any intent besides Intent.ACTION_MAIN brings us (back) to the main screen.

        mContentFragment = new WorkoutDataFragment();

        if (ACTION_SHOW_HEART_RATE.equals(getIntent().getAction())) {
            // Pass an attribute to the main workout fragment to tell it to display the heart rate.
            Bundle bundle = new Bundle();
            bundle.putInt(WorkoutDataFragment.ARGUMENT_WORKOUT_DATA_VIEW,
                    WorkoutDataFragment.VALUE_WORKOUT_VIEW_HEART_RATE);
            mContentFragment.setArguments(bundle);
        }

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.main_content_view, mContentFragment);
        ft.commit();

        addTimeStatusFragment(TimeStatusFragment.DISPLAY_MODE_DARK);

        // Ensure the nav drawer shows the workout data screen as selected.
        mWearableNavigationDrawer.setCurrentItem(
                MyWearableNavigationDrawerAdapter.NAV_DRAWER_FRAGMENT_MAIN, false);
    }

    private void addTimeStatusFragment(int displayMode) {
        Bundle args = new Bundle();
        args.putInt(TimeStatusFragment.EXTRA_DISPLAY_MODE, displayMode);
        mStatusFragment = new TimeStatusFragment();
        mStatusFragment.setArguments(args);
        getFragmentManager().beginTransaction().add(R.id.main_content_view, mStatusFragment).commit();
    }

    private void startWorkoutRecordingService() {
        bindService(new Intent(this, WorkoutRecordingService.class),
                mWorkoutRecordingServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void toggleWorkoutActivityType() {
        // Must callback setWorkoutType() on the service firstChange since setWorkoutActivityTypeUiState()
        // causes both drawers to refresh. The nav drawer UI refresh is handled automatically by
        // its adapter, based on the activity type of the service.
        switch (mWorkoutRecordingService.getWorkoutType()) {
            case RUNNING:
                mWorkoutRecordingService.setWorkoutType(WorkoutType.CYCLING);
                break;
            case CYCLING:
                mWorkoutRecordingService.setWorkoutType(WorkoutType.RUNNING);
                break;
        }
    }

    private void toggleWorkoutRecording() {
        if (WorkoutRecordingService.isRecording) {
            DialogFragment dialogFragment = new EndWorkoutDialogFragment();
            dialogFragment.show(getFragmentManager(), "");
        } else {
            mWorkoutRecordingService.startRecordingWorkout();
        }
    }

    private void toggleHearRateSensor() {
        if (HeartRateSensorService.isActive) {
            mWorkoutRecordingService.stopHeartRateService();
        } else {
            mWorkoutRecordingService.startHeartRateService();
        }
    }

    /*
     * Inner classes (private)
     */

    private class MyWearableNavigationDrawerAdapter
            extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter
            implements WearableNavigationDrawerView.OnItemSelectedListener {

        private static final int NAV_DRAWER_ITEMS = 4;

        private static final int NAV_DRAWER_FRAGMENT_MAIN = 0;
        private static final int NAV_DRAWER_FRAGMENT_MAP = 1;
        private static final int NAV_DRAWER_FRAGMENT_HISTORY = 2;
        private static final int NAV_DRAWER_FRAGMENT_SETTINGS = 3;

        private int mNavigateBackToItemPosition = 0;
        private int mLastNavigationItemPosition = 0;
        private boolean mUserNavigatedBack = false;

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
        public int getCount() {
            return NAV_DRAWER_ITEMS;
        }

        @Override
        public void onItemSelected(int selectedItemPos) {
            // Don't re-create the fragment if the user re-selects the same one.
            if (mLastNavigationItemPosition == selectedItemPos) {
                return;
            }

            // TODO the following logic seems pretty complex for simply allowing back navigation. consider ways to simplify

            // If the user navigated back using the nav drawer, pop the added fragment off the stack
            // and forgo recreating the last fragment.
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStackImmediate();
                mLastNavigationItemPosition = selectedItemPos;

                if (selectedItemPos == NAV_DRAWER_FRAGMENT_MAIN) {
                    mWearableActionDrawer.getController().peekDrawer();
                }

                return;
            }

            // If the user navigated back with swipe, forgo recreating the last fragment.
            if (mUserNavigatedBack) {
                mLastNavigationItemPosition = selectedItemPos;
                mUserNavigatedBack = false;

                if (selectedItemPos == NAV_DRAWER_FRAGMENT_MAIN) {
                    mWearableActionDrawer.getController().peekDrawer();
                }

                return;
            }

            switch (selectedItemPos) {
                case NAV_DRAWER_FRAGMENT_MAIN:
                    mContentFragment = new WorkoutDataFragment();

                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.main_content_view, mContentFragment)
                            .commit();

                    // We add the time/status fragment here instead of in workout data/map
                    // fragments, since the root layout of workout data fragment needs to be a
                    // RecyclerView in order to get the nav drawer behaviour to work properly.
                    // Otherwise, the nav drawer cannot be opened from the closed state (unless the
                    // nested recycler view is at the top).
                    addTimeStatusFragment(TimeStatusFragment.DISPLAY_MODE_DARK);

                    mWearableActionDrawer.getController().peekDrawer();

                    break;

                case NAV_DRAWER_FRAGMENT_MAP:
                    mContentFragment = new WorkoutMapFragment();

                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.main_content_view, mContentFragment)
                            .commit();

                    addTimeStatusFragment(TimeStatusFragment.DISPLAY_MODE_LIGHT);

                    mWearableActionDrawer.getController().closeDrawer();

                    break;

                case NAV_DRAWER_FRAGMENT_HISTORY:
                    mContentFragment = new HistoryFragment();

                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.main_content_view, mContentFragment)
                            .commit();

                    mWearableActionDrawer.getController().closeDrawer();

                    break;

                case NAV_DRAWER_FRAGMENT_SETTINGS:
                    mContentFragment = new SettingsFragment();

                    // Add settings fragment to activity and also add it to the back stack so that
                    // it can be removed with swipe dismiss. The settings fragment overrides the
                    // swipe dismiss behaviour to pop the back stack.
                    getFragmentManager()
                            .beginTransaction()
                            .add(R.id.main_content_view, mContentFragment)
                            .addToBackStack(null)
                            .commit();

                    mWearableActionDrawer.getController().closeDrawer();

                    mNavigateBackToItemPosition = mLastNavigationItemPosition;

                    break;
            }

            mLastNavigationItemPosition = selectedItemPos;
        }

        void navigateBack() {
            mUserNavigatedBack = true;
            mWearableNavigationDrawer.setCurrentItem(mNavigateBackToItemPosition, false);
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case WorkoutRecordingService.ACTION_SERVICE_STATE_CHANGED:
                    // Set the state of the record workout UI action.
                    if (intent.hasExtra(WorkoutRecordingService.EXTRA_IS_RECORDING)) {
                        boolean isRecording = intent.getBooleanExtra(
                                WorkoutRecordingService.EXTRA_IS_RECORDING, false);

                        MenuItem menuItem = mMenu.findItem(R.id.record_menu_item);
                        if (isRecording) {
                            menuItem.setIcon(getDrawable(R.drawable.ic_stop));
                            menuItem.setTitle(getString(R.string.stop_recording));
                        } else {
                            menuItem.setIcon(getDrawable(R.drawable.ic_record));
                            menuItem.setTitle(getString(R.string.record_workout));
                        }
                    }

                    // Set the state of the workout type UI action.
                    if (intent.hasExtra(WorkoutRecordingService.EXTRA_WORKOUT_TYPE)) {
                        int i = intent.getIntExtra(WorkoutRecordingService.EXTRA_WORKOUT_TYPE,
                                WorkoutType.RUNNING.preferencesId);
                        WorkoutType workoutType = WorkoutType.getWorkoutType(i);

                        MenuItem menuItem = mMenu.findItem(R.id.activity_type_menu_item);
                        menuItem.setIcon(getDrawable(workoutType.drawableId));
                        menuItem.setTitle(getString(R.string.activity_type) + ": "
                                + getString(workoutType.titleId));

                        // Notify the nav drawer adapter that the workout type has changed, so that
                        // it can refresh the workout type icon.
                        mWearableNavigationDrawerAdapter.notifyDataSetChanged();
                    }

                    // Set the state of the HRM on/off UI action.
                    if (intent.hasExtra(WorkoutRecordingService.EXTRA_IS_HRM_ON)) {
                        boolean isHrmOn = intent.getBooleanExtra(
                                WorkoutRecordingService.EXTRA_IS_HRM_ON, false);

                        MenuItem menuItem = mMenu.findItem(R.id.heart_rate_menu_item);
                        if (menuItem != null) {
                            if (isHrmOn) {
                                menuItem.setTitle(getString(R.string.hrm_turn_off));
                            } else {
                                menuItem.setTitle(getString(R.string.hrm_turn_on));
                            }
                        }
                    }

                    break;
            }
        }
    }

    private class MyServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mWorkoutRecordingService =
                    ((WorkoutRecordingService.WorkoutRecordingServiceBinder) binder).getService();

            // Once the activity has connected to the workout service, if the activity received a
            // start intent and action (start/stop recording, hrm), tell the service what to do.
            // TODO consider using separate actions rather than inspecting extras
            if (getIntent() != null && getIntent().getAction() != null) {
                switch (getIntent().getAction()) {
                    case ACTION_START_WORKOUT:
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

                        String status = getIntent().getExtras().getString(EXTRA_START_WORKOUT_STATUS);

                        if (EXTRA_START_WORKOUT_STATUS_START.equals(status)
                                && !WorkoutRecordingService.isRecording) {
                            mWorkoutRecordingService.startRecordingWorkout();
                        } else if (EXTRA_START_WORKOUT_STATUS_STOP.equals(status)
                                && WorkoutRecordingService.isRecording) {
                            mWorkoutRecordingService.stopRecordingWorkout();
                        }

                        break;

                    case ACTION_STOP_WORKOUT:
                        mWorkoutRecordingService.stopRecordingWorkout();
                        break;

                    case ACTION_STOP_WORKOUT_CONFIRM:
                        if (WorkoutRecordingService.isRecording) {
                            DialogFragment dialogFragment = new EndWorkoutDialogFragment();
                            dialogFragment.show(getFragmentManager(), "");
                        }

                        break;

                    case ACTION_SHOW_HEART_RATE:
                        mWorkoutRecordingService.startHeartRateService();
                        break;
                }
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mWorkoutRecordingService = null;
        }
    }

    public static class EndWorkoutDialogFragment extends DialogFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.alert_workout_end, container, false);

            View confirmButton = view.findViewById(R.id.confirm_button);
            confirmButton.setOnClickListener(v -> {
                ((MainActivity) getActivity()).mWorkoutRecordingService.stopRecordingWorkout();
                dismiss();
            });

            View cancelButton = view.findViewById(R.id.cancel_button);
            cancelButton.setOnClickListener(v -> dismiss());

            return view;
        }
    }
}
