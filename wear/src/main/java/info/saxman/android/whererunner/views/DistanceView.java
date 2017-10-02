package info.saxman.android.whererunner.views;

import android.content.Context;

import info.saxman.android.whererunner.Utils;
import info.saxman.android.whererunner.model.Workout;

public class DistanceView extends SimpleWorkoutDataView {
    public DistanceView(Context context) {
        super(context);

        mValueLabelTextView.setText(
                Utils.getInstance(getContext()).getLocalizedDistanceLabel());
    }

    @Override
    public void updateWorkoutData(Workout workout) {
        super.updateWorkoutData(workout);

        mValueTextView.setText(
                Utils.getInstance(getContext()).formatDistance(workout.getDistance()));
    }
}
