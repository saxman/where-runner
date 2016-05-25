package com.example.google.whererunner;

import android.Manifest;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.google.whererunner.services.HeartRateSensorService;

public class HeartFragment extends Fragment
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = HeartFragment.class.getSimpleName();
    BroadcastReceiver hrReceiver;
    Button heartButton;

    // ID for body permissions
    private static final int REQUEST_BODY_PERM = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heart, container, false);

        heartButton = (Button) view.findViewById(R.id.heart_button);
        heartButton.setBackgroundResource(R.drawable.ic_heart_red);
        heartButton.setText("...");
        heartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Heart button click");
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        hrReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Show the last reading
                float[] hrValues = intent.getFloatArrayExtra(HeartRateSensorService.HEART_RATE);
                if (hrValues.length > 0) {
                    setHeartRate(hrValues[hrValues.length-1]);
                }
            }
        };
        IntentFilter filter = new IntentFilter(HeartRateSensorService.ACTION_HEART_RATE_CHANGED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(hrReceiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check for the correct permission for HRM
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BODY_SENSORS}, REQUEST_BODY_PERM);
        }
        else {
            getActivity().startService(new Intent(getContext(), HeartRateSensorService.class));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_BODY_PERM:
                Log.d(TAG, "HR permission granted");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getActivity().startService(new Intent(getContext(), HeartRateSensorService.class));
                }
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(hrReceiver);
    }

    private void setHeartRate(float heartRate){
        heartButton.setText(String.valueOf(heartRate));
    }

}
