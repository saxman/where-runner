package com.example.google.whererunner.datatypes;

/**
 * Lat/lng and timestamp
 */
public class LatLng {

    private double lat;
    private double lng;
    private long timestamp;

    public double getLat() {
        return lat;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(float lng) {
        this.lng = lng;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public LatLng(long timestamp, double lat, double lng) {
        this.timestamp = timestamp;
        this.lat = lat;
        this.lng = lng;
    }
}
