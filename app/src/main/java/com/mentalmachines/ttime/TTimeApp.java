package com.mentalmachines.ttime;

import android.app.Application;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.mentalmachines.ttime.services.CopyDBService;
import com.mentalmachines.ttime.services.GTFS_Service;
import com.mentalmachines.ttime.services.GetMBTARequestService;

/**
 * Created by emezias on 2/5/16.
 */
public class TTimeApp extends Application{
    public static final String TAG = "TTimeApp";

    @Override
    public void onCreate() {
        super.onCreate();
        //startService(new Intent(this, GetMBTARequestService.class));
        //startService(new Intent(this, GTFS_Service.class));
        //startService(new Intent(this, CopyDBService.class));
        //final SQLiteDatabase db = new DBHelper(this).getWritableDatabase();
        /*if(DatabaseUtils.queryNumEntries(db, DBHelper.DB_ROUTE_TABLE) > 0) {
            Log.i(TAG, "db exists");
            return;
        } */
    }
}
