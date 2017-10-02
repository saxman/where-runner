package info.saxman.android.whererunner.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

import info.saxman.android.whererunner.model.Workout;
import info.saxman.android.whererunner.services.HeartRateSensorEvent;

/**
 * SQL DB helper for workouts
 */
public class WorkoutDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = WorkoutDatabaseHelper.class.getSimpleName();

    // If the database schema changes, increment the database version
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "workout.db";

    public WorkoutDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(WorkoutDatabaseContract.SQL_CREATE_WORKOUTS);
        db.execSQL(WorkoutDatabaseContract.SQL_CREATE_HEARTRATES);
        db.execSQL(WorkoutDatabaseContract.SQL_CREATE_LOCATIONS);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Database upgrades wipes all data!
        db.execSQL(WorkoutDatabaseContract.SQL_DELETE_WORKOUTS);
        db.execSQL(WorkoutDatabaseContract.SQL_DELETE_HEARTRATES);
        db.execSQL(WorkoutDatabaseContract.SQL_DELETE_LOCATIONS);
        onCreate(db);
    }

    //
    // Utility methods for reading from/writing to tables
    //

    /**
     * Writes a workout to the db
     */
    public long writeWorkout(Workout workout) {
        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(WorkoutDatabaseContract.Workout.COLUMN_NAME_TYPE, workout.getType());
        values.put(WorkoutDatabaseContract.Workout.COLUMN_NAME_START_TIME, workout.getStartTime());
        values.put(WorkoutDatabaseContract.Workout.COLUMN_NAME_END_TIME, workout.getEndTime());
        values.put(WorkoutDatabaseContract.Workout.COLUMN_NAME_DISTANCE, workout.getDistance());
        values.put(WorkoutDatabaseContract.Workout.COLUMN_NAME_SPEED_MAX, workout.getSpeedMax());
        values.put(WorkoutDatabaseContract.Workout.COLUMN_NAME_SPEED_AVG, workout.getSpeedAverage());

        long id = db.insert(WorkoutDatabaseContract.Workout.TABLE_NAME, null, values);

        db.close();

        return id;
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
            values.put(WorkoutDatabaseContract.HeartRate.COLUMN_NAME_TIMESTAMP, hrEvent.getTimestamp());
            values.put(WorkoutDatabaseContract.HeartRate.COLUMN_NAME_HEART_RATE, hrEvent.getHeartRate());
            db.insert(WorkoutDatabaseContract.HeartRate.TABLE_NAME, null, values);
        }

        db.endTransaction();
        db.close();
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
            values.put(WorkoutDatabaseContract.Location.COLUMN_NAME_TIMESTAMP, location.getTime());
            values.put(WorkoutDatabaseContract.Location.COLUMN_NAME_LAT, location.getLatitude());
            values.put(WorkoutDatabaseContract.Location.COLUMN_NAME_LNG, location.getLongitude());
            db.insert(WorkoutDatabaseContract.Location.TABLE_NAME, null, values);
        }

        db.endTransaction();
        db.close();
    }

    /**
     * Read the last 5 workouts in the db
     */
    public ArrayList<Workout> readLastFiveWorkouts() {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
            WorkoutDatabaseContract.Workout._ID,
            WorkoutDatabaseContract.Workout.COLUMN_NAME_TYPE,
            WorkoutDatabaseContract.Workout.COLUMN_NAME_START_TIME,
            WorkoutDatabaseContract.Workout.COLUMN_NAME_END_TIME,
            WorkoutDatabaseContract.Workout.COLUMN_NAME_DISTANCE,
            WorkoutDatabaseContract.Workout.COLUMN_NAME_SPEED_MAX,
            WorkoutDatabaseContract.Workout.COLUMN_NAME_SPEED_AVG
        };

        String sortOrder = WorkoutDatabaseContract.Workout.COLUMN_NAME_START_TIME + " DESC";
        String limit = "5";

        ArrayList<Workout> workouts = new ArrayList<>();

        try (Cursor c = db.query(
            WorkoutDatabaseContract.Workout.TABLE_NAME,  // The table to query
            projection,                          // The columns to return
            null,                                // The columns for the WHERE clause
            null,                                // The values for the WHERE clause
            null,                                // don't group the rows
            null,                                // don't filter by row groups
            sortOrder,                           // The sort order
            limit                                // Limit the nr of results to 5
        )) {
            while (c.moveToNext()) {
                long id = c.getLong(c.getColumnIndexOrThrow(WorkoutDatabaseContract.Workout._ID));
                int type = c.getInt(c.getColumnIndexOrThrow(WorkoutDatabaseContract.Workout.COLUMN_NAME_TYPE));
                long startTime = c.getLong(c.getColumnIndexOrThrow(WorkoutDatabaseContract.Workout.COLUMN_NAME_START_TIME));
                long endTime = c.getLong(c.getColumnIndexOrThrow(WorkoutDatabaseContract.Workout.COLUMN_NAME_END_TIME));
                float distance = c.getFloat(c.getColumnIndexOrThrow(WorkoutDatabaseContract.Workout.COLUMN_NAME_DISTANCE));
                float speedMax = c.getFloat(c.getColumnIndexOrThrow(WorkoutDatabaseContract.Workout.COLUMN_NAME_SPEED_MAX));
                float speedAvg = c.getFloat(c.getColumnIndexOrThrow(WorkoutDatabaseContract.Workout.COLUMN_NAME_SPEED_AVG));

                Workout workout = new Workout(id, type);
                workout.setStartTime(startTime);
                workout.setEndTime(endTime);
                workout.setDistance(distance);
                workout.setSpeedMax(speedMax);
                workout.setAverageSpeed(speedAvg);

                workouts.add(workout);
            }

            c.close();
        } catch(Exception e) {
            Log.e(TAG, "Error reading workouts: " + e.getMessage());
        } finally {
            db.close();
        }

        return workouts;
    }

    //
    // Start of beta async database calls
    //

    /**
     * Interface for handling Workout database read requests
     */
    public interface ReadWorkoutsCallback {

        void onRead(ArrayList<Workout> workouts);
    }

    public void readLastFiveWorkoutsAsync(ReadWorkoutsCallback callback) {
        new ReadWorkoutsTask(callback).execute();
    }

    /**
     * Reads the database asynchronously
     */
    private class ReadWorkoutsTask extends AsyncTask<Void, Void, ArrayList<Workout>> {

        private ReadWorkoutsCallback callback;

        public ReadWorkoutsTask(ReadWorkoutsCallback callback) {
            super();
            this.callback = callback;
        }

        @Override
        protected ArrayList<Workout> doInBackground(Void... ignored) {
            return readLastFiveWorkouts();
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(ArrayList<Workout> workouts) {
            this.callback.onRead(workouts);
        }
    }

}
