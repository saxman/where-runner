package com.example.google.whererunner.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.google.whererunner.datatypes.HeartRate;
import com.example.google.whererunner.datatypes.LatLng;

import java.util.ArrayList;

/**
 * SQL DB helper for workouts
 */
public class WorkoutDbHelper extends SQLiteOpenHelper {

    // If the database schema changes, increment the database version
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "workout.db";

    public WorkoutDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(WorkoutContract.SQL_CREATE_WORKOUTS);
        db.execSQL(WorkoutContract.SQL_CREATE_HEARTRATES);
        db.execSQL(WorkoutContract.SQL_CREATE_LAT_LNGS);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Database upgrades wipes all data!
        db.execSQL(WorkoutContract.SQL_DELETE_WORKOUTS);
        db.execSQL(WorkoutContract.SQL_DELETE_HEARTRATES);
        db.execSQL(WorkoutContract.SQL_DELETE_LAT_LNGS);
        onCreate(db);
    }

    //
    // Utility methods for reading/writing from/to tables
    //

    /**
     * Writes a workout to the db
     */
    public long writeWorkout(WorkoutContract.WorkoutType type, long startTime, long endTime) {
        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(WorkoutContract.Workout.COLUMN_NAME_TYPE, type.getValue());
        values.put(WorkoutContract.Workout.COLUMN_NAME_START_TIME, startTime);
        values.put(WorkoutContract.Workout.COLUMN_NAME_END_TIME, endTime);

        // Insert the new row, returning the primary key value of the new row
        return db.insert(WorkoutContract.Workout.TABLE_NAME, null, values);
    }

    /**
     * Writes an array of heart rate values to the db
     */
    public void writeHeartRates(ArrayList<HeartRate> heartRates) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();

        for (HeartRate heartrate : heartRates) {
            ContentValues values = new ContentValues();
            values.put(WorkoutContract.HeartRate.COLUMN_NAME_TIMESTAMP, heartrate.getTimestamp());
            values.put(WorkoutContract.HeartRate.COLUMN_NAME_HEART_RATE, heartrate.getHeartRate());
            db.insert(WorkoutContract.HeartRate.TABLE_NAME, null, values);
        }
        db.endTransaction();
    }

    /**
     * Write an array of lat lngs to the db
     */
    public void writeLatLngs(ArrayList<LatLng> latlngs) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();

        for (LatLng latlng : latlngs) {
            ContentValues values = new ContentValues();
            values.put(WorkoutContract.LatLng.COLUMN_NAME_TIMESTAMP, latlng.getTimestamp());
            values.put(WorkoutContract.LatLng.COLUMN_NAME_LAT, latlng.getLat());
            values.put(WorkoutContract.LatLng.COLUMN_NAME_LNG, latlng.getLng());
            db.insert(WorkoutContract.HeartRate.TABLE_NAME, null, values);
        }
        db.endTransaction();
    }

}