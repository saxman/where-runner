package info.saxman.android.whererunner.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import info.saxman.android.whererunner.R;
import info.saxman.android.whererunner.Utils;
import info.saxman.android.whererunner.model.Workout;
import info.saxman.android.whererunner.model.WorkoutType;
import info.saxman.android.whererunner.services.WorkoutRecordingService;

public class DistanceSpeedView extends WorkoutDataView {
    private TextView mDurationTextView;
    private TextView mDistanceTextView;

    private TextView mSpeedTextView;
    private TextView mSpeedAvgTextView;

    private ImageView mIcon;
    private final ColorFilter mIconColorFilter;

    private View mDivider1;
    private View mDivider2;

    private WorkoutType mCurrentWorkoutType;

    public DistanceSpeedView(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_workout_data_compound, this);

        mDistanceTextView = this.findViewById(R.id.distance);

        mIcon = this.findViewById(R.id.icon);
        mSpeedTextView = this.findViewById(R.id.data_value);
        mSpeedAvgTextView = this.findViewById(R.id.data_avg);

        mDurationTextView = this.findViewById(R.id.duration);

        mDivider1 = this.findViewById(R.id.divider_1);
        mDivider2 = this.findViewById(R.id.divider_2);

        mIconColorFilter = mIcon.getColorFilter();
    }

    @Override
    public void updateWorkoutData(Workout workout) {
        mDistanceTextView.setText(
                Utils.getInstance(getContext()).formatDistance(workout.getDistance()));

        String currSpeed;
        String avgSpeed;

        WorkoutType workoutType = WorkoutType.getWorkoutType(workout.getType());

        if (workoutType == WorkoutType.CYCLING) {
            currSpeed = Utils.getInstance(getContext()).formatSpeed(workout.getSpeedCurrent());
            avgSpeed = Utils.getInstance(getContext()).formatSpeed(workout.getSpeedAverage());
        } else {
            currSpeed = Utils.getInstance(getContext()).formatPace(workout.getSpeedCurrent());
            avgSpeed = Utils.getInstance(getContext()).formatPace(workout.getSpeedAverage());
        }

        if (mCurrentWorkoutType != workoutType) {
            if (workoutType == WorkoutType.CYCLING) {
                mIcon.setImageResource(R.drawable.ic_cycling_white);
            } else {
                mIcon.setImageResource(R.drawable.ic_running_white);
            }

            mCurrentWorkoutType = workoutType;
        }

        mSpeedTextView.setText(currSpeed);
        mSpeedAvgTextView.setText(avgSpeed);

        long millis = 0;
        if (WorkoutRecordingService.isRecording) {
            millis = System.currentTimeMillis() - workout.getStartTime();
        } else if (workout.getEndTime() != 0) {
            millis = workout.getEndTime() - workout.getStartTime();
        }

        mDurationTextView.setText(
                Utils.getInstance(getContext()).formatDuration(millis));
    }

    @Override
    public void onEnterAmbient() {
        mDurationTextView.getPaint().setAntiAlias(false);
        mDurationTextView.setTextColor(Color.WHITE);

        mDistanceTextView.getPaint().setAntiAlias(false);
        mDistanceTextView.setTextColor(Color.WHITE);

        mSpeedTextView.getPaint().setAntiAlias(false);
        mSpeedTextView.setTextColor(Color.WHITE);

        mSpeedAvgTextView.getPaint().setAntiAlias(false);
        mSpeedAvgTextView.setTextColor(Color.WHITE);

        mDivider1.setBackgroundColor(Color.WHITE);
        mDivider2.setBackgroundColor(Color.WHITE);

        // Set the icon to solid white and remove alpha
        mIcon.setColorFilter(0xffff);
    }

    @Override
    public void onExitAmbient() {
        int textColor = getResources().getColor(R.color.text_primary, null);
        int primaryColor = getResources().getColor(R.color.primary, null);

        mDurationTextView.getPaint().setAntiAlias(true);
        mDurationTextView.setTextColor(textColor);

        mDistanceTextView.getPaint().setAntiAlias(true);
        mDistanceTextView.setTextColor(textColor);

        mSpeedTextView.getPaint().setAntiAlias(true);
        mSpeedTextView.setTextColor(textColor);

        mSpeedAvgTextView.getPaint().setAntiAlias(true);
        mSpeedAvgTextView.setTextColor(textColor);

        mDivider1.setBackgroundColor(primaryColor);
        mDivider2.setBackgroundColor(primaryColor);

        // Re-set the icon color to its default
        mIcon.setColorFilter(mIconColorFilter);
    }
}
