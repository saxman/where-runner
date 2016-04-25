package com.example.google.whererunner;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ImageButton;

import com.example.google.whererunner.framework.WearableFragment;

public class HeartFragment extends WearableFragment {

    private static final String TAG = "HeartFragment";

    ImageButton heartButton;
    boolean heartRateSensorOn = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heart, container, false);
        heartButton = (ImageButton)view.findViewById(R.id.heart_button);
        heartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                if(heartRateSensorOn){
                    ((ImageButton) v).setImageResource(R.drawable.ic_heart);
                }else{
                    ((ImageButton) v).setImageResource(R.drawable.ic_heart_red);
                }
                heartRateSensorOn = !heartRateSensorOn;
                Log.v(TAG, "Heart Rate Sensor is " + (heartRateSensorOn?"ON.":"OFF."));
                // TODO: Broadcast message to start/stop heart rate monitor service.
            }
        });
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
