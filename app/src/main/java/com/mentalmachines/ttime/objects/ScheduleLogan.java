package com.mentalmachines.ttime.objects;

import android.util.Log;

import com.bluelinelabs.logansquare.LoganSquare;
import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.mentalmachines.ttime.DBHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

@JsonObject
public class ScheduleLogan {
    public static final String TAG = "ScheduleLogan";

    final String[] selection = {DBHelper.KEY_TRIP_PERIOD, DBHelper.KEY_DTIME};
    final String whereClause = DBHelper.KEY_STOPID + " LIKE '";
    final String moreWhere = "' AND " + DBHelper.KEY_DAY + "=";
    final static HashMap<String, StopData> inBoundTimes = new HashMap<>();
    final static HashMap<String, StopData> outBoundTimes = new HashMap<>();
    final static Calendar checkStamp = Calendar.getInstance();
    @JsonField
    public String route_id;

    @JsonField
    public ArrayList<DirectionObject> direction;

    public ScheduleLogan() { }
    //empty constructor for Logan Square

    public static final Comparator<StopObject> ORDER = new Comparator<StopObject>() {
        public int compare(StopObject obj1, StopObject obj2) {
            return Long.valueOf(obj2.sch_dep_dt).compareTo(obj1.sch_dep_dt);
        }
    };

    public static void clearMaps() {
        inBoundTimes.clear();
        outBoundTimes.clear();
    }

    public static void makeMaps(Route r) {
        for(StopData data: r.mInboundStops) {
            inBoundTimes.put(data.stopId, data);
        }
        for(StopData data: r.mOutboundStops) {
            outBoundTimes.put(data.stopId, data);
        }
    }

    public static void readJsonStream(InputStream response, int calendarDay) throws IOException {
        Log.d(TAG, "sizes: " + inBoundTimes.size() + ":" + outBoundTimes.size());
        boolean directionInbound;
        StopData stop;
        ArrayList<Long> stopTimes;
        long timestamp;
        ScheduleLogan root = LoganSquare.parse(response, ScheduleLogan.class);
        Log.d(TAG, "route check" + root.route_id + " " +
                "dir size: " + root.direction.size());

        for(DirectionObject data: root.direction) {
            directionInbound = data.direction_id == 0? false:true;
            Log.d(TAG, "trips size: " + data.trip.size());
            for(TripObject vehicle: data.trip)  {
                //Log.d(TAG, "stop size: " + vehicle.stop.size());
                for(StopObject s: vehicle.stop) {
                    timestamp = s.sch_dep_dt * 1000;
                    checkStamp.setTimeInMillis(timestamp);
                    if(checkStamp.get(Calendar.DAY_OF_WEEK) == calendarDay) {
                        if(directionInbound) {
                            stop = inBoundTimes.get(s.stop_id);
                        } else {
                            stop = outBoundTimes.get(s.stop_id);
                        }
                        stopTimes = stop.getScheduleArray(calendarDay);
                        if(!stopTimes.contains(timestamp)) {
                            stopTimes.add(timestamp);
                        }
                    }

                }
            }
            Log.d(TAG, "sizes: " + inBoundTimes.size() + ":" + outBoundTimes.size());
        }
    }

    @JsonObject
    public static class DirectionObject {
        @JsonField
        public int direction_id;
        @JsonField
        public ArrayList<TripObject> trip;
    }

    @JsonObject
    public static class TripObject {
        @JsonField
        public ArrayList<StopObject> stop;
    }

    @JsonObject
    public static class StopObject {
        @JsonField
        public String stop_id;
        @JsonField
        public long sch_dep_dt;
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