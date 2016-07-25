package com.example.google.whererunner.model;

import com.example.google.whererunner.R;

public enum WorkoutType {
    RUNNING(R.drawable.ic_running, R.drawable.ic_running_white, R.string.activity_running),
    CYCLING(R.drawable.ic_cycling, R.drawable.ic_cycling_white, R.string.activity_cycling);

    public int drawableId;
    public int invertedDrawableId;
    public int titleId;

    WorkoutType(int drawableId, int invertedDrawableId, int titleId) {
        this.drawableId = drawableId;
        this.invertedDrawableId = invertedDrawableId;
        this.titleId = titleId;
    }
}
