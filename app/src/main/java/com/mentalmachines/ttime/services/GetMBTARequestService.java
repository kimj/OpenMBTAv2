package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.objects.Alert;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by emezias on 1/11/16.
 * This class runs the API requests
 */
public class GetMBTARequestService extends IntentService {

    public static final String TAG = "GetMBTARequestService";
    final private static JsonFactory factory = new JsonFactory();
    //Base URL
    public static final String BASE = "http://realtime.mbta.com/developer/api/v2/";
    public static final String SUFFIX = "?api_key=3G91jIONLkuTMXbnbF7Leg&format=json";
    public static final String alertsParams ="&include_access_alerts=false&include_service_alerts=true";
    //public static final String alertsParams ="&include_access_alerts=true&include_service_alerts=true";
    public static final String ALERTS = BASE + "alerts" + SUFFIX + alertsParams;

    //JSON keys for the parser
    public static final String STOP_LIST_KEY = "affected_services";
    public static final String SERVICES_KEY = "services";
    public static final String ELEVATORS_KEY = "elevators";
    // Query TypesSTOP_LIST_KEY
    /*    Routes
    http://realtime.mbta.com/developer/api/v2/alerts?api_key=3G91jIONLkuTMXbnbF7Leg&format=json&include_access_alerts=true&include_service_alerts=true
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
        //final Bundle b = intent.getExtras();
        try {
            parseAlertsCall(new JsonFactory().createParser(new URL(ALERTS)));
        } catch (IOException e) {
            Log.e(TAG, "problem with alerts call " + e.getMessage());
            e.printStackTrace();
        }
    }

    void parseAlertsCall(JsonParser parser) throws IOException {
        final ArrayList<Alert> updateList = new ArrayList<>();
        final ArrayList<ServiceData> stopsList = new ArrayList<>();
        final SQLiteDatabase db = new DBHelper(this).getWritableDatabase();
        final ContentValues cv = new ContentValues();
        Alert alert = null;
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
                // now parse the alerts, JSON objects into the data structure and add to the db
                while (true) {
                    token = parser.nextToken();
                    if (token == null) {
                        break;
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ALERT_ID.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert = new Alert();
                        alert.alert_id = parser.getValueAsInt();
                        cv.put(DBHelper.KEY_ALERT_ID, alert.alert_id);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_EFFECT_NAME.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.effect_name = parser.getValueAsString();
                        cv.put(DBHelper.KEY_EFFECT_NAME, alert.effect_name);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_EFFECT.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.effect = parser.getValueAsString();
                        cv.put(DBHelper.KEY_EFFECT, alert.effect);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_CAUSE_NAME.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.cause_name = parser.getValueAsString();
                        cv.put(DBHelper.KEY_CAUSE_NAME, alert.cause_name);
                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_CAUSE.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.cause = parser.getValueAsString();
                        cv.put(DBHelper.KEY_CAUSE, alert.cause);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_SHORT_HEADER_TEXT.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.short_header_text= parser.getValueAsString();
                        cv.put(DBHelper.KEY_SHORT_HEADER_TEXT, alert.short_header_text);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_DESCRIPTION_TEXT.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.description_text= parser.getValueAsString();
                        cv.put(DBHelper.KEY_DESCRIPTION_TEXT, alert.description_text);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_SEVERITY.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.severity= parser.getValueAsString();
                        cv.put(DBHelper.KEY_SEVERITY, alert.severity);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_CREATED_DT.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.created_dt = parser.getValueAsString();
                        cv.put(DBHelper.KEY_CREATED_DT, alert.created_dt);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_LAST_MODIFIED_DT.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.last_modified_dt = parser.getValueAsString();
                        cv.put(DBHelper.KEY_LAST_MODIFIED_DT, alert.last_modified_dt);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_SERVICE_EFFECT_TEXT.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.service_effect_text = parser.getValueAsString();
                        cv.put(DBHelper.KEY_SERVICE_EFFECT_TEXT, alert.service_effect_text);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_TIMEFRAME_TEXT.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.timeframe_text = parser.getValueAsString();
                        cv.put(DBHelper.KEY_TIMEFRAME_TEXT, alert.timeframe_text);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ALERT_LIFECYCLE.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.alert_lifecycle = parser.getValueAsString();
                        cv.put(DBHelper.KEY_ALERT_LIFECYCLE, alert.alert_lifecycle);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_EFFECT_PERIODS.equals(parser.getCurrentName())) {
                        //this is an array of json object, effect start and end time
                        token = parser.nextToken();
                        if (JsonToken.START_ARRAY.equals(token)) {
                            token = parser.nextToken();
                            do {
                                if(JsonToken.START_OBJECT.equals(token)) {
                                    token = parser.nextToken();
                                    if(JsonToken.FIELD_NAME.equals(token)
                                            && DBHelper.KEY_EFFECT_PERIOD_START.equals(parser.getCurrentName())) {
                                        alert.effect_start = parser.getValueAsString();
                                        cv.put(DBHelper.KEY_EFFECT_PERIOD_START, alert.effect_start);
                                    } else if(JsonToken.FIELD_NAME.equals(token)
                                            && DBHelper.KEY_EFFECT_PERIOD_END.equals(parser.getCurrentName())) {
                                        alert.effect_end = parser.getValueAsString();
                                        cv.put(DBHelper.KEY_EFFECT_PERIOD_END, alert.effect_end);
                                    }
                                }
                                token = parser.nextToken();
                            } while(!JsonToken.END_ARRAY.equals(token));
                        } //end effect array

                        if(DatabaseUtils.queryNumEntries(db, DBHelper.DB_ALERTS_TABLE,
                                DBHelper.KEY_ALERT_ID + "=" + Integer.valueOf(alert.alert_id)) == 0) {
                                Log.i(TAG, "adding new alert: " + alert.alert_id +
                                    db.insert(DBHelper.DB_ALERTS_TABLE, "_id", cv));
                        } else {
                            updateList.add(alert);
                        }
                        cv.clear();
                        // now set the alert id into the stops table
                        token = parser.nextToken();
                        if (!JsonToken.START_OBJECT.equals(token)) {
                            //maybe the end of the list of objects
                            break;
                        }

                        token = parser.nextToken();
                        //affected services holds two arrays
                        if(JsonToken.FIELD_NAME.equals(token) && STOP_LIST_KEY.equals(parser.getCurrentName())) {
                            token = parser.nextToken();
                            // now parse the alerts, JSON objects into the data structure and add to the db
                            //two arrays, services and elevators
                            ServiceData affectedSvc = null;
                            if(JsonToken.START_ARRAY.equals(token) && SERVICES_KEY.equals(parser.getCurrentName())) {
                                token = parser.nextToken();
                                while (!JsonToken.END_ARRAY.equals(token)) {
                                    if (token == null) {
                                        break;
                                    }
                                    if(JsonToken.END_OBJECT.equals(token)) {
                                        stopsList.add(affectedSvc);
                                        Log.i(TAG, "adding stop to list: " + affectedSvc.svc_stop_id + " alert id:" + affectedSvc.alert_id);
                                    } else if(JsonToken.START_OBJECT.equals(token)) {
                                        affectedSvc = new ServiceData();
                                        affectedSvc.alert_id = alert.alert_id;
                                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ROUTE_ID.equals((parser.getCurrentName()))) {
                                        token = parser.nextToken();
                                        affectedSvc.svc_route_id = parser.getValueAsString();
                                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_STOPID.equals((parser.getCurrentName()))) {
                                        token = parser.nextToken();
                                        affectedSvc.svc_stop_id = parser.getValueAsString();
                                    }
                                    token = parser.nextToken();
                                }
                            } //end services array

                            //TODO handle elevator alerts, identified only with stops, no routes...
                        }
                    //array of alerts is parsed*
                    }//end while
                }
            }
        } //parser closed
        cv.clear();
        String[] selectArgs;
        for(ServiceData setStop: stopsList) {
            //putting alert id into stops table, most recent will be there if there is more than one
            selectArgs = new String[]{ setStop.svc_stop_id, setStop.svc_route_id};
            cv.put(DBHelper.KEY_ALERT_ID, setStop.alert_id);
            if(DatabaseUtils.queryNumEntries(db, DBHelper.STOPS_INB_TABLE,
                    DBHelper.KEY_STOPID + "=? AND " + DBHelper.KEY_ROUTE_ID + "=?", selectArgs) == 0) {
                //not inbound
                Log.i(TAG, "setting alertid on stop: " + alert.alert_id +
                        db.update(DBHelper.STOPS_OUT_TABLE, cv, DBHelper.KEY_STOPID + "=? AND " + DBHelper.KEY_ROUTE_ID + "=?", selectArgs));
            } else {
                Log.i(TAG, "setting alertid on stop: " + alert.alert_id +
                        db.update(DBHelper.STOPS_INB_TABLE, cv, DBHelper.KEY_STOPID + "=? AND " + DBHelper.KEY_ROUTE_ID + "=?", selectArgs));

            }
        } //end stops list, alerts entered for display in the Route Fragment
        //TODO now run through the list of alerts that is already in the table
        //keep two lists and trim the table, remove alerts that no longer return
    } //end parseAlerts()


    public class ServiceData {
        public String svc_route_id;
        public String svc_stop_id;
        public int alert_id;
    }

}//end class

