package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.ShowScheduleActivity;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.Schedule;
import com.mentalmachines.ttime.objects.ScheduleLogan;
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.objects.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
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
public class LoganScheduleSvc extends IntentService {

    public static final String TAG = "LoganScheduleSvc";
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
    public LoganScheduleSvc() {
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
            Log.i(TAG, "new route from MainActivity? " + mRoute.id);
        } else if(mRoute == null) {
            Log.e(TAG, "missing Route extra!");
            sendBroadcast(true);
            return;
        }

        if(mRoute.id.contains(DBHelper.ASHMONT) || mRoute.id.contains(DBHelper.BRAINTREE)) {
            mRedLineSpecial = true;
        }
        getScheduleTimes();
        //Route passed in from MainActivity is fully populated
        /*if(DBHelper.checkForScheduleTable(mRoute.id)) {
            //read the times from the DB instead of calling the MBTA
            Log.d(TAG, "creating schedule from database: " + mRoute.name);
            final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
            //each stop data object kicks off a thread to read from the db
            //new ScheduleQuery(db, scheduleType, mRoute.mInboundStops, true).start();
            //new ScheduleQuery(db, scheduleType, mRoute.mOutboundStops, false).start();
        } else {
            Log.d(TAG, "creating schedule from network: " + mRoute.name);
            getScheduleTimes();
        }*/

    }

    void sendBroadcast(boolean hasError) {
        Log.d(TAG, "end show schedule service");
        final Intent returnResults = new Intent(TAG);
        returnResults.putExtra(TAG, hasError);
        returnResults.putExtra(LoganScheduleActivity.TAG, scheduleType);
        if(!hasError) {
            returnResults.putExtra(DBHelper.KEY_ROUTE_ID, mRoute);
        } else {
            Log.w(TAG, "error in service, route is not set " + mRoute.name);
        }
        ScheduleLogan.clearMaps();
        Log.i(TAG, "maps cleared" + mRoute.name);
        LocalBroadcastManager.getInstance(this).sendBroadcast(returnResults);
    }

    public static void setDay(Calendar c, int dayOfWeek) {
        c = Calendar.getInstance();
        final int today = c.get(Calendar.DAY_OF_YEAR);
        c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        // The past day of week [ May include today ]
        if(c.get(Calendar.DAY_OF_YEAR) < today) {
            c.add(Calendar.DATE, 7);
        }
    }

    void startParse(int hour, int minute, int duration) {
        c.set(Calendar.HOUR_OF_DAY,hour);
        c.set(Calendar.MINUTE, minute);
        //time set to collect 24hr schedule for tomorrow
        final String url;
        if(mRedLineSpecial) {
            url = LoganScheduleSvc.GETSCHEDULE + "Red" +
                    LoganScheduleSvc.DATETIMEPARAM + Long.valueOf(c.getTimeInMillis()/1000).intValue() +
                    LoganScheduleSvc.TIME_PARAM + duration;
        } else {
            url = LoganScheduleSvc.GETSCHEDULE + mRoute.id +
                    LoganScheduleSvc.DATETIMEPARAM + Long.valueOf(c.getTimeInMillis()/1000).intValue() +
                    LoganScheduleSvc.TIME_PARAM + duration;
        }
        parseSchedulePeriod(url);
    }

    void getScheduleTimes() {
        setDay(c, scheduleType);
        c.set(Calendar.SECOND, 0);
        ScheduleLogan.makeMaps(mRoute);
        c.set(Calendar.DAY_OF_WEEK, scheduleType-1);
        startParse(22, 0, 509);
        //startParse(0, 0, 389);
        c.set(Calendar.DAY_OF_WEEK, scheduleType);
        //390 minutes, 6.5 hours, takes us through morning
        startParse(6, 30, 149);
        //2.5 hours through morning peak
        startParse(9, 0, 389);
        //6.5 hours, takes us through midday
        startParse(15, 30, 179);
        //evening peak, rush hour done
        startParse(18, 30, 330);
        //finish off the scheduleType

        sendBroadcast(false);
    }

    void parseSchedulePeriod(String apiCallString) {
        try {
            HttpURLConnection connection = Utils.getConnector(this, apiCallString);
            Log.d(TAG, "calling " + apiCallString);
            connection.connect();
            if(connection.getResponseCode() != 200) {
                Log.e(TAG, "error with network call to Wiki API! " + connection.getResponseCode());
            } else {
                Log.i(TAG, "wiki query status: " + connection.getResponseMessage());
            }
            Log.i(TAG, "route check? " + mRoute.id);
            final InputStream stream = connection.getInputStream();
            ScheduleLogan.readJsonStream(stream, scheduleType);
            stream.close();
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "error calling server " + e.getMessage());
            //end service here?
        }
    }


}//end class

