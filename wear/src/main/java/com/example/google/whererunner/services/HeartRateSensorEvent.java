package com.example.google.whererunner.services;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A parcelable data class for passing heart rate sensor data between services
 */
public class HeartRateSensorEvent implements Parcelable {

    private float heartRate;
    private long timestamp;
    private int accuracy;

    // Private since only used by Parcelable.Creator
    private HeartRateSensorEvent(Parcel in) {
        heartRate = in.readFloat();
        timestamp = in.readLong();
        accuracy = in.readInt();
    }

    public float getHeartRate() {
        return heartRate;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public HeartRateSensorEvent(float heartRate, long timestamp, int accuracy) {
        this.heartRate = heartRate;
        this.timestamp = timestamp;
        this.accuracy = accuracy;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(heartRate);
        out.writeLong(timestamp);
        out.writeInt(accuracy);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<HeartRateSensorEvent> CREATOR
            = new Parcelable.Creator<HeartRateSensorEvent>() {

        @Override
        public HeartRateSensorEvent createFromParcel(Parcel in) {
            return new HeartRateSensorEvent(in);
        }

        @Override
        public HeartRateSensorEvent[] newArray(int size) {
            return new HeartRateSensorEvent[size];
        }
    };
}