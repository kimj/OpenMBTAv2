package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.TTimeApp;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.Schedule;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by emezias on 1/11/16.
 * This class runs the API requests to get a day's schedule
 * Three schedule objects are created, weekday, saturday, and sunday
 * the schedule activity reads them from here
 * Favorite routes will be persisted in SQLite and read from there (TODO)
 */
public class FullScheduleService extends IntentService {

    public static final String TAG = "FullScheduleService";
    public static Schedule sWeekdays, sSaturday, sSunday;
    Route searchRoute;
    final Time t = new Time();
    final Calendar c= Calendar.getInstance();
    //current date and time
    final StringBuilder strBuild = new StringBuilder(0);
    final HashMap<String, Schedule.StopTimes> mInbound = new HashMap<>();
    final HashMap<String, Schedule.StopTimes> mOutbound = new HashMap<>();
    //Base URL
    public static final String BASE = "http://realtime.mbta.com/developer/api/v2/";
    public static final String SUFFIX = "?api_key=3G91jIONLkuTMXbnbF7Leg&format=json";

    //JSON constants for predictive times, data is not in SQLite
    public static final String ROUTEPARAM = "&route=";
    public static final String DATETIMEPARAM = "&datetime=";
    public static final String SCHEDVERB = "schedulebyroute";
    public static final String ALLHR_PARAM = "&max_trips=100&max_time=1440";
    public static final String GETSCHEDULE = BASE + SCHEDVERB + SUFFIX + ALLHR_PARAM + ROUTEPARAM;
    //http://realtime.mbta.com/developer/api/v2/predictionsbystop?api_key=3G91jIONLkuTMXbnbF7Leg&format=json&stop=70077&include_service_alerts=false

    //required, empty constructor, builds intents
    public FullScheduleService() {
        super(TAG);
    }

    /**
     * The extra on the intent tells the service what to do, which call to make
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        //make the route here in the background
        //make route, read stops from the DB in the background
        final Bundle b = intent.getExtras();
        Log.d(TAG, "handle intent");
        if(b == null) {
            sendBroadcast(true);
            return;
        }
        if(b.containsKey(DBHelper.KEY_ROUTE_ID)) {
            //this is experimental...
            Log.d(TAG, "creating schedule");
            searchRoute = new Route();
            searchRoute.id = b.getString(DBHelper.KEY_ROUTE_ID);
            final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
            searchRoute.name = DBHelper.getRouteName(db, searchRoute.id);
            searchRoute.setStops(db);
            //Route data is fully populated
            try {
                Log.d(TAG, "parse weekday");
                parseWeekdaySchedule();
                sendBroadcast(false);
                Log.d(TAG, "parse saturday");
                parseSaturdaySchedule();
                Log.d(TAG, "parse sunday");
                parseSundaySchedule();
                Log.d(TAG, "end service");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

    }

    void sendBroadcast(boolean hasError) {
        Log.d(TAG, "end service");
        final Intent returnResults = new Intent(TAG);
        returnResults.putExtra(TAG, hasError);
        LocalBroadcastManager.getInstance(this).sendBroadcast(returnResults);
    }


    /**
     * get the weekdays schedule, set it to be the static object
     * @throws IOException
     */
    void parseWeekdaySchedule() throws IOException {
        //add schedule times and direction to the stops
        c.setTimeInMillis(System.currentTimeMillis());
        final int today = c.get(Calendar.DAY_OF_YEAR);
        c.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
        // This past Sunday [ May include today ]
        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        if(c.get(Calendar.DAY_OF_YEAR) < today) {
            c.add(Calendar.DATE,7);
        }

        int tstamp = Long.valueOf(c.getTimeInMillis()/1000).intValue();
        //time set to collect 24hr schedule for tomorrow
        sWeekdays = parseSchedule(GETSCHEDULE + searchRoute.id +
                DATETIMEPARAM + tstamp);
    }

    void parseSaturdaySchedule() throws IOException {
        //add schedule times and direction to the stops
        c.setTimeInMillis(System.currentTimeMillis());
        final int today = c.get(Calendar.DAY_OF_YEAR);
        c.set(Calendar.DAY_OF_WEEK,Calendar.SATURDAY);
        // This past Sunday [ May include today ]
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        if(c.get(Calendar.DAY_OF_YEAR) < today) {
            c.add(Calendar.DATE,7);
        }

        int tstamp = Long.valueOf(c.getTimeInMillis()/1000).intValue();
        //time set to collect 24hr schedule for tomorrow
        Log.d(TAG, "saturday " + tstamp);
        sSaturday = parseSchedule(GETSCHEDULE + searchRoute.id +
                DATETIMEPARAM + tstamp);
    }

    void parseSundaySchedule() throws IOException {
        //add schedule times and direction to the stops
        c.setTimeInMillis(System.currentTimeMillis());
        final int today = c.get(Calendar.DAY_OF_YEAR);
        c.set(Calendar.DAY_OF_WEEK,Calendar.SUNDAY);
        // This past Sunday [ May include today ]
        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        if(c.get(Calendar.DAY_OF_YEAR) < today) {
            c.add(Calendar.DATE,7);
        }

        int tstamp = Long.valueOf(c.getTimeInMillis()/1000).intValue();
        //time set to collect 24hr schedule for tomorrow
        sSunday = parseSchedule(GETSCHEDULE + searchRoute.id +
                DATETIMEPARAM + tstamp);
    }

    Schedule parseSchedule(String apiCallString) throws IOException {
        //these arraylists will serve as the index to find the right stoptimes object
        final JsonParser parser = new JsonFactory().createParser(new URL(apiCallString));
        Log.d(TAG, "schedule call? " + apiCallString);

        final Schedule sch = new Schedule(searchRoute);
        Schedule.StopTimes tmpTimes = null;
        String tripName = "";
        int dirID = 0;
        String directionNm = "", tmp;

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
                Log.w(TAG, "error reading " + searchRoute.name);
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

                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DIR_NM.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        directionNm = parser.getValueAsString();
                        //Log.d(TAG, "direction id set " + directionNm);
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
                            if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_TRIP_SIGN.equals(parser.getCurrentName())) {
                                //keep the trip name only once
                                token = parser.nextToken();
                                tripName = parser.getValueAsString();
                                //Log.d(TAG, "trip name set " + tripName);
                            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.STOP.equals(parser.getCurrentName())) {
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
                                        tmp = parser.getValueAsString();
                                        //stopId
                                        if(dirID == 0) {
                                            //direction id is 0 or 1
                                            tmpTimes = mOutbound.get(tmp);
                                            if(tmpTimes == null) {
                                                tmpTimes = new Schedule.StopTimes();
                                                tmpTimes.stopId = tmp;
                                                mOutbound.put(tmpTimes.stopId, tmpTimes);
                                                //Log.d(TAG, tmp + " new stop times " + tripName);
                                            }
                                        } else {
                                            tmpTimes = mInbound.get(tmp);
                                            if(tmpTimes == null) {
                                                tmpTimes = new Schedule.StopTimes();
                                                tmpTimes.stopId = tmp;
                                                mInbound.put(tmpTimes.stopId, tmpTimes);
                                                //Log.d(TAG, tmp + " new stop times " + tripName);
                                            }
                                        }
                                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DTIME.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        //Log.d(TAG, tmpTimes.stopId + " time added to stopTime " +
                                        tmp = ScheduleService.getTime(parser.getValueAsString(), t, strBuild);
                                        if(t.hour < 5) {
                                            tmpTimes.morning.add(tmp);
                                        } else if(t.hour == 5 && t.minute <= 30) {
                                            tmpTimes.morning.add(tmp);
                                        } else if(t.hour < 9) {
                                            tmpTimes.amPeak.add(tmp);
                                        } else if(t.hour < 14) {
                                            tmpTimes.midday.add(tmp);
                                        } else if(t.hour == 14 && t.minute <= 30) {
                                            tmpTimes.midday.add(tmp);
                                        } else if(t.hour < 17) {
                                            tmpTimes.pmPeak.add(tmp);
                                        } else if(t.hour == 17 && t.minute <= 30) {
                                            tmpTimes.pmPeak.add(tmp);
                                        } else {
                                            tmpTimes.night.add(tmp);
                                        }
                                        /**
                                         * AM Rush Hour: 6:30 AM - 9:00 AM
                                         Midday: 9:00 AM - 3:30 PM
                                         PM Rush Hour: 3:30 PM - 6:30 PM
                                         */
                                        strBuild.setLength(0);
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
        sch.TripsInbound = mInbound.values().toArray(sch.TripsInbound);
        sch.TripsOutbound = mOutbound.values().toArray(sch.TripsOutbound);
        mInbound.clear();
        mOutbound.clear();
        return sch;
    }


}//end class

