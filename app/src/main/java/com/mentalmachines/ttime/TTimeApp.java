package com.mentalmachines.ttime;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.HttpResponseCache;
import android.util.Log;

import com.mentalmachines.ttime.services.DBCreateStopsRoutes;
import com.mentalmachines.ttime.services.GetMBTARequestService;

import java.io.File;
import java.io.IOException;

/**
 * Application class, some utils can be found in here
 * Created by emezias on 2/5/16.
 */
public class TTimeApp extends Application{
    public static final String TAG = "TTimeApp";
    public static DBHelper sHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        sHelper = new DBHelper(this);
        final SQLiteDatabase db = sHelper.getReadableDatabase();
        if(checkNetwork(this)) {
            if(DatabaseUtils.queryNumEntries(db, DBHelper.DB_ROUTE_TABLE) == 0) {
                Log.i(TAG, "no db");
                startService(new Intent(this, DBCreateStopsRoutes.class));
            } else {
                Log.i(TAG, "get alerts");
                startService(new Intent(this, GetMBTARequestService.class));
            }
        } else {
            Log.e(TAG, "network error, app services not started");
        }
        //Create an http cache
        try {
            HttpResponseCache.install(new File(getCacheDir(), "http"), (100 * 1024 * 1024)); // 100 MiB
        } catch (IOException e) {
            Log.e(TAG, "HTTP response cache installation failed", e);
        }
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
