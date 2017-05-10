package info.saxman.android.whererunner.views;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import info.saxman.android.whererunner.R;
import info.saxman.android.whererunner.WhereRunnerApp;
import info.saxman.android.whererunner.model.Workout;
import info.saxman.android.whererunner.model.WorkoutType;
import info.saxman.android.whererunner.services.WorkoutRecordingService;

public class DistanceSpeedView extends WorkoutDataView {
    private TextView mDurationTextView;
    private TextView mDistanceTextView;

    private TextView mSpeedTextView;
    private TextView mSpeedAvgTextView;
    private ImageView mIcon;

    private View mDivider1;
    private View mDivider2;

    private WorkoutType mCurrentWorkoutType;

    public DistanceSpeedView(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_workout_data_compound, this);

        mDistanceTextView = (TextView) this.findViewById(R.id.distance);

        mIcon = (ImageView) this.findViewById(R.id.icon);
        mSpeedTextView = (TextView) this.findViewById(R.id.data_value);
        mSpeedAvgTextView = (TextView) this.findViewById(R.id.data_avg);

        mDurationTextView = (TextView) this.findViewById(R.id.duration);

        mDivider1 = this.findViewById(R.id.divider_1);
        mDivider2 = this.findViewById(R.id.divider_2);
    }

    @Override
    public void updateWorkoutData(Workout workout) {
        mDistanceTextView.setText(WhereRunnerApp.formatDistance(workout.getDistance()));

        String currSpeed;
        String avgSpeed;

        if (WorkoutRecordingService.workoutType == WorkoutType.CYCLING) {
            currSpeed = WhereRunnerApp.formatSpeed(workout.getSpeedCurrent());
            avgSpeed = WhereRunnerApp.formatSpeed(workout.getSpeedAverage());
        } else {
            currSpeed = WhereRunnerApp.formatPace(workout.getSpeedCurrent());
            avgSpeed = WhereRunnerApp.formatPace(workout.getSpeedAverage());
        }

        if (mCurrentWorkoutType != WorkoutRecordingService.workoutType) {
            if (WorkoutRecordingService.workoutType == WorkoutType.CYCLING) {
                mIcon.setImageResource(R.drawable.ic_cycling_white);
            } else {
                mIcon.setImageResource(R.drawable.ic_running_white);
            }

            mCurrentWorkoutType = WorkoutRecordingService.workoutType;
        }

        mSpeedTextView.setText(currSpeed);
        mSpeedAvgTextView.setText(avgSpeed);

        long millis = 0;
        if (WorkoutRecordingService.isRecording) {
            millis = System.currentTimeMillis() - workout.getStartTime();
        } else if (workout.getEndTime() != 0) {
            millis = workout.getEndTime() - workout.getStartTime();
        }

        mDurationTextView.setText(WhereRunnerApp.formatDuration(millis));
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

        // TODO figure out how to remove alpha blending in ambient mode
//                mIcon.setColorFilter(R.color.white);
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
    }
}
