package com.example.google.whererunner;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.model.Workout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryDataFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = HistoryDataFragment.class.getSimpleName();

    public static final String EXTRA_WORKOUT = "WORKOUT";

    private TextView mDateTimeTextView;
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
        final View view = inflater.inflate(R.layout.fragment_history_data, container, false);

        mDateTimeTextView = (TextView) view.findViewById(R.id.date_time);
        mDistanceTextView = (TextView) view.findViewById(R.id.distance);
        mDurationTextView = (TextView) view.findViewById(R.id.duration);
        mSpeedTextView = (TextView) view.findViewById(R.id.speed);

        updateUI();

        return view;
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
        String date = DateUtils.formatDateTime(getActivity(), mWorkout.getStartTime(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME);

        mDateTimeTextView.setText(date);

        if (mWorkout.getDistance() < 1000) {
            mDistanceTextView.setText(
                    String.format(Locale.getDefault(), "%.1f m", mWorkout.getDistance()));
        } else {
            mDistanceTextView.setText(
                    String.format(Locale.getDefault(), "%.2f km", mWorkout.getDistance() / 1000));
        }

        long millis = mWorkout.getEndTime() - mWorkout.getStartTime();
        long[] hms = WhereRunnerApp.millisToHoursMinsSecs(millis);
        long hours = hms[0];
        long minutes = hms[1];
        long seconds = hms[2];
        millis = hms[3];

        if (hours > 0) {
            mDurationTextView.setText(
                    String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds));
        } else {
            mDurationTextView.setText(
                    String.format(Locale.getDefault(), "%02d:%02d.%1d", minutes, seconds, millis / 100));
        }

        mSpeedTextView.setText(
                String.format(Locale.getDefault(), "%.1f / %.1f",
                        mWorkout.getSpeedAverage() * 3600 / 1000,
                        mWorkout.getSpeedMax() * 3600 / 1000));
    }
}
