package com.mentalmachines.ttime.objects;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.mentalmachines.ttime.DBHelper;

import java.util.ArrayList;

public class Schedule{

    public static final String TAG = "Schedule";
	public Route route;
    /**
     * These array lists are parallel to the Inbound/Outbound stops of the route
     */
    public StopTimes[] TripsInbound;
    public StopTimes[] TripsOutbound;
    public volatile boolean[] sLoading = new boolean[5];

    public Schedule(Route r) {
        route = r;
        /*TripsInbound = new StopTimes[r.mInboundStops.size()];
        TripsOutbound = new StopTimes[r.mOutboundStops.size()];*/
        Log.d(TAG, "route sizing: " + r.mInboundStops.size() + ":" + r.mOutboundStops.size());
    }

    /**
     * Every time the T will depart from a specific stop on a route
     * Three schedules for every route, Weekday, Saturday and Sunday
     * The TripsInbound and outbound lists will have a stop times object for every stop on the route
     */
    public static class StopTimes {
        public String stopId;

        public ArrayList<String> morning = new ArrayList<>();
        public ArrayList<String> amPeak = new ArrayList<>();
        public ArrayList<String> midday = new ArrayList<>();
        public ArrayList<String> pmPeak = new ArrayList<>();
        public ArrayList<String> night = new ArrayList<>();
    }

    StopTimes findStop(String stopId, boolean inBound) {
        if(inBound) {
            for(StopTimes times: TripsInbound) {
                if(times.stopId.equals(stopId)) {
                    return times;
                }
            } //end for loop, not returned
        } else {
            for(StopTimes times: TripsOutbound) {
                if(times.stopId.equals(stopId)) {
                    return times;
                }
            } //end for loop, not returned
        }
        return null;
    }
    public void loadSchedule(SQLiteDatabase db, int calendarDay) {

        if(route.mInboundStops.size() != TripsInbound.length) {
            StopTimes stimes;
            for(StopData data: route.mInboundStops) {
                stimes = findStop(data.stopId, true);
                loadTimeArray(db, stimes, data.stopName, calendarDay, 1);
            }
        } else {
            for(int dex = 0; dex < TripsInbound.length; dex++) {
                StopData data = route.mInboundStops.get(dex);
                StopTimes stimes = TripsInbound[dex];
                loadTimeArray(db, stimes, data.stopName, calendarDay, 1);
            }
        }
        //inbound done, now to outbound
        if(route.mOutboundStops.size() != TripsOutbound.length) {
            StopTimes stimes;
            for(StopData data: route.mOutboundStops) {
                stimes = findStop(data.stopId, false);
                loadTimeArray(db, stimes, data.stopName, calendarDay, 1);
            }
        } else {
            for(int dex = 0; dex < TripsOutbound.length; dex++) {
                StopData data = route.mOutboundStops.get(dex);
                StopTimes stimes = TripsOutbound[dex];
                loadTimeArray(db, stimes, data.stopName, calendarDay, 1);
            }
        }

    }

    void loadTimeArray(SQLiteDatabase db, StopTimes times, String stopName, int day, int direction) {
        final ContentValues cv = new ContentValues();

        cv.put(DBHelper.KEY_STOPID, times.stopId);
        cv.put(DBHelper.KEY_STOPNM, stopName);
        cv.put(DBHelper.KEY_DIR_ID, direction);
        cv.put(DBHelper.KEY_DAY, day);
        for(String schedtime: times.morning) {
            cv.put(DBHelper.KEY_TRIP_PERIOD, 0);
            cv.put(DBHelper.KEY_DTIME, schedtime);
            Log.d(TAG, "creating table: " + db.insert(route.name, "_id", cv));
        }
        Log.d(TAG, "creating ampeak");
        for(String schedtime: times.amPeak) {
            cv.put(DBHelper.KEY_TRIP_PERIOD, 1);
            cv.put(DBHelper.KEY_DTIME, schedtime);
            db.insert(route.name, "_id", cv);
        }
        for(String schedtime: times.midday) {
            cv.put(DBHelper.KEY_TRIP_PERIOD, 2);
            cv.put(DBHelper.KEY_DTIME, schedtime);
            db.insert(route.name, "_id", cv);
        }
        Log.d(TAG, "creating pmpeak");
        for(String schedtime: times.pmPeak) {
            cv.put(DBHelper.KEY_TRIP_PERIOD, 3);
            cv.put(DBHelper.KEY_DTIME, schedtime);
            db.insert(route.name, "_id", cv);
        }
        Log.d(TAG, "creating night");
        for(String schedtime: times.night) {
            cv.put(DBHelper.KEY_TRIP_PERIOD, 4);
            cv.put(DBHelper.KEY_DTIME, schedtime);
            db.insert(route.name, "_id", cv);
        }
    }


    /**
     * KEY_STOPID + " TEXT not null,"
     + KEY_STOPNM + " TEXT not null,"
     + KEY_DIR_ID + " NUMERIC not null,"
     + KEY_TRIP_PERIOD + " NUMERIC not null,"
     + KEY_DAY + " NUMERIC not null,"
     + KEY_DTIME*
     * Creator required for class implementing the parcelable interface.

    public static final Parcelable.Creator<Schedule> CREATOR = new Creator<Schedule>() {

        @Override
        public Schedule createFromParcel(Parcel parcel) {
            final Route route = parcel.readTypedObject(Route.CREATOR);
            Schedule schedule = new Schedule(route);

            return schedule;
        }

        @Override
        public Schedule[] newArray(int size) {
            return new Schedule[size];
        }
    };  */
}