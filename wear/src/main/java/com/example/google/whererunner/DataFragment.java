package com.example.google.whererunner;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.google.whererunner.framework.RouteDataService;
import com.example.google.whererunner.framework.WearableFragment;

import java.util.Locale;

public class DataFragment extends WearableFragment implements RouteDataService.RouteDataUpdateListener {

    private static final String LOG_TAG = DataFragment.class.getSimpleName();

    private RouteDataService mRouteDataService;

    private TextView mDurationTextView;
    private TextView mDistanceTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_data, container, false);

        mDistanceTextView = (TextView) view.findViewById(R.id.distance);
        mDurationTextView = (TextView) view.findViewById(R.id.duration);

        updateUI();

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        setRouteDataService(activity);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        setRouteDataService(context);
    }

    private void setRouteDataService(Context context) {
        try {
            mRouteDataService = (RouteDataService) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement RouteDataService");
        }
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

    @Override
    public void onRouteDataUpdated() {
        // TODO update ui (duration) more regularly than whenever a GPS sample is received
        updateUI();
    }

    public void updateUI() {
        mDistanceTextView.setText(String.format(Locale.getDefault(), "%1$,.2f meters", mRouteDataService.getDistance()));
        mDurationTextView.setText(String.format(Locale.getDefault(), "%1$,.3f secs", mRouteDataService.getDuration() / 1000));
    }
}
