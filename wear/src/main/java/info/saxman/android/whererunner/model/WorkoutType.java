package info.saxman.android.whererunner.model;

import info.saxman.android.whererunner.R;

public enum WorkoutType {
    RUNNING(R.drawable.ic_running,
            R.drawable.ic_running_white,
            R.string.activity_running,
            0),
    CYCLING(R.drawable.ic_cycling,
            R.drawable.ic_cycling_white,
            R.string.activity_cycling,
            1);

    public int drawableId;
    public int invertedDrawableId;
    public int titleId;
    public int preferencesId;

    WorkoutType(int drawableId, int invertedDrawableId, int titleId, int prefsId) {
        this.drawableId = drawableId;
        this.invertedDrawableId = invertedDrawableId;
        this.titleId = titleId;
        this.preferencesId = prefsId;
    }

    public static WorkoutType getWorkoutType(int prefsId) {
        WorkoutType type;

        switch (prefsId) {
            case 0:
                type = RUNNING;
                break;
            case 1:
                type = CYCLING;
                break;
            default:
                type = RUNNING;
        }

        return type;
    }
}
