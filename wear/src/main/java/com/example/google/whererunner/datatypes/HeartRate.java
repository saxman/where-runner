package com.example.google.whererunner.datatypes;

/**
 * Heart rate with timestamp
 */
public class HeartRate {

    private float heartRate;
    private long timestamp;

    public float getHeartRate() {
        return heartRate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public HeartRate(long timestamp, float heartRate) {
        this.timestamp = timestamp;
        this.heartRate = heartRate;
    }
}
