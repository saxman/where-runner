package com.example.google.whererunner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.google.whererunner.framework.WearableFragment;

public class HeartFragment extends WearableFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heart, container, false);

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
        // TODO
    }
}
