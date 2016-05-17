package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.TTimeApp;
import com.mentalmachines.ttime.objects.Favorite;
import com.mentalmachines.ttime.objects.StopList;

/**
 * Created by emezias on 2/5/16.
 * This ta
 */
public class NavDrawerTask extends IntentService {

    public static final String TAG = "NavDrawerTask";
    final static String[] mRouteProjection = new String[] {
            DBHelper.KEY_ROUTE_ID, DBHelper.KEY_ROUTE_NAME
    };
    final static String modeWhereClause = DBHelper.KEY_ROUTE_MODE + " like ";

    public NavDrawerTask() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //Log.d(TAG, "starting Drawer adapter data service");
        final Intent tnt = new Intent(TAG);
        final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        final Bundle b = intent.getExtras();
        final Cursor c;
        if(b == null) {
            //favorites selected
            tnt.putExtra(DBHelper.STOP, new StopList(null, Favorite.getFavoriteStops()));
            //tnt.putExtra(DBHelper.STOP, Favorite.getFavoriteStops());
            c = db.query(DBHelper.FAVS_TABLE,
                    mRouteProjection,
                    null, null, null, null, null);
            Log.i(TAG, "selecting faves");
        } else {
            final String mode = intent.getStringExtra(TAG);
            Log.i(TAG, "selecting " + mode);
            c = db.query(DBHelper.DB_ROUTE_TABLE,
                    mRouteProjection,
                    modeWhereClause + "'" + mode + "'",
                    null, null, null, null);
        }


        if(c.moveToFirst()) {
            final String[] routeNames = new String[c.getCount()];
            final String[] routeIds = new String[c.getCount()];
            for(int dex = 0; dex < routeNames.length; dex++) {
                routeNames[dex] = c.getString(1);
                routeIds[dex] = c.getString(0);
                c.moveToNext();
            }
            c.close();
            //route expandable adapter uses these parallel arrays to populate the nav drawer
            tnt.putExtra(DBHelper.KEY_ROUTE_NAME, routeNames);
            tnt.putExtra(DBHelper.KEY_ROUTE_ID, routeIds);
            //Log.d(TAG, "extras attached to intent");
        } else {
            Log.e(TAG, "error with cursor, sending empty broadcast");
        }

        if(!c.isClosed()) {
            c.close();
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(tnt);
    }

    /*
    public static Cursor getFavorites(Context ctx) {
        if(mDB == null || !mDB.isOpen()) {
            mDB = new DBHelper(ctx).getReadableDatabase();
        }
        //select all, table only has one column
        return mDB.query(DBHelper.FAVS_TABLE,
                mFavProjection, null, null, null, null, null);
    }
     */
}
