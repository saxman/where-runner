package info.saxman.android.whererunner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.HashSet;

import info.saxman.android.whererunner.framework.WearableFragment;
import info.saxman.android.whererunner.services.HeartRateSensorService;
import info.saxman.android.whererunner.services.WorkoutRecordingService;
import info.saxman.android.whererunner.views.DistanceSpeedView;
import info.saxman.android.whererunner.views.DistanceView;
import info.saxman.android.whererunner.views.HeartRateView;
import info.saxman.android.whererunner.views.SpeedView;
import info.saxman.android.whererunner.views.WorkoutDataView;

public class WorkoutDataFragment extends WearableFragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = WorkoutDataFragment.class.getSimpleName();

    public static final String ARGUMENT_WORKOUT_DATA_VIEW = "WORKOUT_VIEW";

    public static final int VALUE_WORKOUT_VIEW_DATA = 1;
    public static final int VALUE_WORKOUT_VIEW_HEART_RATE = 2;

    private final BroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();

    private static final int WORKOUT_DURATION_TIMER_INTERVAL_MS = 100;

    private Handler mWorkoutDurationTimer = new Handler();

    private Runnable mWorkoutDurationTimerRunnable = new Runnable() {
        @Override
        public void run() {
            updateUI();
            mWorkoutDurationTimer.postDelayed(mWorkoutDurationTimerRunnable,
                    WORKOUT_DURATION_TIMER_INTERVAL_MS);
        }
    };

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mRecyclerViewLayoutManager;

    private HashSet<WorkoutDataViewHolder> mWorkoutDataViewHolders = new HashSet<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_workout_data, container, false);

        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);

        MyRecyclerViewAdapter recyclerViewAdapter = new MyRecyclerViewAdapter();
        mRecyclerView.setAdapter(recyclerViewAdapter);

        // Assign a linear layout manager so we can get the firstChange fully visible view position.
        mRecyclerViewLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mRecyclerViewLayoutManager);

        // Remove the view holder from the set of instantiated view holders so its content isn't
        // refreshed when data comes in.
        mRecyclerView.setRecyclerListener(new RecyclerView.RecyclerListener() {
            @Override
            public void onViewRecycled(RecyclerView.ViewHolder holder) {
                mWorkoutDataViewHolders.remove(holder);
            }
        });

        // TODO doesn't seem to work...
        // Stop the recycler view scrollbar from showing when the data is updated.
        RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        // Allows snapping to each view in the recycler view.
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(mRecyclerView);

        if (getArguments() != null) {
            int initialWorkoutView = getArguments().getInt(ARGUMENT_WORKOUT_DATA_VIEW,
                    VALUE_WORKOUT_VIEW_DATA);

            if (initialWorkoutView == VALUE_WORKOUT_VIEW_HEART_RATE) {
                mRecyclerView.scrollToPosition(MyRecyclerViewAdapter.HEART_RATE_VIEW);
            }
        }

        // Detect when the user taps on the screen to cycle to tne next workout data display.
        final GestureDetector gestureDetector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent event) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent event) {
                Log.d(LOG_TAG, "tapped!");

                int pos = mRecyclerViewLayoutManager.findFirstCompletelyVisibleItemPosition() + 1;
                pos %= mRecyclerView.getAdapter().getItemCount();

                mRecyclerView.smoothScrollToPosition(pos);

                return true;
            }
        });

        view.setFocusableInTouchMode(true);
        view.setFocusable(true);
        view.setClickable(true);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (WorkoutRecordingService.isRecording) {
            startWorkoutDurationTimer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        updateUI();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WorkoutRecordingService.ACTION_WORKOUT_DATA_UPDATED);
        intentFilter.addAction(WorkoutRecordingService.ACTION_SERVICE_STATE_CHANGED);
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

        for (WorkoutDataViewHolder viewHolder : mWorkoutDataViewHolders) {
            viewHolder.onEnterAmbient();
        }

        stopWorkoutDurationTimer();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        mRecyclerView.setVerticalScrollBarEnabled(true);

        for (WorkoutDataViewHolder viewHolder : mWorkoutDataViewHolders) {
            viewHolder.onExitAmbient();
        }

        if (WorkoutRecordingService.isRecording) {
            startWorkoutDurationTimer();
        }
    }

    @Override
    public void onUpdateAmbient() {
        updateUI();
    }

    //
    // Class methods (private)
    //

    private void startWorkoutDurationTimer() {
        mWorkoutDurationTimer.post(mWorkoutDurationTimerRunnable);
    }

    private void stopWorkoutDurationTimer() {
        mWorkoutDurationTimer.removeCallbacks(mWorkoutDurationTimerRunnable);
    }

    private void updateUI() {
        // Refresh the data in each of the viewholders in the RecyclerView.
        for (WorkoutDataViewHolder viewHolder : mWorkoutDataViewHolders) {
            viewHolder.updateView();
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case WorkoutRecordingService.ACTION_SERVICE_STATE_CHANGED:
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

    private class WorkoutDataViewHolder extends RecyclerView.ViewHolder {
        private WorkoutDataView mView;

        WorkoutDataViewHolder(WorkoutDataView view) {
            super(view);
            mView = view;
        }

        void updateView() {
            mView.updateWorkoutData(WorkoutRecordingService.workout);
        }

        void onEnterAmbient() {
            mView.onEnterAmbient();
        }

        void onExitAmbient() {
            mView.onExitAmbient();
        }
    }

    private class MyRecyclerViewAdapter extends RecyclerView.Adapter<WorkoutDataViewHolder> {
        static final int DISTANCE_SPEED_VIEW = 0;
        static final int DISTANCE_VIEW = 1;
        static final int SPEED_VIEW = 2;
        static final int HEART_RATE_VIEW = 3;

        private final int VIEW_COUNT = 4;

        @Override
        public WorkoutDataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            WorkoutDataView view = null;

            switch (viewType) {
                case DISTANCE_SPEED_VIEW:
                    view = new DistanceSpeedView(getContext());
                    break;
                case DISTANCE_VIEW:
                    view = new DistanceView(getContext());
                    break;
                case SPEED_VIEW:
                    view = new SpeedView(getContext());
                    break;
                case HEART_RATE_VIEW:
                    view = new HeartRateView(getContext());
                    break;
            }

            view.setLayoutParams(new FrameLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));

            WorkoutDataViewHolder viewHolder = new WorkoutDataViewHolder(view);
            mWorkoutDataViewHolders.add(viewHolder);

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(WorkoutDataViewHolder viewHolder, int position) {
            viewHolder.updateView();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return VIEW_COUNT;
        }
    }

}
