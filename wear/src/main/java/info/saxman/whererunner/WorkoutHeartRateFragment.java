package info.saxman.whererunner;

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

import info.saxman.whererunner.framework.WearableFragment;
import info.saxman.whererunner.services.HeartRateSensorEvent;
import info.saxman.whererunner.services.HeartRateSensorService;
import info.saxman.whererunner.services.WorkoutRecordingService;

import java.util.LinkedList;
import java.util.Locale;

public class WorkoutHeartRateFragment extends WearableFragment
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutHeartRateFragment.class.getSimpleName();

    private final BroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();

    private TextView mHrMinMaxText;
    private TextView mHrCurrentText;
    private TextView mHrAverageText;

    /** All TextViews in the fragment which need updating when the app goes in/out of ambient mode. */
    private LinkedList<TextView> mTextViews = new LinkedList<>();

    private String mNoDataString;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNoDataString = getString(R.string.hrm_no_data);
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
        filter.addAction(HeartRateSensorService.ACTION_CONNECTIVITY_CHANGED);
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
        // If we have workout data (active or recently stopped), use it
        if (WorkoutRecordingService.workout.getHeartRateCurrent() > -1) {
            int min = WorkoutRecordingService.workout.getHeartRateMin();
            int max = WorkoutRecordingService.workout.getHeartRateMax();
            float avg = WorkoutRecordingService.workout.getHeartRateAverage();

            // If min and max are outside normal bounds (i.e. they haven't been set), show now data
            if (max <= 0 || min > 1000) {
                mHrMinMaxText.setText(mNoDataString);
            } else {
                mHrMinMaxText.setText(String.format(Locale.getDefault(), "%d / %d", min, max));
            }

            if (WorkoutRecordingService.workout.getHeartRateAverage() == 0) {
                mHrAverageText.setText(mNoDataString);
            } else {
                mHrAverageText.setText(String.format(Locale.getDefault(), "%.1f", avg));
            }
        }

        // If we're actively receiving heart rate samples, display the most recent
        if (HeartRateSensorService.isReceivingAccurateHeartRateSamples) {
            mHrCurrentText.setText(String.valueOf(HeartRateSensorService.lastHeartRateSample.getHeartRate()));
        } else {
            mHrCurrentText.setText(mNoDataString);
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver  {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case HeartRateSensorService.ACTION_HEART_RATE_CHANGED:
                case HeartRateSensorService.ACTION_CONNECTIVITY_CHANGED:
                    break;
            }

            if (!isAmbient()) {
                updateUI();
            }
        }
    }
}
