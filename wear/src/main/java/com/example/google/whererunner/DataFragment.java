package com.example.google.whererunner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.google.whererunner.framework.WearableFragment;

import java.util.Locale;

public class DataFragment extends WearableFragment {

    private static final String LOG_TAG = DataFragment.class.getSimpleName();

    private TextView mDurationTextView;
    private TextView mDistanceTextView;

    private double mDistance;
    private double mDuration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_data, container, false);

        mDistanceTextView = (TextView) view.findViewById(R.id.distance);
        mDurationTextView = (TextView) view.findViewById(R.id.duration);

        return view;
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        // TODO
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        // TODO
    }

    @Override
    public void onUpdateAmbient() {
        updateUI();
    }

    public void updateUI() {
        mDistanceTextView.setText(String.format(Locale.getDefault(), "%1$,.2f meters", mDistance));
        mDurationTextView.setText(String.format(Locale.getDefault(), "%1$,.3f secs", mDuration / 1000));
    }
}
