package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.ShowScheduleActivity;
import com.mentalmachines.ttime.TTimeApp;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.Schedule;
import com.mentalmachines.ttime.objects.StopData;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by emezias on 1/11/16.
 * This class runs the API requests to get a full day's schedule time, based on the route
 * Three schedule objects are created, weekday, saturday, and sunday/holiday
 * the schedule activity reads the public static data from here
 * Favorite routes will be persisted in SQLite and read from there (TODO)
 *
 * For busy routes, using the maxtrips value of 100 did not get the full day
 * Based on those results, the service will make a call for each period of the schedule day
 * and spawn threads to parse these calls in parallel
 */
public class ShowScheduleServiceThreaded extends IntentService {

    public static final String TAG = "ShowScheduleService";
    public static volatile Schedule sWeekdays, sSaturday, sSunday;
    //boolean to help with threading/error handling when switching between the days
    Calendar c = Calendar.getInstance();
    volatile boolean mRedLineSpecial = false;
    public static volatile Route mRoute;
    public static volatile int scheduleType;

    //Base URL
    public static final String BASE = "http://realtime.mbta.com/developer/api/v2/";
    public static final String SUFFIX = "?api_key=3G91jIONLkuTMXbnbF7Leg&format=json";

    //JSON constants for predictive times, data is not in SQLite
    public static final String ROUTEPARAM = "&route=";
    public static final String DATETIMEPARAM = "&datetime=";
    public static final String SCHEDVERB = "schedulebyroute";
    public static final String ALLHR_PARAM = "&max_trips=100";
    public static final String TIME_PARAM = "&max_time=";
    public static final String GETSCHEDULE = BASE + SCHEDVERB + SUFFIX + ALLHR_PARAM + ROUTEPARAM;
    //required, empty constructor, builds intents
    public ShowScheduleServiceThreaded() {
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
        if(b == null || !b.containsKey(TAG)) {
            //error, service requires 2 extras
            sendBroadcast(true);
            Log.w(TAG, "missing route extra, using existing");
            return;
        }
        scheduleType = b.getInt(TAG);
        Log.d(TAG, "call day: " + scheduleType);

        //this is a new route to display, call from Main
        if(b.containsKey(DBHelper.KEY_ROUTE_ID)) {
            mRoute = b.getParcelable(DBHelper.KEY_ROUTE_ID);
            Log.i(TAG, "new route from MainActivity");
            sSunday = new Schedule(mRoute);
            sSaturday = new Schedule(mRoute);
            sWeekdays = new Schedule(mRoute);
        } else if(mRoute == null) {
            Log.e(TAG, "missing Route extra!");
            sendBroadcast(true);
            return;
        }

        if(mRoute.id.contains(DBHelper.ASHMONT) || mRoute.id.contains(DBHelper.BRAINTREE)) {
            mRedLineSpecial = true;
        }

        //Route passed in from MainActivity is fully populated
        if(DBHelper.checkForScheduleTable(mRoute.id)) {
            //read the times from the DB instead of calling the MBTA
            Log.d(TAG, "creating schedule from database: " + mRoute.name);
            final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
            //each stop data object kicks off a thread to read from the db
            new ScheduleQuery(db, scheduleType, mRoute.mInboundStops, true).start();
            new ScheduleQuery(db, scheduleType, mRoute.mOutboundStops, false).start();
        } else {
            Log.d(TAG, "creating schedule from network: " + mRoute.name);
            getScheduleTimes();
        }

    }

    void sendBroadcast(boolean hasError) {
        Log.d(TAG, "end show schedule service");
        final Intent returnResults = new Intent(TAG);
        returnResults.putExtra(TAG, hasError);
        if(!hasError) {
            returnResults.putExtra(ShowScheduleActivity.TAG, scheduleType);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(returnResults);
    }

    /**
     * Calls are executed in parallel to build up the two hash maps
     * Once all 5 calls for a day are done, the schedule is set to the static global variable for that day
     * @param sch Schedule copy built from mRoute
     * @param apiCallString the api call for a specific period
     * @param timing the int constant designating the period
     * @return boolean to indicate whether or not to broadcast
     * @throws IOException
     */
    boolean parseSchedulePeriod(Schedule sch, String apiCallString, int timing) throws IOException {
        final JsonParser parser = new JsonFactory().createParser(new URL(apiCallString));
        final StringBuilder strBuild = new StringBuilder(0);
        final Calendar c = Calendar.getInstance();
        //manage current date and time
        final HashMap<String, Schedule.StopTimes> inboundMap = new HashMap<>();
        final HashMap<String, Schedule.StopTimes> outboundMap = new HashMap<>();
        Log.d(TAG, "schedule call! " + apiCallString);
        boolean skiptrip = false;
        Schedule.StopTimes tmpTimes = null;
        int hrs, mins, dirID = 0, counter = 0;
        String tmp;
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
                            if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_TRIP_NM.equals(parser.getCurrentName())) {
                                token = parser.nextToken();
                                tmp = parser.getValueAsString();

                                if(mRedLineSpecial && !tmp.contains(mRoute.id)) {
                                    //save this trip
                                    skiptrip = true;
                                } else {
                                    skiptrip = false;
                                }
                                //Log.d(TAG, "skiptrip? " + skiptrip);
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
                                        if(tmp != null && tmp.equals("70120")) {
                                            Log.d(TAG, "trip with Fenway Inbound " + counter++);
                                        }
                                        if(dirID == 0) {
                                            //direction id is 0 or 1
                                            tmpTimes = outboundMap.get(tmp);
                                            if(tmpTimes == null) {
                                                tmpTimes = new Schedule.StopTimes();
                                                tmpTimes.stopId = tmp;
                                                outboundMap.put(tmpTimes.stopId, tmpTimes);
                                                //Log.d(TAG, tmp + " new stop times " + tmp);
                                            }
                                        } else {
                                            tmpTimes = inboundMap.get(tmp);
                                            if(tmpTimes == null) {
                                                tmpTimes = new Schedule.StopTimes();
                                                tmpTimes.stopId = tmp;
                                                inboundMap.put(tmpTimes.stopId, tmpTimes);
                                                //Log.d(TAG, tmp + " new stop times " + tmp);
                                            }
                                        }
                                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DTIME.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        if(!skiptrip) {
                                            tmp = parser.getValueAsString();
                                            stamp = 1000 * Long.valueOf(tmp);
                                            getTime(c, tmp);
                                            //save the epoch time, sort before display

                                            hrs = c.get(Calendar.HOUR_OF_DAY);
                                            mins = c.get(Calendar.MINUTE);
                                            if(timing == 0 && (hrs < 6 ||
                                                    (hrs == 6 && mins < 30))) {
                                                if(!tmpTimes.morning.contains(stamp)) {
                                                    tmpTimes.morning.add(stamp);
                                                }
                                            } else if((hrs > 6 && hrs < 9) ||
                                                    (hrs == 9 && mins == 0) ||
                                                    (hrs == 6 && mins > 30)) {
                                                if(!tmpTimes.amPeak.contains(stamp)) {
                                                    tmpTimes.amPeak.add(stamp);
                                                }
                                            } else if((hrs > 9 && hrs < 15) ||
                                                    (hrs == 9 && mins > 0) ||
                                                    (hrs == 15 && mins < 30)) {
                                                if(!tmpTimes.midday.contains(stamp)) {
                                                    tmpTimes.midday.add(stamp);
                                                }
                                            } else if((hrs > 14 && hrs < 18) ||
                                                    (hrs == 18 && mins < 30) ||
                                                    (hrs == 15 && mins > 30)) {
                                                if(!tmpTimes.pmPeak.contains(stamp)) {
                                                    tmpTimes.pmPeak.add(stamp);
                                                }
                                            } else if(hrs > 18 ||
                                                    (hrs == 18 && mins > 30)) {
                                                if(!tmpTimes.night.contains(stamp)) {
                                                    tmpTimes.night.add(stamp);
                                                }
                                            }

                                        }

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
        Log.d(TAG, "parse Complete, timing is " + timing);
        if(!parser.isClosed()) parser.close();
        sch.sLoading[timing] = true;
        //add the parsed times into the schedule
        if(sch.TripsInbound != null) {
            combineTimes(sch, inboundMap, timing);
            Log.d(TAG, "new size " + inboundMap.size());
         }
        sch.TripsInbound = new Schedule.StopTimes[inboundMap.size()];
        inboundMap.values().toArray(sch.TripsInbound);

        if(sch.TripsOutbound != null) {
            combineTimes(sch, outboundMap, timing);
            Log.d(TAG, "new size " + outboundMap.size());
        }
        sch.TripsOutbound = new Schedule.StopTimes[outboundMap.size()];
        outboundMap.values().toArray(sch.TripsOutbound);
        for(boolean b: sch.sLoading) {
            if(!b) {
                return false;
            }
        }
        sendBroadcast(false);
        Log.i(TAG, "broadcasting to main");
        //message activity that schedule is ready
        return true;
    }

    void combineTimes(Schedule sch, HashMap<String, Schedule.StopTimes> map, int timing) {
        Schedule.StopTimes tmpTimes, newData;
        Log.d(TAG, "combine times, map size " + map.size());
        for(int dex = 0; dex < sch.TripsInbound.length; dex++) {
            tmpTimes = sch.TripsInbound[dex];
            newData = map.get(tmpTimes.stopId);
            if (newData == null) {
                map.put(tmpTimes.stopId, tmpTimes);
            } else {
                for(long stamp: tmpTimes.morning) {
                    if(!newData.morning.contains(stamp)) {
                        newData.morning.add(stamp);
                    }
                }
                for(long stamp: tmpTimes.amPeak) {
                    if(!newData.amPeak.contains(stamp)) {
                        newData.amPeak.add(stamp);
                    }
                }
                for(long stamp: tmpTimes.midday) {
                    if(!newData.midday.contains(stamp)) {
                        newData.midday.add(stamp);
                    }
                }
                for(long stamp: tmpTimes.pmPeak) {
                    if(!newData.pmPeak.contains(stamp)) {
                        newData.pmPeak.add(stamp);
                    }
                }
                for(long stamp: tmpTimes.night) {
                    if(!newData.night.contains(stamp)) {
                        newData.night.add(stamp);
                    }
                }
            }
        }
    }

    void setDay(int dayOfWeek) {
        c = Calendar.getInstance();
        final int today = c.get(Calendar.DAY_OF_YEAR);
        c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        // The past day of week [ May include today ]
        if(c.get(Calendar.DAY_OF_YEAR) < today) {
            c.add(Calendar.DATE, 7);
        }
    }

    void startThread(int hour, int minute, int duration, int timing) {
        c.set(Calendar.HOUR_OF_DAY,hour);
        c.set(Calendar.MINUTE, minute);
        //time set to collect 24hr schedule for tomorrow
        final String url;
        if(mRedLineSpecial) {
            url = ShowScheduleServiceThreaded.GETSCHEDULE + "Red" +
                    ShowScheduleServiceThreaded.DATETIMEPARAM + Long.valueOf(c.getTimeInMillis()/1000).intValue() +
                    ShowScheduleServiceThreaded.TIME_PARAM + duration;
        } else {
            url = ShowScheduleServiceThreaded.GETSCHEDULE + mRoute.id +
                    ShowScheduleServiceThreaded.DATETIMEPARAM + Long.valueOf(c.getTimeInMillis()/1000).intValue() +
                    ShowScheduleServiceThreaded.TIME_PARAM + duration;
        }
        new ScheduleCall(url, timing, scheduleType).start();
    }

    void getScheduleTimes() {
        setDay(scheduleType);
        c.set(Calendar.SECOND, 0);
        startThread(0, 0, 389, Schedule.MORNING);
        //390 minutes, 6.5 hours, takes us through morning
        startThread(6, 30, 149, Schedule.AMPEAK);
        //2.5 hours through morning peak
        startThread(9, 0, 389, Schedule.MIDDAY);
        //6.5 hours, takes us through midday
        startThread(15, 30, 179, Schedule.PMPEAK);
        //evening peak, rush hour done
        startThread(18, 30, 330, Schedule.NIGHT);
        //finish off the scheduleType
        Log.d(TAG, "get schedule times called all threads");
    }

    volatile static int inCounter = 0;
    volatile static int outCounter = 0;
    private class ScheduleQuery extends Thread {

        final SQLiteDatabase mDB;
        final ArrayList<StopData> mStops;
        final int mScheduleType;

        public ScheduleQuery(SQLiteDatabase db, int day, ArrayList<StopData> stops, boolean inBound) {
            mDB = db;
            mStops = stops;
            mScheduleType = day;
            if(inBound) inCounter++;
            else outCounter++;
        }

        @Override
        public void run() {
            switch(mScheduleType) {
                case Calendar.TUESDAY:
                    for(StopData s: mStops) {
                        sWeekdays.createStopTimes(mDB, mScheduleType, s.stopId);
                    }
                    break;
                case Calendar.SATURDAY:
                    for(StopData s: mStops) {
                        sSaturday.createStopTimes(mDB, mScheduleType, s.stopId);
                    }
                    break;
                case Calendar.SUNDAY:
                    for(StopData s: mStops) {
                        sSunday.createStopTimes(mDB, mScheduleType, s.stopId);
                    }
                    break;
            }
            if(inCounter == mRoute.mInboundStops.size() &&
                    outCounter == mRoute.mOutboundStops.size()) {
                Log.i(TAG, "schedule complete from DB");
                ShowScheduleServiceThreaded.this.sendBroadcast(false);
                inCounter = 0;
                outCounter = 0;
            }
        }
    } //end ScheduleQuery

    private class ScheduleCall extends Thread {
        final String mUrl;
        final int mPeriod;
        final int mScheduleType;

        public ScheduleCall(String apiCallString, int timing, int day) {
            mUrl = apiCallString;
            mPeriod = timing;
            mScheduleType = day;
            Log.d(TAG, "schedule call: " + mPeriod + ":" + mScheduleType);
        }

        @Override
        public void run() {
            try {
                switch(mScheduleType) {
                    case Calendar.TUESDAY:
                        if(parseSchedulePeriod(sWeekdays, mUrl, mPeriod)) {
                            Log.i(TAG, "schedule period completed: " + mPeriod);
                        }
                        break;
                    case Calendar.SATURDAY:
                        if(parseSchedulePeriod(sSaturday, mUrl, mPeriod)) {
                            Log.i(TAG, "schedule period completed: " + mPeriod);
                        }
                        break;
                    case Calendar.SUNDAY:
                        if(parseSchedulePeriod(sSunday, mUrl, mPeriod)) {
                            Log.i(TAG, "schedule period completed: " + mPeriod);
                        }
                        break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    } //end class

    volatile static DateFormat timeFormat = new SimpleDateFormat("h:mm a");
    static String getTime(Calendar cal, String stamp) {
        cal.setTimeInMillis(1000 * Long.valueOf(stamp));
        return timeFormat.format(cal.getTime());
    }

}//end class

