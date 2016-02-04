package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.objects.Route;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    public static final String TAG = "GetNewzService";
    //reusable for displaying the date
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM-dd");

    // final private static ArrayList<FarooItem> parseList = new ArrayList<>();
    final private static JsonFactory factory = new JsonFactory();
    private static JsonParser parser;
    private static final GregorianCalendar cal = new GregorianCalendar();
    private static final BitmapFactory.Options opts = new BitmapFactory.Options();
    //Base URL
    public static final String DEFAULT = "http://www.faroo.com/api?src=news&rlength=3&f=json&key=BiLaVoSafZjhLpHZZnv9Oe16rdY_";

    public static final String API_KEY = "wX9NwuHnZU2ToO7GmGR9uw";
    public static final String PREFIX = "http://realtime.mbta.com/developer/api/v2/";
    public static final String SUFFIX = "<query>?api_key=" + API_KEY +" &<parameter>=<required/optional parameters>";
    public static final String BY = "by ";
    public static final String COMMA = ", ";

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
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            //make the network call here in the background
            final Bundle b = intent.getExtras();
            FarooItem[] list = null;
            if(b == null) {
                parser = factory.createParser(getURL("ROUTE"));
                list = parseAPICall();
            } else if(b.containsKey(TAG)) {
                //This call is passing in user input, the search term
                final String keyword = b.getString(TAG);
                Log.i(TAG, "Keyword search: " + keyword);
                parser = factory.createParser(new URL(PREFIX + TextUtils.htmlEncode(keyword) + SUFFIX));
                list = parseAPICall();
            //Next two can return without a follow up broadcast
            } else if(b.containsKey(NewzListActivity.TAG)) {
                File f = new File(getExternalFilesDir(null), FILENAME);
                f.setReadable(true);

                //read saved data and return
                if(f.exists()) {
                    parser = factory.createParser(f);
                    list = parseAPICall();
                    Log.d(TAG, "reading file, list count? " + list.length);
                } else {
                    Log.w(TAG, "no file saved, last list not saved");
                    return;
                }
            } else if(b.containsKey(NEWS)) {
                Parcelable[] stuff = b.getParcelableArray(NEWS);
                list = new FarooItem[stuff.length];
                int dex = 0;
                for(Parcelable p: stuff) {
                    list[dex++] = (FarooItem) p;
                }
                return;
            }

            //This part wraps things up and sends the data back to the activity
            final Intent tnt = new Intent(TAG);
            tnt.putExtra(TAG, list);
            if(b != null && b.containsKey(TAG) && list.length == 0) {
                tnt.putExtra(NewzListActivity.TAG, b.getString(TAG));
                tnt.putExtra(PROB, "");
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(tnt);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            final Intent tnt = new Intent(TAG);
            //empty array triggers error in activity
            tnt.putExtra(TAG, new FarooItem[0]);
            tnt.putExtra(PROB, e.getMessage());
            LocalBroadcastManager.getInstance(this).sendBroadcast(tnt);
        }
    }

    /**
     * This method will send a url to Faroo and parse the JSON response
     * Parser is initialized before calling this method
     * @return the list that is parsed
     * @throws IOException
     */
    Route parseAPICall() throws IOException {
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
                    } else if(JsonToken.FIELD_NAME.equals(token) && DBHelper.DB_KEY_ROUTE_MODE.equals(parser.getCurrentName())) {
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
    }
}
