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
import com.mentalmachines.ttime.MainActivity;
import com.mentalmachines.ttime.TTimeApp;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.StopData;

import java.io.IOException;
import java.net.URL;

/**
 * Created by emezias on 1/11/16.
 * This class runs the API requests for the Red Line
 * The database holds 3 different Red line routes
 * JFK - Alewife, Braintree - Alewife, Ashmont - Alewife
 * The MBTA has only one Red line route
 */
public class CurrentTimesRedLine extends IntentService {

    public static final String TAG = "CurrentTimesRedLine";
    public static final String KEY_TRIP_NM = "trip_name";

    Route searchRoute;
    final Time t = new Time();
    final StringBuilder strBuild = new StringBuilder(0);

    //http://realtime.mbta.com/developer/api/v2/predictionsbystop?api_key=3G91jIONLkuTMXbnbF7Leg&format=json&stop=70077&include_service_alerts=false

    //required, empty constructor, builds intents
    public CurrentTimesRedLine() {
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
            Log.i(TAG, "start prediction service for " + searchRoute.name);
            getRouteSchedTimes();
            getPredictions();

        } catch (IOException e) {
            Log.e(TAG, "Exception in Schedule Service " + e.getMessage());
            e.printStackTrace();
            endService(true);
        }

    }

    void getPredictions( ) throws IOException {
        //add times to the stops in mInbound and searchRoute.mOutboundStops
        String value = "";
        StopData stop = null;
        int dex = -1;
        boolean skiptrip = false;
        final JsonParser parser = new JsonFactory().createParser(new URL(CurrentScheduleService.GETROUTEPREDTIMES + "Red"));
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
                            //inside array, getting stops embedded into the trip
                            token = parser.nextToken();
                            if(JsonToken.FIELD_NAME.equals(token) && KEY_TRIP_NM.equals(parser.getCurrentName())) {
                                token = parser.nextToken();
                                value = parser.getValueAsString();
                                if(value.contains(searchRoute.id)) {
                                    //save this trip
                                    skiptrip = false;
                                } else {
                                    skiptrip = true;
                                }
                            } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_VEHICLE.equals(parser.getCurrentName())) {
                                //may want vehicle_timestamp...
                                parser.skipChildren();
                            } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.STOP.equals(parser.getCurrentName())) {
                                if(skiptrip) {
                                    parser.skipChildren();
                                } else {
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
                                                getTime(value, t, strBuild);
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
                                                addAwayTimes(value, strBuild);
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
                                } //trip was not skipped, all stops recorded

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
        returnResults.putExtra(TAG, searchRoute);
        returnResults.putExtra(MainActivity.TAG, hasError);

        LocalBroadcastManager.getInstance(this).sendBroadcast(returnResults);
    }

    void getRouteSchedTimes() throws IOException {
        //add schedule times to the empty stops on the route
        t.setToNow();
        StopData stop = null;
        int dirID = Long.valueOf(t.toMillis(false)/1000).intValue();
        String tmp;
        boolean skiptrip = false;
        final JsonParser parser = new JsonFactory().createParser(new URL(
                CurrentScheduleService.GETROUTESCHEDTIMES + searchRoute.id + CurrentScheduleService.DATETIMEPARAM + dirID));
        Log.d(TAG, "schedule call? " + CurrentScheduleService.GETROUTESCHEDTIMES + "Red" + CurrentScheduleService.DATETIMEPARAM + dirID);
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
                            if(JsonToken.FIELD_NAME.equals(token) && KEY_TRIP_NM.equals(parser.getCurrentName())) {
                                token = parser.nextToken();
                                tmp = parser.getValueAsString();
                                if(tmp.contains(searchRoute.id)) {
                                    //save this trip
                                    skiptrip = false;
                                } else {
                                    skiptrip = true;
                                }
                            } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_VEHICLE.equals(parser.getCurrentName())) {
                                //may want vehicle_timestamp...
                                parser.skipChildren();
                            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.STOP.equals(parser.getCurrentName())) {
                                //put the times into the Schedule's stop times object...
                                token = parser.nextToken();
                                if (!JsonToken.START_ARRAY.equals(token)) {
                                    break;
                                }
                                //trip name, trip id need to get to STOPS array and add times into the hours/minutes
                                while (!JsonToken.END_ARRAY.equals(token)) {
                                    if(skiptrip) {
                                        parser.skipChildren();
                                    } else {
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
                                            tmp = getTime(tmp, t, strBuild);

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
                                    } //trip not skipped, stops saved

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
     * Calculate the prediction time offset and add it on to the string builder
     * @param value
     * @param builder
     */
    public static void addAwayTimes(String value, StringBuilder builder) {
        final int offsetSecs = Integer.valueOf(value);
        builder.append(" (");
        if(offsetSecs > 60) {
            builder.append(offsetSecs / 60).append("m ").append(offsetSecs % 60).append("s").append(")");
        } else {
            builder.append(offsetSecs).append("s").append(")");
        }
    }

    /**
     * utility methods to format times when making a call for schedule or prediction data
     * @param stamp, JSON string representing epoch time
     * @param t, Time class, scratch variable used throughout the parser
     * @param builder, StringBuilder - object updated in this method
     * @return
     */
    public static String getTime (String stamp, Time t, StringBuilder builder) {
        t.set(1000 * Long.valueOf(stamp));
        t.normalize(false);
        builder.append(hourHandle(t.hour)).append(":").append(pad(t.minute));
        if(t.hour >= 12) {
            builder.append("PM");
        } else {
            builder.append("AM");
        }
        return builder.toString();
    }

    public static String pad(int c) {
        if (c >= 10) return String.valueOf(c);
        else return ("0" + String.valueOf(c));
    }

    public static String hourHandle(int c) {
        //hour is offset by 1
        c++;
        if (c > 12) return String.valueOf(c-12);
        else if (c == 0) return String.valueOf(12);
        return String.valueOf(c);
    }

}//end class

