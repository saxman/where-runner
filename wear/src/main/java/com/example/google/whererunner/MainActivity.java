package com.example.google.whererunner;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.util.Log;

import com.example.google.whererunner.framework.RouteDataService;
import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class MainActivity extends WearableActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    private MyRouteDataService mRouteDataService = new MyRouteDataService();
    private Location mInitialLocation;
    private Location mLastLocation;
    private double mDistance;
    private double mDuration;
    private ArrayList<Location> mPath = new ArrayList<>();

    private LocationService mLocationService;
    boolean mServiceBound = false;
    private LocationChangedReceiver mLocationChangedReceiver;
    private HeartRateChangedReceiver mHeartRateChangedReceiver;

    private Fragment mCurrentViewPagerFragment;

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1;

    private static final int FRAGMENT_MAIN = 0;
    private static final int FRAGMENT_SETTINGS = 1;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        WearableNavigationDrawer navDrawer = (WearableNavigationDrawer) findViewById(R.id.nav_drawer);
        navDrawer.setAdapter(new MyWearableNavigationDrawerAdapter());

        mCurrentViewPagerFragment = new MainFragment();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, mCurrentViewPagerFragment).commit();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mGoogleApiClient.connect();

        Intent intent = new Intent(this, FusedLocationService.class);
        startService(intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mLocationChangedReceiver != null) {
            unregisterReceiver(mLocationChangedReceiver);
        }
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();

        if (mServiceBound) {
            unbindService(mServiceConnection);
            mServiceBound = false;
        }

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mLocationChangedReceiver == null) {
            mLocationChangedReceiver = new LocationChangedReceiver();
        }

        IntentFilter intentFilter = new IntentFilter(LocationService.ACTION_LOCATION_CHANGED);
        registerReceiver(mLocationChangedReceiver, intentFilter);

        if (mLocationChangedReceiver == null) {
            mLocationChangedReceiver = new LocationChangedReceiver();
        }

        IntentFilter heartRateIntentFilter = new IntentFilter(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        registerReceiver(mLocationChangedReceiver, heartRateIntentFilter);
    }

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

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG_TAG, "GoogleApiClient connected");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
        } else {
            getLastLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation();
                }
            }
        }
    }

    private void getLastLocation() {
        Log.d(LOG_TAG, "Retrieving last know location");

        try {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            Log.d(LOG_TAG, "Last known location: " + location.toString());

            if (location != null) {
                mInitialLocation = location;

                if (mCurrentViewPagerFragment instanceof RouteDataService.RouteDataUpdateListener) {
                    ((RouteDataService.RouteDataUpdateListener) mCurrentViewPagerFragment).onRouteDataUpdated(mRouteDataService);
                }

            } else {
                Log.w(LOG_TAG, "Unable to retrieve user's last known location");
            }
        } catch (SecurityException e) {
            // noop since getLastLocation is only called directly after location access has been granted
            Log.e(LOG_TAG, "Exception getting last location", e);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            LocationServiceBinder binder = (LocationServiceBinder) service;
            mLocationService = (LocationService) binder.getService();
            mServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBound = false;
        }
    };

    private class HeartRateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(HeartRateSensorService.ACTION_HEART_RATE_CHANGED)) {
                Log.d(LOG_TAG, "Heart Rate Sensor Updated");
            }
        }
    }

    private class LocationChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(LocationService.ACTION_LOCATION_CHANGED)) {
                Log.d(LOG_TAG, "Location received");

                Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);

                if (mLastLocation != null) {
                    mDistance += mLastLocation.distanceTo(location);
                    // TODO duration should be calculated in real time an not based on when location samples come in
                    mDuration = mLastLocation.getTime() - mPath.get(0).getTime();
                }

                mPath.add(location);
                mLastLocation = location;

                if (mCurrentViewPagerFragment instanceof RouteDataService.RouteDataUpdateListener) {
                    ((RouteDataService.RouteDataUpdateListener) mCurrentViewPagerFragment).onRouteDataUpdated(mRouteDataService);
                }
            }
        }
    }

    class MyWearableNavigationDrawerAdapter implements WearableNavigationDrawer.WearableNavigationDrawerAdapter {
        @Override
        public String getItemText(int pos) {
            switch (pos) {
                case FRAGMENT_MAIN:
                    return getString(R.string.recording);
                case FRAGMENT_SETTINGS:
                    return getString(R.string.settings);
            }

            return null;
        }

        @Override
        public Drawable getItemDrawable(int pos) {
            switch (pos) {
                case FRAGMENT_MAIN:
                    return getDrawable(R.drawable.ic_running);
                case FRAGMENT_SETTINGS:
                    return getDrawable(R.drawable.ic_settings);
            }

            return null;
        }

        @Override
        public void onItemSelected(int pos) {
            Fragment fragment = null;

            switch (pos) {
                case FRAGMENT_MAIN:
                    fragment = new MainFragment();
                    break;
                case FRAGMENT_SETTINGS:
                    fragment = new SettingsFragment();
                    break;
            }

            mCurrentViewPagerFragment = fragment;
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    public int toggleRecording() {
        Log.d(LOG_TAG, "Toggling location recording");
        return mLocationService.toggleLocationUpdates();
    }

    private class MyRouteDataService implements RouteDataService {
        @Override
        public Location getInitialLocation() {
            return mInitialLocation;
        }

        @Override
        public double getDistance() {
            return mDistance;
        }

        @Override
        public double getDuration() {
            return mDuration;
        }

        @Override
        public ArrayList<Location> getRoute() {
            return mPath;
        }
    }
}
