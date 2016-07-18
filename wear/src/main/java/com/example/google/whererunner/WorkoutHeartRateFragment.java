package com.example.google.whererunner;

import android.app.Fragment;
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

import org.w3c.dom.Text;

import java.util.LinkedList;
import java.util.Locale;

public class WorkoutHeartRateFragment extends WearableFragment
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutHeartRateFragment.class.getSimpleName();

    private BroadcastReceiver mBroadcastReceiver;

    private TextView mHrMinMaxText;
    private TextView mHrCurrentText;
    private TextView mHrAverageText;

    private LinkedList<TextView> mTextViews = new LinkedList<>();

    private HeartRateSensorEvent mHrSensorEvent;

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

        mBroadcastReceiver = new BroadcastReceiver() {
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
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        filter.addAction(HeartRateSensorService.ACTION_HEART_RATE_SENSOR_TIMEOUT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);

        super.onStop();
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
        double min = 0, max = 0, average = 0, current = 0;

        if (WorkoutRecordingService.isRecording) {
            min = WorkoutRecordingService.workout.getHeartRateMin();
            max = WorkoutRecordingService.workout.getHeartRateMax();
            average = WorkoutRecordingService.workout.getHeartRateAverage();
            current = WorkoutRecordingService.workout.getHeartRateCurrent();
        } else if (mHrSensorEvent != null) {
            min = mHrSensorEvent.getMinHeartRate();
            max = mHrSensorEvent.getMaxHeartRate();
            current = mHrSensorEvent.getHeartRate();
        }

        mHrCurrentText.setText(String.valueOf(current));
        mHrAverageText.setText(String.valueOf(average));
        mHrMinMaxText.setText(String.format(Locale.getDefault(), "%.1f / %.1f", min, max));
    }
}
