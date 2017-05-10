package info.saxman.android.whererunner.views;

import android.content.Context;
import android.widget.FrameLayout;

import info.saxman.android.whererunner.model.Workout;

public abstract class WorkoutDataView extends FrameLayout {
    public WorkoutDataView(Context context) {
        super(context);
    }

    public abstract void updateWorkoutData(Workout workout);

    public abstract void onEnterAmbient();

    public abstract void onExitAmbient();
}
