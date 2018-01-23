package info.saxman.android.whererunner.persistence;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import info.saxman.android.whererunner.model.HeartRateSample;
import info.saxman.android.whererunner.model.LocationSample;
import info.saxman.android.whererunner.model.Workout;

public class WorkoutRepository {

    private AppDatabase mAppDatabase;

    public WorkoutRepository(Context context) {
        mAppDatabase = Room.databaseBuilder(context, AppDatabase.class, AppDatabase.WORKOUT_DB_NAME + ".db")
                .addMigrations(AppDatabase.MIGRATION_1_2).build();
    }

    public void storeWorkout(Workout workout) {
        new Thread(() -> {
            mAppDatabase.workoutDao().insertWorkout(workout);
        }).start();
    }

    public void retrieveLastFiveWorkoutsAsync(DataReadObserver<ArrayList<Workout>> observer) {
        // TODO best way to do post to the UI thread?
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                //noinspection unchecked
                observer.onDataReadComplete((ArrayList<Workout>) inputMessage.obj);
            }
        };

        // Retrieve the data off the main thread, but once retrieved, post it to the main thread.
        new Thread(() -> {
            Message message = handler.obtainMessage();
            message.obj = new ArrayList<>(Arrays.asList(mAppDatabase.workoutDao().findLastFiveWorkouts()));
            handler.sendMessage(message);
        }).start();
    }

    public void storeHeartRateSamples(List<HeartRateSample> samples) {
        new Thread(() -> {
            mAppDatabase.workoutDao().insertHeartRateSamples(samples);
        }).start();
    }

    public void storeLocationSamples(List<LocationSample> samples) {
        new Thread(() -> {
            mAppDatabase.workoutDao().insertLocationSamples(samples);
        }).start();
    }

    public interface DataReadObserver<T> {
        void onDataReadComplete(T workouts);
    }
}
