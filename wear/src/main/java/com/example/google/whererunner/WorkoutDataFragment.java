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

public class WorkoutDataFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutDataFragment.class.getSimpleName();

    private TextView mDurationTextView;
    private TextView mDistanceTextView;
    private TextView mSpeedTextView;

    private BroadcastReceiver mBroadcastReceiver;

    private Timer mDurationTimer;

    private static final int DURATION_TIMER_INTERVAL_MS = 100;

    public static final String EXTRA_START_TIME = "START_TIME";
    public static final String EXTRA_DISTANCE = "DISTANCE";
    public static final String EXTRA_SPEED_CURRENT = "SPEED_CURRENT";
    public static final String EXTRA_SPEED_AVERAGE = "SPEED_AVERAGE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // TODO migrate to instancing in containing activity/fragment
    public static final WorkoutDataFragment newInstance(Workout workout)
    {
        Bundle bundle = new Bundle(4);
        bundle.putDouble(EXTRA_START_TIME, workout.getStartTime());
        bundle.putDouble(EXTRA_DISTANCE, workout.getStartTime());
        bundle.putDouble(EXTRA_SPEED_CURRENT, workout.getStartTime());
        bundle.putDouble(EXTRA_SPEED_AVERAGE, workout.getStartTime());

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

        if (WorkoutRecordingService.isRecording) {
            startDurationTimer();
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
                        // noop... just update the UI
                        break;

                    case WorkoutRecordingService.ACTION_RECORDING_STATUS_CHANGED:
                        if (WorkoutRecordingService.isRecording) {
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
        intentFilter.addAction(WorkoutRecordingService.ACTION_RECORDING_STATUS_CHANGED);
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
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
        if (WorkoutRecordingService.workout.getDistance() < 1000) {
            mDistanceTextView.setText(
                    String.format(Locale.getDefault(), "%.1f m",
                            WorkoutRecordingService.workout.getDistance()));
        } else {
            mDistanceTextView.setText(
                    String.format(Locale.getDefault(), "%.2f km",
                            WorkoutRecordingService.workout.getDistance() / 1000));
        }

        long millis = 0;
        if (WorkoutRecordingService.isRecording) {
            millis = System.currentTimeMillis() - WorkoutRecordingService.workout.getStartTime();
        } else if (WorkoutRecordingService.workout.getEndTime() != 0) {
            millis = WorkoutRecordingService.workout.getEndTime() - WorkoutRecordingService.workout.getStartTime();
        }

        long[] hms = WhereRunnerApp.millisToHoursMinsSecs(millis);
        long hours = hms[0];
        long minutes = hms[1];
        long seconds = hms[2];
        millis = hms[3];

        if (hours > 0) {
            mDurationTextView.setText(
                    String.format(Locale.getDefault(), "%d:%02d:%02d",
                            hours, minutes, seconds));
        } else {
            mDurationTextView.setText(
                    String.format(Locale.getDefault(), "%02d:%02d.%1d",
                            minutes, seconds, millis / 100));
        }

        mSpeedTextView.setText(
                String.format(Locale.getDefault(), "%.1f / %.1f",
                        WorkoutRecordingService.workout.getSpeedCurrent() * 3600 / 1000,
                        WorkoutRecordingService.workout.getSpeedAverage() * 3600 / 1000));
    }
}
