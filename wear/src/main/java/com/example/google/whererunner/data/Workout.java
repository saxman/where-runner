package com.example.google.whererunner.data;


public class Workout {

    private int type;
    private long id;
    private long startTime;
    private long endTime;

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

    public Workout(long id, int type, long startTime, long endTime) {
        this.id = id;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
    }

}
