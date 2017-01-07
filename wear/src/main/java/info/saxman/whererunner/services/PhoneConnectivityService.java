package info.saxman.whererunner.services;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

public class PhoneConnectivityService extends Service {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = PhoneConnectivityService.class.getSimpleName();

    public static final String ACTION_PHONE_CONNECTIVITY_CHANGED = "PHONE_CONNECTIVITY_CHANGED";
    public static final String EXTRA_IS_PHONE_CONNECTED = "IS_PHONE_CONNECTED";

    private static final int UPDATE_INTERVAL_MS = 5000;

    public static boolean isPhoneConnected = false;

    private GoogleApiClient mGoogleApiClient;

    private Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        mStatusChecker.run();
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                    }
                })
                .build();
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "Destroying service");

        mHandler.removeCallbacks(mStatusChecker);

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mGoogleApiClient.connect();

        return null;
    }

    private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            final boolean wasPhoneConnected = isPhoneConnected;

            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                @Override
                public void onResult(NodeApi.GetConnectedNodesResult result) {
                    boolean isNodeNearby = false;
                    for (Node node : result.getNodes()) {
                        // only nearby nodes can give GPS results
                        if (node.isNearby()) {
                            isNodeNearby = true;
                        }
                    }

                    if (wasPhoneConnected != isNodeNearby) {
                        isPhoneConnected = isNodeNearby;

                        Intent intent = new Intent(ACTION_PHONE_CONNECTIVITY_CHANGED);
                        intent.putExtra(EXTRA_IS_PHONE_CONNECTED, isPhoneConnected);
                        LocalBroadcastManager.getInstance(PhoneConnectivityService.this).sendBroadcast(intent);
                    }

                    mHandler.postDelayed(mStatusChecker, UPDATE_INTERVAL_MS);
                }
            }, 500, TimeUnit.MILLISECONDS);
        }
    };
}
