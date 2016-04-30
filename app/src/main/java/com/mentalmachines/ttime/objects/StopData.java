package com.mentalmachines.ttime.objects;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.mentalmachines.ttime.services.LoganScheduleSvc;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by CaptofOuterSpace on 1/23/2016.
 * Revised to support parcelable and transfer data using intents
 */
public class StopData implements Parcelable {
    public static final String TAG = "StopData";

    public String stopName; //to display
    public String stopId; //to check for alerts
    public String stopLat, stopLong; //to open Map
    public String stopAlert = null;
    public String predicTimes = "";
    public String schedTimes = "";
    //TODO change to TreeSet
    public ArrayList<Long> weekdayTimestamps = new ArrayList<>();
    public ArrayList<Long> saturdayTimestamps = new ArrayList<>();
    public ArrayList<Long> sundayTimestamps = new ArrayList<>();

    public StopData(){}

    public StopData(StopData makeCopy) {
        stopName = makeCopy.stopName;
        stopId = makeCopy.stopId;
        stopLat = makeCopy.stopLat;
        stopLong = makeCopy.stopLong;
        stopAlert = makeCopy.stopAlert;
    }

    public ArrayList<Long> getScheduleArray(int scheduleType) {
        switch(scheduleType) {
            case Calendar.TUESDAY:
                return weekdayTimestamps;
            case Calendar.SATURDAY:
                return saturdayTimestamps;
            case Calendar.SUNDAY:
                return sundayTimestamps;
        }
        Log.w(TAG, "weird schedule type");
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        // insert the key value pairs to the bundle
        dest.writeString(stopName);
        dest.writeString(stopId);
        dest.writeString(stopLat);
        dest.writeString(stopLong);
        dest.writeString(stopAlert);
        dest.writeString(predicTimes);
        dest.writeString(schedTimes);
        dest.writeList(weekdayTimestamps);
        dest.writeList(saturdayTimestamps);
        dest.writeList(sundayTimestamps);
        // write the value to the parcel
    }

    /**
     * Creator required for class implementing the parcelable interface.
     */
    public static final Parcelable.Creator<StopData> CREATOR = new Creator<StopData>() {

        @Override
        public StopData createFromParcel(Parcel source) {
            // read the data from the parcel in the same order as the write
            StopData s = new StopData();
            s.stopName = source.readString();
            s.stopId = source.readString();
            s.stopLat = source.readString();
            s.stopLong = source.readString();
            s.stopAlert = source.readString();
            s.predicTimes = source.readString();
            s.schedTimes = source.readString();
            s.weekdayTimestamps = new ArrayList<Long>();
            source.readList(s.weekdayTimestamps, null);
            s.saturdayTimestamps = new ArrayList<Long>();
            source.readList(s.saturdayTimestamps, null);
            s.sundayTimestamps = new ArrayList<Long>();
            source.readList(s.sundayTimestamps, null);
            return s;
        }

        @Override
        public StopData[] newArray(int size) {
            return new StopData[size];
        }
    };
}
