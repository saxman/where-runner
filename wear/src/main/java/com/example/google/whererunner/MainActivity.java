package com.example.google.whererunner;

import com.example.google.whererunner.framework.RouteDataService;
import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.FusedLocationService;
import com.example.google.whererunner.services.GpsLocationService;
import com.example.google.whererunner.services.LocationService;
import com.example.google.whererunner.services.LocationServiceBinder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.widget.TextClock;

import java.util.ArrayList;

public class MainActivity extends WearableActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback, RouteDataService,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private GridViewPager mViewPager;
    private FragmentGridPagerAdapter mViewPagerAdapter;

    private TextClock mTextClock;

    private GoogleApiClient mGoogleApiClient;

    private Location mLastLocation;
    private double mDistance;
    private double mDuration;
    private ArrayList<Location> mPath = new ArrayList<>();

    private LocationService mLocationService;
    boolean mServiceBound = false;
    private LocationChangedReceiver mLocationChangedReceiver;

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1;

    private static final int FRAGMENT_ROUTE = 0;
    private static final int FRAGMENT_DATA = 1;
    private static final int FRAGMENT_HEART = 2;
    private static final int FRAGMENT_CONTROL = 3;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        mViewPager = (GridViewPager) findViewById(R.id.pager);
        mViewPagerAdapter = new MyFragmentGridPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);

        mTextClock = (TextClock) findViewById(R.id.time);

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

        Intent intent;
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            intent = new Intent(this, GpsLocationService.class);
        } else {
            intent = new Intent(this, FusedLocationService.class);
        }

        // XXX
        intent = new Intent(this, FusedLocationService.class);

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
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        mTextClock.setFormat12Hour("h:mm");
        mTextClock.setFormat24Hour("H:mm");

        Point p = mViewPager.getCurrentItem();
        Fragment fragment = mViewPagerAdapter.findExistingFragment(p.y, p.x);
        if (fragment instanceof WearableFragment) {
            ((WearableFragment) fragment).onEnterAmbient(ambientDetails);
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        mTextClock.setFormat12Hour("h:mm:ss");
        mTextClock.setFormat24Hour("H:mm:ss");

        Point p = mViewPager.getCurrentItem();
        Fragment fragment = mViewPagerAdapter.findExistingFragment(p.y, p.x);
        if (fragment instanceof WearableFragment) {
            ((WearableFragment) fragment).onExitAmbient();
        }
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();

        Point p = mViewPager.getCurrentItem();
        Fragment fragment = mViewPagerAdapter.findExistingFragment(p.y, p.x);
        if (fragment instanceof WearableFragment) {
            ((WearableFragment) fragment).onUpdateAmbient();
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

            if (location != null) {
                Log.d(LOG_TAG, "Last known location " + location.toString());

                Point p = mViewPager.getCurrentItem();
                if (p.y == FRAGMENT_ROUTE) {
                    ((RouteMapFragment) mViewPagerAdapter.findExistingFragment(p.y, p.x)).setInitialLocation(location);
                }
            } else {
                Log.w(LOG_TAG, "Unable to retrieve user's last known location");
            }
        } catch (SecurityException e) {
            // noop since getLastLocation is only called after location access has been granted
            // TODO fix lint check and remove SecurityException
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

    private class LocationChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(LocationService.ACTION_LOCATION_CHANGED)) {
                Log.d(LOG_TAG, "Location received");

                Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);

                if (mLastLocation != null) {
                    LatLng lastLl = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    LatLng nextLl = new LatLng(location.getLatitude(), location.getLongitude());
                    mDistance += SphericalUtil.computeDistanceBetween(lastLl, nextLl);

                    mDuration = mLastLocation.getTime() - mPath.get(0).getTime();
                }

                mPath.add(location);
                mLastLocation = location;

                Point p = mViewPager.getCurrentItem();
                Fragment fragment = mViewPagerAdapter.findExistingFragment(p.y, p.x);
                if (fragment instanceof RouteDataService.RouteDataUpdateListener) {
                    ((RouteDataService.RouteDataUpdateListener) fragment).onRouteDataUpdated();
                }
            }
        }
    }

    public int toggleRecording() {
        Log.d(LOG_TAG, "Toggling location recording");

        return mLocationService.toggleLocationUpdates();
    }

    private class MyFragmentGridPagerAdapter extends FragmentGridPagerAdapter {
        public MyFragmentGridPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getFragment(int row, int col) {
            Log.d(LOG_TAG, "Getting fragment at row index " + row);

            Fragment fragment = null;

            switch (row) {
                case FRAGMENT_ROUTE:
                    fragment = new RouteMapFragment();
                    break;
                case FRAGMENT_DATA:
                    fragment = new DataFragment();
                    break;
                case FRAGMENT_HEART:
                    fragment = new HeartFragment();
                    break;
                case FRAGMENT_CONTROL:
                    fragment = new ControlFragment();
                    break;
            }

            return fragment;
        }

        @Override
        public int getRowCount() {
            return 4;
        }

        @Override
        public int getColumnCount(int i)
        {
            return 1;
        }
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
