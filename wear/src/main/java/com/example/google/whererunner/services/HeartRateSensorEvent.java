package com.example.google.whererunner.services;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A parcelable data class for passing heart rate sensor data between services
 */
public class HeartRateSensorEvent implements Parcelable {

    private float heartRate;
    private float minHeartRate;
    private float maxHeartRate;
    private long timestamp;

    public float getHeartRate() {
        return heartRate;
    }

    public float getMinHeartRate() {
        return minHeartRate;
    }

    public float getMaxHeartRate() {
        return maxHeartRate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public HeartRateSensorEvent(float heartRate, float minHeartRate, float maxHeartRate, long timestamp) {
        this.heartRate = heartRate;
        this.minHeartRate = minHeartRate;
        this.maxHeartRate= maxHeartRate;
        this.timestamp = timestamp;
    }

    //
    // Parcel specific code
    //

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(heartRate);
        out.writeFloat(minHeartRate);
        out.writeFloat(maxHeartRate);
        out.writeLong(timestamp);
    }

    // Private so that only the `CREATOR` field can access.
    private HeartRateSensorEvent(Parcel in) {
        heartRate = in.readFloat();
        minHeartRate = in.readFloat();
        maxHeartRate = in.readFloat();
        timestamp = in.readLong();
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