package com.mentalmachines.ttime.objects;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.TTimeApp;

import java.util.ArrayList;

/**
 * Created by emezias on 5/3/16.
 * This is a place to collect DB methods that manage the favorites creation & deletion
 * Both Routes and Stops can be favorites
 */
public class Favorite {
    public static final String TAG = "Favorite";
    public static final String[] primaryKeySelection = new String[]{DBHelper.KEY_ID};
    public static final String[] faveTableCols = new String[] {
            DBHelper.KEY_DIR_ID, DBHelper.KEY_STOPID };
    public static final String[] stopsProjection = new String[] {
            DBHelper.KEY_STOPID, DBHelper.KEY_STOPNM, DBHelper.KEY_STOPLT,
            DBHelper.KEY_STOPLN, DBHelper.KEY_ALERT_ID };
    public static final String STOP_ID_WHERE = DBHelper.KEY_STOPID + " like '";
    public static final String STOP_FAV_CHK = DBHelper.KEY_STOPID + "=? AND " + DBHelper.KEY_DIR_ID + "=?";

    public static boolean isFavoriteRoute(String routeNm) {
        final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        final boolean isFavorite = isFavoriteRoute(db, routeNm);
        return isFavorite;
    }

    public static boolean isFavoriteRoute(SQLiteDatabase db, String routeNm) {
        final Cursor c = db.query(DBHelper.FAVE_ROUTES, null, DBHelper.KEY_ROUTE_NAME + " like '" + routeNm + "'", null, null, null, null);
        final boolean returnVal;
        returnVal = c.getCount() != 0;
        if(!c.isClosed()) c.close();
        return returnVal;
    }

    public static boolean dropFavoriteRoute(String routeId) {
        final SQLiteDatabase db = TTimeApp.sHelper.getWritableDatabase();
        if(db.delete(DBHelper.FAVE_ROUTES, DBHelper.KEY_ROUTE_NAME + " like '" + routeId + "'", null) == 1) {
            return true;
        }
        return false;
    }

    /**
     * Set Favorite can be called with a route or a stop, returns success as true
     * @param routeNm
     * @param routeId
     * @return true means success
     */
    public static boolean setFavoriteRoute(String routeNm, String routeId) {
        final SQLiteDatabase db = TTimeApp.sHelper.getWritableDatabase();
        final boolean isFavorite;
        final Cursor c = db.query(DBHelper.FAVE_ROUTES, null, DBHelper.KEY_ROUTE_NAME + " like '" + routeNm + "'", null, null, null, null);
        if(c.getCount() > 0) {
            //not a favorite and found in the table
            Log.i(TAG, "dropping favorite " + routeNm + db.delete(
                    DBHelper.FAVE_ROUTES, DBHelper.KEY_ROUTE_NAME + " like '" + routeNm + "'", null));
            isFavorite = false;
        } else {
            final ContentValues cv = new ContentValues();
            cv.put(DBHelper.KEY_ROUTE_NAME, routeNm);
            cv.put(DBHelper.KEY_ROUTE_ID, routeId);
            Log.i(DBHelper.TAG, "adding favorite " + routeNm + " row:" + db.insert(
                    DBHelper.FAVE_ROUTES, "_id", cv));
            cv.clear();
            isFavorite = true;
        }

        if(!c.isClosed()) c.close();
        return isFavorite;
    }

    /**
     * The favorites button now has two groups, routes and stops
     * This method sets a stop as a favorite for easy access from the drawer
     * @param stopId
     * @return whether or not the boolean is a saved favorite
     */
    public static boolean setFavoriteStop(String stopId) {
        final SQLiteDatabase db = TTimeApp.sHelper.getWritableDatabase();
        final int direction_id = getStopDirection(db, stopId);

        boolean isFavorite = false;
        String[] args = new String[] { stopId, direction_id+""};
        Cursor c = db.query(DBHelper.FAVESTOPS_TABLE, null,
                DBHelper.KEY_STOPID + " like ? AND " + DBHelper.KEY_DIR_ID + "=?" ,
                args, null, null, null);
        if(c.getCount() > 0) {
            //not a favorite and found in the table
            Log.i(TAG, "dropping favorite stop " + stopId + db.delete(
                    DBHelper.FAVE_ROUTES, DBHelper.KEY_STOPID + " like ? AND " + DBHelper.KEY_DIR_ID + "=?", args));
        } else {
            final ContentValues cv = new ContentValues();
            cv.put(DBHelper.KEY_STOPID, stopId);
            cv.put(DBHelper.KEY_DIR_ID, direction_id);
            Log.i(DBHelper.TAG, "adding favorite stop " + db.insert(
                    DBHelper.FAVESTOPS_TABLE, "_id", cv));
            cv.clear();
            isFavorite = true;
        }

        if(!c.isClosed()) c.close();
        return isFavorite;
    }

    public static ArrayList<StopData> getFavoriteStops() {
        final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        //select two columns, all rows
        Cursor c = db.query(DBHelper.FAVESTOPS_TABLE, faveTableCols, null, null, null, null, null);
        Cursor faveRows;
        int direction;
        String stopid;
        ArrayList<StopData> stopList = new ArrayList<>();
        if(c.moveToFirst()) {
            do {
                direction = c.getInt(0);
                stopid = c.getString(1);
                faveRows = db.query(direction == 0? DBHelper.STOPS_OUT_TABLE:DBHelper.STOPS_INB_TABLE,
                        stopsProjection, STOP_ID_WHERE + stopid + "'", null, null, null, null);
                if (faveRows.moveToFirst()) {
                    final StopData tmp = new StopData(faveRows);
                    Log.d(TAG, "new stop ? " + tmp.stopName + ":" + tmp.stopId);
                    stopList.add(tmp);

                } else {
                    Log.w(TAG, "error selecting stop: " + stopid);
                }
            } while(c.moveToNext());
            Log.d(TAG, "stops list ready " + stopList.size());
            if(!c.isClosed()) {
                c.close();
            }
            if(faveRows!= null && !faveRows.isClosed()) {
                faveRows.close();
            }
            return stopList;
        } else {
            if(c != null && !c.isClosed()) {
                c.close();
            }
            Log.w(TAG, "no favorite stops");
            return null;
        }
    }

    /**
     * Get the direction for a unique stopid, existing or to be added
     * @param favoriteStopid, unique stopid string
     * @return 1 for inbound, zero for outbound, -1 for error
     */
    public static int getStopDirection(String favoriteStopid) {
        final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        return getStopDirection(db, favoriteStopid);
    }

    public static int getStopDirection(SQLiteDatabase db, String favoriteStopid) {
        Cursor c = db.query(DBHelper.STOPS_INB_TABLE, primaryKeySelection,
                STOP_ID_WHERE + favoriteStopid + "'", null,
                null, null, null);
        if(c.getCount() > 0) {
            if(!c.isClosed()) c.close();
            return 1;
        }
        c = db.query(DBHelper.STOPS_OUT_TABLE, primaryKeySelection,
                STOP_ID_WHERE + favoriteStopid + "'", null,
                null, null, null);
        if(c.getCount() > 0) {
            if(!c.isClosed()) c.close();
            return 0;
        }
        return -1;

    }

    public static boolean isStopFavorite(String stopId) {
        final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        //select two columns, all rows
        final int direction = getStopDirection(db, stopId);
        if(direction < 0) {
            Log.w(TAG, "error with given stopid " + stopId);
            return false;
        }
        Cursor fave = db.query(DBHelper.FAVESTOPS_TABLE, primaryKeySelection,
                STOP_FAV_CHK, new String[] { stopId + "", direction+"" }, null, null, null, null);
        if(fave.getCount() > 0) {
            if(!fave.isClosed()) fave.close();
            return true;
        }
        if(!fave.isClosed()) fave.close();
        return false;
    }

    public static boolean dropFavoriteStop(String stopid) {
        final SQLiteDatabase db = TTimeApp.sHelper.getWritableDatabase();
        if(db.delete(DBHelper.FAVESTOPS_TABLE, DBHelper.KEY_STOPID + " like '" + stopid + "'", null) == 1) {
            return true;
        }
        return false;
    }
}
