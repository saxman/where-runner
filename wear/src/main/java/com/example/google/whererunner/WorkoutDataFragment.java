package com.example.google.whererunner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.LocationService;
import com.example.google.whererunner.services.WorkoutRecordingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class WorkoutDataFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutDataFragment.class.getSimpleName();

    private TextView mDurationTextView;
    private TextView mDistanceTextView;

    private boolean mIsRecording = false;
    private List<Location> mPathLocations = new ArrayList<>();

    private double mDistance;
    private double mDuration;

    private BroadcastReceiver mLocationChangedReceiver;

    private Timer mDurationTimer;
    private double mStartTime = 0;

    private static final int DURATION_TIMER_INTERVAL_MS = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_workout_data, container, false);

        mDistanceTextView = (TextView) view.findViewById(R.id.distance);
        mDurationTextView = (TextView) view.findViewById(R.id.duration);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mLocationChangedReceiver == null) {
            mLocationChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case LocationService.ACTION_LOCATION_CHANGED:
                            // TODO don't need to listen for location changes until recording started

                            if (mIsRecording) {
                                Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);
                                mPathLocations.add(location);

                                // Calculate distance between last two coordinates and add to aggregate
                                if (mPathLocations.size() > 2) {
                                    Location priorLocation = mPathLocations.get(mPathLocations.size() - 2);

                                    float[] results = new float[1];
                                    Location.distanceBetween(
                                            priorLocation.getLatitude(), priorLocation.getLongitude(),
                                            location.getLatitude(), location.getLongitude(),
                                            results);

                                    mDistance += results[0];
                                }
                            }

                            break;

                        case WorkoutRecordingService.ACTION_RECORDING_STATUS:
                            mIsRecording = intent.getBooleanExtra(WorkoutRecordingService.EXTRA_IS_RECORDING, false);

                            if (mIsRecording) {
                                // If recording, but no duration timer yet, the recording has been re-started
                                if (mDurationTimer == null) {
                                    mStartTime = System.currentTimeMillis();
                                    mDuration = 0;
                                    mDistance = 0;

                                    startDurationTimer();
                                }
                            } else {
                                stopDurationTimer();
                                mPathLocations.clear();
                            }

                            break;
                    }

                    if (!isAmbient()) {
                        updateUI();
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);
        intentFilter.addAction(WorkoutRecordingService.ACTION_RECORDING_STATUS);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mLocationChangedReceiver, intentFilter);

        Intent intent = new Intent(WorkoutRecordingService.ACTION_REPORT_RECORDING_STATUS);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mLocationChangedReceiver);
        stopDurationTimer();

        super.onStop();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
    }

    @Override
    public void onUpdateAmbient() {
        updateUI();
    }

    private void startDurationTimer() {
        mDurationTimer = new Timer();
        mDurationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mDuration = System.currentTimeMillis() - mStartTime;

                        if (!isAmbient()) {
                            updateUI();
                        }
                    }
                });
            }
        }, DURATION_TIMER_INTERVAL_MS, DURATION_TIMER_INTERVAL_MS);
    }

    private void stopDurationTimer() {
        if (mDurationTimer != null) {
            mDurationTimer.cancel();
            mDurationTimer = null;
        }
    }

    private void updateUI() {
        mDistanceTextView.setText(String.format(Locale.getDefault(), "%1$,.1f meters", mDistance));
        mDurationTextView.setText(String.format(Locale.getDefault(), "%1$,.1f secs", mDuration / 1000));
    }
}
