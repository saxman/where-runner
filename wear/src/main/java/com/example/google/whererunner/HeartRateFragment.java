package com.example.google.whererunner;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.google.whererunner.services.HeartRateSensorService;

public class HeartRateFragment extends Fragment
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String LOG_TAG = HeartRateFragment.class.getSimpleName();

    private BroadcastReceiver hrReceiver;

    private TextView mHrMaxText;
    private TextView mHrInstantaneousText;
    private TextView mHrMinText;
    private View mHrContainer;

    private boolean isHrmActive = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heart_rate, container, false);

        mHrMaxText = (TextView) view.findViewById(R.id.hr_max);
        mHrMinText = (TextView) view.findViewById(R.id.hr_min);
        mHrInstantaneousText = (TextView) view.findViewById(R.id.hr_instantaneous);
        mHrContainer = view.findViewById(R.id.hr_container);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        hrReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case HeartRateSensorService.ACTION_HEART_RATE_CHANGED:
                        if (!isHrmActive) {
                            // Ensure the min/max views are visible, since they're gone by default
                            mHrMinText.setVisibility(View.VISIBLE);
                            mHrMaxText.setVisibility(View.VISIBLE);

                            mHrContainer.setBackgroundResource(R.drawable.ic_heart_red);
                            isHrmActive = true;
                        }

                        float val = intent.getFloatExtra(HeartRateSensorService.EXTRA_HEART_RATE, -1);
                        mHrInstantaneousText.setText(String.valueOf(val));

                        val = intent.getFloatExtra(HeartRateSensorService.EXTRA_HEART_RATE_MIN, -1);
                        mHrMinText.setText(String.valueOf(val));

                        val = intent.getFloatExtra(HeartRateSensorService.EXTRA_HEART_RATE_MAX, -1);
                        mHrMaxText.setText(String.valueOf(val));

                        /// TODO animate the heart to beat at the current rate

                        break;

                    case HeartRateSensorService.ACTION_HEART_RATE_SENSOR_TIMEOUT:
                        mHrContainer.setBackgroundResource(R.drawable.ic_heart);
                        isHrmActive = false;

                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        filter.addAction(HeartRateSensorService.ACTION_HEART_RATE_SENSOR_TIMEOUT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(hrReceiver, filter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(hrReceiver);

        super.onStop();
    }
}
