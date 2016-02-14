package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.objects.Alert;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * Created by emezias on 1/11/16.
 * This class runs the Faroo query sent in by the NewzList class
 * It returns the data to the list activity
 *
 * It also persists the list date when the app ends, or there is a pause
 * This will allow the app to show data at launch without delay, 99.99% of the time
 *
 * Intent extras determine the action of the service
 * News will return the saved data, NewsListActivityTag will save the list that's passed in
 */
public class GetMBTARequestService extends IntentService {

    public static final String TAG = "GetMBTARequestService";
    //reusable for displaying the date
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM-dd");

    // final private static ArrayList<FarooItem> parseList = new ArrayList<>();
    final private static JsonFactory factory = new JsonFactory();
    //private static JsonParser parser;
    private static final GregorianCalendar cal = new GregorianCalendar();
    private static final BitmapFactory.Options opts = new BitmapFactory.Options();
    //Base URL
    public static final String BASE = "http://realtime.mbta.com/developer/api/v2/";
    public static final String SUFFIX = "?api_key=3G91jIONLkuTMXbnbF7Leg&format=json";
    public static final String ROUTES = BASE + "routes" + SUFFIX;
    public static final String STOPS = BASE + "stopsbyroute" + SUFFIX + "&route=";
    public static final String alertsParams ="&include_access_alerts=true&include_service_alerts=true";
    public static final String ALERTS = BASE + "alerts" + SUFFIX + alertsParams;

    // Query Types
    /*    Routes
    routes list of all routes for which data can be requested
    routesbystop a list of routes that serve a particular stop
    Stops
    stopsbyroute a list of stops for a particular route
    stopsbylocation a list of the stops nearest a particular location
    Schedule
    schedulebystop scheduled arrivals and departures at a particular stop
    schedulebyroute scheduled arrivals and departures for a particular route
    schedulebytrip scheduled arrivals and departures for a particular trip
    Predictions and Vehicle Locations
    predictionsbystop arrival/departure predictions, plus vehicle locations and alert headers, for a stop
    predictionsbyroute arrival/departure predictions, plus vehicle locations and alert headers, for a route
    predictionsbytrip arrival/departure predictions, plus vehicle location, for a trip
    vehiclesbyroute vehicle locations for a route */
    //JSON keys
    public static final String OPENP = "\n(";
    public static final String CLOSEP = ")\n";

    //required constructor
    public GetMBTARequestService() {
        super(TAG);
    }

    /**
     * The extra on the intent tells the service what to do
     *
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
            //LocalBroadcastManager.getInstance(this).sendBroadcast(tnt);


        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            final Intent tnt = new Intent(TAG);
            //empty array triggers error in activity
            LocalBroadcastManager.getInstance(this).sendBroadcast(tnt);
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
                        mode_name = parser.getValueAsString();
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
                                Log.d(TAG, "inserting row " + cv.get(DBHelper.KEY_ROUTE_NAME) + ": " + db.insert(DBHelper.DB_ROUTE_TABLE, "", cv));
                                final Intent tnt = new Intent(this, GetMBTARequestService.class);
                                tnt.putExtra(TAG, cv.getAsString(DBHelper.KEY_ROUTE_ID));
                                startService(tnt);
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
        String table = DBHelper.DB_OUT_TABLE;
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
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DIR_NM.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        Log.d(TAG, "direction name check: " + parser.getValueAsString());
                        if (parser.getValueAsString().equals("Outbound")) {
                            table = DBHelper.DB_OUT_TABLE;
                        } else {
                            table = DBHelper.DB_INB_TABLE;
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

    void parseAlertsCall(JsonParser parser) throws IOException {
        final SQLiteDatabase db = new DBHelper(this).getWritableDatabase();
        if(DatabaseUtils.queryNumEntries(db, DBHelper.DB_ALERTS_TABLE) > 0) {
            Log.d(TAG, "db exists");
            return;
        }
        final ContentValues cv = new ContentValues();

        String mode_name = null;
        Alert alert = new Alert();
        while (!parser.isClosed()) {
            //start parsing, get the token
            JsonToken token = parser.nextToken();
            if (token == null)
                break;
            if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ALERTS.equals(parser.getCurrentName())) {
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
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ALERT_ID.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.alert_id = parser.getValueAsInt();
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_EFFECT_NAME.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.effect_name = parser.getValueAsString();
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_EFFECT.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.effect = parser.getValueAsString();
                    }else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_CAUSE.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.cause = parser.getValueAsString();
                    }else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_CAUSE_NAME.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.cause_name = parser.getValueAsString();
                    }else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_HEADER_TEXT.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.header_text= parser.getValueAsString();
                    }else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DESCRIPTION_TEXT.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.description_text= parser.getValueAsString();
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_EFFECT_PERIODS.equals(parser.getCurrentName())) {
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
                                Log.d(TAG, "inserting row " + cv.get(DBHelper.KEY_ROUTE_NAME) + ": " + db.insert(DBHelper.DB_ROUTE_TABLE, "", cv));
                                final Intent tnt = new Intent(this, GetMBTARequestService.class);
                                tnt.putExtra(TAG, cv.getAsString(DBHelper.KEY_ROUTE_ID));
                                startService(tnt);
                                cv.clear();
                            }
                        }//array of alerts is parsed
                    }//end while
                }
            }
        }
    } //end parseAlerts()

}//end class


    /**
     * This method will send a url to Faroo and parse the JSON response
     * Parser is initialized before calling this method
     * @return the list that is parsed
     * @throws IOException
     *
    Route parseRoutesCall() throws IOException {
        Route.RouteData route;
        String mode;
        // continue parsing the token till the end of input is reached
        while (!parser.isClosed()) {
            //start parsing, get the token
            JsonToken token = parser.nextToken();
            if (token == null)
                break;
            if (JsonToken.FIELD_NAME.equals(token) && MODE.equals(parser.getCurrentName())) {
                // The first token should be the start of an array of 10 articles
                token = parser.nextToken();
                if (!JsonToken.START_ARRAY.equals(token)) {
                    // bail out, news items are in an array, show error to user
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
                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ROUTE_MODE.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        mode = parser.getText();
                        //Log.d(TAG, "Title: " + article.title);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper..equals(parser.getCurrentName())) {
                        //parsing related articles array
                        Log.d(TAG, "related articles");
                        token = parser.nextToken();
                        if (JsonToken.START_ARRAY.equals(token)) {
                            // read array of related articles, arbitrary number is 3, up to 20 possible
                            route = new Route.RouteData();
                            route.mode_name = mode;
                            while (!JsonToken.END_ARRAY.equals(token)) {
                                token = parser.nextToken();
                                if (JsonToken.START_OBJECT.equals(token)) {
                                    //store the next strings in parallel arrays
                                    token = parser.nextToken();
                                    if (JsonToken.FIELD_NAME.equals(token) && FarooItem.TITLE.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        if(JsonToken.VALUE_NULL.equals(token)) {
                                            article.relatedNewsTitle[dex] = getString(com.mentalmachines.openmbta.openmbtav2.R.string.related_src);
                                        } else {
                                            article.relatedNewsTitle[dex] = parser.getText();
                                        }
                                    }
                                    token = parser.nextToken();
                                    if (JsonToken.FIELD_NAME.equals(token) && FarooItem.URL.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        article.relatedNewsURL[dex] = parser.getText();
                                        //Log.d(TAG, "Item URL: " + article.relatedNewsURL[dex]);
                                    }
                                    token = parser.nextToken();
                                    if (JsonToken.FIELD_NAME.equals(token) && FarooItem.DOMAIN.equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        article.relatedNewsTitle[dex] = article.relatedNewsTitle[dex] + " " +
                                                OPENP + parser.getText() + CLOSEP;
                                        //Log.d(TAG, "read in related story # " + dex + " " + article.relatedNewsTitle[dex]);
                                        dex++;
                                        //could save source separately, no need for the extra field
                                    } //keep parsing these object until the end of the object then go to the next object
                                } //end the object read
                            }//end the related articles array
                        } else {
                            Log.w(TAG, "no array of related articles");
                        }
                        parseList.add(article);
                        article = new FarooItem();
                    }//end related tag
                }

            }

        } //parser now closed!

        FarooItem[] returnArray = new FarooItem[parseList.size()];
        returnArray = parseList.toArray(returnArray);
        parseList.clear();
        return returnArray;
        //clean up the parseList ArrayList collection and use a simpler, faster array structure
    } */

