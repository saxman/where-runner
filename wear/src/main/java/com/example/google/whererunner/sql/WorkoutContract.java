package com.example.google.whererunner.sql;

import android.provider.BaseColumns;

/**
 * Schema definitions for storing workouts in sql
 */
public final class WorkoutContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public WorkoutContract() {}

    // Workout type mappings
    public enum WorkoutType {
        RUNNING(0), RIDING(1);

        private final int value;

        WorkoutType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Defines the workout table
     */
    public static abstract class Workout implements BaseColumns {
        public static final String TABLE_NAME = "workout";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_START_TIME = "starttime";
        public static final String COLUMN_NAME_END_TIME = "endtime";
    }

    /**
     * Defines the heartrate table
     */
    public static abstract class HeartRate implements BaseColumns {
        public static final String TABLE_NAME = "heartrate";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_HEART_RATE = "heartrate";
    }

    /**
     * Defines the location coordinates table
     */
    public static abstract class LatLng implements BaseColumns {
        public static final String TABLE_NAME = "latlng";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_LAT = "lat";
        public static final String COLUMN_NAME_LNG = "lng";
    }

    //
    // SQL data types and syntax
    //

    private static final String INT_TYPE = " INT";
    private static final String REAL_TYPE = " REAL";
    private static final String NOT_NULL = " NOT NULL";
    private static final String COMMA_SEP = ",";

    /**
     * Create the workout table
     */
     static final String SQL_CREATE_WORKOUTS =
            "CREATE TABLE " + Workout.TABLE_NAME + " (" +
                    Workout._ID + " INTEGER PRIMARY KEY," +
                    Workout.COLUMN_NAME_TYPE + INT_TYPE + NOT_NULL + COMMA_SEP +
                    Workout.COLUMN_NAME_START_TIME + INT_TYPE + NOT_NULL + COMMA_SEP +
                    Workout.COLUMN_NAME_END_TIME + INT_TYPE + NOT_NULL +
                    " )";

    /**
     * Drop the workout table
     */
     static final String SQL_DELETE_WORKOUTS =
            "DROP TABLE IF EXISTS " + Workout.TABLE_NAME;

    /**
     * Create the heartrate table
     */
     static final String SQL_CREATE_HEARTRATES =
            "CREATE TABLE " + HeartRate.TABLE_NAME + " (" +
                    HeartRate._ID + " INTEGER PRIMARY KEY," +
                    HeartRate.COLUMN_NAME_TIMESTAMP + INT_TYPE + NOT_NULL + COMMA_SEP +
                    HeartRate.COLUMN_NAME_HEART_RATE + REAL_TYPE + NOT_NULL +
                    " )";

    /**
     * Drop the heartrate table
     */
     static final String SQL_DELETE_HEARTRATES =
            "DROP TABLE IF EXISTS " + HeartRate.TABLE_NAME;

    /**
     * Create the latlng table
     */
     static final String SQL_CREATE_LAT_LNGS =
            "CREATE TABLE " + LatLng.TABLE_NAME + " (" +
                    LatLng._ID + " INTEGER PRIMARY KEY," +
                    LatLng.COLUMN_NAME_TIMESTAMP + INT_TYPE + NOT_NULL + COMMA_SEP +
                    LatLng.COLUMN_NAME_LAT + REAL_TYPE + NOT_NULL + COMMA_SEP +
                    LatLng.COLUMN_NAME_LNG + REAL_TYPE + NOT_NULL +
                    " )";

    /**
     * Drop the latlng table
     */
     static final String SQL_DELETE_LAT_LNGS =
            "DROP TABLE IF EXISTS " + LatLng.TABLE_NAME;

}
