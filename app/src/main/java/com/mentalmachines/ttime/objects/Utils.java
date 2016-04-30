package com.mentalmachines.ttime.objects;

import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.mentalmachines.ttime.R;

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
    public static String getTime(Calendar cal, String stamp) {
        cal.setTimeInMillis(1000 * Long.valueOf(stamp));
        return timeFormat.format(cal.getTime());
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
}
