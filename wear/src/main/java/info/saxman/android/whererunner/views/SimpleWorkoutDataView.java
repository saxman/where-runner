package info.saxman.android.whererunner.views;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import info.saxman.android.whererunner.R;
import info.saxman.android.whererunner.Utils;
import info.saxman.android.whererunner.model.Workout;
import info.saxman.android.whererunner.services.WorkoutRecordingService;

/**
 * WorkoutDataView that displays duration and another, primary data point (e..g distance,
 * time, HR), as well as a smaller data point display, intended for the min, max, or
 * average. The view also includes an optional icon (default invisible) positioned to the
 * left of the primary data point.
 */
public abstract class SimpleWorkoutDataView extends WorkoutDataView {
    protected TextView mDurationTextView;
    protected TextView mValueTextView;
    protected TextView mValueAvgTextView;
    protected TextView mValueLabelTextView;
    protected View mDivider;
    protected ImageView mIcon;

    public SimpleWorkoutDataView(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_workout_data_simple, this);

        mIcon = this.findViewById(R.id.icon);
        mValueTextView = this.findViewById(R.id.data_value);
        mValueAvgTextView = this.findViewById(R.id.data_avg);
        mValueLabelTextView = this.findViewById(R.id.value_label);
        mDurationTextView = this.findViewById(R.id.duration);
        mDivider = this.findViewById(R.id.divider_2);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void updateWorkoutData(Workout workout) {
        long millis = 0;
        if (WorkoutRecordingService.isRecording) {
            millis = System.currentTimeMillis() - workout.getStartTime();
        } else if (workout.getEndTime() != 0) {
            millis = workout.getEndTime() - workout.getStartTime();
        }

        mDurationTextView.setText(Utils.getInstance(getContext()).formatDuration(millis));
    }

    @Override
    public void onEnterAmbient() {
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

    @Override
    public void onExitAmbient() {
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
