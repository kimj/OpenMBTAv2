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
public class ShowScheduleService extends IntentService {

    public static final String TAG = "ShowScheduleService";
    public static volatile Schedule sWeekdays, sSaturday, sSunday;
    //boolean to help with threading/error handling when switching between the days
    final Calendar c = Calendar.getInstance();

    public static volatile Route mRoute;
    public static volatile int scheduleType;

    //manage current date and time
    final static HashMap<String, Schedule.StopTimes> mInbound = new HashMap<>();
    final static HashMap<String, Schedule.StopTimes> mOutbound = new HashMap<>();
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
    //http://realtime.mbta.com/developer/api/v2/predictionsbystop?api_key=3G91jIONLkuTMXbnbF7Leg&format=json&stop=70077&include_service_alerts=false
    public static final int DAYINMILLIS = 86400000;
    //required, empty constructor, builds intents
    public ShowScheduleService() {
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
            mInbound.clear();
            mOutbound.clear();
            Log.i(TAG, "service reset");
            sSunday = new Schedule(mRoute);
            sSaturday = new Schedule(mRoute);
            sWeekdays = new Schedule(mRoute);
        }

        if(mRoute == null){
            Log.e(TAG, "missing Route extra!");
            sendBroadcast(true);
            return;
        }

        //Route passed in from MainActivity is fully populated
        Log.d(TAG, "creating schedule from network: " + mRoute.name);
        getScheduleTimes();
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
        Log.d(TAG, "schedule call! " + apiCallString);

        Schedule.StopTimes tmpTimes = null;
        int hrs, mins, dirID = 0;
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
                                        tmp = parser.getValueAsString();
                                        //stopId
                                        if(dirID == 0) {
                                            //direction id is 0 or 1
                                            tmpTimes = mOutbound.get(tmp);
                                            if(tmpTimes == null) {
                                                tmpTimes = new Schedule.StopTimes();
                                                tmpTimes.stopId = tmp;
                                                mOutbound.put(tmpTimes.stopId, tmpTimes);
                                                //Log.d(TAG, tmp + " new stop times " + tmp);
                                            }
                                        } else {
                                            tmpTimes = mInbound.get(tmp);
                                            if(tmpTimes == null) {
                                                tmpTimes = new Schedule.StopTimes();
                                                tmpTimes.stopId = tmp;
                                                mInbound.put(tmpTimes.stopId, tmpTimes);
                                                //Log.d(TAG, tmp + " new stop times " + tmp);
                                            }
                                        }
                                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DTIME.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        //Log.d(TAG, tmpTimes.stopId + " time added to stopTime " +
                                        /**
                                         * AM Rush Hour: 6:30 AM - 9:00 AM
                                         Midday: 9:00 AM - 3:30 PM
                                         PM Rush Hour: 3:30 PM - 6:30 PM
                                         */
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
        sch.sLoading[timing] = true;
        for(boolean b: sch.sLoading) {
            if(!b) {
                Log.i(TAG, "still working, calendar day " + scheduleType);
                return false;
            }
        }

        sch.TripsInbound = new Schedule.StopTimes[mInbound.size()];
        mInbound.values().toArray(sch.TripsInbound);
        //sort each stop times array

        sch.TripsOutbound = new Schedule.StopTimes[mOutbound.size()];
        mOutbound.values().toArray(sch.TripsOutbound);

        Log.d(TAG, "bools set, sizes: " + mInbound.size() + ":" + mOutbound.size());
        Log.d(TAG, "array sizes: " + sch.TripsInbound.length + ":" + sch.TripsOutbound.length);
        mInbound.clear();
        mOutbound.clear();
        sendBroadcast(false);
        Log.i(TAG, "broadcasting to main");
        //message activity that schedule is ready
        return true;
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

    void getScheduleTimes() {
        setDay(scheduleType);

        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        int tstamp = Long.valueOf(c.getTimeInMillis()/1000).intValue();
        //time set to collect 24hr schedule for tomorrow

        String url = ShowScheduleService.GETSCHEDULE + mRoute.id +
                ShowScheduleService.DATETIMEPARAM + tstamp +
                ShowScheduleService.TIME_PARAM + "389";
        //390 minutes, 6.5 hours, takes us through morning
        (new ScheduleCall(url, Schedule.MORNING, scheduleType)).start();
        //executorService.submit(new ScheduleCall(new Schedule(r), url, 0, scheduleType));
        Log.d(TAG, "calndr check: " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE));

        c.set(Calendar.HOUR, 6);
        c.set(Calendar.MINUTE, 30);
        c.set(Calendar.AM_PM, Calendar.AM);
        tstamp = Long.valueOf(c.getTimeInMillis()/1000).intValue();
        url = ShowScheduleService.GETSCHEDULE + mRoute.id +
                ShowScheduleService.DATETIMEPARAM + tstamp +
                ShowScheduleService.TIME_PARAM + "149";
        (new ScheduleCall(url, Schedule.AMPEAK, scheduleType)).start();
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
        (new ScheduleCall(url, Schedule.MIDDAY, scheduleType)).start();
        Log.d(TAG, "calndr check: " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE));

        c.set(Calendar.HOUR, 3);
        c.set(Calendar.MINUTE, 30);
        c.set(Calendar.AM_PM, Calendar.PM);
        tstamp = Long.valueOf(c.getTimeInMillis()/1000).intValue();
        url = ShowScheduleService.GETSCHEDULE + mRoute.id +
                ShowScheduleService.DATETIMEPARAM + tstamp +
                ShowScheduleService.TIME_PARAM + "179";
        (new ScheduleCall(url, Schedule.PMPEAK, scheduleType)).start();
        //evening peak, rush hour done
        Log.d(TAG, "calndr check: " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE));

        c.set(Calendar.HOUR, 6);
        c.set(Calendar.MINUTE, 30);
        c.set(Calendar.AM_PM, Calendar.PM);
        tstamp = Long.valueOf(c.getTimeInMillis()/1000).intValue();
        url = ShowScheduleService.GETSCHEDULE + mRoute.id +
                ShowScheduleService.DATETIMEPARAM + tstamp +
                ShowScheduleService.TIME_PARAM + "330";
        (new ScheduleCall(url, Schedule.NIGHT, scheduleType)).start();
        //finish off the scheduleType
        Log.d(TAG, "calndr check: " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE));
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
                ShowScheduleService.this.sendBroadcast(false);
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

