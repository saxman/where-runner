package info.saxman.android.whererunner.views;

import android.content.Context;
import android.view.View;

import info.saxman.android.whererunner.WhereRunnerApp;
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
