package com.mentalmachines.ttime.objects.gtfs;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The commented fields are not used by the app
 * There are more than a million rows in this raw file, they cannot be loaded into a collection
 * One by one each (modified) row of the raw file is loaded into the db
 */
public class TransitStopTimes {
    public static final String TAG = "TransitStopTimes";

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
        final ContentValues cv = new ContentValues();
        final SQLiteDatabase db = new DBHelper(ctx).getWritableDatabase();
        final BufferedReader rawReader = new BufferedReader(new InputStreamReader(ctx.getResources().openRawResource(R.raw.stop_times)));
        String line = "";
        Log.i(TAG, "reading the big file");
        TransitStopTimes schedTime;
        try {
            while((line = rawReader.readLine()) != null) {
                schedTime = new TransitStopTimes(line);
                for(TransitTrip trip: TransitCalendar.weekdays) {
                    if(trip.trip_id.equals(schedTime.trip_id)) {
                        loadTable(schedTime, trip, DBHelper.WEEKDAY_TABLE, db, cv);
                    }
                }
                //now friday, saturday and sunday
                //Log.i(TAG, "fri, sat, sun, " + schedTime.trip_id);
                for(TransitTrip trip: TransitCalendar.fridays) {
                    if(trip.trip_id.equals(schedTime.trip_id)) {
                        loadTable(schedTime, trip, DBHelper.FRIDAY_TABLE, db, cv);
                    }
                }

                for(TransitTrip trip: TransitCalendar.saturdays) {
                    if(trip.trip_id.equals(schedTime.trip_id)) {
                        loadTable(schedTime, trip, DBHelper.SATURDAY_TABLE, db, cv);
                    }
                }

                for(TransitTrip trip: TransitCalendar.sundays) {
                    if(trip.trip_id.equals(schedTime.trip_id)) {
                        loadTable(schedTime, trip, DBHelper.SUNDAY_TABLE, db, cv);
                    }
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading GTFS");
            e.printStackTrace();
        }
        db.execSQL(DBHelper.WEEKDAY_DEX);
        db.execSQL(DBHelper.FRIDAY_DEX);
        db.execSQL(DBHelper.SATURDAY_DEX);
        db.execSQL(DBHelper.SUNDAY_DEX);
        db.close();
    }

    static void loadTable(TransitStopTimes sched, TransitTrip trip,
                          String table, SQLiteDatabase db, ContentValues cv) {
        cv.put(DBHelper.KEY_ROUTE_ID, trip.route_id);
        cv.put(DBHelper.KEY_DIR_ID, trip.direction_id);
        cv.put(DBHelper.KEY_STOPID, sched.stop_id);
        cv.put(DBHelper.KEY_TRIP_ID, trip.trip_id);
        cv.put(DBHelper.KEY_TRIP_SIGN, trip.trip_headsign);
        cv.put(DBHelper.KEY_ARR_TIME, sched.arrival_time);
        cv.put(DBHelper.KEY_DEP_TIME, sched.departure_time);
        Log.i(TAG, table + ": " + db.insert(DBHelper.FRIDAY_TABLE, "", cv));

        cv.clear();
    }

}
