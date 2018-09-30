package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.data.DBHelper;
import com.mentalmachines.ttime.TTimeApp;
import com.mentalmachines.ttime.data.model.Alert;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by emezias on 1/11/16.
 * This class runs the API requests
 */
public class GetMBTARequestService extends IntentService {

    public static final String TAG = "GetMBTARequestService";
    //Base URL
    public static final String BASE = "http://realtime.mbta.com/developer/api/v2/";
    public static final String SUFFIX = "?api_key=3G91jIONLkuTMXbnbF7Leg&format=json";
    public static final String alertsParams ="&include_access_alerts=false&include_service_alerts=true";
    //public static final String alertsParams ="&include_access_alerts=true&include_service_alerts=true";
    public static final String ALERTS = BASE + "alerts" + SUFFIX + alertsParams;
    //http://realtime.mbta.com/developer/api/v2/alerts?api_key=3G91jIONLkuTMXbnbF7Leg&format=json&include_access_alerts=true&include_service_alerts=true

    //JSON keys for the parser
    public static final String STOP_LIST_KEY = "affected_services";
    public static final String SERVICES_KEY = "services";

    final StringBuilder strBuild = new StringBuilder(0);
    SQLiteDatabase mDB;
    Cursor c;

    //required constructor
    public GetMBTARequestService() {
        super(TAG);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(c != null && !c.isClosed()) {
            c.close();
        }
    }

    /**
     * The extra on the intent tells the service what to do, which call to make
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        //make the network call here in the background
        final Bundle b = intent.getExtras();
        try {
            if(b == null) {
                Log.d(TAG, "starting alerts svc");
                mDB = TTimeApp.sHelper.getWritableDatabase();
                Log.i("TTime", ALERTS);
                parseAlertsCall(new JsonFactory().createParser(new URL(ALERTS)));
            } else {
                Log.e(TAG, "not starting svc for route " + b.getString(TAG));
                //getPredictions(b.getString(TAG));
            }

        } catch (IOException e) {
            Log.e(TAG, "problem with alerts call " + e.getMessage());
            e.printStackTrace();
        }
    }

    void parseAlertsCall(JsonParser parser) throws IOException {
        final ArrayList<ServiceData> stopsList = new ArrayList<>();
        ArrayList<AlertHolder> alertsInTable = selectAlerts( );
        long timestamp = -1l;
        if(alertsInTable.size() > 0){
            Long.valueOf(alertsInTable.get(0).lastModified);
        }
        final ContentValues cv = new ContentValues();
        Alert alert = null;
        while (!parser.isClosed()) {
            //start parsing, get the token
            JsonToken token = parser.nextToken();
            if (token == null)
                break;
            if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ALERTS.equals(parser.getCurrentName())) {
                //begin with an array named alerts
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
                // now parse the alerts, JSON objects into the data structure and add to the mDB
                while (true) {
                    token = parser.nextToken();
                    if (token == null) {
                        break;
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ALERT_ID.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert = new Alert();
                        alert.alert_id = parser.getValueAsString();
                        cv.put(DBHelper.KEY_ALERT_ID, alert.alert_id);
                        //Log.i(TAG, "parsing a new alert? " + alert.alert_id);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_EFFECT_NAME.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.effect_name = parser.getValueAsInt();
                        cv.put(DBHelper.KEY_EFFECT_NAME, alert.effect_name);
                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_EFFECT.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        alert.effect = parser.getValueAsString();
                        cv.put(DBHelper.KEY_EFFECT, alert.effect);
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
                        //Let's skip this alert if it already exists in the dabase based on the last timestamp
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
                        //this is an array with one json object, effect start and end time
                        //begin with an array named alerts
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
                        //start arry, start object...
                        token = parser.nextToken();
                        if(JsonToken.FIELD_NAME.equals(token)
                                && DBHelper.KEY_EFFECT_PERIOD_START.equals(parser.getCurrentName())) {
                            token = parser.nextToken();
                            alert.effect_start = parser.getValueAsString();
                            cv.put(DBHelper.KEY_EFFECT_PERIOD_START, alert.effect_start);
                            //Log.i(TAG, "effect periods start: " + alert.effect_start);
                        } else if(JsonToken.FIELD_NAME.equals(token)
                                && DBHelper.KEY_EFFECT_PERIOD_END.equals(parser.getCurrentName())) {
                            token = parser.nextToken();
                            alert.effect_end = parser.getValueAsString();
                            cv.put(DBHelper.KEY_EFFECT_PERIOD_END, alert.effect_end);
                        }
                        //end effect array, now insert the alert
                        if(Long.valueOf(alert.created_dt) > timestamp) {
                                Log.i(TAG, "loading new alert: " + alert.alert_id +
                                        mDB.insert(DBHelper.DB_ALERTS_TABLE, "_id", cv));
                        } else {
                            //this alert is already in the table
                            int dex = -1;
                            for(AlertHolder a: alertsInTable) {
                                if(a.alert_id.equals(alert.alert_id)) {
                                    dex = alertsInTable.indexOf(a);
                                    break;
                                }
                            }
                            if(dex >= 0) {
                                if(!alertsInTable.get(dex).lastModified.equals(alert.last_modified_dt)) {
                                    Log.i(TAG, "updating alert: " + mDB.update(DBHelper.DB_ALERTS_TABLE, cv,
                                            DBHelper.KEY_ALERT_ID + " like " + cv.getAsString(DBHelper.KEY_ALERT_ID), null));
                                }
                                alertsInTable.remove(dex);
                                //remove any alert from the existing list that is in this return from the server
                            }

                        }
                        cv.clear();
                        //end effect periods
                        //TODO handle elevator alerts, identified only with stops, no routes...
                    } else if (JsonToken.FIELD_NAME.equals(token) && STOP_LIST_KEY.equals(parser.getCurrentName())) {
                        token = parser.nextToken();
                        if(!JsonToken.START_OBJECT.equals(token)) {
                            //maybe the end of the list of objects
                            break;
                        }
                        //Log.d(TAG, "at the service, route stop list");
                        token = parser.nextToken();
                        ServiceData affectedSvc = null;
                        if(JsonToken.FIELD_NAME.equals(token) && SERVICES_KEY.equals(parser.getCurrentName())) {
                            token = parser.nextToken();
                            //now parse the services objects, skip most fields in each object
                            while (!JsonToken.END_ARRAY.equals(token)) {
                                if (token == null) {
                                    break;
                                }
                                if(JsonToken.START_OBJECT.equals(token)) {
                                    affectedSvc = new ServiceData();
                                    affectedSvc.alert_id = alert.alert_id;
                                } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ROUTE_ID.equals((parser.getCurrentName()))) {
                                    token = parser.nextToken();
                                    affectedSvc.svc_route_id = parser.getValueAsString();
                                } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_STOPID.equals((parser.getCurrentName()))) {
                                    token = parser.nextToken();
                                    affectedSvc.svc_stop_id = parser.getValueAsString();
                                } else if(JsonToken.END_OBJECT.equals(token)) {
                                    if(Long.valueOf(alert.last_modified_dt) > timestamp) {
                                        //no need to add alerts that are already in the stops table
                                        if(affectedSvc.svc_stop_id != null) {
                                            stopsList.add(affectedSvc);
                                            //Log.d(TAG, "adding stop to list: " + affectedSvc.svc_stop_id + " alert id:" + affectedSvc.alert_id);
                                        }
                                    }

                                }
                                token = parser.nextToken();
                            }//end services array
                        } //end affected services object
                    }

                }//end while
                //Log.d(TAG, "finished parsing alerts!");
            }
        } //parser closed, now set the alerts into the stops table
        cv.clear();
        String[] selectArgs;

        for(ServiceData setStop: stopsList) {
            //putting alert id into stops table, most recent will be there if there is more than one
            if(setStop.svc_stop_id == null) break;
            //alerts can affect an entire route, TODO - handle this
            selectArgs = new String[]{ setStop.svc_stop_id, setStop.svc_route_id};
            cv.put(DBHelper.KEY_ALERT_ID, setStop.alert_id);
            if(!mDB.isOpen()) TTimeApp.sHelper.getReadableDatabase();
            c = mDB.query(DBHelper.STOPS_INB_TABLE,
                    new String[] { DBHelper.KEY_STOPID }, DBHelper.KEY_STOPID + "=? AND " + DBHelper.KEY_ROUTE_ID + "=?", selectArgs,
                    null, null, null);
            if(c.getCount() == 0) {
                //not inbound
                Log.i(TAG, "setting alertid on stop: " + alert.alert_id +
                        mDB.update(DBHelper.STOPS_OUT_TABLE, cv, DBHelper.KEY_STOPID + "=? AND " + DBHelper.KEY_ROUTE_ID + "=?", selectArgs));
            } else {
                Log.i(TAG, "setting alertid on stop: " + alert.alert_id +
                        mDB.update(DBHelper.STOPS_INB_TABLE, cv, DBHelper.KEY_STOPID + "=? AND " + DBHelper.KEY_ROUTE_ID + "=?", selectArgs));

            }
        } //end stops list, alerts entered for display in the Route Fragment
        //Now any remaining alerts in the alertsInTable ArrayList have to be deleted from the table
        if(alertsInTable.size() > 0) {
            for(AlertHolder oldAlert: alertsInTable) {
                updateStops(oldAlert.alert_id);
            }
        }
    } //end parseAlerts()

    void updateStops(String alertId){
        /**
         * This code is crashing if it is still running at launch when the user changes directions on the displayed route
         */
        Log.i(TAG, "clearing old alert from stops table " + alertId);
        c = mDB.query(DBHelper.STOPS_INB_TABLE,
                new String[] { DBHelper.KEY_STOPID }, DBHelper.KEY_ALERT_ID + " like " + alertId,
                null, null, null, null);
        final ContentValues cv = new ContentValues();
        cv.put(DBHelper.KEY_ALERT_ID, "");
        if(c.moveToFirst()) {
            mDB.update(DBHelper.STOPS_INB_TABLE, cv, DBHelper.KEY_ALERT_ID + " like " + alertId, null);
        }
        c = mDB.query(DBHelper.STOPS_OUT_TABLE,
                new String[] { DBHelper.KEY_STOPID }, DBHelper.KEY_ALERT_ID + " like " + alertId,
                null, null, null, null);
        if(c.moveToFirst()) {

            mDB.update(DBHelper.STOPS_OUT_TABLE, cv, DBHelper.KEY_ALERT_ID + " like " + alertId, null);
        }
        Log.i(TAG, "deleting from Alerts table: " + mDB.delete(DBHelper.DB_ALERTS_TABLE, DBHelper.KEY_ALERT_ID + " like " + alertId, null));
    }

    ArrayList<AlertHolder> selectAlerts() {
        final ArrayList<AlertHolder> tmp = new ArrayList<>();
        c = mDB.query(DBHelper.DB_ALERTS_TABLE,
                new String[] { DBHelper.KEY_ALERT_ID, DBHelper.KEY_LAST_MODIFIED_DT },
                null, null, null, null, DBHelper.KEY_LAST_MODIFIED_DT + " desc");
        if(c.getCount() > 0 && c.moveToFirst()) {
            AlertHolder alert;
            do {
                alert = new AlertHolder();
                alert.alert_id = c.getString(0);
                alert.lastModified = c.getString(1);
                tmp.add(alert);
            } while(c.moveToNext());
        }

        //Log.i(TAG, "Alerts in table " + tmp.size());
        return tmp;
    }

    //using this to identify which alerts are new
    public class AlertHolder {
        public String lastModified;
        public String alert_id;
    }

    public class ServiceData {
        public String svc_route_id;
        public String svc_stop_id;
        public String alert_id;
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

    /*    Routes
    routes list of all routes for which data can be requested
    routesbystop a list of routes that serve a particular stop
    StopData
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
