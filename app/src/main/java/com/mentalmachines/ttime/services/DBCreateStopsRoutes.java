package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.DBHelper;

import java.io.IOException;
import java.net.URL;
import java.util.GregorianCalendar;

/**
 * Created by emezias on 1/11/16.
 * This class will build the stops and routes tables
 */
public class DBCreateStopsRoutes extends IntentService {

    public static final String TAG = "DBCreateStopsRoutes";
    final private static JsonFactory factory = new JsonFactory();
    //private static JsonParser parser;
    private static final GregorianCalendar cal = new GregorianCalendar();
    private static final BitmapFactory.Options opts = new BitmapFactory.Options();
    //Base URL
    public static final String BASE = "http://realtime.mbta.com/developer/api/v2/";
    public static final String SUFFIX = "?api_key=3G91jIONLkuTMXbnbF7Leg&format=json";
    public static final String ROUTES = BASE + "routes" + SUFFIX;
    public static final String STOPS = BASE + "stopsbyroute" + SUFFIX + "&route=";

    //required constructor
    public DBCreateStopsRoutes() {
        super(TAG);
    }

    /**
     * The extra on the intent tells the service to get stops for a route
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            //make the network call here in the background
            final Bundle b = intent.getExtras();
            if(b == null) {
                final JsonParser parser = factory.createParser(new URL(ROUTES));
                parseRoutesCall(parser);
            } else if(b.containsKey(TAG)) {
                //This call is creating the stops tables
                final String route = b.getString(TAG);
                Log.i(TAG, "route search: " + route);
                parseStopsCall(route);
            }

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    void parseRoutesCall(JsonParser parser) throws IOException {
        final SQLiteDatabase db = new DBHelper(this).getWritableDatabase();
        if(DatabaseUtils.queryNumEntries(db, DBHelper.DB_ROUTE_TABLE) > 0) {
            Log.d(TAG, "db exists");
            return;
        }
        final ContentValues cv = new ContentValues();

        String mode_name = null;
        //int rtType = -1;
        while (!parser.isClosed()) {
            //start parsing, get the token
            JsonToken token = parser.nextToken();
            if (token == null)
                break;
            if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ROUTE_MODE.equals(parser.getCurrentName())) {
                //begin with an array named mode
                token = parser.nextToken();
                if (!JsonToken.START_ARRAY.equals(token)) {
                    // bail out, no array, can also show error to user
                    break;
                }
                token = parser.nextToken();
                // each element of the array is an object holding an article/news item so the next token -> start object
                if (!JsonToken.START_OBJECT.equals(token)) {
                    //maybe the end of the list of objects
                    break;
                }
                // now parse the series of JSON objects into items, add to the list with date and create new
                while (true) {
                    token = parser.nextToken();
                    if (token == null) {
                        break;
                    } /* SKIPPING the int, might be a faster search result to search for the type instead of the name
                    else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ROUTE_TYPE.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        rtType = parser.getValueAsInt();
                    } */else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ROUTE_MODE_NM.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        if(!parser.getValueAsString().equals("null") && !parser.getValueAsString().isEmpty()) {
                            mode_name = parser.getValueAsString();
                        }

                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.ROUTE.equals(parser.getCurrentName())) {
                        //this is an array of routes
                        token = parser.nextToken();
                        if (!JsonToken.START_ARRAY.equals(token)) {
                            // bail out, no routes
                            break;
                        }
                        token = parser.nextToken();
                        // each element of the array is a route to combine with the mode name and route type
                        if (!JsonToken.START_OBJECT.equals(token)) {
                            //maybe the end of the list of objects
                            break;
                        }
                        while (!JsonToken.END_ARRAY.equals(token)) {
                            token = parser.nextToken();
                            if (token == null) {
                                break;
                            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ROUTE_ID.equals(parser.getCurrentName())) {
                                /*rtData.type = rtType;
                                rtData.mode_name = mode_name;*/
                                token = parser.nextToken();
                                cv.put(DBHelper.KEY_ROUTE_ID, parser.getValueAsString());
                                //cv.put(DBHelper.KEY_ROUTE_TYPE, rtType);
                                cv.put(DBHelper.KEY_ROUTE_MODE, mode_name);
                            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ROUTE_NAME.equals(parser.getCurrentName())) {
                                token = parser.nextToken();
                                cv.put(DBHelper.KEY_ROUTE_NAME, parser.getValueAsString());
                            } else if (JsonToken.END_OBJECT.equals(token)) {
                                token = parser.nextToken();
                                //logic to load only buses and subways
                                if(cv.get(DBHelper.KEY_ROUTE_MODE).equals(DBHelper.BUS_MODE) || cv.get(DBHelper.KEY_ROUTE_MODE).equals(DBHelper.SUBWAY_MODE)) {
                                    Log.d(TAG, "inserting row " + cv.get(DBHelper.KEY_ROUTE_NAME) + ": " + db.insert(DBHelper.DB_ROUTE_TABLE, "", cv));
                                    final Intent tnt = new Intent(this, DBCreateStopsRoutes.class);
                                    tnt.putExtra(TAG, cv.getAsString(DBHelper.KEY_ROUTE_ID));
                                    startService(tnt);
                                } else {
                                    Log.i(TAG, "skipping route " + cv.getAsString(DBHelper.KEY_ROUTE_ID) +
                                            " mode is: " + cv.get(DBHelper.KEY_ROUTE_MODE));
                                }

                                cv.clear();
                            }

                        }//array of routes is parsed

                    }//end while

                }
            }
        }
    } //end routes

    void parseStopsCall(String route) throws IOException {
        JsonParser parser = factory.createParser(new URL(STOPS + route));
        final SQLiteDatabase db = new DBHelper(this).getWritableDatabase();
        //TODO error check that the route isn't already in the db
        final ContentValues cv = new ContentValues();
        cv.put(DBHelper.KEY_ROUTE_ID, route);
        String table = DBHelper.STOPS_OUT_TABLE;
        while (!parser.isClosed()) {
            //start parsing, get the token
            JsonToken token = parser.nextToken();
            if (token == null)
                break;
            if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DIR.equals(parser.getCurrentName())) {
                //begin with an array named mode
                token = parser.nextToken();
                if (!JsonToken.START_ARRAY.equals(token)) {
                    // bail out, no array, can also show error to user
                    break;
                }
                token = parser.nextToken();
                // each element of the array is an object holding an article/news item so the next token -> start object
                if (!JsonToken.START_OBJECT.equals(token)) {
                    //maybe the end of the list of objects
                    break;
                }
                // now parse the series of JSON objects into items, add to the list with date and create new
                while (true) {
                    token = parser.nextToken();
                    if (token == null) {
                        break;
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DIR_ID.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        //Log.d(TAG, "direction name check: " + parser.getValueAsString());
                        if (parser.getValueAsString().equals("1")) {
                            table = DBHelper.STOPS_OUT_TABLE;
                        } else {
                            table = DBHelper.STOPS_INB_TABLE;
                        }
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.STOP.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        //parse the json array of stops
                        if (!JsonToken.START_ARRAY.equals(token)) {
                            // bail out, no array, can also show error to user
                            break;
                        }
                        token = parser.nextToken();
                        // each element of the array is an object holding an article/news item so the next token -> start object
                        if (!JsonToken.START_OBJECT.equals(token)) {
                            //maybe the end of the list of objects
                            break;
                        }
                        while (!JsonToken.END_ARRAY.equals(token)) {
                            token = parser.nextToken();
                            if (token == null) {
                                break;
                            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_STOP_ORD.equals(parser.getCurrentName())) {
                                token = parser.nextToken();
                                cv.put(DBHelper.KEY_STOP_ORD, parser.getValueAsString());
                            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_STOPID.equals(parser.getCurrentName())) {
                                token = parser.nextToken();
                                cv.put(DBHelper.KEY_STOPID, parser.getValueAsString());
                            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_STOPNM.equals(parser.getCurrentName())) {
                                token = parser.nextToken();
                                cv.put(DBHelper.KEY_STOPNM, parser.getValueAsString());
                            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_STOPLT.equals(parser.getCurrentName())) {
                                token = parser.nextToken();
                                cv.put(DBHelper.KEY_STOPLT, parser.getValueAsString());
                            } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_STOPLN.equals(parser.getCurrentName())) {
                                token = parser.nextToken();
                                cv.put(DBHelper.KEY_STOPLN, parser.getValueAsString());
                            } else if (JsonToken.END_OBJECT.equals(token)) {
                                token = parser.nextToken();
                                Log.d(TAG, "inserting row into " + table + " " + cv.get(DBHelper.KEY_STOPNM) + ": " + db.insert(table, "", cv));
                                cv.clear();
                                cv.put(DBHelper.KEY_ROUTE_ID, route);
                            }
                        }
                    }
                }
            }
        }
    }


}//end class

