package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.objects.StopList;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by emezias on 1/11/16.
 * This class runs the API requests
 */
public class StopService extends IntentService {

    public static final String TAG = "StopService";
    StopData mainStop;

    //JSON constants for predictive times, data is not in SQLite
    public static final String STOPPARAM = "&stop=";
    public static final String STOPVERB = "schedulebystop";
    public static final String GETSTOPTIMES = ScheduleService.BASE + STOPVERB + ScheduleService.SUFFIX + STOPPARAM;
    //http://realtime.mbta.com/developer/api/v2/schedulebystop?api_key=wX9NwuHnZU2ToO7GmGR9uw&stop=6538&format=json

    public static final String TRIP_KEY = "trip";
    final StringBuilder strBuild = new StringBuilder(0);

    //required, empty constructor, builds intents
    public StopService() {
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
        mainStop = b.getParcelable(TAG);
        final ArrayList<StopData> nearby = new ArrayList<>();
        final SQLiteDatabase db = DBHelper.getHelper(this).getReadableDatabase();
        Cursor stopNameCursor = db.query(true, DBHelper.STOPS_INB_TABLE, Route.mStopProjection,
                null, null, null, null, "_id ASC", null);
        ArrayList<StopData> tmp = Route.makeStops(stopNameCursor);
        float[] results = new float[1];
        final double mainLong = Double.valueOf(mainStop.stopLong);
        final double mainLat = Double.valueOf(mainStop.stopLat);
        for(StopData stop: tmp) {
            Location.distanceBetween(mainLong, mainLat,
                    Double.valueOf(stop.stopLong), Double.valueOf(stop.stopLat), results);
            //find other stops within 25m of the mainStop
            if(results[0] < 25.0f && !nearby.contains(stop)) {
                nearby.add(stop);
            }
        }
        stopNameCursor = db.query(true, DBHelper.STOPS_OUT_TABLE, Route.mStopProjection,
                null, null, null, null, "_id ASC", null);
        tmp = Route.makeStops(stopNameCursor);
        for(StopData stop: tmp) {
            Location.distanceBetween(mainLong, mainLat,
                    Double.valueOf(stop.stopLong), Double.valueOf(stop.stopLat), results);
            //find other stops within 25m of the mainStop
            if(results[0] < 25.0f && !nearby.contains(stop)) {
                nearby.add(stop);
            }
        }
        //now have list of stop ids within 25 meters of the stop passed in
        //call the server for each id and parse the schedule/predictions
        if(!stopNameCursor.isClosed()) {
            stopNameCursor.close();
        }
        DBHelper.close(db);
        tmp.clear();
        //cleanup completed
        try {
            Log.d(TAG, "stops to parse? " + nearby.size());
            for(StopData stop: nearby) {
                parseStop(stop);
                if(stop.schedTimes.isEmpty()) {
                    stop.schedTimes = getString(R.string.noSched);
                }
            }

            //This part wraps things up and sends a message back to the activity with data for the stop detail
            endService(nearby);

        } catch (IOException e) {
            Log.e(TAG, "problem with stop service " + e.getMessage());
            e.printStackTrace();
            endService(null);
        }
    }

    void parseStop(StopData stop) throws IOException {
        //add schedule times and direction to the stops
        final Time t = new Time();
        String routeName = "", directionNm = "";
        final JsonParser parser = new JsonFactory().createParser(new URL(GETSTOPTIMES + stop.stopId));
        Log.d(TAG, "stops call? " + GETSTOPTIMES + stop.stopId);

        while (!parser.isClosed()) {
            //start parsing, get the token
            JsonToken token = parser.nextToken();
            if (token == null) {
                Log.e(TAG, "null token");
                break;
            }
            //running through tokens to get to direction array, "error" key comes up before any other
            if (JsonToken.FIELD_NAME.equals(token) && "error".equals(parser.getCurrentName())) {
                //This route is done for the night or the server is hosed -> something is wrong
                parser.close();
                Log.w(TAG, "error reading " + stop.stopName);
                token = null;
            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ROUTE_MODE.equals(parser.getCurrentName())) {
                token = parser.nextToken();
                if (!JsonToken.START_ARRAY.equals(token)) {
                    Log.e(TAG, "mode parsing error after stop id and name");
                    break;
                }

                while (!JsonToken.END_ARRAY.equals(token)) {
                    //the array can be empty if the stop is closed
                    token = parser.nextToken();
                    if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ROUTE.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        if (!JsonToken.START_ARRAY.equals(token)) {
                            Log.e(TAG, "mode parsing error after stop id and name");
                            break;
                        }

                        while (!JsonToken.END_ARRAY.equals(token)) {
                            token = parser.nextToken();
                            if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ROUTE_NAME.equals(parser.getCurrentName())) {
                                //Some stops might have multiple routes
                                token = parser.nextToken();
                                routeName = Route.readableName(this, parser.getValueAsString());
                                //Log.d(TAG, "stop's route " + routeName);
                            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DIR.equals(parser.getCurrentName())) {
                                //no names at the top of this return, straight into objects
                                //direction, begin array, begin object
                                Log.d(TAG, "direction array");
                                token = parser.nextToken();
                                if (!JsonToken.START_ARRAY.equals(token)) {
                                    break;
                                }
                                //direction array, then read the trips array
                                while (!JsonToken.END_ARRAY.equals(token)) {
                                    token = parser.nextToken();

                                    if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DIR_NM.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        directionNm = parser.getValueAsString();
                                        Log.d(TAG, "direction id set " + directionNm);

                                    } else if (JsonToken.FIELD_NAME.equals(token) && TRIP_KEY.equals(parser.getCurrentName())) {
                                        //array of trips with stops inside, may be several trips returned
                                        token = parser.nextToken();
                                        if (!JsonToken.START_ARRAY.equals(token)) {
                                            break;
                                        }
                                        //trip array has trips, vehicles and stops
                                        //Log.d(TAG, "trip array");
                                        while (!JsonToken.END_ARRAY.equals(token)) {
                                            //running through the trips, read the trip array into times
                                            token = parser.nextToken();

                                            if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_SCH_TIME.equals(parser.getCurrentName())) {
                                                token = parser.nextToken();
                                                t.set(1000 * Long.valueOf(parser.getValueAsString()));
                                                t.normalize(false);
                                                getTime(t);
                                                //put the scheduled time into the string builder
                                            } else if (JsonToken.END_OBJECT.equals(token)) {
                                                //end of the trip, add in String
                                                if (!stop.schedTimes.isEmpty()) {
                                                    stop.schedTimes = stop.schedTimes + ", " + strBuild.toString();
                                                } else {
                                                    strBuild.insert(0, getString(R.string.scheduled));
                                                    stop.schedTimes = strBuild.toString();
                                                }
                                                if (stop.predicTimes.isEmpty()) {
                                                    stop.predicTimes = routeName + ": " + directionNm;
                                                }
                                                strBuild.setLength(0);
                                                //Log.d(TAG, "trip parsed: " + stop.predicTimes + " " + stop.schedTimes);
                                            } //end object

                                        }//trip array end
                                        token = JsonToken.NOT_AVAILABLE;
                                        //ending the trip array should not end the  parsing
                                    }//trip field

                                }
                                token = JsonToken.NOT_AVAILABLE;

                            }//end dir field

                        }//route array end
                        token = JsonToken.NOT_AVAILABLE;
                    }
                } //end mode array, end array token

            } //array following stop name inside here

        } //parser is closed
        if (stop.schedTimes.isEmpty()) {
            Log.e(TAG, "error with stop detail, no trips or times");
        }
    }

    void endService(ArrayList<StopData> nearby) {
        final StopList details = new StopList(mainStop, nearby);
        final Intent returnResults = new Intent(TAG);
        returnResults.putExtra(TAG, details);
        LocalBroadcastManager.getInstance(this).sendBroadcast(returnResults);
    }

    //utility methods to format times when making a call for prediction data
    public String getTime (Time t) {
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

