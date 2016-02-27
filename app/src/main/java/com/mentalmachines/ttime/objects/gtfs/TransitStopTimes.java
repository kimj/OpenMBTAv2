package com.mentalmachines.ttime.objects.gtfs;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * The commented fields are not used by the app
 * There are more than a million rows in this raw file, they cannot be loaded into a collection
 * One by one each (modified) row of the raw file is loaded into the db
 */
public class TransitStopTimes {
    public static final String TAG = "TransitStopTimes";

    static SQLiteDatabase mDB;
    final static ContentValues cv = new ContentValues();

    public String trip_id;
    public String arrival_time;
    public String departure_time;
    public String stop_id;
    //public String stop_sequence;
    //public String stop_headsign;
    //public String pickup_type;
    //public String drop_off_type;

    public TransitStopTimes(String str){
        String[] temp = str.split(",");

        this.trip_id = temp[0];
        this.arrival_time = temp[1];
        this.departure_time = temp[2];
        this.stop_id = temp[3];
        //this.stop_sequence = temp[4];
		/*this.stop_headsign = temp[5];
        this.pickup_type = temp[6];
        this.drop_off_type = temp[7];*/
    }

    public static void createScheduleEntries(Context ctx) {
        /*mDB = new DBHelper(ctx).getWritableDatabase();
        final BufferedReader rawReader = new BufferedReader(new InputStreamReader(ctx.getResources().openRawResource(R.raw.stop_times)));
        String line = "";
        Log.i(TAG, "reading the big file");
        TransitStopTimes sched;
        try {
            while((line = rawReader.readLine()) != null) {
                sched = new TransitStopTimes(line);
                cv.put(DBHelper.KEY_STOPID, sched.stop_id);
                cv.put(DBHelper.KEY_ARR_TIME, sched.arrival_time);
                cv.put(DBHelper.KEY_DEP_TIME, sched.departure_time);
                cv.put(DBHelper.KEY_ARR_TIME, sched.arrival_time);
                cv.put(DBHelper.KEY_DEP_TIME, sched.departure_time);
                //TODO would be nice to create an array list for weekdays, saturday, sunday and use contains
                for(TransitTrip trip: TransitCalendar.weekdays) {
                    if(trip.trip_id.equals(sched.trip_id)) {
                        //weekday trip
                        cv.put(DBHelper.KEY_TRIP_ID, trip.trip_id);
                        cv.put(DBHelper.KEY_TRIP_SIGN, trip.trip_headsign);
                        cv.put(DBHelper.KEY_ROUTE_ID, trip.route_id);
                        if(trip.isBus) {
                            if(trip.direction_id.equals("0")) {
                                Log.d(TAG, trip.route_id.length() < 3? DBHelper.WEEKDAY_TABLE_BUS_OUT:DBHelper.WEEKDAY_TABLE_4BUS_OUT
                                        + ": " + trip.trip_id + ", " + sched.stop_id + ", " + sched.arrival_time);
                                Log.d(TAG, "row " + mDB.insert(trip.route_id.length() < 3? DBHelper.WEEKDAY_TABLE_BUS_OUT:DBHelper.WEEKDAY_TABLE_4BUS_OUT, "", cv));
                            } else {
                                Log.i(TAG, "bus in " + sched.arrival_time + " "
                                        + mDB.insert(trip.route_id.length() < 3? DBHelper.WEEKDAY_TABLE_BUS_IN:DBHelper.WEEKDAY_TABLE_4BUS_IN, "", cv));
                            }
                        } else {
                            //trip is subway
                            if(trip.direction_id.equals("0")) {
                                Log.d(TAG, DBHelper.WEEKDAY_TABLE_SUB_OUT + ": " + trip.trip_id + ", " + sched.stop_id + ", " + sched.arrival_time);
                                Log.d(TAG, "row " + mDB.insert(DBHelper.WEEKDAY_TABLE_SUB_OUT, "", cv));
                            } else {
                                Log.i(TAG, "subway in " + sched.arrival_time + " " + mDB.insert(DBHelper.WEEKDAY_TABLE_SUB_IN, "", cv));
                            }
                        }

                    }
                }
                //check saturday and sunday next
                for(TransitTrip trip: TransitCalendar.saturdays) {
                    if(trip.trip_id.equals(sched.trip_id)) {
                        cv.put(DBHelper.KEY_TRIP_ID, trip.trip_id);
                        cv.put(DBHelper.KEY_TRIP_SIGN, trip.trip_headsign);
                        cv.put(DBHelper.KEY_ROUTE_ID, trip.route_id);
                        if(trip.isBus) {
                            if(trip.direction_id.equals("0")) {
                                Log.d(TAG, trip.route_id.length() < 3? DBHelper.SATURDAY_TABLE_BUS_OUT:DBHelper.SATURDAY_TABLE_4BUS_OUT + ": "
                                        + trip.trip_id + ", " + sched.stop_id + ", " + sched.arrival_time);
                                Log.d(TAG, "row " + mDB.insert(trip.route_id.length() < 3? DBHelper.SATURDAY_TABLE_BUS_OUT:DBHelper.SATURDAY_TABLE_4BUS_OUT, "", cv));
                            } else {
                                Log.i(TAG, "bus in " + sched.arrival_time + " "
                                        + mDB.insert(trip.route_id.length() < 3? DBHelper.SATURDAY_TABLE_BUS_IN:DBHelper.SATURDAY_TABLE_4BUS_IN, "", cv));
                            }
                        } else {
                            //trip is subway
                            if(trip.direction_id.equals("0")) {
                                Log.d(TAG, DBHelper.SATURDAY_TABLE_SUB_OUT + ": " + trip.trip_id + ", " + sched.stop_id + ", " + sched.arrival_time);
                                Log.d(TAG, "row " + mDB.insert(DBHelper.SATURDAY_TABLE_SUB_OUT, "", cv));
                            } else {
                                Log.i(TAG, "subway in " + sched.arrival_time + " " + mDB.insert(DBHelper.SATURDAY_TABLE_SUB_IN, "", cv));
                            }
                        }
                    }
                }

                for(TransitTrip trip: TransitCalendar.sundays) {
                    if(trip.trip_id.equals(sched.trip_id)) {
                        cv.put(DBHelper.KEY_TRIP_ID, trip.trip_id);
                        cv.put(DBHelper.KEY_TRIP_SIGN, trip.trip_headsign);
                        cv.put(DBHelper.KEY_ROUTE_ID, trip.route_id);
                        if(trip.isBus) {
                            if(trip.direction_id.equals("0")) {
                                Log.d(TAG, trip.route_id.length() < 3? DBHelper.SUNDAY_TABLE_BUS_OUT:DBHelper.SUNDAY_TABLE_4BUS_OUT + ": " + trip.trip_id + ", " + sched.stop_id + ", " + sched.arrival_time);
                                Log.d(TAG, "row " + mDB.insert(trip.route_id.length() < 3? DBHelper.SUNDAY_TABLE_BUS_OUT:DBHelper.SUNDAY_TABLE_4BUS_OUT, "", cv));
                            } else {
                                Log.i(TAG, "bus in " + sched.arrival_time + " "
                                        + mDB.insert(trip.route_id.length() < 3? DBHelper.SUNDAY_TABLE_BUS_IN:DBHelper.SUNDAY_TABLE_4BUS_IN, "", cv));
                            }
                        } else {
                            //trip is subway
                            if(trip.direction_id.equals("0")) {
                                Log.d(TAG, DBHelper.SUNDAY_TABLE_SUB_OUT + ": " + trip.trip_id + ", " + sched.stop_id + ", " + sched.arrival_time);
                                Log.d(TAG, "row " + mDB.insert(DBHelper.SUNDAY_TABLE_SUB_OUT, "", cv));
                            } else {
                                Log.i(TAG, "subway in " + sched.arrival_time + " " + mDB.insert(DBHelper.SUNDAY_TABLE_SUB_IN, "", cv));
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading GTFS");
            e.printStackTrace();
        }

        mDB.execSQL(DBHelper.WEEKDAY_DEX_B_IN);
        mDB.execSQL(DBHelper.WEEKDAY_DEX_B_OUT);
        mDB.execSQL(DBHelper.WEEKDAY_DEX_SUB_IN);
        mDB.execSQL(DBHelper.WEEKDAY_DEX_SUB_OUT);
        mDB.execSQL(DBHelper.SATURDAY_DEX_B_IN);
        mDB.execSQL(DBHelper.SATURDAY_DEX_B_OUT);
        mDB.execSQL(DBHelper.SATURDAY_DEX_SUB_IN);
        mDB.execSQL(DBHelper.SATURDAY_DEX_SUB_OUT);
        mDB.execSQL(DBHelper.SUN_DEX_B_IN);
        mDB.execSQL(DBHelper.SUN_DEX_B_OUT);
        mDB.execSQL(DBHelper.SUN_DEX_SUB_IN);
        mDB.execSQL(DBHelper.SUN_DEX_SUB_OUT);
        mDB.close();*/
    }

}
