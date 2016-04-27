package com.example.google.whererunner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.example.google.whererunner.framework.WearableFragment;
import com.example.google.whererunner.services.HeartRateSensorService;

public class HeartFragment extends WearableFragment {

    private static final String TAG = "HeartFragment";

    Button heartButton;
    //TextView textView;
    boolean heartRateSensorOn = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heart, container, false);
        //textView = (TextView)view.findViewById(R.id.heart_rate_text);
        heartButton = (Button)view.findViewById(R.id.heart_button);
        heartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                if(heartRateSensorOn){
                    // Turn off
                    ((Button) v).setBackgroundResource(R.drawable.ic_heart);
                    ((Button) v).setText("N/A");
                    Intent intent = new Intent()
                            .setAction(HeartRateSensorService.ACTION_HEART_RATE_STOP);
                    getActivity().sendBroadcast(intent);
                }else{
                    // Turn on
                    ((Button) v).setBackgroundResource(R.drawable.ic_heart_red);
                    ((Button) v).setText("...");
                    Intent intent = new Intent()
                            .setAction(HeartRateSensorService.ACTION_HEART_RATE_START);
                    getActivity().sendBroadcast(intent);
                }
                heartRateSensorOn = !heartRateSensorOn;
                Log.v(TAG, "Heart Rate Sensor is " + (heartRateSensorOn?"ON.":"OFF."));
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

    public void setHeartRate(float heartRate){
        heartButton.setText(""+heartRate);
    }

    public void disableHeartRate(){
        heartButton.setEnabled(false);
        heartButton.setText("Heart Rate Monitor not found");
    }
}
