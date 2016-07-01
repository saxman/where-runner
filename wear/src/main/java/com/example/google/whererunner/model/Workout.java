package com.example.google.whererunner.model;


import android.os.Parcel;
import android.os.Parcelable;

public class Workout implements Parcelable {

    private int type;
    private long id;
    private long startTime;
    private long endTime;
    private double distance;
    private double averageSpeed;

    public Workout(long id, int type, long startTime, long endTime) {
        this.id = id;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Private since only used by Parcelable.Creator
    private Workout(Parcel parcel) {
        type = parcel.readInt();
        id = parcel.readLong();
        startTime = parcel.readLong();
        endTime = parcel.readLong();
        distance = parcel.readDouble();
        averageSpeed = parcel.readDouble();
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

    public double getAverageSpeed() {
        return averageSpeed;
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
        parcel.writeDouble(averageSpeed);
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
