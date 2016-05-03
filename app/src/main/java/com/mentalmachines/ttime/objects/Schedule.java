package com.mentalmachines.ttime.objects;

import android.content.ContentValues;
import android.database.Cursor;
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

        public ArrayList<Long> morning = new ArrayList<>();
        public ArrayList<Long> amPeak = new ArrayList<>();
        public ArrayList<Long> midday = new ArrayList<>();
        public ArrayList<Long> pmPeak = new ArrayList<>();
        public ArrayList<Long> night = new ArrayList<>();
    }

    public boolean scheduleIsLoaded() {
        for(boolean b: sLoading) {
            if(!b) return b;
        }
        return true;
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

    public static final int MORNING = 0;
    public static final int AMPEAK = 1;
    public static final int MIDDAY = 2;
    public static final int PMPEAK = 3;
    public static final int NIGHT = 4;
    final String[] selection = { DBHelper.KEY_DTIME};
    final String whereClause = DBHelper.KEY_STOPID + " LIKE '";
    final String moreWhere = "' AND " + DBHelper.KEY_DAY + "=";

    public void createStopTimes(SQLiteDatabase db, int day, String stopID) {
        final Cursor c = db.query(DBHelper.getRouteTableName(route.id),
                selection,
                whereClause + stopID + moreWhere + day, null, //parameters embedded into where clause
                null, null, "_id ASC");
                //String groupBy, String having, String orderBy)
        if(!c.moveToFirst()) {
            Log.w(TAG, "issue reading schedule table for stop: " + stopID);
            return;
        }
        final StopTimes times = new StopTimes();
        times.stopId = stopID;
        do {
            switch (c.getInt(0)) {
                case MORNING:
                    times.morning.add(c.getLong(1));
                    break;
                case AMPEAK:
                    times.amPeak.add(c.getLong(1));
                    break;
                case MIDDAY:
                    times.midday.add(c.getLong(1));
                    break;
                case PMPEAK:
                    times.pmPeak.add(c.getLong(1));
                    break;
                case NIGHT:
                    times.night.add(c.getLong(1));
                    break;
            }
        } while (c.moveToNext());
        Log.d(TAG, "times loaded: " + c.getCount());
        c.close();
    }

    /**
     * The next few methods all load the schedule into a new database table
     * They are called by the SaveScheduleService class
     * the data structure is created from MBTA servers
     */
    public void loadSchedule(SQLiteDatabase db, int calendarDay) {
        //Route and Schedule data is fully populated, now create table
        db.execSQL(DBHelper.getRouteTableSql(route.id));
        //index the table after the data is loaded
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
        final String table = DBHelper.getRouteTableName(route.id);
        Log.d(TAG, "load array: " + stopName + ":" + day + ":" + direction);
        for(Long schedtime: times.morning) {
            cv.put(DBHelper.KEY_STOPID, times.stopId);
            cv.put(DBHelper.KEY_STOPNM, stopName);
            cv.put(DBHelper.KEY_DIR_ID, direction);
            cv.put(DBHelper.KEY_DAY, day);
            cv.put(DBHelper.KEY_DTIME, schedtime);
            Log.d(TAG, "creating table: " + db.insert(table, "_id", cv));
            cv.clear();
        }
        Log.d(TAG, "creating ampeak");
        for(Long schedtime: times.amPeak) {
            cv.put(DBHelper.KEY_STOPID, times.stopId);
            cv.put(DBHelper.KEY_STOPNM, stopName);
            cv.put(DBHelper.KEY_DIR_ID, direction);
            cv.put(DBHelper.KEY_DAY, day);
            cv.put(DBHelper.KEY_DTIME, schedtime);
            db.insert(table, "_id", cv);
            cv.clear();
        }
        for(Long schedtime: times.midday) {
            cv.put(DBHelper.KEY_STOPID, times.stopId);
            cv.put(DBHelper.KEY_STOPNM, stopName);
            cv.put(DBHelper.KEY_DIR_ID, direction);
            cv.put(DBHelper.KEY_DAY, day);
            cv.put(DBHelper.KEY_DTIME, schedtime);
            db.insert(table, "_id", cv);
            cv.clear();
        }
        Log.d(TAG, "creating pmpeak");
        for(Long schedtime: times.pmPeak) {
            cv.put(DBHelper.KEY_STOPID, times.stopId);
            cv.put(DBHelper.KEY_STOPNM, stopName);
            cv.put(DBHelper.KEY_DIR_ID, direction);
            cv.put(DBHelper.KEY_DAY, day);
            cv.put(DBHelper.KEY_DTIME, schedtime);
            db.insert(table, "_id", cv);
            cv.clear();
        }
        Log.d(TAG, "creating night");
        for(Long schedtime: times.night) {
            cv.put(DBHelper.KEY_STOPID, times.stopId);
            cv.put(DBHelper.KEY_STOPNM, stopName);
            cv.put(DBHelper.KEY_DIR_ID, direction);
            cv.put(DBHelper.KEY_DAY, day);
            cv.put(DBHelper.KEY_DTIME, schedtime);
            db.insert(table, "_id", cv);
            cv.clear();
        }
        DBHelper.setIndexOnRouteTable(db, route.id);

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