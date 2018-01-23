package info.saxman.android.whererunner.persistence;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import info.saxman.android.whererunner.model.HeartRateSample;
import info.saxman.android.whererunner.model.LocationSample;
import info.saxman.android.whererunner.model.Workout;

@Dao
public interface WorkoutDao {
    @Insert
    void insertWorkout(Workout workout);

    @Query("SELECT * FROM workout ORDER BY startTime DESC LIMIT 5")
    Workout[] findLastFiveWorkouts();

    @Insert
    void insertHeartRateSamples(List<HeartRateSample> samples);

    @Insert
    void insertLocationSamples(List<LocationSample> samples);
}
