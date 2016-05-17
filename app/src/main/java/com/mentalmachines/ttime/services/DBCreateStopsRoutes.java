package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.TTimeApp;

import java.io.IOException;
import java.net.URL;

/**
 * Created by emezias on 1/11/16.
 * This class will build the stops and routes tables
 */
public class DBCreateStopsRoutes extends IntentService {

    public static final String TAG = "DBCreateStopsRoutes";
    final private static JsonFactory factory = new JsonFactory();
    //Base URL
    public static final String BASE = "http://realtime.mbta.com/developer/api/v2/";
    public static final String SUFFIX = "?api_key=3G91jIONLkuTMXbnbF7Leg&format=json";
    public static final String ROUTES = BASE + "routes" + SUFFIX;
    public static final String STOPS = BASE + "stopsbyroute" + SUFFIX + "&route=";
    //http://realtime.mbta.com/developer/api/v2/stopsbyroute?api_key=3G91jIONLkuTMXbnbF7Leg&format=json&route=1
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
        final SQLiteDatabase mDB = TTimeApp.sHelper.getWritableDatabase();
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
                        parser.nextToken();
                        if(!parser.getValueAsString().equals("null") && !parser.getValueAsString().isEmpty()) {
                            mode_name = parser.getValueAsString();
                        }

                    } else if (JsonToken.FIELD_NAME.equals(token) && DBHelper.KEY_ROUTE.equals(parser.getCurrentName())) {
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
                                    Log.d(TAG, "inserting row " + cv.get(DBHelper.KEY_ROUTE_NAME) + ": " + mDB.insert(DBHelper.DB_ROUTE_TABLE, "", cv));
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
        Log.i(TAG, "adjusting for 3 red line routes");
        mDB.execSQL(BRAIN_INBOUND);
        mDB.execSQL(BRAIN_OUTBOUND);
        mDB.execSQL(ASHMONT_INBOUND);
        mDB.execSQL(ASHMONT_OUTBOUND);
        mDB.execSQL(ROUTE_TABLE_UPDATE);

    } //end routes

    void parseStopsCall(String route) throws IOException {
        final SQLiteDatabase mDB = TTimeApp.sHelper.getWritableDatabase();
        JsonParser parser = factory.createParser(new URL(STOPS + route));
        //check that the route isn't already in the mDB?
        final ContentValues cv = new ContentValues();
        cv.put(DBHelper.KEY_ROUTE_ID, route);
        String table = "";
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
                            table = DBHelper.STOPS_INB_TABLE;
                        } else {
                            table = DBHelper.STOPS_OUT_TABLE;
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
                                Log.d(TAG, "inserting stop " + route + " " +
                                        cv.get(DBHelper.KEY_STOPID) + ": " + mDB.insert(table, "", cv));
                                if(route.equals("1")) {
                                    Log.d(TAG, cv.toString());
                                }
                                cv.clear();
                                cv.put(DBHelper.KEY_ROUTE_ID, route);
                            }
                        }
                    }
                }
            }
        }

    }

    /************************* Red line scripts **************************/
    //these strings create extra tables to more easily navigate the red line
    static final String BRAIN_OUTBOUND = "insert into stops_outbound " +
            "('route_id','stop_id','stop_name','stop_lat','stop_lon') " +
            "values ('Braintree','70061','Alewife','42.395428','-71.142483')," +
            "('Braintree','70063','Davis - Inbound','42.39674','-71.121815')," +
            "('Braintree','70065','Porter - Inbound','42.3884','-71.119149')," +
            "('Braintree','70067','Harvard - Inbound','42.373362','-71.118956')," +
            "('Braintree','70069','Central - Inbound','42.365486','-71.103802')," +
            "('Braintree','70071','Kendall/MIT - Inbound','42.36249079','-71.08617653')," +
            "('Braintree','70073','Charles/MGH - Inbound','42.361166','-71.070628')," +
            "('Braintree','70075','Park Street - to Ashmont/Braintree','42.35639457','-71.0624242')," +
            "('Braintree','70077','Downtown Crossing - to Ashmont/Braintree','42.355518','-71.060225')," +
            "('Braintree','70079','South Station - Outbound','42.352271','-71.055242')," +
            "('Braintree','70081','Broadway - Outbound','42.342622','-71.056967')," +
            "('Braintree','70083','Andrew - Outbound','42.330154','-71.057655')," +
            "('Braintree','70095','JFK/UMASS Braintree - Outbound','42.320685','-71.052391')," +
            "('Braintree','70097','North Quincy - Outbound','42.275275','-71.029583')," +
            "('Braintree','70099','Wollaston - Outbound','42.2665139','-71.0203369')," +
            "('Braintree','70101','Quincy Center - Outbound','42.251809','-71.005409')," +
            "('Braintree','70103','Quincy Adams - Outbound','42.233391','-71.007153')," +
            "('Braintree','70105','Braintree','42.2078543','-71.0011385');";

    static final String BRAIN_INBOUND = "insert into stops_inbound " +
            "('route_id','stop_id','stop_name','stop_lat','stop_lon') " +
            "values ('Braintree','70105','Braintree','42.2078543','-71.0011385')," +
            "('Braintree','70104','Quincy Adams - Inbound','42.233391','-71.007153')," +
            "('Braintree','70102','Quincy Center - Inbound','42.251809','-71.005409')," +
            "('Braintree','70100','Wollaston - Inbound','42.2665139','-71.0203369')," +
            "('Braintree','70098','North Quincy - Inbound','42.275275','-71.029583')," +
            "('Braintree','70096','JFK/UMASS Braintree - Inbound','42.320685','-71.052391')," +
            "('Braintree','70084','Andrew - Inbound','42.330154','-71.057655')," +
            "('Braintree','70082','Broadway - Inbound','42.342622','-71.056967')," +
            "('Braintree','70080','South Station - Inbound','42.352271','-71.055242')," +
            "('Braintree','70078','Downtown Crossing - to Alewife','42.355518','-71.060225')," +
            "('Braintree','70076','Park Street - to Alewife','42.35639457','-71.0624242')," +
            "('Braintree','70074','Charles/MGH - Outbound','42.361166','-71.070628')," +
            "('Braintree','70072','Kendall/MIT - Outbound','42.36249079','-71.08617653')," +
            "('Braintree','70070','Central - Outbound','42.365486','-71.103802')," +
            "('Braintree','70068','Harvard - Outbound','42.373362','-71.118956')," +
            "('Braintree','70066','Porter - Outbound','42.3884','-71.119149')," +
            "('Braintree','70064','Davis - Outbound','42.39674','-71.121815')," +
            "('Braintree','70061','Alewife','42.395428','-71.142483');";

    static final String ASHMONT_INBOUND = "insert into stops_inbound " +
            "('route_id','stop_id','stop_name','stop_lat','stop_lon') values " +
            "('Ashmont','70094','Ashmont - Inbound','42.284652','-71.064489')," +
            "('Ashmont','70092','Shawmut - Inbound','42.29312583','-71.06573796')," +
            "('Ashmont','70090','Fields Corner - Inbound','42.300093','-71.061667')," +
            "('Ashmont','70088','Savin Hill - Inbound','42.31129','-71.053331')," +
            "('Ashmont','70086','JFK/UMASS Ashmont - Inbound','42.320685','-71.052391')," +
            "('Ashmont','70084','Andrew - Inbound','42.330154','-71.057655')," +
            "('Ashmont','70082','Broadway - Inbound','42.342622','-71.056967')," +
            "('Ashmont','70080','South Station - Inbound','42.352271','-71.055242')," +
            "('Ashmont','70078','Downtown Crossing - to Alewife','42.355518','-71.060225')," +
            "('Ashmont','70076','Park Street - to Alewife','42.35639457','-71.0624242')," +
            "('Ashmont','70074','Charles/MGH - Outbound','42.361166','-71.070628')," +
            "('Ashmont','70072','Kendall/MIT - Outbound','42.36249079','-71.08617653')," +
            "('Ashmont','70070','Central - Outbound','42.365486','-71.103802')," +
            "('Ashmont','70068','Harvard - Outbound','42.373362','-71.118956')," +
            "('Ashmont','70066','Porter - Outbound','42.3884','-71.119149')," +
            "('Ashmont','70064','Davis - Outbound','42.39674','-71.121815')," +
            "('Ashmont','70061','Alewife','42.395428','-71.142483');";

    static final String ASHMONT_OUTBOUND = "insert into stops_outbound ('route_id','stop_id','stop_name','stop_lat','stop_lon') values " +
            "('Ashmont','70061','Alewife','42.395428','-71.142483')," +
            "('Ashmont','70063','Davis - Inbound','42.39674','-71.121815')," +
            "('Ashmont','70065','Porter - Inbound','42.3884','-71.119149')," +
            "('Ashmont','70067','Harvard - Inbound','42.373362','-71.118956')," +
            "('Ashmont','70069','Central - Inbound','42.365486','-71.103802')," +
            "('Ashmont','70071','Kendall/MIT - Inbound','42.36249079','-71.08617653')," +
            "('Ashmont','70073','Charles/MGH - Inbound','42.361166','-71.070628')," +
            "('Ashmont','70075','Park Street - to Ashmont/Braintree','42.35639457','-71.0624242')," +
            "('Ashmont','70077','Downtown Crossing - to Ashmont/Braintree','42.355518','-71.060225')," +
            "('Ashmont','70079','South Station - Outbound','42.352271','-71.055242')," +
            "('Ashmont','70081','Broadway - Outbound','42.342622','-71.056967')," +
            "('Ashmont','70083','Andrew - Outbound','42.330154','-71.057655')," +
            "('Ashmont','70085','JFK/UMASS Ashmont - Outbound','42.320685','-71.052391')," +
            "('Ashmont','70087','Savin Hill - Outbound','42.31129','-71.053331')," +
            "('Ashmont','70089','Fields Corner - Outbound','42.300093','-71.061667')," +
            "('Ashmont','70091','Shawmut - Outbound','42.29312583','-71.06573796')," +
            "('Ashmont','70093','Ashmont - Outbound','42.284652','-71.064489');";

    static final String ROUTE_TABLE_UPDATE = "insert into route_table " +
            "('mode','route_id','route_name') values " +
            "('Subway','Ashmont','Ashmont')," +
            "('Subway','Braintree','Braintree');";
}//end class

