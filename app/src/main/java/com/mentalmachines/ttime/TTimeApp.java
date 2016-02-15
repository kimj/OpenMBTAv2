package com.mentalmachines.ttime;

import android.app.Application;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.mentalmachines.ttime.services.DBCreateStopsRoutes;

/**
 * Created by emezias on 2/5/16.
 */
public class TTimeApp extends Application{
    public static final String TAG = "TTimeApp";

    @Override
    public void onCreate() {
        super.onCreate();
        final SQLiteDatabase db = new DBHelper(this).getWritableDatabase();
        if(DatabaseUtils.queryNumEntries(db, DBHelper.DB_ROUTE_TABLE) == 0) {
            Log.i(TAG, "no db");
            startService(new Intent(this, DBCreateStopsRoutes.class));
        } else {
            RouteExpandableAdapter.initBusList(this);
            Log.i(TAG, "initializing bus routes");
        }

    }
}
