package info.saxman.whererunner;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import info.saxman.whererunner.model.Workout;

public class HistoryDataFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = HistoryDataFragment.class.getSimpleName();

    public static final String EXTRA_WORKOUT = "WORKOUT";

    private TextView mDateTimeTextView;
    private TextView mDurationTextView;
    private TextView mDistanceTextView;
    private TextView mSpeedTextView;

    private Workout mWorkout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWorkout = getArguments().getParcelable(EXTRA_WORKOUT);
    }

    public static HistoryDataFragment newInstance(Workout workout)
    {
        HistoryDataFragment fragment = new HistoryDataFragment();

        Bundle bundle = new Bundle(1);
        bundle.putParcelable(EXTRA_WORKOUT, workout);

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_history_data, container, false);

        mDateTimeTextView = (TextView) view.findViewById(R.id.date_time);
        mDistanceTextView = (TextView) view.findViewById(R.id.distance);
        mDurationTextView = (TextView) view.findViewById(R.id.duration);
        mSpeedTextView = (TextView) view.findViewById(R.id.speed);

        updateUI();

        return view;
    }

    private void updateUI() {
        mDateTimeTextView.setText(WhereRunnerApp.formatDateTime(mWorkout.getStartTime()));
        mDistanceTextView.setText(WhereRunnerApp.formatDistance(mWorkout.getDistance()));
        mDurationTextView.setText(WhereRunnerApp.formatDuration(mWorkout.getEndTime() - mWorkout.getStartTime()));
        mSpeedTextView.setText(WhereRunnerApp.formatSpeed(mWorkout.getSpeedAverage()) + " / " + WhereRunnerApp.formatSpeed(mWorkout.getSpeedMax()));
    }
}
