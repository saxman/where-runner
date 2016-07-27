package com.example.google.whererunner.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Workout implements Parcelable {
    private long id;

    private long startTime;
    private long endTime;

    private int type;
    private float distance;

    private float speedMax;
    private float speedCurrent;

    private float heartRateAverage;
    private int heartRateMin = Integer.MAX_VALUE;
    private int heartRateMax = Integer.MIN_VALUE;
    private int heartRateCurrent;

    public Workout() {}

    public Workout(long id, int type) {
        this.id = id;
        this.type = type;
    }

    public Workout(long startTime) {
        this.startTime = startTime;
    }

    // Private since only used by Parcelable.Creator
    private Workout(Parcel parcel) {
        type = parcel.readInt();
        id = parcel.readLong();
        startTime = parcel.readLong();
        endTime = parcel.readLong();
        distance = parcel.readFloat();
        speedMax = parcel.readFloat();
        speedCurrent = parcel.readFloat();
        heartRateAverage = parcel.readFloat();
        heartRateMin = parcel.readInt();
        heartRateMax = parcel.readInt();
        heartRateCurrent = parcel.readInt();
    }

    public int getType() {
        return type;
    }

    public long getId() {
        return id;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public float getDistance() {
        return distance;
    }

    public float getSpeedAverage() {
        long time = endTime == 0 ? System.currentTimeMillis() : endTime;
        return distance / (time / 1000);
    }

    public float getSpeedMax() {
        return speedMax;
    }

    public float getSpeedCurrent() {
        return speedCurrent;
    }

    public int getHeartRateMin() {
        return heartRateMin;
    }

    public int getHeartRateMax() {
        return heartRateMax;
    }

    public int getHeartRateCurrent() {
        return heartRateCurrent;
    }

    public float getHeartRateAverage() {
        return heartRateAverage;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public void setSpeedMax(float speed) {
        speedMax = speed;
    }

    public void setCurrentSpeed(float speed) {
        speedCurrent = speed;

        if (speedCurrent > speedMax) {
            speedMax = speedCurrent;
        }
    }

    public void setCurrentHeartRate(int heartRate) {
        heartRateCurrent = heartRate;

        if (heartRateCurrent > heartRateMax) {
            heartRateMax = heartRateCurrent;
        }

        if (heartRateCurrent < heartRateMin) {
            heartRateMin = heartRateCurrent;
        }
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setHeartRateAverage(float heartRateAverage) {
        this.heartRateAverage = heartRateAverage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(type);
        parcel.writeLong(id);
        parcel.writeLong(startTime);
        parcel.writeLong(endTime);
        parcel.writeDouble(distance);

        parcel.writeFloat(speedMax);
        parcel.writeFloat(speedCurrent);

        parcel.writeFloat(heartRateAverage);
        parcel.writeInt(heartRateMin);
        parcel.writeInt(heartRateMax);
        parcel.writeInt(heartRateCurrent);
    }

    public static final Parcelable.Creator<Workout> CREATOR = new Parcelable.Creator<Workout>() {
        @Override
        public Workout createFromParcel(Parcel in) {
            return new Workout(in);
        }

        @Override
        public Workout[] newArray(int size) {
            return new Workout[size];
        }
    };
}
