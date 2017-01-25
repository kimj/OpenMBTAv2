package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.TTimeApp;
import com.mentalmachines.ttime.objects.Favorite;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.ScheduleParser;
import com.mentalmachines.ttime.objects.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Calendar;

/**
 * Created by emezias on 1/11/16.
 * This class runs the API requests to get all 3 schedule days times, based on the route
 * The data is saved to a newly built table
 *
 * For busy routes, using the maxtrips value of 100 did not get the full day
 * Based on those results, the service will make a call for each period of the schedule day
 * and spawn threads to parse these calls in parallel
 *
 * TODO combine this class with the GetScheduleService
 */
public class SaveFavorites extends IntentService {

    public static final String TAG = "SaveFavorites";
    //boolean to help with threading/error handling when switching between the days
    volatile boolean mRedLineSpecial = false;
    Route mRoute;
    SQLiteDatabase mDB;
    Calendar c = Calendar.getInstance();

    //required, empty constructor, builds intents
    public SaveFavorites() {
        super(TAG);
    }

    public static Intent newInstance(Context ctx, Route r) {
        final Intent tnt = new Intent(ctx, SaveFavorites.class);
        tnt.putExtra(DBHelper.KEY_ROUTE_ID, r);
        return tnt;
    }

    public static Intent newInstance(Context ctx, String stopId) {
        final Intent tnt = new Intent(ctx, SaveFavorites.class);
        tnt.putExtra(DBHelper.STOP, stopId);
        return tnt;
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
            //error, service requires 2 extras
            Log.e(TAG, "missing route extra");
            return;
        }
        //is it a stop?
        if(b.containsKey(DBHelper.STOP)) {
            final String stopid = b.getString(DBHelper.STOP, null);
            final boolean result = Favorite.setFavoriteStop(
                    b.getString(DBHelper.STOP, null));
            sendSignal(result);
            return;
        }

        //this is a new route to display, call from Main
        if(b.containsKey(DBHelper.KEY_ROUTE_ID)) {
            mRoute = b.getParcelable(DBHelper.KEY_ROUTE_ID);
            Log.i(TAG, "new route from MainActivity? " + mRoute.id);
        } else if(mRoute == null) {
            Log.e(TAG, "missing Route extra!");
            return;
        }

        if(mRoute.id.contains(DBHelper.ASHMONT) || mRoute.id.contains(DBHelper.BRAINTREE)) {
            mRedLineSpecial = true;
        }

        //Route passed in from MainActivity is fully populated
        if(DBHelper.checkForScheduleTable(mRoute.id)) {
            return;
        }
        mDB = TTimeApp.sHelper.getWritableDatabase();
        getScheduleTimes(Calendar.TUESDAY);
        getScheduleTimes(Calendar.SATURDAY);
        getScheduleTimes(Calendar.SUNDAY);

        //DEBUG
        startService(new Intent(this, CopyDBService.class));
    }

    String getUrl(int hour, int minute, int duration) {
        c.set(Calendar.HOUR_OF_DAY,hour);
        c.set(Calendar.MINUTE, minute);
        //time set to collect 24hr schedule for tomorrow
        final String url;
        if(mRedLineSpecial) {
            url = GetScheduleService.GETSCHEDULE + DBHelper.REDLINE +
                    GetScheduleService.DATETIMEPARAM + Long.valueOf(c.getTimeInMillis()/1000).intValue() +
                    GetScheduleService.TIME_PARAM + duration;
        } else {
            url = GetScheduleService.GETSCHEDULE + mRoute.id +
                    GetScheduleService.DATETIMEPARAM + Long.valueOf(c.getTimeInMillis()/1000).intValue() +
                    GetScheduleService.TIME_PARAM + duration;
        }
        return url;
    }

    void getScheduleTimes(int scheduleType) {
        c = GetScheduleService.setDay(c, scheduleType);
        c.set(Calendar.SECOND, 0);

        c.set(Calendar.DAY_OF_WEEK, scheduleType-1);

        loadSchedulePeriod(getUrl(22, 0, 509), scheduleType);
        c.set(Calendar.DAY_OF_WEEK, scheduleType);
        //390 minutes, 6.5 hours, takes us through morning
        loadSchedulePeriod(getUrl(6, 30, 149), scheduleType);
        //2.5 hours through morning peak
        loadSchedulePeriod(getUrl(9, 0, 389), scheduleType);
        //6.5 hours, takes us through midday
        loadSchedulePeriod(getUrl(15, 30, 179), scheduleType);
        //evening peak, rush hour done
        loadSchedulePeriod(getUrl(18, 30, 330), scheduleType);
        //finish off the scheduleType
    }

    void loadSchedulePeriod(String apiCallString, int scheduleType) {
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
            ScheduleParser.loadScheduleIntoDB(mDB, scheduleType, stream, mRoute);
            stream.close();
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "error calling server " + e.getMessage());
            //end service here?
        }
    }

    public void sendSignal(boolean hasError) {
        final Intent returnResults = new Intent(TAG);
        returnResults.putExtra(TAG, hasError);

        LocalBroadcastManager.getInstance(this).sendBroadcast(returnResults);
    }

}//end class

