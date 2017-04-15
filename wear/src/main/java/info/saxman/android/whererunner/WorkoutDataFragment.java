package info.saxman.android.whererunner;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import info.saxman.android.whererunner.framework.WearableFragment;
import info.saxman.android.whererunner.model.Workout;
import info.saxman.android.whererunner.model.WorkoutType;
import info.saxman.android.whererunner.services.HeartRateSensorService;
import info.saxman.android.whererunner.services.WorkoutRecordingService;

public class WorkoutDataFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutDataFragment.class.getSimpleName();

    private final BroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();

    private Timer mWorkoutDurationTimer;

    private static final int WORKOUT_DURATION_TIMER_INTERVAL_MS = 100;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mRecyclerViewLayoutManager;

    private HashSet<MyRecyclerViewAdapter.DataViewHolder> myViewHolders = new HashSet<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_workout_data, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);

        MyRecyclerViewAdapter recyclerViewAdapter = new MyRecyclerViewAdapter();
        mRecyclerView.setAdapter(recyclerViewAdapter);

        // Assign a linear layout manager so we can get the first fully visible view position.
        mRecyclerViewLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mRecyclerViewLayoutManager);

        // Remove the view holder from the set of instantiated view holders so its content isn't
        // refreshed when data comes in.
        mRecyclerView.setRecyclerListener(new RecyclerView.RecyclerListener() {
            @Override
            public void onViewRecycled(RecyclerView.ViewHolder holder) {
                myViewHolders.remove(holder);
            }
        });

        // Allows snapping to each view in the recycler view.
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(mRecyclerView);

        if (getArguments() != null) {
            int initialWorkoutView = getArguments().getInt(
                    WorkoutMainFragment.ARGUMENT_WORKOUT_VIEW,
                    WorkoutMainFragment.VALUE_WORKOUT_VIEW_DATA);

            if (initialWorkoutView == WorkoutMainFragment.VALUE_WORKOUT_VIEW_HEART_RATE) {
                mRecyclerView.scrollToPosition(recyclerViewAdapter.HEART_RATE_VIEW);
            }
        }

        if (WorkoutRecordingService.isRecording) {
            startWorkoutDurationTimer();
        }

        // Detect when the user taps on the screen to cycle to tne next workout data display.
        final GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent event) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent event) {
                int pos = mRecyclerViewLayoutManager.findFirstCompletelyVisibleItemPosition() + 1;
                pos %= mRecyclerView.getAdapter().getItemCount();

                mRecyclerView.smoothScrollToPosition(pos);

                return true;
            }
        });

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        updateUI();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WorkoutRecordingService.ACTION_WORKOUT_DATA_UPDATED);
        intentFilter.addAction(WorkoutRecordingService.ACTION_RECORDING_STATUS_CHANGED);
        intentFilter.addAction(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        intentFilter.addAction(HeartRateSensorService.ACTION_CONNECTIVITY_CHANGED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onStop() {
        stopWorkoutDurationTimer();
        super.onStop();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        // Disable the scrollbar so that it's not shown in ambient mode while the data updates.
        mRecyclerView.setVerticalScrollBarEnabled(false);

        for (MyRecyclerViewAdapter.DataViewHolder viewHolder : myViewHolders) {
            viewHolder.onEnterAmbient();
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        mRecyclerView.setVerticalScrollBarEnabled(true);

        for (MyRecyclerViewAdapter.DataViewHolder viewHolder : myViewHolders) {
            viewHolder.onExitAmbient();
        }
    }

    @Override
    public void onUpdateAmbient() {
        updateUI();
    }

    private void startWorkoutDurationTimer() {
        mWorkoutDurationTimer = new Timer();
        mWorkoutDurationTimer.scheduleAtFixedRate(new TimerTask() {
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
        }, WORKOUT_DURATION_TIMER_INTERVAL_MS, WORKOUT_DURATION_TIMER_INTERVAL_MS);
    }

    private void stopWorkoutDurationTimer() {
        if (mWorkoutDurationTimer != null) {
            mWorkoutDurationTimer.cancel();
            mWorkoutDurationTimer = null;
        }
    }

    private void updateUI() {
        // The data (e.g. duration) on the various pages can get out of sync...
//        int pos = mRecyclerViewLayoutManager.findFirstCompletelyVisibleItemPosition();
//        if (pos != RecyclerView.NO_POSITION) {
//            MyRecyclerViewAdapter.DataViewHolder viewHolder = (MyRecyclerViewAdapter.DataViewHolder) mRecyclerView.findViewHolderForAdapterPosition(pos);
//            viewHolder.updateView();
//        }

        // Causes flickering and jank.
//        int pos = mRecyclerViewLayoutManager.findFirstCompletelyVisibleItemPosition();
//        myRecyclerViewAdapter.notifyItemChanged(pos);

        // Refresh the data in each of the viewholders in the RecyclerView.
        for (MyRecyclerViewAdapter.DataViewHolder viewHolder : myViewHolders) {
            viewHolder.updateView();
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case WorkoutRecordingService.ACTION_RECORDING_STATUS_CHANGED:
                    if (WorkoutRecordingService.isRecording) {
                        startWorkoutDurationTimer();
                    } else {
                        stopWorkoutDurationTimer();
                    }

                    break;

                case WorkoutRecordingService.ACTION_WORKOUT_DATA_UPDATED:
                case HeartRateSensorService.ACTION_HEART_RATE_CHANGED:
                case HeartRateSensorService.ACTION_CONNECTIVITY_CHANGED:
                    break;
            }

            if (!isAmbient()) {
                updateUI();
            }
        }
    }

    private class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.DataViewHolder> {
        final int DISTANCE_VIEW = 0;
        final int SPEED_VIEW = 1;
        final int HEART_RATE_VIEW = 2;

        abstract class DataViewHolder extends RecyclerView.ViewHolder {
            View mView;

            TextView mDurationTextView;
            TextView mValueTextView;
            TextView mValueAvgTextView;
            TextView mValueLabelTextView;
            View mDivider;
            ImageView mIcon;

            DataViewHolder(View view) {
                super(view);

                mView = view;

                mIcon = (ImageView) view.findViewById(R.id.icon);
                mValueTextView = (TextView) view.findViewById(R.id.value_main);
                mValueAvgTextView = (TextView) view.findViewById(R.id.value_avg);
                mValueLabelTextView = (TextView) view.findViewById(R.id.value_label);
                mDurationTextView = (TextView) view.findViewById(R.id.duration);
                mDivider = view.findViewById(R.id.divider);
            }

            public void updateView() {
                Workout workout = WorkoutRecordingService.workout;

                long millis = 0;
                if (WorkoutRecordingService.isRecording) {
                    millis = System.currentTimeMillis() - workout.getStartTime();
                } else if (workout.getEndTime() != 0) {
                    millis = workout.getEndTime() - workout.getStartTime();
                }

                mDurationTextView.setText(WhereRunnerApp.formatDuration(millis));
            }

            void onEnterAmbient() {
                mDurationTextView.getPaint().setAntiAlias(false);
                mValueTextView.getPaint().setAntiAlias(false);
                mValueAvgTextView.getPaint().setAntiAlias(false);
                mValueLabelTextView.getPaint().setAntiAlias(false);

                mDurationTextView.setTextColor(Color.WHITE);
                mValueTextView.setTextColor(Color.WHITE);
                mValueAvgTextView.setTextColor(Color.WHITE);
                mValueLabelTextView.setTextColor(Color.WHITE);

                mDivider.setBackgroundColor(Color.WHITE);
            }

            void onExitAmbient() {
                mDurationTextView.getPaint().setAntiAlias(true);
                mValueTextView.getPaint().setAntiAlias(true);
                mValueAvgTextView.getPaint().setAntiAlias(true);
                mValueLabelTextView.getPaint().setAntiAlias(true);

                int textColor = getResources().getColor(R.color.text_primary, null);
                int primaryColor = getResources().getColor(R.color.primary, null);

                mDurationTextView.setTextColor(textColor);
                mValueTextView.setTextColor(textColor);
                mValueAvgTextView.setTextColor(textColor);
                mValueLabelTextView.setTextColor(textColor);

                mDivider.setBackgroundColor(primaryColor);
            }
        }

        class DistanceViewHolder extends DataViewHolder {
            DistanceViewHolder(View view) {
                super(view);
                mValueLabelTextView.setText(WhereRunnerApp.getLocalizedDistanceLabel());
                updateView();
            }

            @Override
            public void updateView() {
                super.updateView();
                mValueTextView.setText(WhereRunnerApp.formatDistance(
                        WorkoutRecordingService.workout.getDistance()));
            }
        }

        class SpeedViewHolder extends DataViewHolder {
            SpeedViewHolder(View view) {
                super(view);
                mValueAvgTextView.setVisibility(View.VISIBLE);
                updateView();
            }

            @Override
            public void updateView() {
                super.updateView();

                Workout workout = WorkoutRecordingService.workout;

                String currSpeed;
                String avgSpeed;

                if (WorkoutRecordingService.workoutType == WorkoutType.CYCLING) {
                    mValueLabelTextView.setText(WhereRunnerApp.getLocalizedSpeedLabel());
                    currSpeed = WhereRunnerApp.formatSpeed(workout.getSpeedCurrent());
                    avgSpeed = WhereRunnerApp.formatSpeed(workout.getSpeedAverage());
                } else {
                    mValueLabelTextView.setText(WhereRunnerApp.getLocalizedPaceLabel());
                    currSpeed = WhereRunnerApp.formatPace(workout.getSpeedCurrent());
                    avgSpeed = WhereRunnerApp.formatPace(workout.getSpeedAverage());
                }

                mValueTextView.setText(currSpeed);
                mValueAvgTextView.setText(avgSpeed);
            }
        }

        class HeartRateViewHolder extends DataViewHolder {
            ObjectAnimator mHeartAnimator;
            double mAnimationDurationMs;

            public HeartRateViewHolder(View view) {
                super(view);

                mIcon.setVisibility(View.VISIBLE);

                mValueAvgTextView.setVisibility(View.VISIBLE);
                mValueAvgTextView.setText("");
                mValueLabelTextView.setText(R.string.heart_rate);

                mHeartAnimator = ObjectAnimator.ofPropertyValuesHolder(mIcon,
                        PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                        PropertyValuesHolder.ofFloat("scaleY", 1.2f));

                mHeartAnimator.setRepeatCount(ObjectAnimator.INFINITE);
                mHeartAnimator.setRepeatMode(ObjectAnimator.REVERSE);

                mHeartAnimator.addListener(new AnimatorListenerAdapter() {
                    // If the animation is forward (0) or reverse (1).
                    int mAnimationDirection;

                    @Override
                    public void onAnimationStart(Animator animation) {
                        mAnimationDirection = 0;
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                        mAnimationDirection = ++mAnimationDirection % 2;

                        if (mAnimationDirection == 0) {
                            animation.setInterpolator(new AccelerateInterpolator());

                            // Update the animation duration when it restarts.
                            animation.setDuration((long) mAnimationDurationMs / 2);
                        } else {
                            animation.setInterpolator(new DecelerateInterpolator());
                        }
                    }
                });

                updateView();
            }

            @Override
            void onEnterAmbient() {
                super.onEnterAmbient();

                if (mHeartAnimator.isStarted()) {
                    mHeartAnimator.pause();
                }

                mIcon.setVisibility(View.INVISIBLE);
            }

            @Override
            void onExitAmbient() {
                super.onExitAmbient();

                if (mHeartAnimator.isPaused()) {
                    mHeartAnimator.resume();
                }

                mIcon.setVisibility(View.VISIBLE);
            }

            @Override
            public void updateView() {
                super.updateView();

                Workout workout = WorkoutRecordingService.workout;

                // If we're actively receiving heart rate samples, display the most recent and
                // update the heart beat animation.
                if (HeartRateSensorService.isReceivingAccurateHeartRateSamples) {
                    int bpm = HeartRateSensorService.lastHeartRateSample.getHeartRate();

                    mValueTextView.setText(String.valueOf(bpm));

                    mAnimationDurationMs = 1 / (bpm / 60000.0);

                    if (!mHeartAnimator.isStarted()) {
                        mIcon.setImageResource(R.drawable.ic_heart);
                        mHeartAnimator.setDuration((long) mAnimationDurationMs / 2);
                        mHeartAnimator.start();
                    }
                } else {
                    mValueTextView.setText(R.string.hrm_no_data);

                    if (mHeartAnimator.isStarted()) {
                        mHeartAnimator.cancel();
                        mIcon.setImageResource(R.drawable.ic_heart_outline);
                    }
                }

                // If we have average bpm, show it.
                if (workout.getHeartRateAverage() != 0) {
                    mValueAvgTextView.setText(String.format(Locale.getDefault(), "%.1f", workout.getHeartRateAverage()));
                }
            }
        }

        @Override
        public MyRecyclerViewAdapter.DataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            DataViewHolder viewHolder = null;
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_workout_data, parent, false);

            switch (viewType) {
                case DISTANCE_VIEW:
                    viewHolder = new DistanceViewHolder(view);
                    break;
                case SPEED_VIEW:
                    viewHolder = new SpeedViewHolder(view);
                    break;
                case HEART_RATE_VIEW:
                    viewHolder = new HeartRateViewHolder(view);
                    break;
            }

            myViewHolders.add(viewHolder);

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(MyRecyclerViewAdapter.DataViewHolder viewHolder, int position) {
            viewHolder.updateView();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
