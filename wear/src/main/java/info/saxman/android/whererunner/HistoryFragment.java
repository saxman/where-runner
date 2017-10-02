package info.saxman.android.whererunner;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import info.saxman.android.whererunner.model.Workout;
import info.saxman.android.whererunner.persistence.WorkoutDatabaseHelper;

public class HistoryFragment extends Fragment {
    @SuppressWarnings("unused")
    private static final String LOG_TAG = HistoryFragment.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private MyRecyclerViewAdapter mRecyclerViewAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_history, container, false);

        mRecyclerViewAdapter = new MyRecyclerViewAdapter();

        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Allows snapping to each view in the recycler view.
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(mRecyclerView);

        final View loadingView = view.findViewById(R.id.loading);
        final View noDataView = view.findViewById(R.id.no_data);

        WorkoutDatabaseHelper dbHelper = new WorkoutDatabaseHelper(getActivity());
        dbHelper.readLastFiveWorkoutsAsync(new WorkoutDatabaseHelper.ReadWorkoutsCallback() {
            @Override
            public void onRead(ArrayList<Workout> workouts) {
                loadingView.setVisibility(View.GONE);

                if (workouts.size() == 0) {
                    noDataView.setVisibility(View.VISIBLE);
                } else {
                    mRecyclerViewAdapter.setWorkouts(workouts);
                    mRecyclerViewAdapter.notifyDataSetChanged();
                }
            }
        });

        return view;
    }

    private class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder> {
        ArrayList<Workout> mWorkouts;

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView mDateTimeTextView;
            TextView mDurationTextView;
            TextView mDistanceTextView;
            TextView mSpeedTextView;

            MyViewHolder(View view) {
                super(view);

                mDateTimeTextView = view.findViewById(R.id.date_time);
                mDistanceTextView = view.findViewById(R.id.distance);
                mDurationTextView = view.findViewById(R.id.duration);
                mSpeedTextView = view.findViewById(R.id.data_value);
            }
        }

        MyRecyclerViewAdapter() {
            mWorkouts = new ArrayList<>(5);
        }

        @Override
        public MyRecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_data, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyRecyclerViewAdapter.MyViewHolder viewHolder, int position) {
            Workout workout = mWorkouts.get(position);

            viewHolder.mDateTimeTextView.setText(
                    Utils.getInstance(getContext()).formatDateTime(workout.getStartTime()));
            viewHolder.mDistanceTextView.setText(
                    Utils.getInstance(getContext()).formatDistance(workout.getDistance()));
            viewHolder.mDurationTextView.setText(
                    Utils.getInstance(getContext()).formatDuration(
                            workout.getEndTime() - workout.getStartTime()));
            viewHolder.mSpeedTextView.setText(
                    Utils.getInstance(getContext()).formatSpeed(
                            workout.getSpeedAverage()) + " / "
                            + Utils.getInstance(getContext()).formatSpeed(
                                    workout.getSpeedMax()));
        }

        @Override
        public int getItemCount() {
            return mWorkouts.size();
        }

        public void setWorkouts(ArrayList<Workout> workouts) {
            mWorkouts = workouts;
        }
    }
}
