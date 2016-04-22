package com.mentalmachines.ttime.objects;

import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by emezias on 4/21/16.
 * Class to perform common functions needed across activities
 */
public class Utils {
    public static final String TAG = "Utils";
    private static int sWidth = -1;


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
}
