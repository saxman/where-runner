package info.saxman.android.whererunner.views;

import android.content.Context;
import android.view.View;

import info.saxman.android.whererunner.Utils;
import info.saxman.android.whererunner.model.Workout;
import info.saxman.android.whererunner.model.WorkoutType;
import info.saxman.android.whererunner.services.WorkoutRecordingService;

public class SpeedView extends SimpleWorkoutDataView {
    public SpeedView(Context context) {
        super(context);

        mValueAvgTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void updateWorkoutData(Workout workout) {
        super.updateWorkoutData(workout);

        String currSpeed;
        String avgSpeed;

        WorkoutType workoutType = WorkoutType.getWorkoutType(workout.getType());

        if (workoutType == WorkoutType.CYCLING) {
            mValueLabelTextView.setText(Utils.getInstance(getContext()).getLocalizedSpeedLabel());
            currSpeed = Utils.getInstance(getContext()).formatSpeed(workout.getSpeedCurrent());
            avgSpeed = Utils.getInstance(getContext()).formatSpeed(workout.getSpeedAverage());
        } else {
            mValueLabelTextView.setText(Utils.getInstance(getContext()).getLocalizedPaceLabel());
            currSpeed = Utils.getInstance(getContext()).formatPace(workout.getSpeedCurrent());
            avgSpeed = Utils.getInstance(getContext()).formatPace(workout.getSpeedAverage());
        }

        mValueTextView.setText(currSpeed);
        mValueAvgTextView.setText(avgSpeed);
    }
}
