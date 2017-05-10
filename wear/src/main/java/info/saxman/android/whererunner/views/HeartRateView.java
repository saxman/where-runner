package info.saxman.android.whererunner.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.util.Locale;

import info.saxman.android.whererunner.R;
import info.saxman.android.whererunner.model.Workout;
import info.saxman.android.whererunner.services.HeartRateSensorService;
import info.saxman.android.whererunner.services.WorkoutRecordingService;

public class HeartRateView extends SimpleWorkoutDataView {
    private ObjectAnimator mHeartAnimator;
    private double mAnimationDurationMs;

    public HeartRateView(Context context) {
        super(context);

        mIcon.setVisibility(View.VISIBLE);

        mValueAvgTextView.setVisibility(View.VISIBLE);
        mValueAvgTextView.setText("0.0");
        mValueLabelTextView.setText(R.string.heart_rate);

        mHeartAnimator = ObjectAnimator.ofPropertyValuesHolder(mIcon,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f));

        mHeartAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        mHeartAnimator.setRepeatMode(ObjectAnimator.REVERSE);

        mHeartAnimator.addListener(new AnimatorListenerAdapter() {
            // If the animation is forward (0) or reverse (1).
            int mAnimationDirection;

            @Override
            public void onAnimationStart(Animator animation) {
                mAnimationDirection = 0;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                mAnimationDirection = ++mAnimationDirection % 2;

                if (mAnimationDirection == 0) {
                    animation.setInterpolator(new AccelerateInterpolator());

                    // Update the animation duration when it restarts.
                    animation.setDuration((long) mAnimationDurationMs / 2);
                } else {
                    animation.setInterpolator(new DecelerateInterpolator());
                }
            }
        });
    }

    @Override
    public void onEnterAmbient() {
        super.onEnterAmbient();

        if (mHeartAnimator.isStarted()) {
            mHeartAnimator.pause();
        }

        mIcon.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        if (mHeartAnimator.isPaused()) {
            mHeartAnimator.resume();
        }

        mIcon.setVisibility(View.VISIBLE);
    }

    @Override
    public void updateWorkoutData(Workout workout) {
        super.updateWorkoutData(workout);

        // If we're actively receiving heart rate samples, display the most recent and
        // update the heart beat animation.
        if (HeartRateSensorService.isReceivingAccurateHeartRateSamples) {
            int bpm = HeartRateSensorService.lastHeartRateSample.getHeartRate();

            mValueTextView.setText(String.valueOf(bpm));

            mAnimationDurationMs = 1 / (bpm / 60000.0);

            if (!mHeartAnimator.isStarted()) {
                mIcon.setImageResource(R.drawable.ic_heart);
                mHeartAnimator.setDuration((long) mAnimationDurationMs / 2);
                mHeartAnimator.start();
            }
        } else {
            mValueTextView.setText(R.string.hrm_no_data);

            if (mHeartAnimator.isStarted()) {
                mHeartAnimator.cancel();
                mIcon.setImageResource(R.drawable.ic_heart_outline);
            }
        }

        // If we have average bpm, show it.
        if (workout.getHeartRateAverage() != 0) {
            mValueAvgTextView.setText(String.format(Locale.getDefault(), "%.1f", workout.getHeartRateAverage()));
        }
    }
}
