package com.example.google.whererunner.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Workout implements Parcelable {
    private long id;

    private long startTime;
    private long endTime;

    private int type;
    private double distance;

    private double speedMax;
    private double speedCurrent;

    private double heartRateAverage;
    private double heartRateMin;
    private double heartRateMax;
    private double heartRateCurrent;

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
        distance = parcel.readDouble();
        speedMax = parcel.readDouble();
        speedCurrent = parcel.readDouble();
        heartRateAverage = parcel.readDouble();
        heartRateMin = parcel.readDouble();
        heartRateMax = parcel.readDouble();
        heartRateCurrent = parcel.readDouble();
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

    public double getDistance() {
        return distance;
    }

    public double getSpeedAverage() {
        long time = endTime == 0 ? System.currentTimeMillis() : endTime;
        return distance / (time / 1000);
    }

    public double getSpeedMax() {
        return speedMax;
    }

    public double getSpeedCurrent() {
        return speedCurrent;
    }

    public double getHeartRateMin() {
        return heartRateMin;
    }

    public double getHeartRateMax() {
        return heartRateMax;
    }

    public double getHeartRateCurrent() {
        return heartRateCurrent;
    }

    public double getHeartRateAverage() {
        return heartRateAverage;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setSpeedMax(double speed) {
        speedMax = speed;
    }

    public void setCurrentSpeed(double speed) {
        speedCurrent = speed;

        if (speedCurrent > speedMax) {
            speedMax = speedCurrent;
        }
    }

    public void setCurrentHeartRate(double heartRate) {
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

        parcel.writeDouble(speedMax);
        parcel.writeDouble(speedCurrent);

        parcel.writeDouble(heartRateAverage);
        parcel.writeDouble(heartRateMin);
        parcel.writeDouble(heartRateMax);
        parcel.writeDouble(heartRateCurrent);
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
