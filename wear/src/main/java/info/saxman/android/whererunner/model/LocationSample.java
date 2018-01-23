package info.saxman.android.whererunner.model;

import android.arch.persistence.room.Entity;

@Entity(tableName = "location", primaryKeys = {"timestamp", "lat", "lng"})
public class LocationSample {
    public long timestamp;

    public double lat;
    public double lng;
}