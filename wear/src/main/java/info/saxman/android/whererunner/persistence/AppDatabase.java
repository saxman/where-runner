package info.saxman.android.whererunner.persistence;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.DatabaseConfiguration;
import android.arch.persistence.room.InvalidationTracker;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;

import info.saxman.android.whererunner.model.HeartRateSample;
import info.saxman.android.whererunner.model.LocationSample;
import info.saxman.android.whererunner.model.Workout;

@Database(version = 2, entities = {Workout.class, HeartRateSample.class, LocationSample.class})
public abstract class AppDatabase extends RoomDatabase {
    static final String WORKOUT_DB_NAME = "workout";

    // Migration from original schema to Room-managed database.
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE workout_new (id INTEGER NOT NULL, type INTEGER NOT NULL, startTime INTEGER NOT NULL, endTime INTEGER NOT NULL, speedAvg REAL NOT NULL, speedMax REAL NOT NULL, distance REAL NOT NULL, PRIMARY KEY(id))");
            database.execSQL("INSERT INTO workout_new (id, type, startTime, endTime, speedAvg, speedMax, distance) SELECT _id, type, starttime, endtime, speedAvg, speedMax, distance FROM workout");
            database.execSQL("DROP TABLE workout");
            database.execSQL("ALTER TABLE workout_new RENAME TO workout");

            database.execSQL("CREATE TABLE location_new (timestamp INTEGER NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, PRIMARY KEY(timestamp, lat, lng))");
            database.execSQL("INSERT INTO location_new (timestamp, lat, lng) SELECT timestamp, lat, lng FROM location");
            database.execSQL("DROP TABLE location");
            database.execSQL("ALTER TABLE location_new RENAME TO location");

            database.execSQL("CREATE TABLE heartrate_new (timestamp INTEGER NOT NULL, heartRate INTEGER NOT NULL, PRIMARY KEY(timestamp, heartRate))");
            database.execSQL("INSERT INTO heartrate_new (timestamp, heartRate) SELECT timestamp, heartRate FROM heartrate");
            database.execSQL("DROP TABLE heartrate");
            database.execSQL("ALTER TABLE heartrate_new RENAME TO heartrate");
        }
    };

    @Override
    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration config) {
        return null;
    }

    @Override
    protected InvalidationTracker createInvalidationTracker() {
        return null;
    }

    abstract public WorkoutDao workoutDao();
}
