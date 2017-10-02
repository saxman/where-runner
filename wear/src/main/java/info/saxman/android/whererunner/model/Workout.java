package info.saxman.android.whererunner.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Workout implements Parcelable {
    private long mId;

    private long mStartTime;
    private long mEndTime;

    private int mType;
    private float mDistance;

    private float mSpeedMax;
    private float mSpeedCurrent;
    private float mSpeedAverage;

    private float mHeartRateAverage;
    private int mHeartRateMin = Integer.MAX_VALUE;
    private int mHeartRateMax = Integer.MIN_VALUE;
    private int mHeartRateCurrent = -1;

    public Workout() {}

    public Workout(long id, int type) {
        this.mId = id;
        this.mType = type;
    }

    public Workout(long startTime) {
        this.mStartTime = startTime;
    }

    // Private since only used by Parcelable.Creator
    private Workout(Parcel parcel) {
        mType = parcel.readInt();
        mId = parcel.readLong();
        mStartTime = parcel.readLong();
        mEndTime = parcel.readLong();
        mDistance = parcel.readFloat();
        mSpeedMax = parcel.readFloat();
        mSpeedCurrent = parcel.readFloat();
        mSpeedAverage = parcel.readFloat();
        mHeartRateAverage = parcel.readFloat();
        mHeartRateMin = parcel.readInt();
        mHeartRateMax = parcel.readInt();
    }

    public void setType(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }

    public long getId() {
        return mId;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }

    public float getDistance() {
        return mDistance;
    }

    /**
     * @return Average speed for entire workout in meters / millisecond
     */
    public float getSpeedAverage() {
        // TODO calculate average speed if dist/time loaded from db.
        return mSpeedAverage;
    }

    /**
     * @return Max observed speed in meters / millisecond
     */
    public float getSpeedMax() {
        return mSpeedMax;
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
        this.mStartTime = startTime;
    }

    public void setDistance(float distance) {
        this.mDistance = distance;
    }

    public void setSpeedMax(float speed) {
        mSpeedMax = speed;
    }

    public void setCurrentSpeed(float speed) {
        mSpeedCurrent = speed;

        if (mSpeedCurrent > mSpeedMax) {
            mSpeedMax = mSpeedCurrent;
        }
    }

    public void setAverageSpeed(float speed) {
        mSpeedAverage = speed;
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
        this.mEndTime = endTime;
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
        parcel.writeInt(mType);
        parcel.writeLong(mId);
        parcel.writeLong(mStartTime);
        parcel.writeLong(mEndTime);
        parcel.writeDouble(mDistance);

        parcel.writeFloat(mSpeedMax);
        parcel.writeFloat(mSpeedCurrent);
        parcel.writeFloat(mSpeedAverage);

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
