package info.saxman.android.whererunner.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

@Entity
public class Workout implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public long id;

    private int type;

    private long startTime;
    private long endTime;

    private float distance;

    private float speedMax;

    @ColumnInfo(name = "speedAvg")
    private float speedAverage;

    @Ignore
    private float mSpeedCurrent;

    @Ignore
    private float mHeartRateAverage;
    @Ignore
    private int mHeartRateMin = Integer.MAX_VALUE;
    @Ignore
    private int mHeartRateMax = Integer.MIN_VALUE;
    @Ignore
    private int mHeartRateCurrent = -1;

    public Workout() {}

    // Private since only used by Parcelable.Creator
    private Workout(Parcel parcel) {
        type = parcel.readInt();
        id = parcel.readLong();
        startTime = parcel.readLong();
        endTime = parcel.readLong();
        distance = parcel.readFloat();
        speedMax = parcel.readFloat();
        mSpeedCurrent = parcel.readFloat();
        speedAverage = parcel.readFloat();
        mHeartRateAverage = parcel.readFloat();
        mHeartRateMin = parcel.readInt();
        mHeartRateMax = parcel.readInt();
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public long getId() {
        return id;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public float getDistance() {
        return distance;
    }

    /**
     * @return Average speed for entire workout in meters / millisecond
     */
    public float getSpeedAverage() {
        // TODO calculate average speed if dist/time loaded from db.
        return speedAverage;
    }

    /**
     * @return Max observed speed in meters / millisecond
     */
    public float getSpeedMax() {
        return speedMax;
    }

    /**
     * @return Current speed in meters / millisecond
     */
    public float getSpeedCurrent() {
        return mSpeedCurrent;
    }

    public int getHeartRateMin() {
        return mHeartRateMin;
    }

    public int getHeartRateMax() {
        return mHeartRateMax;
    }

    public float getHeartRateAverage() {
        return mHeartRateAverage;
    }

    public int getHeartRateCurrent() {
        return mHeartRateCurrent;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public void setSpeedMax(float speed) {
        speedMax = speed;
    }

    public void setSpeedCurrent(float speed) {
        mSpeedCurrent = speed;

        if (mSpeedCurrent > speedMax) {
            speedMax = mSpeedCurrent;
        }
    }

    public void setSpeedAverage(float speed) {
        speedAverage = speed;
    }

    public void setCurrentHeartRate(int heartRate) {
        mHeartRateCurrent = heartRate;

        if (mHeartRateCurrent > mHeartRateMax) {
            mHeartRateMax = mHeartRateCurrent;
        }

        if (mHeartRateCurrent < mHeartRateMin) {
            mHeartRateMin = mHeartRateCurrent;
        }
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setHeartRateAverage(float heartRateAverage) {
        this.mHeartRateAverage = heartRateAverage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(type);
        parcel.writeLong(id);
        parcel.writeLong(startTime);
        parcel.writeLong(endTime);
        parcel.writeDouble(distance);

        parcel.writeFloat(speedMax);
        parcel.writeFloat(mSpeedCurrent);
        parcel.writeFloat(speedAverage);

        parcel.writeFloat(mHeartRateAverage);
        parcel.writeInt(mHeartRateMin);
        parcel.writeInt(mHeartRateMax);
    }

    public static final Parcelable.Creator<Workout> CREATOR = new Parcelable.Creator<Workout>() {
        @Override
        public Workout createFromParcel(Parcel in) {
            return new Workout(in);
        }

        @Override
        public Workout[] newArray(int size) {
            return new Workout[size];
        }
    };
}
