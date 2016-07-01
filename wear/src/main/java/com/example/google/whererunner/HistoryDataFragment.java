package com.example.google.whererunner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.model.Workout;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HistoryDataFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = HistoryDataFragment.class.getSimpleName();

    public static final String EXTRA_WORKOUT = "WORKOUT";

    private TextView mDurationTextView;
    private TextView mDistanceTextView;
    private TextView mSpeedTextView;

    private Workout mWorkout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWorkout = getArguments().getParcelable(EXTRA_WORKOUT);
    }

    public static HistoryDataFragment newInstance(Workout workout)
    {
        HistoryDataFragment fragment = new HistoryDataFragment();

        Bundle bundle = new Bundle(1);
        bundle.putParcelable(EXTRA_WORKOUT, workout);

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workout_data, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDistanceTextView = (TextView) view.findViewById(R.id.distance);
        mDurationTextView = (TextView) view.findViewById(R.id.duration);
        mSpeedTextView = (TextView) view.findViewById(R.id.speed);

        updateUI();
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
    public void onUpdateAmbient() {}

    private void updateUI() {
        if (mWorkout.getDistance() < 1000) {
            mDistanceTextView.setText(String.format(Locale.getDefault(), "%.1f meters", mWorkout.getDistance()));
        } else {
            mDistanceTextView.setText(String.format(Locale.getDefault(), "%.3f km", mWorkout.getDistance() / 1000));
        }

        long millis = mWorkout.getEndTime() - mWorkout.getStartTime();
        long[] hms = WhereRunnerApp.millisToHoursMinsSecs(millis);
        long hours = hms[0];
        long minutes = hms[1];
        long seconds = hms[2];

        if (hours > 0) {
            mDurationTextView.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds));
        } else {
            mDurationTextView.setText(String.format(Locale.getDefault(), "%02d:%04.1f", minutes, millis / 1000f));
        }

        mSpeedTextView.setText(String.format(Locale.getDefault(), "%.1f", mWorkout.getAverageSpeed()));
    }
}
