package com.mentalmachines.ttime.objects;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.bluelinelabs.logansquare.LoganSquare;
import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.mentalmachines.ttime.DBHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

@JsonObject
public class ScheduleParser {
    public static final String TAG = "ScheduleParser";

    final static String[] selection = {DBHelper.KEY_DTIME};
    //TODO drop stop name from table?
    final static String whereClause = DBHelper.KEY_STOPID + " LIKE '";
    final static String moreWhere = "' AND " + DBHelper.KEY_DAY + "=";

    final static Calendar checkStamp = Calendar.getInstance();

    @JsonField
    public String route_id;

    @JsonField
    public ArrayList<DirectionObject> direction;

    public ScheduleParser() { }
    //empty constructor for Logan Square

    public static Route readJsonStream(InputStream response, int calendarDay, Route route) throws IOException {
        //Log.d(TAG, "sizes: " + route.mOutboundStops.size() + ":" + route.mInboundStops.size());

        ArrayList<Long> stopTimes;
        long timestamp;
        ScheduleParser root = LoganSquare.parse(response, ScheduleParser.class);

        for (DirectionObject data : root.direction) {

            if (data.direction_id == 1)  {
                if(route.mInboundStops != null && route.mInboundStops.size() > 0) {
                    for (StopData routeStop : route.mInboundStops) {
                        stopTimes = routeStop.getScheduleArray(calendarDay);
                        for (TripObject vehicle : data.trip) {
                            //match times from server to stops in the route
                            for (StopObject s : vehicle.stop) {
                                timestamp = s.sch_dep_dt * 1000;
                                checkStamp.setTimeInMillis(timestamp);
                                if(checkStamp.get(Calendar.DAY_OF_WEEK) == calendarDay) {
                                    //exclude any overrun to the next day
                                    if (routeStop.stopId.equals(s.stop_id)
                                            && !stopTimes.contains(timestamp)) {
                                        stopTimes.add(timestamp);
                                    }
                                }
                            }
                        }//end trips
                    } //end inBound stops on route
                } else {
                    Log.v(TAG, "route is missing inbound stops " + route.name);
                }
            } else {
                if(route.mOutboundStops != null || route.mOutboundStops.size() > 0) {
                    for (StopData routeStop : route.mOutboundStops) {
                        stopTimes = routeStop.getScheduleArray(calendarDay);
                        //match above for outbound
                        for (TripObject vehicle : data.trip) {
                            //Log.d(TAG, "stop size: " + vehicle.stop.size());
                            for (StopObject s : vehicle.stop) {
                                timestamp = s.sch_dep_dt * 1000;
                                checkStamp.setTimeInMillis(timestamp);
                                if(checkStamp.get(Calendar.DAY_OF_WEEK) == calendarDay) {
                                    if (routeStop.stopId.equals(s.stop_id)
                                        && !stopTimes.contains(timestamp)) {
                                        stopTimes.add(timestamp);
                                    }
                                }
                            }
                        }//end trips
                    } //end outBound stops on route
                } else {
                    Log.v(TAG, "route is missing outbound stops " + route.name);
                }
            }

        }//end direction

        return route;
    }
        /*
        THis was working okay, want to dump the map collection

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
                        if(stop == null) {
                            Log.w(TAG, "missing stop!");
                        } else {
                            stopTimes = stop.getScheduleArray(calendarDay);
                            if(!stopTimes.contains(timestamp)) {
                                stopTimes.add(timestamp);
                            }
                        }

                    }

                }
            }
            Log.d(TAG, "sizes: " + inBoundTimes.size() + ":" + outBoundTimes.size());
        }*/


    public static void loadScheduleIntoDB(SQLiteDatabase db,
               int calendarDay, InputStream response, Route route) throws IOException {
        //Route and Schedule data is fully populated, now create table
        db.execSQL(DBHelper.getRouteTableSql(route.id));
        //index the table after the data is loaded
        final String table = DBHelper.getRouteTableName(route.id);
        ContentValues cv = new ContentValues();
        long timestamp;
        ScheduleParser root = LoganSquare.parse(response, ScheduleParser.class);
        Log.d(TAG, "route check" + root.route_id + " " +
                "dir size: " + root.direction.size());
        for(DirectionObject data: root.direction) {
            //Log.dTAG, "trips size: " + data.trip.size());
            for(TripObject vehicle: data.trip)  {
                //Log.d(TAG, "stop size: " + vehicle.stop.size());
                for(StopObject s: vehicle.stop) {
                    timestamp = s.sch_dep_dt * 1000;
                    checkStamp.setTimeInMillis(timestamp);
                    if(checkStamp.get(Calendar.DAY_OF_WEEK) == calendarDay) {
                        cv.put(DBHelper.KEY_STOPID, s.stop_id);
                        cv.put(DBHelper.KEY_STOPNM, s.stop_name);
                        cv.put(DBHelper.KEY_DIR_ID, data.direction_id);
                        cv.put(DBHelper.KEY_DAY, calendarDay);
                        cv.put(DBHelper.KEY_DTIME, timestamp);
                        Log.d(TAG, "creating table: " +
                                db.insertWithOnConflict(table, "_id", cv, SQLiteDatabase.CONFLICT_IGNORE));
                        cv.clear();
                    }

                }
            }
        }
        Log.d(TAG, "finished table entries " + calendarDay + ":" + route.name);
    }

    public static Route loadTimesFromDB(SQLiteDatabase db, Route route, int scheduleDay) {
        final String table = DBHelper.getRouteTableName(route.id);
        if(route.mInboundStops != null && route.mInboundStops.size() > 0) {
            route.mInboundStops = queryTimesForDirection(db, table, route.mInboundStops, scheduleDay);
        }
        //now the other way...
        if(route.mOutboundStops != null && route.mOutboundStops.size() > 0) {
            route.mOutboundStops = queryTimesForDirection(db, table, route.mOutboundStops, scheduleDay);
        }
        return route;
    }

    static ArrayList<StopData> queryTimesForDirection(SQLiteDatabase db, String table, ArrayList<StopData> routeStops, int scheduleDay) {
        Cursor c = null;
        ArrayList<Long> times;
        for(StopData data: routeStops) {
            if(data.getScheduleArray(scheduleDay).size() == 0) {
                c = db.query(table, selection, whereClause + data.stopId + moreWhere + scheduleDay,
                        null, null, null, null);
                times = data.getScheduleArray(scheduleDay);
                if(c.moveToFirst()) {
                    do {
                        times.add(c.getLong(0));
                    } while(c.moveToNext());
                }
            }
        }
        if(c != null && !c.isClosed()) {
            c.close();
        }
        return routeStops;
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
        @JsonField
        public String stop_name;
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