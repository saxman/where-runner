package com.example.google.whererunner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GpsStatus;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.GpsLocationService;

import java.util.Locale;

public class GpsStatusFragment extends WearableFragment {

    private static final String LOG_TAG = GpsStatusFragment.class.getSimpleName();

    private TextView mTtffTextView;
    private TextView mSatellitesTextView;

    private BroadcastReceiver mGpsStatusChangeReceiver;

    private int mTtff = -1;
    private int mSatellites = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_gps_status, container, false);

        mSatellitesTextView = (TextView) view.findViewById(R.id.satellites);
        mTtffTextView = (TextView) view.findViewById(R.id.ttff);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mGpsStatusChangeReceiver == null) {
            mGpsStatusChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int status = intent.getIntExtra(GpsLocationService.EXTRA_GPS_STATUS, -1);

                    switch (status) {
                        case GpsStatus.GPS_EVENT_FIRST_FIX:
                            mTtff = intent.getIntExtra(GpsLocationService.EXTRA_GPS_TTFF, -1);
                            break;
                        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                            mSatellites = intent.getIntExtra(GpsLocationService.EXTRA_GPS_SATELLITES, -1);
                            break;
                    }

                    if (!isAmbient()) {
                        updateUI();
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter(GpsLocationService.ACTION_GPS_STATUS_CHANGED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mGpsStatusChangeReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mGpsStatusChangeReceiver);

        super.onStop();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        // TODO
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        // TODO
    }

    @Override
    public void onUpdateAmbient() {
        updateUI();
    }

    private void updateUI() {
        String s;

        if (mTtff == -1) {
            s = "TTFF unk";
        } else {
            s = String.format(Locale.getDefault(), "%1$,.1fs to TTFF", mTtff / 1000f);
        }

        mSatellitesTextView.setText(s);

        s = String.format(Locale.getDefault(), "%d satellites", mSatellites);
        mTtffTextView.setText(s);
    }
}
