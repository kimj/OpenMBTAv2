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
import com.mentalmachines.ttime.TTimeApp;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.objects.StopList;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by emezias on 1/11/16.
 * This class runs the API requests
 */
public class StopService extends IntentService {

    public static final String TAG = "StopService";

    //JSON constants for predictive times, data is not in SQLite
    public static final String STOPPARAM = "&stop=";
    public static final String SCHEDHOUR = "&max_time=120";
    public static final String STOPVERB = "schedulebystop";
    public static final String STPREDVERB = "predictionsbystop";
    public static final String GETPREDICTIMES = ScheduleService.BASE + STPREDVERB + ScheduleService.SUFFIX + STOPPARAM;
    public static final String GETSTOPTIMES = ScheduleService.BASE + STOPVERB + ScheduleService.SUFFIX + SCHEDHOUR + STOPPARAM;
    //http://realtime.mbta.com/developer/api/v2/schedulebystop?api_key=wX9NwuHnZU2ToO7GmGR9uw&stop=6538&format=json
    StopData mainStop;
    ArrayList<StopData> returnList;
    final StringBuilder strBuild = new StringBuilder(0);
    final Time t = new Time();
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
        nearby.add(new StopData(mainStop));

        final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        Cursor stopNameCursor = db.query(true, DBHelper.STOPS_INB_TABLE, Route.mStopProjection,
                null, null, null, null, null, null);
        returnList = Route.makeStops(stopNameCursor);
        //Log.d(TAG, "inbound total stops" + returnList.size());
        float[] results = new float[1];
        final double mainLong = Double.valueOf(mainStop.stopLong);
        final double mainLat = Double.valueOf(mainStop.stopLat);
        for(StopData stop: returnList) {
            Location.distanceBetween(mainLong, mainLat,
                    Double.valueOf(stop.stopLong), Double.valueOf(stop.stopLat), results);
            //find other stops within 25m of the mainStop
            if(results[0] < 25.0f && !stop.stopId.equals(mainStop.stopId) && !nearby.contains(stop)) {
                nearby.add(stop);
            }
        }
        returnList.clear();
        Log.d(TAG, "inbound stops" + nearby.size());
        stopNameCursor = db.query(true, DBHelper.STOPS_OUT_TABLE, Route.mStopProjection,
                null, null, null, null, null, null);
        returnList = Route.makeStops(stopNameCursor);
        for(StopData stop: returnList) {
            Location.distanceBetween(mainLong, mainLat,
                    Double.valueOf(stop.stopLong), Double.valueOf(stop.stopLat), results);
            //find other stops within 25m of the mainStop
            if(results[0] < 25.0f && !stop.stopId.equals(mainStop.stopId) && !nearby.contains(stop)) {
                nearby.add(stop);
            }
        }
        Log.d(TAG, "total nearby stops" + nearby.size());
        //now have list of stop ids within 25 meters of the stop passed in
        //call the server for each id and parse the schedule/predictions
        if(!stopNameCursor.isClosed()) {
            stopNameCursor.close();
        }
        returnList.clear();
        //cleanup completed
        Collections.sort(nearby, new Comparator<StopData>() {
            @Override
            public int compare(final StopData object1, final StopData object2) {
                return object1.stopId.compareTo(object2.stopId);
            }
        });
        String id = "";
        for(StopData stp: nearby) {
            if(stp.stopId.equals(id)) {
                returnList.add(stp);
                Log.d(TAG, "stp added? " + id);
            }
            id = stp.stopId;
        }

        if(!returnList.isEmpty()) {
            Log.d(TAG, "total ids that match" + returnList.size());
            //need to remove duplicates in reverse order to preserve the saved index value
            for(StopData stp: returnList) {
                Log.d(TAG, "removing stop data " + nearby.remove(stp));
            }
            //would be better to figure out how the dup is getting through
            returnList.clear();
            nearby.trimToSize();
            Log.d(TAG, "new total nearby stops" + nearby.size());
        }
        try {
            Log.d(TAG, "stops to parse? " + nearby.size());
            for(StopData stop: nearby) {
                returnList.add(stop);
                parseStop(stop);
            }
            nearby.clear();
            Log.d(TAG, "predictions? " + returnList.size());
            for(StopData stop: returnList) {
                parseStopPredictions(stop);
            }
            //This part wraps things up and sends a message back to the activity with data for the stop detail
            setXtrasAndBroadcast();

        } catch (IOException e) {
            Log.e(TAG, "problem with stop service " + e.getMessage());
            e.printStackTrace();
            returnList.clear();
            setXtrasAndBroadcast();
        }
    }

    void parseStopPredictions(StopData stop) throws IOException {
        //add schedule times and direction to the stops
        String routeName = "", directionNm = "", tmp;
        boolean found = false;
        final JsonParser parser = new JsonFactory().createParser(new URL(GETPREDICTIMES + stop.stopId));
        Log.d(TAG, "predictions call? " + GETPREDICTIMES + stop.stopId);

        int dex, offsetSecs = 0;
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
                                        //Log.d(TAG, "handle idDir here? " + Route.readableName(this, routeName) + " " + directionNm);
                                        while (!JsonToken.END_ARRAY.equals(token)) {
                                            //running through the trips, read the trip array into times
                                            token = parser.nextToken();
                                            if(JsonToken.FIELD_NAME.equals(token) && DBHelper.PRED_TIME.equals(parser.getCurrentName())) {
                                                token = parser.nextToken();
                                                tmp = parser.getValueAsString();
                                                Log.d(TAG, "predTime?");
                                                if(stop == null || tmp.isEmpty() || tmp == null) {
                                                    Log.w(TAG, "skipping prediction time field");
                                                } else {
                                                    t.set(1000 * Long.valueOf(tmp));
                                                    t.normalize(false);
                                                    getTime(t);
                                                    //predicted time is now set into the string builder
                                                    Log.d(TAG, "stringbuilder time" + strBuild.toString());
                                                }
                                                //This time will go into the stop field below with the pre away key to put min/sec with the time
                                            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_PREAWAY.equals(parser.getCurrentName())) {
                                                token = parser.nextToken();
                                                tmp = parser.getValueAsString();
                                                Log.d(TAG, "pred away");
                                                if (stop == null || tmp.isEmpty() || tmp == null) {
                                                    Log.w(TAG, "skipping seconds prediction field");
                                                    //this is not possible... pred time always has the away key
                                                } else {
                                                    offsetSecs = Integer.valueOf(tmp);
                                                    strBuild.append(" (");
                                                    if (offsetSecs > 60) {
                                                        strBuild.append(offsetSecs / 60).append("m ").append(offsetSecs % 60).append("s").append(")");
                                                    } else {
                                                        strBuild.append(offsetSecs).append("s").append(")");
                                                    }
                                                    Log.d(TAG, "stringbuilder predicTimes" + strBuild.toString());
                                                }

                                            } else if(JsonToken.END_OBJECT.equals(token)) {
                                                //end of the trip
                                                tmp = Route.readableName(this, routeName) + " " + directionNm;
                                                if(!stop.schedTimes.contains(tmp)) {
                                                    for(StopData s: returnList) {
                                                        if(s.schedTimes.contains(tmp)) {
                                                            stop = s;
                                                            Log.d(TAG, "new stop? " + stop.stopName);
                                                            found = true;
                                                            break;
                                                        }
                                                    }
                                                } else {
                                                    found = true;
                                                }
                                                if(found) {
                                                    if (!stop.predicTimes.isEmpty()) {
                                                        if(strBuild.length() > 0 && !stop.predicTimes.contains(strBuild.toString())) {
                                                            stop.predicTimes = stop.predicTimes + "\n" + strBuild.toString();
                                                        }

                                                    } else {
                                                        stop.predicTimes = getString(R.string.actual) + strBuild.toString();
                                                    }
                                                    Log.d(TAG, stop.stopId + " set stop predicTimes" + stop.predicTimes);
                                                    found = false;
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
        Log.i(TAG, "parser closed, prediction parsing complete");
    }

    void parseStop(StopData stop) throws IOException {
        //add schedule times and direction to the stops
        String routeName = "", directionNm = "", tmp;
        final JsonParser parser = new JsonFactory().createParser(new URL(GETSTOPTIMES + stop.stopId));
        Log.d(TAG, "stops call? " + GETSTOPTIMES + stop.stopId);
        StopData newStop = null;
        int dex;
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
                                if(!routeName.isEmpty()) {
                                    newStop = new StopData(stop);
                                    stop = newStop;
                                }
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
                                        if(directionNm.isEmpty()) {
                                            directionNm = parser.getValueAsString();
                                            //this assignment should fix a glitch with the first stop to parse
                                            stop.schedTimes = "";
                                        } else {
                                            newStop = new StopData(stop);
                                            stop = newStop;
                                            directionNm = parser.getValueAsString();
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
                                            if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_SCH_TIME.equals(parser.getCurrentName())) {
                                                token = parser.nextToken();
                                                t.set(1000 * Long.valueOf(parser.getValueAsString()));
                                                t.normalize(false);
                                                getTime(t);
                                                //put the scheduled time into the string builder
                                            } else if(JsonToken.END_OBJECT.equals(token)) {
                                                //end of the trip
                                                tmp = Route.readableName(this, routeName) + " " + directionNm;
                                                if(newStop != null) {
                                                    dex = 0;
                                                    for(StopData d: returnList) {
                                                        if(d.schedTimes.contains(tmp)) {
                                                            Log.w(TAG, "setting stop");
                                                            break;
                                                        }
                                                        dex++;
                                                    }
                                                    if(dex != returnList.size()) {
                                                        //index of the stop data to replace
                                                        stop = returnList.get(dex);
                                                    } else {
                                                        returnList.add(stop);
                                                    }
                                                }
                                                if(!stop.schedTimes.isEmpty()) {
                                                    //add a new time into the String
                                                    stop.schedTimes = stop.schedTimes + ", " + strBuild.toString();
                                                } else {
                                                    //start the schedule string and add in a new stopdata, as needed
                                                    Log.d(TAG, "new routename: " + tmp);
                                                    strBuild.insert(0, tmp + " " + getString(R.string.scheduled) + "\n");
                                                    stop.schedTimes = strBuild.toString();
                                                }

                                                newStop = null;
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

    void setXtrasAndBroadcast() {
        final ArrayList<StopData> deleteThese = new ArrayList<>();
        final Intent returnResults = new Intent(TAG);
        if(!returnList.isEmpty()) {
            //This cleanup of empty stops is klugey, to discuss
            for(StopData s: returnList) {
                if(s.schedTimes.isEmpty() && s.predicTimes.isEmpty()) {
                    //north station had empties, could be schedule time of day
                    deleteThese.add(s);
                }
            }
            if(!deleteThese.isEmpty()) {
                for(StopData s: deleteThese) {
                    returnList.remove(s);
                }
                Log.d(TAG, "stops to delete? " + deleteThese.size());
                deleteThese.clear();
            }
            Collections.sort(returnList, new Comparator<StopData>() {
                @Override
                public int compare(final StopData object1, final StopData object2) {
                    return object1.schedTimes.compareTo(object2.schedTimes);
                }
            });
            //final StopList details = new StopList(mainStop, returnList);
            returnResults.putExtra(TAG, new StopList(mainStop, returnList));
            Log.d(TAG, "stops to return? " + returnList.size());
        }
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

