package com.mentalmachines.ttime;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mentalmachines.ttime.objects.Alert;
import com.mentalmachines.ttime.objects.Route;

import java.util.ArrayList;

/**
 * Created by CaptofOuterSpace on 1/23/2016.
 */
public class DBHelper extends SQLiteOpenHelper {
    public static final String TAG = "DBAdapter";
    public static final int DB_VERSION = 1;
    public static final String DBNAME = "ttimedb.sqlite3";
    public static final String DB_ROUTE_TABLE = "route_table";
    public static final String STOPS_INB_TABLE = "stops_inbound";
    public static final String STOPS_OUT_TABLE = "stops_outbound";
    public static final String FAVS_TABLE = "favorites_table";
    public static final String FAVESTOPS_TABLE = "favorites_stops";

    public static final String BRAINTREE = "Braintree";
    public static final String ASHMONT = "Ashmont";
    public static final String KEY_TRIP_NM = "trip_name";
    //Route table keys
    public static final String KEY_ROUTE_MODE = "mode";
    public static final String KEY_ROUTE_MODE_NM = "mode_name";
    public static final String KEY_ROUTE_ID = "route_id";
    public static final String KEY_ROUTE_NAME = "route_name";
    //public static final String KEY_ROUTE_TYPE = "route_type";
    public static final String KEY_ROUTE = "route";
    public static final String BUS_MODE = "Bus";
    public static final String SUBWAY_MODE = "Subway";

    //Stop keys, some duplication with routes
    public static final String KEY_DIR = "direction";
    public static final String KEY_DIR_NM = "direction_name";
    public static final String KEY_DIR_ID = "direction_id";
    //Inbound, Outbound, Northbound and Southbound
    public static final String STOP = "stop";
    public static final String KEY_STOPID = "stop_id";
    public static final String KEY_STOPNM = "stop_name";
    public static final String KEY_STOPLT = "stop_lat";
    public static final String KEY_STOPLN = "stop_lon";

    public static final String KEY_TRIP = "trip";
    public static final String KEY_VEHICLE = "vehicle";
    public static final String KEY_DAY = "schedule_day";
    public static final String KEY_DTIME = "sch_dep_dt";

    public static final String TBL_PREFIX = "SCH";
    public static final String INDX_PREFIX = "DEX";
    public static final String KEY_ID = "_id";

    static String SCHEDULE_COLS = "(_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_STOPID + " TEXT not null,"
            + KEY_STOPNM + " TEXT not null,"
            + KEY_DIR_ID + " NUMERIC not null,"
            + KEY_DAY + " NUMERIC not null,"
            + KEY_DTIME + " NUMERIC not null);";

    public static final String TABLE_PREFIX = "create table if not exists ";

    String CREATE_FAVS_TABLE  = TABLE_PREFIX + FAVS_TABLE + "("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_ROUTE_ID + " TEXT unique not null, "
            + KEY_ROUTE_NAME + " TEXT unique not null);";
    private static final String ROUTE_FAVS_DEX = "CREATE UNIQUE INDEX ROUTE_FAVS_DEX ON " + FAVS_TABLE + "("
            + KEY_ROUTE_NAME + ");";

    String CREATE_FAVESTOPS_TABLE  = TABLE_PREFIX + FAVESTOPS_TABLE + "("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_STOPID + " TEXT not null);";

    private static final String FAVESTOPS_DEX = "CREATE UNIQUE INDEX FAVESTOPS_DEX ON " + FAVESTOPS_TABLE + "("
            + KEY_STOPID + ");";

    String CREATE_DB_TABLE_ROUTE  = TABLE_PREFIX + DB_ROUTE_TABLE + "("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_ROUTE_MODE + " TEXT not null, "
            + KEY_ROUTE_ID + " TEXT unique not null, "
            + KEY_ROUTE_NAME + " TEXT unique not null);";

    public static String getRouteTableName(String routeID) {
        if(routeID.contains("-")) {
            routeID = routeID.replace("-", "_");
            Log.d(TAG, "replaced -" + routeID);
        }
        return TBL_PREFIX + routeID;
    }

    public static boolean checkForScheduleTable(String routeID) {
        final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        return checkForScheduleTable(db, routeID);
    }

    public static boolean checkForScheduleTable(SQLiteDatabase db, String routeID) {
        Cursor cursor = db.rawQuery(
                "select DISTINCT tbl_name from sqlite_master where tbl_name = 'SCH"+routeID+"'", null);
        if(cursor.getCount() > 0) {
            cursor.close();
            return true;
        }
        if(!cursor.isClosed()) cursor.close();
        return false;
    }

    public static String getRouteTableSql(String routeID) {
        return TABLE_PREFIX + getRouteTableName(routeID) + SCHEDULE_COLS;
    }

    public static void setIndexOnRouteTable(SQLiteDatabase db, String routeID) {
        db.execSQL("CREATE INDEX IF NOT EXISTS " + INDX_PREFIX + getRouteTableName(routeID) + " ON " + getRouteTableName(routeID) +
                " (" + KEY_STOPID + "," + KEY_DIR_ID + "," + KEY_DAY + ");");
    }

    private static final String RTINDEX = "CREATE INDEX RTINDEX ON " + DB_ROUTE_TABLE + "("
            + KEY_ROUTE_ID + ");";
    private static final String STOP_IN_DEX = "CREATE UNIQUE INDEX STOP_IN_DEX ON " + STOPS_INB_TABLE + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + ");";
    private static final String STOP_OUT_DEX = "CREATE UNIQUE INDEX STOP_OUT_DEX ON " + STOPS_OUT_TABLE + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + ");";

    public static final String KEY_ALERTS= "alerts";
    public static final String KEY_ALERT_ID = "alert_id";
    public static final String KEY_EFFECT_NAME = "effect_name";
    public static final String KEY_EFFECT = "effect";
    //public static final String KEY_CAUSE_NAME = "cause_name";
    //these are the same...
    public static final String KEY_CAUSE = "cause";
    //public static final String KEY_HEADER_TEXT = "header_text";
    public static final String KEY_SHORT_HEADER_TEXT = "short_header_text";
    public static final String KEY_DESCRIPTION_TEXT = "description_text";
    public static final String KEY_SEVERITY = "severity";
    public static final String KEY_CREATED_DT = "created_dt";
    public static final String KEY_LAST_MODIFIED_DT = "last_modified_dt";
    public static final String KEY_SERVICE_EFFECT_TEXT = "service_effect_text";
    public static final String KEY_TIMEFRAME_TEXT = "timeframe_text";
    public static final String KEY_ALERT_LIFECYCLE = "alert_lifecycle";
    public static final String KEY_EFFECT_PERIOD_START = "effect_start";
    public static final String KEY_EFFECT_PERIOD_END = "effect_end";
    public static final String KEY_EFFECT_PERIODS = "effect_periods";

    public static final String DB_ALERTS_TABLE = "alerts";

    String CREATE_DB_TABLE_ALERTS = "create table " + DB_ALERTS_TABLE + "("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_ALERT_ID + " TEXT not null,"
            + KEY_EFFECT + " TEXT not null,"
            + KEY_EFFECT_NAME + " int not null,"
            + KEY_CAUSE + " TEXT,"
            + KEY_SHORT_HEADER_TEXT + " TEXT,"
            + KEY_DESCRIPTION_TEXT + " TEXT,"
            + KEY_SEVERITY + " TEXT,"
            + KEY_CREATED_DT + " TEXT not null,"
            + KEY_LAST_MODIFIED_DT + " TEXT not null,"
            + KEY_SERVICE_EFFECT_TEXT + " TEXT,"
            + KEY_TIMEFRAME_TEXT + " TEXT,"
            + KEY_ALERT_LIFECYCLE + " TEXT ,"
            + KEY_EFFECT_PERIOD_START + " TEXT,"
            + KEY_EFFECT_PERIOD_END + " TEXT);";

    public static final String STOPS_COLS = "("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_ROUTE_ID + " TEXT not null,"
            + KEY_STOPID + " TEXT not null,"
            + KEY_STOPNM + " TEXT not null,"
            + KEY_ALERT_ID + " NUMERIC,"
            + KEY_STOPLT + " NUMERIC not null,"
            + KEY_STOPLN + " NUMERIC not null);";

    public static final String KEY_PREAWAY = "pre_away";
    public static final String KEY_SCH_TIME = "sch_arr_dt";
    //Property of “trip.” String representation of an integer.
    //Scheduled arrival time at the stop for the trip, in epoch time
    //Example: “1361989260”
    public static final String PRED_TIME = "pre_dt";


    // alert ids are saved to the stop table
    // parse alerts in reverse chron order, save alert id to stop table
    //check the stop table alert is still valid, else look for other alert id for stop


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion
                + " to "
                + newVersion + ", which may destroy all old data");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DB_TABLE_ROUTE);
        db.execSQL(CREATE_DB_TABLE_ALERTS);
        db.execSQL(CREATE_FAVS_TABLE);
        db.execSQL(CREATE_FAVESTOPS_TABLE);

        db.execSQL(TABLE_PREFIX + STOPS_INB_TABLE + STOPS_COLS);
        db.execSQL(TABLE_PREFIX + STOPS_OUT_TABLE + STOPS_COLS);
        db.execSQL(RTINDEX);
        db.execSQL(STOP_IN_DEX);
        db.execSQL(STOP_OUT_DEX);
        db.execSQL(FAVESTOPS_DEX);
        db.execSQL(ROUTE_FAVS_DEX);
    }

    public DBHelper(Context context) {
        super(context, DBNAME, null, DB_VERSION);
    }

    //Some utilities
    public static String[] makeArrayFromCursor(Cursor c, int colIndex) {
        if(c.getCount() > 0 && c.moveToFirst()) {
            final String[] tmp = new String[c.getCount()];
            for(int dex = 0; dex < tmp.length; dex++) {
                tmp[dex] = c.getString(colIndex);
                c.moveToNext();
            }
            return tmp;
        }
        Log.w(TAG, "bad cursor, no array");
        return null;
    }

    public static String getRouteName(SQLiteDatabase db, String routeID) {
        final Cursor c = db.query(DBHelper.DB_ROUTE_TABLE, new String[]{DBHelper.KEY_ROUTE_NAME},
                Route.routeStopsWhereClause + "'" + routeID + "'", null,
                null, null, null);
        if(c.moveToFirst()) {
            final String name = c.getString(0);
            c.close();
            return name;
        }
        return null;
    }

    //This junk manages db concurrency, many reads and one write
    //volatile means one copy in Global memory
    /*private volatile static DBHelper instance;

    *//**
     * DBHelper is a singleton, the app has one db connection, DBHelper
     * The application class instantiates so, no other write, no need to synchronize
     * Seem to have issues clisng cursors!
     * Whenever getHelper is called after the app is launched will already have created the helper instance
     * @param context
     * @return
     *//*
    public static DBHelper getHelper(Context context){
        if(instance == null) {
            instance = new DBHelper(context);
        }
        return instance;
    }*/

    final static String[] mAlertProjection = new String[]{
                    DBHelper.KEY_ALERT_ID,
                    DBHelper.KEY_EFFECT_NAME, DBHelper.KEY_EFFECT,
                    DBHelper.KEY_CAUSE, DBHelper.KEY_SHORT_HEADER_TEXT,
                    DBHelper.KEY_DESCRIPTION_TEXT, DBHelper.KEY_SEVERITY,
                    DBHelper.KEY_CREATED_DT, DBHelper.KEY_LAST_MODIFIED_DT,
                    DBHelper.KEY_TIMEFRAME_TEXT,
                    DBHelper.KEY_ALERT_LIFECYCLE, DBHelper.KEY_EFFECT_PERIOD_START,
                    DBHelper.KEY_EFFECT_PERIOD_END
            };

    public static ArrayList<Alert> getAllAlerts(){
        SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        Cursor alertsCursor = db.query(DBHelper.DB_ALERTS_TABLE, mAlertProjection,
                null, null, null, null, null, null);

        ArrayList<Alert> alerts = new ArrayList<Alert>();
        if (alertsCursor.getCount() > 0 && alertsCursor.moveToFirst()) {
            do {
                Alert a = new Alert();
                a.alert_id = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_ALERT_ID));
                a.effect_name = alertsCursor.getInt(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_NAME));
                a.effect = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT));
                a.cause = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_CAUSE));
                a.description_text = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_DESCRIPTION_TEXT));
                a.short_header_text= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_SHORT_HEADER_TEXT));
                a.severity= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_SEVERITY));
                a.created_dt= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_CREATED_DT));
                a.last_modified_dt= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_LAST_MODIFIED_DT));
                a.timeframe_text= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_TIMEFRAME_TEXT));
                a.alert_lifecycle= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_ALERT_LIFECYCLE));
                a.effect_start = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_PERIOD_START));
                a.effect_end= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_PERIOD_END));
                alerts.add(a);
            } while (alertsCursor.moveToNext());
            alertsCursor.close();
        }
        return alerts;
    }

    public static Alert getAlertById(String alertId){
        SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        Cursor alertsCursor = db.query(DBHelper.DB_ALERTS_TABLE, mAlertProjection,
                KEY_ALERT_ID + " like ?", new String[]{ alertId }, null, null, null, null);

        Alert alert = new Alert();
        if (alertsCursor.getCount() > 0 && alertsCursor.moveToFirst()) {
            alert .alert_id = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_ALERT_ID));
            alert .effect_name = alertsCursor.getInt(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_NAME));
            alert .effect = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT));
            alert.cause = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_CAUSE));
            alert.description_text = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_DESCRIPTION_TEXT));
            alert.short_header_text= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_SHORT_HEADER_TEXT));
            alert.severity= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_SEVERITY));
            alert.created_dt= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_CREATED_DT));
            alert.last_modified_dt= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_LAST_MODIFIED_DT));
            alert.timeframe_text= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_TIMEFRAME_TEXT));
            alert.alert_lifecycle= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_ALERT_LIFECYCLE));
            alert.effect_start = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_PERIOD_START));
            alert.effect_end= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_PERIOD_END));
            alertsCursor.close();
        }
        return alert;
    }

    public static ArrayList<Alert> getAlertsByStopAlertId(String alertId){
        SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
        //what about outbound stops? this is going to miss data where inbound and outbound stops are not the same
        String sqlString = "SELECT * FROM alerts WHERE alert_id IN (SELECT DISTINCT alert_id FROM stops_inbound WHERE alert_id IS NOT NULL AND route_id = (SELECT route_id FROM stops_inbound WHERE alert_id = ?));";
        Cursor alertsCursor = db.rawQuery(sqlString, new String[] {alertId});
        ArrayList<Alert> alerts = new ArrayList<Alert>();
        if (alertsCursor.getCount() > 0 && alertsCursor.moveToFirst()) {
            do {
                Alert a = new Alert();
                a.alert_id = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_ALERT_ID));
                a.effect_name = alertsCursor.getInt(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_NAME));
                a.effect = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT));
                a.cause = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_CAUSE));
                a.description_text = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_DESCRIPTION_TEXT));
                a.short_header_text= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_SHORT_HEADER_TEXT));
                a.severity= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_SEVERITY));
                a.created_dt= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_CREATED_DT));
                a.last_modified_dt= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_LAST_MODIFIED_DT));
                a.timeframe_text= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_TIMEFRAME_TEXT));
                a.alert_lifecycle= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_ALERT_LIFECYCLE));
                a.effect_start = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_PERIOD_START));
                a.effect_end= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_PERIOD_END));
                alerts.add(a);
            } while (alertsCursor.moveToNext());
            alertsCursor.close();
        }
        return alerts;
    }

    //Constants for setting colors, comparing to route names
    public static final String SILVERLLINE = "Silver";
    public static final String BUS_YELLOW = "Bus";
    public static final String GREENLINE = "Green";
    public static final String BLUELINE = "Blue";
    public static final String ORNG_LINE = "Orange";
    public static final String REDLINE= "Red";
}