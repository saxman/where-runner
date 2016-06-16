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

    public void setHeartRate(float heartRate) {
        this.heartRate = heartRate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public HeartRate(long timestamp, float heartRate) {
        this.timestamp = timestamp;
        this.heartRate = heartRate;
    }
}
