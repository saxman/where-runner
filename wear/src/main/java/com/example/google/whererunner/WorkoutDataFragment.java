package com.example.google.whererunner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.model.Workout;
import com.example.google.whererunner.services.WorkoutRecordingService;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class WorkoutDataFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutDataFragment.class.getSimpleName();

    private TextView mDurationTextView;
    private TextView mDistanceTextView;
    private TextView mSpeedTextView;

    private boolean mIsRecording = false;

    private double mDistance;
    private double mDuration;
    private double mAverageSpeed;
    private double mSpeed;

    private BroadcastReceiver mBroadcastReceiver;

    private Timer mDurationTimer;
    private double mStartTime;

    private static final int DURATION_TIMER_INTERVAL_MS = 100;

    public static final String EXTRA_START_TIME = "START_TIME";
    public static final String EXTRA_DISTANCE = "DISTANCE";
    public static final String EXTRA_SPEED = "SPEED";
    public static final String EXTRA_AVERAGE_SPEED = "AVERAGE_SPEED";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStartTime = WorkoutRecordingService.startTime;
        mDistance = WorkoutRecordingService.distance;
        mSpeed = WorkoutRecordingService.speed;
        mAverageSpeed = WorkoutRecordingService.averageSpeed;
    }

    // TODO migrate to instancing in containing activity/fragment
    public static final WorkoutDataFragment newInstance(Workout workout)
    {
        Bundle bundle = new Bundle(4);
        bundle.putDouble(EXTRA_START_TIME, workout.getStartTime());
        bundle.putDouble(EXTRA_DISTANCE, workout.getStartTime());
        bundle.putDouble(EXTRA_SPEED, workout.getStartTime());
        bundle.putDouble(EXTRA_AVERAGE_SPEED, workout.getStartTime());

        WorkoutDataFragment fragment = new WorkoutDataFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_workout_data, container, false);

        mDistanceTextView = (TextView) view.findViewById(R.id.distance);
        mDurationTextView = (TextView) view.findViewById(R.id.duration);
        mSpeedTextView = (TextView) view.findViewById(R.id.speed);

        mIsRecording = WorkoutRecordingService.isRecording;

        if (mIsRecording) {
            // start the timer to update the workout duration
            startDurationTimer();
        } else {
            mDuration = WorkoutRecordingService.stopTime - mStartTime;
        }

        updateUI();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case WorkoutRecordingService.ACTION_WORKOUT_DATA_UPDATED:
                        mDistance = WorkoutRecordingService.distance;
                        mAverageSpeed = WorkoutRecordingService.averageSpeed;
                        mSpeed = WorkoutRecordingService.speed;
                        break;

                    case WorkoutRecordingService.ACTION_RECORDING_STATUS:
                        mIsRecording = intent.getBooleanExtra(WorkoutRecordingService.EXTRA_IS_RECORDING, false);

                        if (mIsRecording) {
                            // TODO should get start time from the service, perhaps as an extra?
                            mStartTime = System.currentTimeMillis();
                            startDurationTimer();
                        } else {
                            stopDurationTimer();
                        }

                        break;
                }

                if (!isAmbient()) {
                    updateUI();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WorkoutRecordingService.ACTION_WORKOUT_DATA_UPDATED);
        intentFilter.addAction(WorkoutRecordingService.ACTION_RECORDING_STATUS);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);
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
        if (mDistance < 1000) {
            mDistanceTextView.setText(String.format(Locale.getDefault(), "%.1f meters", mDistance));
        } else {
            mDistanceTextView.setText(String.format(Locale.getDefault(), "%.3f km", mDistance / 1000));
        }

        long millis = (long) mDuration;
        long[] hms = WhereRunnerApp.millisToHoursMinsSecs(millis);
        long hours = hms[0];
        long minutes = hms[1];
        long seconds = hms[2];

        if (hours > 0) {
            mDurationTextView.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds));
        } else {
            mDurationTextView.setText(String.format(Locale.getDefault(), "%02d:%04.1f", minutes, millis / 1000f));
        }

        mSpeedTextView.setText(String.format(Locale.getDefault(), "%.1f / %.1f m/s", mSpeed, mAverageSpeed));
    }
}
