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
import com.mentalmachines.ttime.objects.StopData;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by emezias on 1/11/16.
 * This class runs the API requests
 */
public class ScheduleService extends IntentService {

    public static final String TAG = "ScheduleService";
    Route searchRoute;

    //Base URL
    public static final String BASE = "http://realtime.mbta.com/developer/api/v2/";
    public static final String SUFFIX = "?api_key=3G91jIONLkuTMXbnbF7Leg&format=json";

    //JSON constants for predictive times, data is not in SQLite
    public static final String ROUTEPARAM = "&route=";
    public static final String PREDVERB = "predictionsbyroute";
    public static final String SCHEDVERB = "schedulebyroute";
    public static final String MAXHR_PARAM = "&max_time=1440&max_trips=100";
    public static final String GETROUTETIMES = BASE + PREDVERB + SUFFIX + ROUTEPARAM;
    public static final String GETROUTESCHED = BASE + SCHEDVERB + SUFFIX + MAXHR_PARAM + ROUTEPARAM;
    //http://realtime.mbta.com/developer/api/v2/predictionsbystop?api_key=3G91jIONLkuTMXbnbF7Leg&format=json&stop=70077&include_service_alerts=false

    final StringBuilder strBuild = new StringBuilder(0);

    //required, empty constructor, builds intents
    public ScheduleService() {
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
        if(b == null || !b.containsKey(DBHelper.KEY_ROUTE_ID)) {
            Log.e(TAG, "bad route id");
            endService(true);
            return;
        }
        searchRoute = new Route();
        searchRoute.id = b.getString(DBHelper.KEY_ROUTE_ID);

        final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        if(b.containsKey(DBHelper.KEY_ROUTE_NAME)) {
            searchRoute.name = b.getString(DBHelper.KEY_ROUTE_NAME);
        } else {
            searchRoute.name = DBHelper.getRouteName(db, searchRoute.id);
        }
        searchRoute.setStops(db);
        //Route data is fully populated

        try {
            if(b.containsKey(TAG)) {
                //call for the schedule
                Log.i(TAG, "start schedule service for " + searchRoute.name);
                parseDaySchedule();
            } else {
                Log.i(TAG, "start prediction service for " + searchRoute.name);
                getSchedule();
                getPredictions();
            }

        } catch (IOException e) {
            Log.e(TAG, "Exception in Schedule Service " + e.getMessage());
            e.printStackTrace();
            endService(true);
        }

    }

    void getPredictions( ) throws IOException {
        //add times to the stops in mInbound and searchRoute.mOutboundStops
        final Time t = new Time();
        String value = "";
        StopData stop = null;
        int dex = -1;
        final JsonParser parser = new JsonFactory().createParser(new URL(GETROUTETIMES + searchRoute.id));
        while (!parser.isClosed()) {
            //start parsing, get the token
            JsonToken token = parser.nextToken();
            if (token == null) {
                break;
            }
            //running through while to get to direction array, "error" key comes up first
            if (JsonToken.FIELD_NAME.equals(token) && "error".equals(parser.getCurrentName())) {
                //This route is done for the night or the server is hosed -> something is wrong
                parser.close();
            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DIR.equals(parser.getCurrentName())) {
                //no names at the top of this return, straight into objects
                //Log.d(TAG, "direction");
                token = parser.nextToken();
                if(!JsonToken.START_ARRAY.equals(token)) {
                    break;
                }
                int offsetSecs;
                int directionId = 0;

                while(true) {
                    token = parser.nextToken();
                    if (token == null) { //TODO check for error
                        break;
                    }
                    //most of the data in this array is ignored, just the direction and the stops in the trip array
                    //Log.d(TAG, "direction array");
                    if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DIR_ID.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        directionId = Integer.valueOf(parser.getValueAsString());
                        //Log.d(TAG, "direction id set " + directionId);
                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_TRIP.equals(parser.getCurrentName())) {
                        //array of trips with stops inside, may be several trips returned
                        token = parser.nextToken();
                        if(!JsonToken.START_ARRAY.equals(token)) {
                            break;
                        }
                        //trip array has trips, vehicles and stops
                        //Log.d(TAG, "trip array");
                        while(!JsonToken.END_ARRAY.equals(token)) {
                            //getting stops embedded into the trip
                            token = parser.nextToken();
                            if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_VEHICLE.equals(parser.getCurrentName())) {
                                //may want vehicle_timestamp...
                                parser.skipChildren();
                            } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.STOP.equals(parser.getCurrentName())) {
                                //finally at stop array, skip past start array
                                token = parser.nextToken();
                                //Log.d(TAG, "stop");
                                while(!JsonToken.END_ARRAY.equals(token)) {
                                    //running through the stops
                                    token = parser.nextToken();
                                    if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_STOPID.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        value = parser.getValueAsString();
                                        if(value == null || value.equals("N/A") || value.isEmpty()){
                                            Log.w(TAG, "empty stop array, skipping");
                                            //skip this object
                                            stop = null;
                                        } else {
                                            if(directionId == 0) {
                                                for(dex = 0; dex < searchRoute.mOutboundStops.size(); dex++) {
                                                //for(StopData stopData: searchRoute.mOutboundStops) {
                                                    if(searchRoute.mOutboundStops.get(dex).stopId.equals(value)) {
                                                        stop = searchRoute.mOutboundStops.get(dex);
                                                        break;
                                                    }
                                                }
                                            } else {
                                                for(dex = 0; dex < searchRoute.mInboundStops.size(); dex++) {
                                                    if(searchRoute.mInboundStops.get(dex).stopId.equals(value)) {
                                                        stop = searchRoute.mInboundStops.get(dex);
                                                        break;
                                                    }
                                                }
                                            }
                                            //Log.i(TAG, "stop set here" + stop.stopId + ":" + stop.stopName);
                                        }
                                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.PRED_TIME.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        value = parser.getValueAsString();
                                        if(stop == null || value.isEmpty() || value == null) {
                                            Log.w(TAG, "skipping prediction time field");
                                        } else {
                                            getTime(value, t);
                                            //predicted time is now set into the string builder
                                        }
                                        //This time will go into the stop field below with the pre away key to put min/sec with the time
                                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_PREAWAY.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        value = parser.getValueAsString();
                                        if(stop == null || value.isEmpty() || value == null) {
                                            Log.w(TAG, "skipping seconds prediction field");
                                            //this is not possible... pred time always has the away key
                                        } else {
                                            offsetSecs = Integer.valueOf(value);
                                            strBuild.append(" (");
                                            if(offsetSecs > 60) {
                                                strBuild.append(offsetSecs/60).append("m ").append(offsetSecs % 60).append("s").append(")");
                                            } else {
                                                strBuild.append(offsetSecs).append("s").append(")");
                                            }
                                            if(!stop.predicTimes.isEmpty()) {
                                                stop.predicTimes = stop.predicTimes + "\n" + strBuild.toString();
                                            } else {
                                                stop.predicTimes = strBuild.toString();
                                            }
                                            //Log.d(TAG, stop.stopId + " stop predicTimes" + stop.predicTimes);
                                            strBuild.setLength(0);
                                            //risky? proper order?
                                        }
                                    }

                                } //end while stops
                                //here we need to change the token from end array in order to continue parsing
                                token = JsonToken.NOT_AVAILABLE;
                            } //end stop field
                        } //end trip array
                        //here again we need to change the token from end array in order to continue parsing
                        token = JsonToken.NOT_AVAILABLE;
                    } //end if field is trip
                } //end direction array
            } //end direction if condition, need to read both directions
        } //parser is closed
        Log.i(TAG, "parser closed, times complete");
        //This part wraps things up and sends a message back to the activity
        endService(false);
    }

    void endService(boolean hasError) {
        final Intent returnResults = new Intent(TAG);
        if(hasError) {
            //the parser threw an exception - show the user an error
            LocalBroadcastManager.getInstance(this).sendBroadcast(returnResults);
            return;
        }
        ArrayList<StopData> clearStops = new ArrayList<>();
        for(StopData s: searchRoute.mOutboundStops) {
            if(s.schedTimes.isEmpty() && s.predicTimes.isEmpty()) {
                clearStops.add(s);
            }
        }
        if(!clearStops.isEmpty()) {
            for(StopData emptyStop: clearStops) {
                searchRoute.mOutboundStops.remove(emptyStop);
            }
        }
        clearStops.clear();
        for(StopData s: searchRoute.mInboundStops) {
            if(s.schedTimes.isEmpty() && s.predicTimes.isEmpty()) {
                clearStops.add(s);
            }
        }
        if(!clearStops.isEmpty()) {
            for(StopData emptyStop: clearStops) {
                searchRoute.mInboundStops.remove(emptyStop);
            }
        }
        clearStops.clear();
        //attach results and return them to the activity
        returnResults.putExtra(TAG, searchRoute);
        LocalBroadcastManager.getInstance(this).sendBroadcast(returnResults);
    }

    void getSchedule() throws IOException {
        //add schedule times to the empty stops on the route
        final Time t = new Time();
        t.setToNow();
        StopData stop = null;
        int dirID = Long.valueOf(t.toMillis(false)/1000).intValue();
        String tmp;
        final JsonParser parser = new JsonFactory().createParser(new URL(
                BASE + SCHEDVERB + SUFFIX + ROUTEPARAM + searchRoute.id
                        + "&max_time=120&datetime=" + dirID));
        Log.d(TAG, "schedule call? " + BASE + SCHEDVERB + SUFFIX + ROUTEPARAM + searchRoute.id  + "&datetime=" + dirID);
        dirID = 0;
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
                                    if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_STOPID.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        tmp = parser.getValueAsString();
                                        //stopId
                                        if (dirID == 0) {
                                            //direction id is 0, Outbound
                                            for(StopData s: searchRoute.mOutboundStops) {
                                                if(s.stopId.equals(tmp)) {
                                                    stop = s;
                                                    break;
                                                }
                                            }

                                        } else {
                                            for(StopData s: searchRoute.mInboundStops) {
                                                if(s.stopId.equals(tmp)) {
                                                    stop = s;
                                                    break;
                                                }
                                            }
                                        }
                                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DTIME.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        tmp = parser.getValueAsString();
                                        tmp = getTime(tmp, t);

                                        if(t.toMillis(false) > System.currentTimeMillis()) {
                                            //stale data for a stop comes through
                                            //don't show passed times...
                                            if(stop.schedTimes.isEmpty()) {
                                                stop.schedTimes = tmp;
                                            } else {
                                                stop.schedTimes = stop.schedTimes + ", " + tmp;
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
        //TODO select alerts and set ids into StopData, as needed

    }

    /**
     * First effort is using MAXHR_PARAM to get the full day's schedule
     * The plan is to build a table and display the full schedule
     * datetime (optional) Epoch time after which schedule should be returned
     If included then must be within the next seven (7) days
     * @throws IOException
     */
    void parseDaySchedule() throws IOException {
        //add schedule times and direction to the stops
        final Time t = new Time();
        t.setToNow();
        t.hour = 0;
        t.minute = 0;
        t.second = 0;
        t.yearDay = t.yearDay+1;
        t.normalize(false);
        int tstamp = Long.valueOf(t.toMillis(false)/1000).intValue();
        //time set to collect 24hr schedule for tomorrow

        ArrayList<String> inStops, outStops;
        inStops = new ArrayList<>(searchRoute.mInboundStops.size());
        for(StopData s: searchRoute.mInboundStops) {
            inStops.add(s.stopId);
        }
        outStops = new ArrayList<>(searchRoute.mOutboundStops.size());
        for(StopData s: searchRoute.mOutboundStops) {
            outStops.add(s.stopId);
        }
        //these arraylists will serve as the index to find the right stoptimes object
        Schedule sch = new Schedule(searchRoute);
        Schedule.StopTimes tmpTimes = null;
        String tripName = "";
        int dirID = 0, dex;
        String directionNm = "", tmp;
        final JsonParser parser = new JsonFactory().createParser(new URL(GETROUTESCHED + searchRoute.id +
            "&datetime=" + tstamp));
        Log.d(TAG, "schedule call? " + GETROUTESCHED + searchRoute.id + "&datetime=" + tstamp);

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
                        Log.d(TAG, "direction id set " + directionNm);
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
                                Log.d(TAG, "trip name set " + tripName);
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
                                    if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_STOPID.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        tmp = parser.getValueAsString();
                                        //stopId
                                        if(dirID == 0) {
                                            //direction id is 0 or 1
                                            dex = outStops.indexOf(tmp);
                                            if(dex < 0) {
                                                Log.e(TAG, "error with stops array");
                                                break;
                                            }
                                            if(sch.TripsOutbound.size() > dex) {
                                                tmpTimes = sch.TripsOutbound.get(dex);
                                            } else {
                                                tmpTimes = new Schedule.StopTimes();
                                                tmpTimes.stopId = tmp;
                                                sch.TripsOutbound.add(tmpTimes);
                                                Log.d(TAG, tmp + " new stop times " + tripName);
                                            }
                                        } else {
                                            dex = inStops.indexOf(tmp);
                                            if(dex < 0) {
                                                Log.e(TAG, "error with stops array");
                                                break;
                                            }
                                            if(sch.TripsInbound.size() > dex) {
                                                tmpTimes = sch.TripsInbound.get(dex);
                                            } else {
                                                tmpTimes = new Schedule.StopTimes();
                                                tmpTimes.stopId = tmp;
                                                sch.TripsInbound.add(tmpTimes);
                                                Log.d(TAG, tmp + " new stop times " + tripName);
                                            }
                                        }
                                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DTIME.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        Log.d(TAG, "time added to stopTime " + getTime(parser.getValueAsString(), t));
                                        tmpTimes.hours.add(t.hour);
                                        tmpTimes.minutes.add(t.minute);
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
        //TODO select alerts and set ids into StopData, as needed
    }

    //utility methods to format times when making a call for prediction data
    public String getTime (String stamp, Time t) {
        t.set(1000 * Long.valueOf(stamp));
        t.normalize(false);
        strBuild.append(hourHandle(t.hour)).append(":").append(pad(t.minute));
        if(t.hour >= 12) {
            strBuild.append("PM");
        } else {
            strBuild.append("AM");
        }
        return strBuild.toString();
    }

    public static String pad(int c) {
        if (c >= 10) return String.valueOf(c);
        else return ("0" + String.valueOf(c));
    }

    public static String hourHandle(int c) {
        if (c > 12) return String.valueOf(c-12);
        else if (c == 0) return String.valueOf(12);
        return String.valueOf(c);
    }

}//end class

