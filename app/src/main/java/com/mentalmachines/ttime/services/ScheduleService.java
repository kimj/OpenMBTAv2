package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.StopData;

import java.io.IOException;
import java.net.URL;

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
    public static final String alertsParams ="&include_access_alerts=false&include_service_alerts=false";

    //JSON constants for predictive times, data is not in SQLite
    public static final String STOPPARAM = "&stop=";
    public static final String ROUTEPARAM = "&route=";
    public static final String STOPVERB = "predictionsbystop";
    public static final String ROUTEVERB = "predictionsbyroute";
    public static final String GETSTOPTIMES = BASE + STOPVERB + SUFFIX + STOPPARAM;
    public static final String GETROUTETIMES = BASE + ROUTEVERB + SUFFIX + ROUTEPARAM;
    //http://realtime.mbta.com/developer/api/v2/predictionsbystop?api_key=3G91jIONLkuTMXbnbF7Leg&format=json&stop=70077&include_service_alerts=false

    //Predicted amount of time until the vehicle arrives at the stop, in seconds
    public static final String TRIP_KEY = "trip";
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
        searchRoute = new Route();
        searchRoute.name = b.getString(DBHelper.KEY_ROUTE_NAME);
        searchRoute.id = b.getString(DBHelper.KEY_ROUTE_ID);
        searchRoute.setStops(this);
        Log.i(TAG, "stops check " + searchRoute.mInboundStops.size());
        try {
            getTimesForRoute(searchRoute.id);

        } catch (IOException e) {
            Log.e(TAG, "problem with alerts call " + e.getMessage());
            e.printStackTrace();
            endService();
        }
    }

    void getTimesForRoute(String route) throws IOException {
        //add times to the stops in mInbound and searchRoute.mOutboundStops
        final Time t = new Time();
        String value = "";
        StopData stop = null;
        int dex = -1;
        final JsonParser parser = new JsonFactory().createParser(new URL(GETROUTETIMES + route));
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
                    } else if(JsonToken.FIELD_NAME.equals(token) && TRIP_KEY.equals(parser.getCurrentName())) {
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
                            if(JsonToken.FIELD_NAME.equals(token) && "vehicle".equals(parser.getCurrentName())) {
                                //may want vehicle_timestamp...
                                while(!JsonToken.END_OBJECT.equals(token)) {
                                    token = parser.nextToken();
                                }
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
                                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_SCH_TIME.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        value = parser.getValueAsString();
                                        if(stop == null || value.isEmpty() || value == null) {
                                            Log.w(TAG, "skipping schedule time field");
                                        } else {
                                            t.set(1000 * Long.valueOf(value));
                                            t.normalize(false);
                                            if(stop.schedTimes.isEmpty()) {
                                                stop.schedTimes = getTime(t);
                                            } else {
                                                stop.schedTimes = stop.schedTimes + ", " + getTime(t);
                                            }
                                            strBuild.setLength(0);
                                            Log.i(TAG, "stop schedule" + stop.schedTimes);
                                        }

                                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.PRED_TIME.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        value = parser.getValueAsString();
                                        if(stop == null || value.isEmpty() || value == null) {
                                            Log.w(TAG, "skipping prediction time field");
                                        } else {
                                            t.set(1000 * Long.valueOf(value));
                                            t.normalize(false);
                                            getTime(t);
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
                                                strBuild.append(offsetSecs/60).append("m ").append(offsetSecs % 60).append("s").append(")");;
                                            } else {
                                                strBuild.append(offsetSecs).append("s").append(")");;
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

                                    } /*else if(JsonToken.END_OBJECT.equals(token)) {
                                        //end of the stop, insert row
                                        if(stop != null) {
                                            Log.d(TAG, "stop object complete " + stop.stopId);
                                        }
                                    }*/

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
        endService();
    }

    void endService() {
        final Intent returnResults = new Intent(TAG);
        returnResults.putExtra(TAG, searchRoute);
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

