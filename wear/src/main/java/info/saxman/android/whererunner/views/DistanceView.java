package info.saxman.android.whererunner.views;

import android.content.Context;
import android.view.LayoutInflater;

import info.saxman.android.whererunner.R;
import info.saxman.android.whererunner.WhereRunnerApp;
import info.saxman.android.whererunner.model.Workout;
import info.saxman.android.whererunner.services.WorkoutRecordingService;

public class DistanceView extends SimpleWorkoutDataView {
    public DistanceView(Context context) {
        super(context);

        mValueLabelTextView.setText(WhereRunnerApp.getLocalizedDistanceLabel());
    }

    @Override
    public void updateWorkoutData(Workout workout) {
        super.updateWorkoutData(workout);

        mValueTextView.setText(WhereRunnerApp.formatDistance(workout.getDistance()));
    }
}
