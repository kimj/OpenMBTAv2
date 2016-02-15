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
import com.mentalmachines.ttime.objects.Alert;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * Created by emezias on 1/11/16.
 * This class runs the API requests
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


    //required constructor
    public GetMBTARequestService() {
        super(TAG);
    }

    /**
     * The extra on the intent tells the service what to do, which call to make
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        //make the network call here in the background
        final Bundle b = intent.getExtras();
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

