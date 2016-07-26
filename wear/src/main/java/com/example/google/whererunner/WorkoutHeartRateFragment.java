package com.example.google.whererunner;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.HeartRateSensorEvent;
import com.example.google.whererunner.services.HeartRateSensorService;
import com.example.google.whererunner.services.WorkoutRecordingService;

import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WorkoutHeartRateFragment extends WearableFragment
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutHeartRateFragment.class.getSimpleName();

    private static final long HR_SAMPLE_DELAY_THRESHOLD_MS = 10000;

    private final BroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();

    private TextView mHrMinMaxText;
    private TextView mHrCurrentText;
    private TextView mHrAverageText;

    private LinkedList<TextView> mTextViews = new LinkedList<>();

    private HeartRateSensorEvent mHrSensorEvent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't both using the last HR sample if it's too old
        if (HeartRateSensorService.lastHeartRateSensorEvent != null) {
            long delta = System.currentTimeMillis() - HeartRateSensorService.lastHeartRateSensorEvent.getTimestamp();
            if (delta < HR_SAMPLE_DELAY_THRESHOLD_MS) {
                mHrSensorEvent = HeartRateSensorService.lastHeartRateSensorEvent;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workout_heartrate, container, false);

        mHrMinMaxText = (TextView) view.findViewById(R.id.hr_min_max);
        mHrAverageText = (TextView) view.findViewById(R.id.hr_average);
        mHrCurrentText = (TextView) view.findViewById(R.id.hr_current);

        mTextViews.add(mHrMinMaxText);
        mTextViews.add(mHrAverageText);
        mTextViews.add(mHrCurrentText);
        mTextViews.add((TextView) view.findViewById(R.id.hr_title));
        mTextViews.add((TextView) view.findViewById(R.id.hr_min_max_title));
        mTextViews.add((TextView) view.findViewById(R.id.hr_average_title));
        mTextViews.add((TextView) view.findViewById(R.id.hr_current_title));

        updateUI();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        filter.addAction(HeartRateSensorService.ACTION_HEART_RATE_SENSOR_TIMEOUT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        for (TextView view : mTextViews) {
            view.getPaint().setAntiAlias(false);
            view.setTextColor(Color.WHITE);
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        for (TextView view : mTextViews) {
            view.getPaint().setAntiAlias(true);
            view.setTextColor(getResources().getColor(R.color.text_primary, null));
        }
    }

    @Override
    public void onUpdateAmbient() {
        updateUI();
    }

    private void updateUI() {
        if (WorkoutRecordingService.isRecording) {
            mHrCurrentText.setText(String.valueOf(WorkoutRecordingService.workout.getHeartRateCurrent()));
            mHrAverageText.setText(String.valueOf(WorkoutRecordingService.workout.getHeartRateAverage()));
            mHrMinMaxText.setText(String.format(Locale.getDefault(),"%.1f / %.1f",
                    WorkoutRecordingService.workout.getHeartRateMin(),
                    WorkoutRecordingService.workout.getHeartRateMax()));
        } else if (mHrSensorEvent != null) {
            mHrCurrentText.setText(String.valueOf(mHrSensorEvent.getHeartRate()));
        } else {
            mHrCurrentText.setText(getString(R.string.hrm_no_data));
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver  {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case HeartRateSensorService.ACTION_HEART_RATE_CHANGED:
                    mHrSensorEvent = intent.getParcelableExtra(HeartRateSensorService.EXTRA_HEART_RATE);
                    break;
                case HeartRateSensorService.ACTION_HEART_RATE_SENSOR_TIMEOUT:
                    mHrSensorEvent = null;
                    break;
            }

            if (!isAmbient()) {
                updateUI();
            }
        }
    }
}
