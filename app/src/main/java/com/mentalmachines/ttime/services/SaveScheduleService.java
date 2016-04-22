package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.TTimeApp;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.Schedule;
import com.mentalmachines.ttime.objects.StopData;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by emezias on 3/29/16.
 * This class is like the ShowScheduleService but threads do not run in parallel
 * It is called when a route is set as a favorite to create a schedule table for that route
 * There are somewhere around 50K rows in the Green line table, each row a stop and time
 */
public class SaveScheduleService extends IntentService {

    public static final String TAG = "SaveScheduleService";
    final Calendar c = Calendar.getInstance();

    Route mRoute;
    //http://realtime.mbta.com/developer/api/v2/predictionsbystop?api_key=3G91jIONLkuTMXbnbF7Leg&format=json&stop=70077&include_service_alerts=false
    HashMap<String, String> nameMap = new HashMap<>();
    String mTablename;
    SQLiteDatabase db;
    //required, empty constructor, builds intents
    public SaveScheduleService() {
        super(TAG);
    }

    /**
     * The extra on the intent is the route that is being saved into a schedule table
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        //make the route here in the background
        //make route, read stops from the DB in the background
        Log.d(TAG, "handle intent");
        final Bundle b = intent.getExtras();
        if(b == null || !b.containsKey(DBHelper.KEY_ROUTE_ID)) {
            Log.w(TAG, "error, bad extras when saving schedule");
            return;
        }
        //the call from main includes the route id
        mRoute = b.getParcelable(DBHelper.KEY_ROUTE_ID);
        Log.i(TAG, "creating schedule table for " + mRoute.name);
        db = TTimeApp.sHelper.getWritableDatabase();
        if(DBHelper.checkForScheduleTable(mRoute.id)) {
            return;
        }
        db.execSQL(DBHelper.getRouteTableSql(mRoute.id));
        Log.d(TAG, "creating table command " + DBHelper.getRouteTableSql(mRoute.id));
        for(StopData data: mRoute.mInboundStops) {
            nameMap.put(data.stopId, data.stopName);
        }
        for(StopData data: mRoute.mOutboundStops) {
            nameMap.put(data.stopId, data.stopName);
        }
        mTablename = DBHelper.getRouteTableName(mRoute.id);
        //calls from the schedule activity use the same route
        Log.d(TAG, "table name " + DBHelper.getRouteTableName(mRoute.id));
        try {
            getScheduleTimes(Calendar.TUESDAY);
            getScheduleTimes(Calendar.SATURDAY);
            getScheduleTimes(Calendar.SUNDAY);
            Log.d(TAG, "all 3 calendar days parsed into DB");
            startService(new Intent(this, CopyDBService.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calls for each period build up the two hash maps
     * Once all 5 calls for a day are done, the schedule is set to the static global variable for that day
     * @param apiCallString the api call for a specific period
     * @return boolean to indicate whether or not to broadcast
     * @throws IOException
     *
     * TODO parse directly into the db
     */
    public void parseSchedulePeriod(String apiCallString, int timing, int calendarDay) throws IOException {
        final JsonParser parser = new JsonFactory().createParser(new URL(apiCallString));
        final StringBuilder strBuild = new StringBuilder(0);
        final Calendar c = Calendar.getInstance();
        Log.d(TAG, "schedule call! " + apiCallString);
        final ContentValues cv = new ContentValues();

        int hrs, mins, dirID = 0;
        String tmp, stopID = "";
        Long stamp;

        while (!parser.isClosed()) {
            //start parsing, get the token
            JsonToken token = parser.nextToken();
            if (token == null) {
                Log.w(TAG, "null token");
                break;
            }
            //running through tokens to get to direction array, "error" key comes up before any other
            if (JsonToken.FIELD_NAME.equals(token) && "error".equals(parser.getCurrentName())) {
                //This route is done for the night or the server is hosed -> something is wrong
                parser.close();
                Log.w(TAG, "error parsing schedule");
                token = null;
            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DIR.equals(parser.getCurrentName())) {
                //no names at the top of this return, straight into objects
                //direction, begin array, begin object
                //Log.d(TAG, "direction array");
                token = parser.nextToken();
                if (!JsonToken.START_ARRAY.equals(token)) {
                    break;
                }
                //direction array, then read the trips array
                while (!JsonToken.END_ARRAY.equals(token)) {
                    token = parser.nextToken();

                    if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DIR_ID.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        dirID = Integer.valueOf(parser.getValueAsString()).intValue();

                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_TRIP.equals(parser.getCurrentName())) {
                        //array of trips with stops inside, may be several trips returned
                        token = parser.nextToken();
                        if (!JsonToken.START_ARRAY.equals(token)) {
                            break;
                        }
                        //trip name, trip id need to get to STOPS array
                        while (!JsonToken.END_ARRAY.equals(token)) {
                            //running through the trips, read the trip array into times
                            token = parser.nextToken();
                            if (JsonToken.FIELD_NAME.equals(token) && DBHelper.STOP.equals(parser.getCurrentName())) {
                                //put the times into the Schedule's stop times object...
                                token = parser.nextToken();
                                if (!JsonToken.START_ARRAY.equals(token)) {
                                    break;
                                }
                                //trip name, trip id need to get to STOPS array and add times into the hours/minutes
                                while (!JsonToken.END_ARRAY.equals(token)) {
                                    token = parser.nextToken();
                                    //begin object
                                    if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_STOPID.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        stopID = parser.getValueAsString();

                                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DTIME.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        tmp = parser.getValueAsString();
                                        stamp = 1000 * Long.valueOf(tmp);
                                        getTime(c, tmp);
                                        //save the epoch time, sort before display
                                        hrs = c.get(Calendar.HOUR_OF_DAY);
                                        mins = c.get(Calendar.MINUTE);

                                        cv.put(DBHelper.KEY_STOPID, stopID);
                                        cv.put(DBHelper.KEY_STOPNM, nameMap.get(stopID));
                                        cv.put(DBHelper.KEY_DIR_ID, dirID);
                                        cv.put(DBHelper.KEY_DAY, calendarDay);
                                        cv.put(DBHelper.KEY_DTIME, stamp);
                                        if(timing == 0 && (hrs < 6 ||
                                                (hrs == 6 && mins < 30))) {
                                            cv.put(DBHelper.KEY_TRIP_PERIOD, Schedule.MORNING);
                                        } else if((hrs > 6 && hrs < 9) ||
                                                (hrs == 9 && mins == 0) ||
                                                (hrs == 6 && mins > 30)) {
                                            cv.put(DBHelper.KEY_TRIP_PERIOD, Schedule.AMPEAK);
                                        } else if((hrs > 9 && hrs < 15) ||
                                                (hrs == 9 && mins > 0) ||
                                                (hrs == 15 && mins < 30)) {
                                            cv.put(DBHelper.KEY_TRIP_PERIOD, Schedule.MIDDAY);
                                        } else if((hrs > 14 && hrs < 18) ||
                                                (hrs == 18 && mins < 30) ||
                                                (hrs == 15 && mins > 30)) {
                                            cv.put(DBHelper.KEY_TRIP_PERIOD, Schedule.PMPEAK);
                                        } else if(hrs > 18 ||
                                                (hrs == 18 && mins > 30)) {
                                            cv.put(DBHelper.KEY_TRIP_PERIOD, Schedule.NIGHT);
                                        }
                                        //using replace in case of possible duplicates, check for existing
                                        //Log.d(TAG, "content values: " + cv.toString());
                                        if(cv.containsKey(DBHelper.KEY_TRIP_PERIOD)) {
                                            db.replace(mTablename, "_id", cv);
                                        }
                                        strBuild.setLength(0);
                                        cv.clear();

                                    }
                                }//end stop array, all stops in the trip are set
                                token = JsonToken.NOT_AVAILABLE;
                            } //end stop token

                        }//trip array end
                        token = JsonToken.NOT_AVAILABLE;
                        //ending the trip array should not end the  parsing
                    }//trip field
                }
                token = JsonToken.NOT_AVAILABLE;
            }//end dir field

        } //parser is closed
        Log.d(TAG, "parse Complete");
        if(!parser.isClosed()) parser.close();
    }

    void setDay(int dayOfWeek) {
        c.setTimeInMillis(System.currentTimeMillis());
        final int today = c.get(Calendar.DAY_OF_YEAR);
        c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        // The past day of week [ May include today ]
        if(c.get(Calendar.DAY_OF_YEAR) < today) {
            c.add(Calendar.DATE, 7);
        }
    }

    void getScheduleTimes(int calendarDay) throws IOException {
        setDay(calendarDay);

        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        int tstamp = Long.valueOf(c.getTimeInMillis()/1000).intValue();
        //time set to collect 24hr schedule for tomorrow
        String url = ShowScheduleService.GETSCHEDULE + mRoute.id +
                ShowScheduleService.DATETIMEPARAM + tstamp +
                ShowScheduleService.TIME_PARAM + "389";
        //390 minutes, 6.5 hours, takes us through morning

        parseSchedulePeriod(url, Schedule.MORNING, calendarDay);
        //executorService.submit(new ScheduleCall(new Schedule(r), url, 0, scheduleType));
        Log.d(TAG, "calndr check: " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE));

        c.set(Calendar.HOUR, 6);
        c.set(Calendar.MINUTE, 30);
        c.set(Calendar.AM_PM, Calendar.AM);
        tstamp = Long.valueOf(c.getTimeInMillis()/1000).intValue();
        url = ShowScheduleService.GETSCHEDULE + mRoute.id +
                ShowScheduleService.DATETIMEPARAM + tstamp +
                ShowScheduleService.TIME_PARAM + "149";
        parseSchedulePeriod(url, Schedule.AMPEAK, calendarDay);
        //2.5 hours through morning peak
        Log.d(TAG, "calndr check: " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE));

        c.set(Calendar.HOUR, 9);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.AM_PM, Calendar.AM);
        tstamp = Long.valueOf(c.getTimeInMillis()/1000).intValue();
        url = ShowScheduleService.GETSCHEDULE + mRoute.id +
                ShowScheduleService.DATETIMEPARAM + tstamp +
                ShowScheduleService.TIME_PARAM + "389";
        //6.5 hours, takes us through midday
        parseSchedulePeriod(url, Schedule.MIDDAY, calendarDay);
        Log.d(TAG, "calndr check: " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE));

        c.set(Calendar.HOUR, 3);
        c.set(Calendar.MINUTE, 30);
        c.set(Calendar.AM_PM, Calendar.PM);
        tstamp = Long.valueOf(c.getTimeInMillis()/1000).intValue();
        url = ShowScheduleService.GETSCHEDULE + mRoute.id +
                ShowScheduleService.DATETIMEPARAM + tstamp +
                ShowScheduleService.TIME_PARAM + "179";
        parseSchedulePeriod(url, Schedule.PMPEAK, calendarDay);
        //evening peak, rush hour done
        Log.d(TAG, "calndr check: " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE));

        c.set(Calendar.HOUR, 6);
        c.set(Calendar.MINUTE, 30);
        c.set(Calendar.AM_PM, Calendar.PM);
        tstamp = Long.valueOf(c.getTimeInMillis()/1000).intValue();
        url = ShowScheduleService.GETSCHEDULE + mRoute.id +
                ShowScheduleService.DATETIMEPARAM + tstamp +
                ShowScheduleService.TIME_PARAM + "330";
        parseSchedulePeriod(url, Schedule.NIGHT, calendarDay);
        //finish off this schedule day...
        Log.d(TAG, "calndr check: " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE));
    }

    public static final DateFormat timeFormat = new SimpleDateFormat("h:mm a");
    String getTime(Calendar cal, String stamp) {
        cal.setTimeInMillis(1000 * Long.valueOf(stamp));
        return timeFormat.format(cal.getTime());
    }

}//end class

