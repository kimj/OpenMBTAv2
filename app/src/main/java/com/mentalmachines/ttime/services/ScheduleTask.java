package com.mentalmachines.ttime.services;

import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.Schedule;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by emezias on 3/22/16.
 */
public class ScheduleTask extends AsyncTask<Object, Void, Schedule> {
    public static final String TAG = "ScheduleTask";

    final Calendar c = Calendar.getInstance();
    int mPeriod, mScheduleType; //this is the calendar day for the schedule that is running
    String url;

    public static volatile Schedule sWeekdays, sSaturday, sSunday;
    final static HashMap<String, Schedule.StopTimes> mInbound = new HashMap<>();
    final static HashMap<String, Schedule.StopTimes> mOutbound = new HashMap<>();

    public ScheduleTask(Route r, int scheduleType) {
        sSaturday = new Schedule(r);
        sSunday = new Schedule(r);
        sWeekdays = new Schedule(r);

        c.setTimeInMillis(System.currentTimeMillis());
        setDay(scheduleType);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        url = FullScheduleService.GETSCHEDULE + sWeekdays.route.id +
                FullScheduleService.DATETIMEPARAM + Long.valueOf(c.getTimeInMillis()/1000).intValue() +
                FullScheduleService.TIME_PARAM + "389";
        mScheduleType = scheduleType;
        mPeriod = 0;
    }

    public ScheduleTask(int period, int scheduleType) {
        mScheduleType = scheduleType;
        mPeriod = period;
        setDay(scheduleType);
        switch (period) {
            case 1:
                c.set(Calendar.HOUR, 6);
                c.set(Calendar.MINUTE, 30);
                c.set(Calendar.AM_PM, Calendar.AM);
                url = FullScheduleService.GETSCHEDULE + sWeekdays.route.id +
                        FullScheduleService.DATETIMEPARAM + Long.valueOf(c.getTimeInMillis()/1000).intValue() +
                        FullScheduleService.TIME_PARAM + "149";
                break;
            case 2:
                c.set(Calendar.HOUR, 9);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.AM_PM, Calendar.AM);
                url = FullScheduleService.GETSCHEDULE + sWeekdays.route.id +
                        FullScheduleService.DATETIMEPARAM + Long.valueOf(c.getTimeInMillis()/1000).intValue() +
                        FullScheduleService.TIME_PARAM + "389";
                break;
            case 3:
                c.set(Calendar.HOUR, 3);
                c.set(Calendar.MINUTE, 30);
                c.set(Calendar.AM_PM, Calendar.PM);
                url = FullScheduleService.GETSCHEDULE + sWeekdays.route.id +
                        FullScheduleService.DATETIMEPARAM + Long.valueOf(c.getTimeInMillis()/1000).intValue() +
                        FullScheduleService.TIME_PARAM + "179";
                break;
            case 4:
                c.set(Calendar.HOUR, 6);
                c.set(Calendar.MINUTE, 30);
                c.set(Calendar.AM_PM, Calendar.PM);
                url = FullScheduleService.GETSCHEDULE + sWeekdays.route.id +
                        FullScheduleService.DATETIMEPARAM + Long.valueOf(c.getTimeInMillis()/1000).intValue() +
                        FullScheduleService.TIME_PARAM + "330";
                break;
        }
    }

    @Override
    protected Schedule doInBackground(Object... params) {
        //all three Schedule objects have the same route
        //url String is ready to parse
        Log.d(TAG, "calndr check: " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE));
        final JsonParser parser;
        try {
            parser = new JsonFactory().createParser(new URL(url));
            final StringBuilder strBuild = new StringBuilder(0);
            Log.d(TAG, "schedule call? " + url);
            int hours = 23, minutes = 0;
            switch(mScheduleType) {
                //using Android Time, hours are offset by 1
                case MORNING:
                    hours = 5;
                    minutes = 30;
                    break;
                case AMPEAK:
                    hours = 8;
                    minutes = 0;
                    break;
                case MIDDAY:
                    hours = 14;
                    minutes = 30;
                    break;
                case PMPEAK:
                    hours = 17;
                    minutes = 30;
                    break;
                case NIGHT:
                    hours = 23;
                    minutes = 0;
                    break;
            }
            Schedule.StopTimes tmpTimes = null;
            int dirID = 0;
            String tmp;
            final Time t = new Time();
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
                                            //drop any times out of the period
                                            // the server returns complete trips
                                            // trips that start in this interval may run past the last minute of the period and show twice
                                            if(t.hour <= hours && t.minute < minutes) {
                                                switch (mPeriod) {
                                                    case MORNING:
                                                        tmpTimes.morning.add(tmp);
                                                        break;
                                                    case AMPEAK:
                                                        tmpTimes.amPeak.add(tmp);
                                                        break;
                                                    case MIDDAY:
                                                        tmpTimes.midday.add(tmp);
                                                        break;
                                                    case PMPEAK:
                                                        tmpTimes.pmPeak.add(tmp);
                                                        break;
                                                    case NIGHT:
                                                        tmpTimes.night.add(tmp);
                                                        break;
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
            final Schedule sch = getSchedule();
            sch.sLoading[mPeriod] = true;
            for(boolean b: sch.sLoading) {
                if(!b) {
                    return null;
                }
            }
            //sch.TripsInbound = new Schedule.StopTimes[mInbound.size()];
            mInbound.values().toArray(sch.TripsInbound);
            //sch.TripsOutbound = new Schedule.StopTimes[mOutbound.size()];
            mOutbound.values().toArray(sch.TripsOutbound);
            Log.d(TAG, "sch array sizes: " + sch.TripsInbound.length + ":" + sch.TripsOutbound.length);
            return sch;
            //message activity that schedule is ready

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    Schedule getSchedule() {
        switch (mScheduleType) {
            case Calendar.TUESDAY:
                return sWeekdays;
            case Calendar.SUNDAY:
                return sSunday;
            case Calendar.SATURDAY:
                return sSaturday;
        }
        return null;
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
    /**
     * AM Rush Hour: 6:30 AM - 9:00 AM
     Midday: 9:00 AM - 3:30 PM
     PM Rush Hour: 3:30 PM - 6:30 PM
     */

    public static void resetTask() {
        sWeekdays = null;
        sSaturday = null;
        sSunday = null;
        mInbound.clear();
        mOutbound.clear();
        Log.i(TAG, "service reset");
    }

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

    public static final int MORNING = 0;
    public static final int AMPEAK = 1;
    public static final int MIDDAY = 2;
    public static final int PMPEAK = 3;
    public static final int NIGHT = 4;
}
