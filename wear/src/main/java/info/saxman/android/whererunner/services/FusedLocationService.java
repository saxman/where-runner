package info.saxman.android.whererunner.services;

import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class FusedLocationService extends LocationService {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = FusedLocationService.class.getSimpleName();

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback = new MyLocationCallback();

    @Override
    public void onCreate() {
        super.onCreate();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Request location samples at regular intervals, if they're available
        mLocationRequest = new LocationRequest()
                .setInterval(LOCATION_UPDATE_INTERVAL_MS)
                .setFastestInterval(LOCATION_UPDATE_INTERVAL_MS)
                .setMaxWaitTime(LOCATION_UPDATE_INTERVAL_MS)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // The parent class (LocationService) will start the location updates once it is bound,
        // so no need to do so here.
    }

    @Override
    protected void startLocationUpdates() {
        if (checkPermission()) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    @Override
    protected void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        isReceivingAccurateLocationSamples = false;
    }

    private class MyLocationCallback extends LocationCallback {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                onLocationChanged(location);
            }
        }
    }
}
