package com.mentalmachines.ttime.objects;

import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.MainActivity;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.fragments.AlertDetailFragment;
import com.mentalmachines.ttime.fragments.AlertsFragment;
import com.mentalmachines.ttime.fragments.RouteFragment;
import com.mentalmachines.ttime.fragments.StopFragment;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by emezias on 4/21/16.
 * Class to perform common functions needed across activities
 */
public class Utils {
    public static final String TAG = "Utils";
    private static int sWidth = -1;

    public static final DateFormat timeFormat = new SimpleDateFormat("h:mm a");
    //note schedule times do not need multiplication
    public static String getTime(Calendar cal, String stamp) {
        cal.setTimeInMillis(1000 * Long.valueOf(stamp));
        return timeFormat.format(cal.getTime());
    }

    /**
     * Here is the fragment transaction, screen transition
     * @param ctx Context is needed to create the Fragment Manager
     * @param oldFragment this is the fragment that needs to hide
     * @param newFragmentName this is the new fragment, identified by the TAG string
     * @param dataObject some fragments take an object parameter to newInstance, that's the last parameter
     * @return the fragment that is now showing
     */
    public static Fragment fragmentChange(MainActivity ctx, Fragment oldFragment, String newFragmentName, Object dataObject) {
        final FragmentManager mgr = ctx.getSupportFragmentManager();
        final FragmentTransaction tx = mgr.beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        if(oldFragment != null) {
            tx.hide(oldFragment);
        }
        Fragment newFragment = null;
        switch (newFragmentName) {
            case RouteFragment.TAG:
                if(mgr.findFragmentByTag(((Route) dataObject).id) != null) {
                    newFragment = mgr.findFragmentByTag(((Route) dataObject).id);
                    tx.show(newFragment);
                } else {
                    newFragment = RouteFragment.newInstance((Route) dataObject);
                    tx.add(R.id.container, newFragment, ((Route) dataObject).id).addToBackStack(((Route) dataObject).id);
                }
                ctx.mFab.setVisibility(View.VISIBLE);
                break;
            case StopFragment.TAG:
                if(mgr.findFragmentByTag(((StopData) dataObject).stopId) != null) {
                    newFragment = mgr.findFragmentByTag(((StopData) dataObject).stopId);
                    tx.show(newFragment);
                } else {
                    newFragment = StopFragment.newInstance((StopData) dataObject);
                    tx.add(R.id.container, newFragment, ((StopData) dataObject).stopId).addToBackStack(((StopData) dataObject).stopId);
                }
                ctx.mFab.setVisibility(View.INVISIBLE);
                break;
            case AlertsFragment.TAG:
                //if there is no data, show all alerts, otherwise open specific alert
                if (dataObject == null) {
                    newFragment = mgr.findFragmentByTag(AlertsFragment.TAG);
                    if (newFragment == null) {
                        newFragment = new AlertsFragment();
                        tx.add(R.id.container, newFragment).addToBackStack(AlertsFragment.TAG);
                    } else {
                        ((AlertsFragment)newFragment).updateAlertsListView(null);
                        tx.show(newFragment);
                    }
                    //finished with list of all alerts - TODO route alert list
                } else {
                    newFragment = mgr.findFragmentByTag(AlertDetailFragment.TAG);
                    if (newFragment != null) {
                        final Alert alert = DBHelper.getAlertById((String) dataObject);
                        if (alert == null) {
                            Toast.makeText(ctx, "Error finding that alert", Toast.LENGTH_SHORT).show();
                            return oldFragment;
                        }
                        ((AlertDetailFragment) newFragment).showAlert(alert);
                    } else {
                        newFragment = AlertDetailFragment.newInstance((String) dataObject);
                        tx.add(R.id.container, newFragment).addToBackStack(AlertDetailFragment.TAG);
                    }
                }
                //data object parameter is the alert id, should be set with newInstance instead
                ctx.mFab.setVisibility(View.INVISIBLE);
                break;
        }
        tx.commit();
        //mgr.executePendingTransactions();
        return newFragment;
        //may need to check that newFragment is not null
    }

    public static int getScreenWidth(Context ctx) {
        if (sWidth < 0) {
            final Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            final Point size = new Point();
            display.getSize(size);
            /*int width = size.x;
            int height = size.y;*/
            sWidth = size.x;
        }
        return sWidth;
    }

    /**
     * This method can be called before trying to reach the MBTA server
     * @param ctx
     * @return
     */
    public static boolean checkNetwork(Context ctx) {
        final NetworkInfo info = ((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if(info == null || !info.isConnected()) {
            Log.e(TAG, "network not found");
            return false;
        }
        return true;
    }

    public static HttpURLConnection getConnector(Context ctx, String urlString) throws IOException {
        if(!checkNetwork(ctx)) {
            return null;
        }
        //consolidating the http connection and network check
        final URL url = new URL(urlString);
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        return urlConnection;
    }

    /**
     * This might be called with the schedule times or the predicted times
     * It checks the times in the String and returns 3 comma separated values
     * Have to be careful where schedtimes is changed to include the route name?
     * @param timeField, sched or predic times string
     * @return human readable upcoming times
     */
    final static StringBuilder stringBuilder = new StringBuilder();
    static Calendar now = Calendar.getInstance();
    public static String trimStopTimes(String empty,  StopData s) {
        //send back 3 that are after the current time
        //TODO hasten parse by stopping after 3 times are set?
        //could improve parse times by saving timestamps
        final int sz;
        if(s == null || (sz = s.scheduleTimes.size()) < 1) {
            return empty;
        }
        stringBuilder.setLength(0);
        Log.d(TAG, "number of times? " + sz);
        int counter = 0;
        for(int dex = 0; dex < s.scheduleTimes.size(); dex++) {

            now.setTimeInMillis(s.scheduleTimes.get(dex));
            if(now.getTimeInMillis() > System.currentTimeMillis()) {
                counter++;
                stringBuilder.append(Utils.timeFormat.format(now.getTime()));
                if(counter < 3 && counter < sz-1) {
                    stringBuilder.append(", ");
                } else {
                    return stringBuilder.toString();
                }
            } else {
                Log.d(TAG, "skipping time, #" + dex);
            }
        }

        if(counter == 0) {
            return empty;
        }

        return stringBuilder.toString();
    }

    public static String nearbyTimes(String empty,  StopData s) {
        //send back 3 that are after the current time
        //TODO hasten parse by stopping after 3 times are set?
        //could improve parse times by saving timestamps
        final int sz;
        if(s == null || (sz = s.scheduleTimes.size()) < 1) {
            return empty;
        }
        stringBuilder.setLength(0);
        Log.d(TAG, "number of times? " + sz);
        int counter = 0;
        //counter helps to manage if some of the schedule times are in the past
        for(int dex = 0; dex < sz; dex++) {
            now.setTimeInMillis(s.scheduleTimes.get(dex));
            if(now.getTimeInMillis() > System.currentTimeMillis()) {
                counter++;
                stringBuilder.append(Utils.timeFormat.format(now.getTime()));
                if(counter < 3 && counter < sz-1) {
                    stringBuilder.append("\n          ");
                } else {
                    break;
                }
            } else {
                Log.d(TAG, "skipping time, #" + dex);
            }
        }

        if(counter == 0) {
            return empty;
        }
        return stringBuilder.toString();
    }

    static void addInPrediction(StringBuilder text, int offsetSecs) {

        if (offsetSecs > 60) {
            now.set(Calendar.MINUTE, (now.get(Calendar.MINUTE) + (offsetSecs/60)));
            now.set(Calendar.SECOND, (now.get(Calendar.SECOND) + (offsetSecs % 60)));
            text.append(Utils.timeFormat.format(now.getTime())).append(" (");
            text.append(offsetSecs / 60).append("m ").append(offsetSecs % 60).append("s").append(")");
        } else {
            now.set(Calendar.SECOND, offsetSecs);
            text.append(Utils.timeFormat.format(now.getTime())).append(" (");
            text.append(offsetSecs).append("s").append(")");
        }
    }

    public static String setPredictions(String actual, StopData s) {
        final int sz;
        if(s == null || (sz = s.predictionSecs.size()) < 1 || s.predictionTimestamp == 0) {
            return "";
        }
        stringBuilder.setLength(0);
        now.setTimeInMillis(System.currentTimeMillis());
        Log.d(TAG, "Current time is :" + now.getTime());
        int counter = 0;

        long timeToSet;
        stringBuilder.append(actual).append(" ");
        for(int dex = 0; dex < s.predictionSecs.size(); dex++) {
            timeToSet = s.predictionTimestamp + s.predictionSecs.get(dex);
            //now.setTimeInMillis(timeToSet);
            now.setTimeInMillis(timeToSet);
            Log.d(TAG, "Current time is :" + now.getTime());
            if(now.getTimeInMillis() > System.currentTimeMillis()) {
                counter++;
                //stringBuilder.append(Utils.timeFormat.format(now.getTime()));
                addInPrediction(stringBuilder, s.predictionSecs.get(dex));
                if(counter == 3 || counter == sz-1) {
                    return stringBuilder.toString();
                } else {
                    stringBuilder.append("\n");
                }
            } else {
                Log.d(TAG, "prd, skipping time, #" + dex);
            }
        }
        if(stringBuilder.length() == actual.length()+1) {
            //string contains only actual:
            return "";
        }
        return stringBuilder.toString();
    }
}
