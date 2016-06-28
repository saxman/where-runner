package com.example.google.whererunner;

import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.google.whererunner.sql.WorkoutDbHelper;


public class HistoryFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = SettingsFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        renderNrWorkouts(view);

        return view;
    }

    /**
     * Renders the number of workouts in the text field
     */
    private void renderNrWorkouts(View view) {
        WorkoutDbHelper mDbHelper = new WorkoutDbHelper(getContext());
        long nrWorkouts = mDbHelper.readNrWorkouts();
        TextView textView = (TextView) view.findViewById(R.id.text_view);
        textView.setText(getText(R.string.nr_workouts) + " " + nrWorkouts);
    }



}
