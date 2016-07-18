package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.TTimeApp;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.objects.StopList;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by emezias on 1/11/16.
 * Notice the Stop param defined in this service
 * This class runs the API requests to find stops near a location
 * Then it gets the schedule and predicted times for those stops
 */
public class StopService extends IntentService {

    public static final String TAG = "StopService";

    //JSON constants for predictive times, data is not in SQLite
    public static final String STOPPARAM = "&stop=";
    public static final String SCHEDHOUR = "&max_time=120";
    public static final String STOPVERB = "schedulebystop";
    public static final String STPREDVERB = "predictionsbystop";
    public static final String GETPREDICTIMES = GetTimesForRoute.BASE + STPREDVERB + GetTimesForRoute.SUFFIX + STOPPARAM;
    public static final String GETSTOPTIMES = GetTimesForRoute.BASE + STOPVERB + GetTimesForRoute.SUFFIX + SCHEDHOUR + STOPPARAM;
    //http://realtime.mbta.com/developer/api/v2/schedulebystop?api_key=wX9NwuHnZU2ToO7GmGR9uw&stop=6538&format=json
    StopData mainStop;

    final static StringBuilder strBuild = new StringBuilder(0);
    final static Calendar t = Calendar.getInstance();
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
        if(b == null || !b.containsKey(TAG)) {
            Log.w(TAG, "stop service missing extras");
            return;
        }
        mainStop = b.getParcelable(TAG);
        final ArrayList<StopData> returnList = createStopList(mainStop);

        try {
            Log.d(TAG, "predictions? " + returnList.size());
            //for(StopData stop: returnList) {
            StopData stop;
            for(int dex = 0; dex < returnList.size(); dex++) {
                stop = returnList.get(dex);
                parseStop(stop, returnList);
                parseStopPredictions(this, stop, returnList);
            }
            //This part wraps things up and sends a message back to the activity with data for the stop detail
            setXtrasAndBroadcast(returnList);

        } catch (IOException e) {
            Log.e(TAG, "problem with stop service " + e.getMessage());
            e.printStackTrace();
            returnList.clear();
            setXtrasAndBroadcast(returnList);
        }
    }

    static void runStopLoop(ArrayList<StopData> stopList, HashMap<String, StopData> nearby,
                            String stopCode, Double mainLong, Double mainLat) {
        float[] results = new float[1];
        for(StopData stop: stopList) {
            if(!stop.stopId.equals(stopCode)) {
                Location.distanceBetween(mainLat, mainLong,
                        Double.valueOf(stop.stopLat), Double.valueOf(stop.stopLong), results);
                //find other stops within ~100ft of the mainStop
                if(results[0] < 33f) {
                    nearby.put(stop.stopId, stop);
                }

            } else {
                Log.d(TAG, "same stop: " + stop.stopId);
            }
        }
    }

    public static ArrayList<StopData> createStopList(Location findNearHere) {
        final HashMap<String, StopData> nearby = new HashMap<>();

        final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        Cursor stopNameCursor = db.query(true, DBHelper.STOPS_INB_TABLE, Route.mStopProjection,
                null, null, null, null, null, null);
        ArrayList<StopData> stopList = Route.makeStops(stopNameCursor);

        //Log.d(TAG, "inbound total stops " + stopList.size());
        runStopLoop(stopList, nearby, "", findNearHere.getLongitude(), findNearHere.getLatitude());
        stopList.clear();
        //Log.d(TAG, "inbound stops? " + nearby.size());
        stopNameCursor = db.query(true, DBHelper.STOPS_OUT_TABLE, Route.mStopProjection,
                null, null, null, null, null, null);
        stopList = Route.makeStops(stopNameCursor);
        //Log.d(TAG, "outbound total stops " + stopList.size());
        runStopLoop(stopList, nearby, "", findNearHere.getLongitude(), findNearHere.getLatitude());

        //Log.d(TAG, "total nearby stops" + nearby.size());
        //now have list of stop ids within 25 meters of the stop passed in
        //call the server for each id and parse the schedule/predictions
        if(!stopNameCursor.isClosed()) {
            stopNameCursor.close();
        }
        stopList.clear();
        stopList.addAll(nearby.values());
        //Log.d(TAG, "total stops, hashmap values " + stopList.size());

        Collections.sort(stopList, new Comparator<StopData>() {
            @Override
            public int compare(final StopData object1, final StopData object2) {
                return object1.stopName.compareTo(object2.stopName);
            }
        });
        //Log.d(TAG, "sorted and ready to return, total stops " + stopList.size());

        return stopList;
    }

    public static ArrayList<StopData> createStopList(StopData mainStop) {
        final HashMap<String, StopData> nearby = new HashMap<>();
        nearby.put(mainStop.stopId, mainStop);

        final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        Cursor stopNameCursor = db.query(true, DBHelper.STOPS_INB_TABLE, Route.mStopProjection,
                null, null, null, null, null, null);
        ArrayList<StopData> stopList = Route.makeStops(stopNameCursor);

        //Log.d(TAG, "inbound total stops " + stopList.size());
        final Double mainLat = Double.valueOf(mainStop.stopLat);
        final Double mainLong = Double.valueOf(mainStop.stopLong);
        runStopLoop(stopList, nearby, mainStop.stopId, mainLong, mainLat);
        stopList.clear();
        //Log.d(TAG, "inbound stops" + nearby.size());
        stopNameCursor = db.query(true, DBHelper.STOPS_OUT_TABLE, Route.mStopProjection,
                null, null, null, null, null, null);
        stopList = Route.makeStops(stopNameCursor);
        runStopLoop(stopList, nearby, mainStop.stopId, mainLong, mainLat);

        //Log.d(TAG, "total nearby stops" + nearby.size());
        //now have list of stop ids within 25 meters of the stop passed in
        //call the server for each id and parse the schedule/predictions
        if(!stopNameCursor.isClosed()) {
            stopNameCursor.close();
        }
        stopList.clear();
        stopList.addAll(nearby.values());
        //cleanup completed
        stopList.remove(mainStop);
        Collections.sort(stopList, new Comparator<StopData>() {
            @Override
            public int compare(final StopData object1, final StopData object2) {
                return object1.stopName.compareTo(object2.stopName);
            }
        });
        stopList.add(0, mainStop);
        return stopList;
    }

    public static void parseStopPredictions(Context ctx, StopData stop, ArrayList<StopData> returnList) throws IOException {
        //add schedule times and direction to the stops
        String routeName = "", directionNm = "", tmp;
        boolean found = false;
        final JsonParser parser = new JsonFactory().createParser(new URL(GETPREDICTIMES + stop.stopId));
        //Log.d(TAG, "predictions call? " + GETPREDICTIMES + stop.stopId);
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
                //returnList.remove(stop);
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
                                routeName = parser.getValueAsString();
                                Log.d(TAG, "stop's route " + routeName);
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
                                    if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DIR_NM.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        directionNm = parser.getValueAsString();
                                        /*if(directionNm.isEmpty()) {
                                            directionNm = parser.getValueAsString();
                                            //this assignment should fix a glitch with the first stop to parse
                                            //stop.predicTimes = "";
                                        } else {
                                            directionNm = parser.getValueAsString();
                                        }*/
                                        Log.d(TAG, "direction id set " + directionNm);
                                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_TRIP.equals(parser.getCurrentName())) {
                                        //array of trips with stops inside, may be several trips returned
                                        token = parser.nextToken();
                                        if (!JsonToken.START_ARRAY.equals(token)) {
                                            break;
                                        }
                                        while (!JsonToken.END_ARRAY.equals(token)) {
                                            //running through the trips, read the trip array into times
                                            token = parser.nextToken();
                                            if(JsonToken.FIELD_NAME.equals(token) && DBHelper.PRED_TIME.equals(parser.getCurrentName())) {
                                                token = parser.nextToken();
                                                tmp = parser.getValueAsString();
                                                //Log.d(TAG, "predTime?");
                                                if(stop == null || tmp.isEmpty() || tmp == null) {
                                                    Log.w(TAG, "skipping prediction time field");
                                                } else {
                                                    stop.scheduleTimes.add(1000 * Long.valueOf(tmp));
                                                    GetTimesForRoute.getTime(tmp, t, strBuild);
                                                }
                                                //This time will go into the stop field below with the pre away key to put min/sec with the time
                                            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_PREAWAY.equals(parser.getCurrentName())) {
                                                token = parser.nextToken();
                                                tmp = parser.getValueAsString();
                                                if (stop == null || tmp.isEmpty() || tmp == null) {
                                                    Log.w(TAG, "skipping seconds prediction field");
                                                    //this is not possible... pred time always has the away key
                                                } else {
                                                    stop.predictionSecs.add(Integer.valueOf(tmp));
                                                    GetTimesForRoute.addAwayTimes(tmp, strBuild);
                                                    Log.d(TAG, "stringbuilder predicTimes" + strBuild.toString());
                                                }

                                            } else if(JsonToken.END_OBJECT.equals(token)) {
                                                //end of the trip, put the timing data  into the stop
                                                tmp = Route.readableName(ctx, routeName) + " " + directionNm;
                                                if(stop.predicTimes.isEmpty()) {
                                                    strBuild.insert(0, ctx.getString(R.string.actual) + " ");
                                                    stop.predicTimes = strBuild.toString();
                                                } else if(stop.schedTimes.contains(tmp)) {
                                                    //add another time to the current stop
                                                    if(strBuild.length() > 0 && !stop.predicTimes.contains(strBuild.toString())) {
                                                        stop.predicTimes = stop.predicTimes + "\n" + strBuild.toString();
                                                    }
                                                } else {
                                                    //find the right stop data
                                                    for(StopData s: returnList) {
                                                        if(s.schedTimes.contains(tmp)) {
                                                            stop = s;
                                                            Log.d(TAG, "new stop? " + stop.stopName);
                                                            found = true;
                                                            break;
                                                        }
                                                    }
                                                    if(found) {
                                                        if (stop.predicTimes.isEmpty()) {
                                                            strBuild.insert(0, ctx.getString(R.string.actual) + " ");
                                                            stop.predicTimes = strBuild.toString();
                                                        } else if(strBuild.length() > 0 && !stop.predicTimes.contains(strBuild.toString())) {
                                                            stop.predicTimes = stop.predicTimes + "\n" + strBuild.toString();
                                                        }
                                                        found = false;
                                                    } //end stop found, could be duplicate data
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
        Log.i(TAG, "parser closed, prediction parsing complete");
    }

    void parseStop(StopData stop, ArrayList<StopData> listdata) throws IOException {
        //add schedule times and direction to the stops
        String routeName = "", directionNm = "", tmp;
        final JsonParser parser = new JsonFactory().createParser(new URL(GETSTOPTIMES + stop.stopId));
        Log.d(TAG, "stops call? " + GETSTOPTIMES + stop.stopId);
        StopData newStop = null;

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
                //returnList.remove(stop);
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
                                routeName = parser.getValueAsString();
                                Log.d(TAG, "stop's route " + routeName);
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
                                    if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DIR_NM.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        directionNm = parser.getValueAsString();
                                        if(directionNm.isEmpty()) {
                                            //this assignment fixes a glitch with the first stop to parse
                                            stop.schedTimes = "";
                                        }
                                        //Log.d(TAG, "direction id set " + directionNm);
                                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_TRIP.equals(parser.getCurrentName())) {
                                        //array of trips with stops inside, may be several trips returned
                                        token = parser.nextToken();
                                        if (!JsonToken.START_ARRAY.equals(token)) {
                                            break;
                                        }
                                        //Log.d(TAG, "handle idDir here? " + Route.readableName(this, routeName) + " " + directionNm);
                                        while (!JsonToken.END_ARRAY.equals(token)) {
                                            //running through the trips, read the trip array into times
                                            token = parser.nextToken();
                                            if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_SCH_TIME.equals(parser.getCurrentName())) {
                                                token = parser.nextToken();
                                                stop.scheduleTimes.add(1000 * Long.valueOf(parser.getValueAsString()));
                                                GetTimesForRoute.getTime(parser.getValueAsString(), t, strBuild);
                                                //put the scheduled time into the string builder
                                            } else if(JsonToken.END_OBJECT.equals(token)) {
                                                //end of the trip
                                                tmp = Route.readableName(this, routeName) + " " + directionNm;
                                                if(stop.schedTimes.isEmpty()) {
                                                    //Log.d(TAG, "new routename: " + tmp);
                                                    strBuild.insert(0, tmp + " " + getString(R.string.scheduled));
                                                    stop.schedTimes = strBuild.toString();
                                                } else if(stop.schedTimes.contains(tmp)) {
                                                    //add a new time into the same route/stop
                                                    stop.schedTimes = stop.schedTimes + ", " + strBuild.toString();
                                                } else {
                                                    //new stop
                                                    for(StopData sd: listdata) {
                                                        if(sd.schedTimes.contains(tmp)) {
                                                            //Log.w(TAG, "found stop");
                                                            newStop = sd;
                                                            break;
                                                        }
                                                    } //end for
                                                    if(newStop == null) {
                                                        newStop = new StopData(stop);
                                                        //Log.d(TAG, "new routename: " + tmp);
                                                        strBuild.insert(0, tmp + " " + getString(R.string.scheduled));
                                                        newStop.schedTimes = strBuild.toString();
                                                        listdata.add(newStop);
                                                    } else {
                                                        newStop.schedTimes = newStop.schedTimes + ", " + strBuild.toString();
                                                    }
                                                    stop = newStop;
                                                    newStop = null;
                                                    //Log.d(TAG, "new stop: " + stop.schedTimes);
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
        //TODO select alerts and set ids into StopData, as needed
    }

    void setXtrasAndBroadcast(ArrayList<StopData> returnList) {

        final Intent returnResults = new Intent(TAG);
        if(returnList.isEmpty()) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(returnResults);
            return;
        }
        returnResults.putExtra(TAG, new StopList(mainStop, returnList));
        Log.d(TAG, "stops to return? " + returnList.size());

        LocalBroadcastManager.getInstance(this).sendBroadcast(returnResults);
    }

}//end class

