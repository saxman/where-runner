package info.saxman.whererunner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import info.saxman.whererunner.framework.WearableFragment;
import info.saxman.whererunner.model.Workout;
import info.saxman.whererunner.services.WorkoutRecordingService;

import java.util.LinkedList;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

public class WorkoutDataFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutDataFragment.class.getSimpleName();

    private TextView mDurationTextView;
    private TextView mDistanceTextView;
    private TextView mSpeedTextView;

    private TextView mHeartRateTitle;
    private TextView mHeartRateTextView;

    private LinkedList<TextView> mTextViews = new LinkedList<>();

    private final BroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();

    private Timer mDurationTimer;

    private static final int DURATION_TIMER_INTERVAL_MS = 100;

    private boolean mIsImmersiveMode = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_workout_data, container, false);

        mDistanceTextView = (TextView) view.findViewById(R.id.distance);
        mDurationTextView = (TextView) view.findViewById(R.id.duration);
        mSpeedTextView = (TextView) view.findViewById(R.id.speed);

        mHeartRateTitle = (TextView) view.findViewById(R.id.heart_rate_title);
        mHeartRateTextView = (TextView) view.findViewById(R.id.heart_rate);

        mTextViews.add(mDistanceTextView);
        mTextViews.add(mDurationTextView);
        mTextViews.add(mSpeedTextView);
        mTextViews.add(mHeartRateTextView);
        mTextViews.add(mHeartRateTitle);
        mTextViews.add((TextView) view.findViewById(R.id.distance_title));
        mTextViews.add((TextView) view.findViewById(R.id.duration_title));
        mTextViews.add((TextView) view.findViewById(R.id.speed_title));

        if (WorkoutRecordingService.isRecording) {
            startDurationTimer();
        }

        updateUI();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WorkoutRecordingService.ACTION_WORKOUT_DATA_UPDATED);
        intentFilter.addAction(WorkoutRecordingService.ACTION_RECORDING_STATUS_CHANGED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onStop() {
        stopDurationTimer();
        super.onStop();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        for (TextView view : mTextViews) {
            view.getPaint().setAntiAlias(false);
            view.setTextColor(Color.WHITE);
        }

        if (!mIsImmersiveMode) {
            mHeartRateTextView.setVisibility(View.VISIBLE);
            mHeartRateTitle.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        for (TextView view : mTextViews) {
            view.getPaint().setAntiAlias(true);
            view.setTextColor(getResources().getColor(R.color.text_primary, null));
        }

        if (!mIsImmersiveMode) {
            mHeartRateTextView.setVisibility(View.INVISIBLE);
            mHeartRateTitle.setVisibility(View.INVISIBLE);
        }
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
                // Do nothing if the user pressed the record button before this fragment
                // has been attached to an activity
                if (getActivity() == null) {
                    return;
                }

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
        mDistanceTextView.setText(WhereRunnerApp.formatDistance(WorkoutRecordingService.workout.getDistance()));

        long millis = 0;
        if (WorkoutRecordingService.isRecording) {
            millis = System.currentTimeMillis() - WorkoutRecordingService.workout.getStartTime();
        } else if (WorkoutRecordingService.workout.getEndTime() != 0) {
            millis = WorkoutRecordingService.workout.getEndTime() - WorkoutRecordingService.workout.getStartTime();
        }

        mDurationTextView.setText(WhereRunnerApp.formatDuration(millis));

        String currSpeed = WhereRunnerApp.formatSpeed(WorkoutRecordingService.workout.getSpeedCurrent());
        String avgSpeed = WhereRunnerApp.formatSpeed(WorkoutRecordingService.workout.getSpeedAverage());
        mSpeedTextView.setText(String.format(Locale.getDefault(), "%s / %s", currSpeed, avgSpeed));
    }

    public void setImmersiveMode(boolean isImmersiveMode) {
        mIsImmersiveMode = isImmersiveMode;

        if (isImmersiveMode) {
            mHeartRateTextView.setVisibility(View.VISIBLE);
            mHeartRateTitle.setVisibility(View.VISIBLE);

            AlphaAnimation anim = new AlphaAnimation(0f, 1.0f);
            anim.setDuration(250);
            mHeartRateTextView.setAlpha(1f);
            mHeartRateTextView.startAnimation(anim);

            anim.setDuration(250);
            mHeartRateTitle.setAlpha(1f);
            mHeartRateTitle.startAnimation(anim);
        } else {
            mHeartRateTextView.setVisibility(View.INVISIBLE);
            mHeartRateTitle.setVisibility(View.INVISIBLE);
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
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
    }
}
