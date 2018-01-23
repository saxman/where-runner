package info.saxman.android.whererunner.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "heartrate", primaryKeys = {"timestamp", "heartRate"})
public class HeartRateSample implements Parcelable {

    public long timestamp;

    public int heartRate;

    public HeartRateSample() {}

    // Private since only used by Parcelable.Creator
    private HeartRateSample(Parcel in) {
        heartRate = in.readInt();
        timestamp = in.readLong();
    }

    @Ignore
    public HeartRateSample(int heartRate, long timestamp) {
        this.heartRate = heartRate;
        this.timestamp = timestamp;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(heartRate);
        out.writeLong(timestamp);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<HeartRateSample> CREATOR
            = new Parcelable.Creator<HeartRateSample>() {

        @Override
        public HeartRateSample createFromParcel(Parcel in) {
            return new HeartRateSample(in);
        }

        @Override
        public HeartRateSample[] newArray(int size) {
            return new HeartRateSample[size];
        }
    };
}