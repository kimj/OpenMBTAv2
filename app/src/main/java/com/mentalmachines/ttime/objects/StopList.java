package com.mentalmachines.ttime.objects;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class StopList implements Parcelable {
    public static final String TAG = "StopList";
    public StopData mainStop;
    public ArrayList<StopData> mStopList;

    public StopList() { }

    public StopList(StopData m, ArrayList<StopData> list) {
        mainStop = m;
        mStopList = list;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(mainStop, 0);
        parcel.writeTypedList(mStopList);
    }

    /**
     * Creator required for class implementing the parcelable interface.
     */
    public static final Creator<StopList> CREATOR = new Creator<StopList>() {

        @Override
        public StopList createFromParcel(Parcel parcel) {
            StopList r = new StopList();
            r.mainStop = parcel.readParcelable(StopData.class.getClassLoader());
            r.mStopList = parcel.createTypedArrayList(StopData.CREATOR);
            return r;
        }

        @Override
        public StopList[] newArray(int size) {
            return new StopList[size];
        }
    };
}
