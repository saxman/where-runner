package com.example.google.whererunner;

import android.app.Fragment;
import android.os.Bundle;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.CircularButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.google.whererunner.services.LocationService;

public class ControlFragment extends Fragment implements View.OnClickListener {

    private CircularButton mRecordingToggleButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        mRecordingToggleButton = (CircularButton) view.findViewById(R.id.record_button);
        mRecordingToggleButton.setOnClickListener(this);

        // TODO don't assume default state is recording...
        mRecordingToggleButton.setImageResource(R.drawable.icon_stop);

        return view;
    }

    @Override
    public void onClick(View view) {
        int status = ((MainActivity) getActivity()).toggleRecording();

        if (status == LocationService.LOCATION_UPDATING) {
            mRecordingToggleButton.setImageResource(R.drawable.icon_stop);
        } else {
            mRecordingToggleButton.setImageResource(R.drawable.icon_record);
        }
    }
}
