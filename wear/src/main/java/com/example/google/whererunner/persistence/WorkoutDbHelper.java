package com.example.google.whererunner.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import com.example.google.whererunner.model.Workout;
import com.example.google.whererunner.services.HeartRateSensorEvent;

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
        db.execSQL(WorkoutContract.SQL_CREATE_LOCATIONS);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Database upgrades wipes all data!
        db.execSQL(WorkoutContract.SQL_DELETE_WORKOUTS);
        db.execSQL(WorkoutContract.SQL_DELETE_HEARTRATES);
        db.execSQL(WorkoutContract.SQL_DELETE_LOCATIONS);
        onCreate(db);
    }

    //
    // Utility methods for reading from/writing to tables
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
    public void writeHeartRates(ArrayList<HeartRateSensorEvent> hrEvents) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();

        for (HeartRateSensorEvent hrEvent : hrEvents) {
            ContentValues values = new ContentValues();
            // TODO: Save min/max heart rate?
            values.put(WorkoutContract.HeartRate.COLUMN_NAME_TIMESTAMP, hrEvent.getTimestamp());
            values.put(WorkoutContract.HeartRate.COLUMN_NAME_HEART_RATE, hrEvent.getHeartRate());
            db.insert(WorkoutContract.HeartRate.TABLE_NAME, null, values);
        }

        db.endTransaction();
    }

    /**
     * Write an array of locations to the db
     */
    public void writeLocations(ArrayList<Location> locations) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();

        for (Location location : locations) {
            ContentValues values = new ContentValues();
            location.getTime();
            values.put(WorkoutContract.Location.COLUMN_NAME_TIMESTAMP, location.getTime());
            values.put(WorkoutContract.Location.COLUMN_NAME_LAT, location.getLatitude());
            values.put(WorkoutContract.Location.COLUMN_NAME_LNG, location.getLongitude());
            db.insert(WorkoutContract.Location.TABLE_NAME, null, values);
        }

        db.endTransaction();
    }

    /**
     * Count the number of workouts in the db
     */
    public long readNrWorkouts() {
        SQLiteDatabase db = this.getReadableDatabase();
        return DatabaseUtils.queryNumEntries(db, WorkoutContract.Workout.TABLE_NAME);
    }

    /**
     * Read the last 5 workouts in the db
     */
    public ArrayList<Workout> readLastFiveWorkouts() {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
            WorkoutContract.Workout._ID,
            WorkoutContract.Workout.COLUMN_NAME_TYPE,
            WorkoutContract.Workout.COLUMN_NAME_START_TIME,
            WorkoutContract.Workout.COLUMN_NAME_END_TIME,
        };

        String sortOrder = WorkoutContract.Workout.COLUMN_NAME_START_TIME + " DESC";
        String limit = "5";

        // TODO: make this a workout data type
        ArrayList<Workout> workouts = new ArrayList<>();

        try (Cursor c = db.query(
            WorkoutContract.Workout.TABLE_NAME,  // The table to query
            projection,                          // The columns to return
            null,                                // The columns for the WHERE clause
            null,                                // The values for the WHERE clause
            null,                                // don't group the rows
            null,                                // don't filter by row groups
            sortOrder,                           // The sort order
            limit                                // Limit the nr of results to 5
        )) {

            while (c.moveToNext()) {
                long id = c.getLong(
                        c.getColumnIndexOrThrow(WorkoutContract.Workout._ID)
                );
                int type = c.getInt(
                        c.getColumnIndexOrThrow(WorkoutContract.Workout.COLUMN_NAME_TYPE)
                );
                long startTime = c.getLong(
                        c.getColumnIndexOrThrow(WorkoutContract.Workout.COLUMN_NAME_START_TIME)
                );
                long endTime = c.getLong(
                        c.getColumnIndexOrThrow(WorkoutContract.Workout.COLUMN_NAME_END_TIME)
                );
                workouts.add(new Workout(id, type, startTime, endTime));
            }
        }
        return workouts;

    }

}