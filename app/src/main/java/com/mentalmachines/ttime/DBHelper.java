package com.mentalmachines.ttime;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by CaptofOuterSpace on 1/23/2016.
 */
public class DBHelper extends SQLiteOpenHelper {
    public static final String TAG = "DBAdapter";
    public static final int DB_VERSION = 1;
    public static final String DBNAME = "ttimedb.sqlite3";

    //Route table keys
    public static final String KEY_ROUTE_MODE = "mode";
    public static final String KEY_ROUTE_MODE_NM = "mode_name";
    public static final String KEY_ROUTE_ID = "route_id";
    public static final String KEY_ROUTE_NAME = "route_name";
    //public static final String KEY_ROUTE_TYPE = "route_type";
    public static final String ROUTE = "route";
    public static final String DB_ROUTE_TABLE = "route_table";
    public static final String BUS_MODE = "'Bus'";
    public static final String SUBWAY_MODE = "Subway";

    String CREATE_DB_TABLE_ROUTE  = TABLE_PREFIX + DB_ROUTE_TABLE + "("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_ROUTE_MODE + " TEXT not null, "
            + KEY_ROUTE_ID + " TEXT not null, "
            + KEY_ROUTE_NAME + " TEXT not null);";

    //Stop keys, some duplication with routes
    public static final String KEY_DIR = "direction";
    //public static final String KEY_DIR_NM = "direction_name";
    public static final String KEY_DIR_ID = "direction_id";
    //Inbound, Outbound
    public static final String STOP = "stop";
    public static final String KEY_STOP_ORD = "stop_order";
    public static final String KEY_STOPID = "stop_id";
    public static final String KEY_STOPNM = "stop_name";
    public static final String KEY_STOPLT = "stop_lat";
    public static final String KEY_STOPLN = "stop_lon";

    public static final String STOPS_INB_TABLE = "stops_inbound";
    public static final String STOPS_OUT_TABLE = "stops_outbound";
    public static final String STOPS_COLS = "("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_ROUTE_ID + " TEXT not null,"
            + KEY_STOPID + " TEXT not null,"
            + KEY_STOP_ORD + " INT not null,"
            + KEY_STOPNM + " TEXT not null,"
            + KEY_STOPLT + " NUMERIC not null,"
            + KEY_STOPLN + " NUMERIC not null);";

    public static final String KEY_TRIP_ID = "trip_id";
    public static final String KEY_TRIP_SIGN = "trip_headsign";
    //public static final String KEY_DIR_ID = "direction_id";
    public static final String KEY_ARR_TIME = "arrival_time";
    public static final String KEY_DEP_TIME = "departure_time";

    String SCHEDULE_COLS = "(_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_ROUTE_ID + " TEXT not null,"
            + KEY_STOPID + " TEXT not null,"
            + KEY_TRIP_ID + " TEXT not null,"
            + KEY_TRIP_SIGN + " TEXT,"
            + KEY_ARR_TIME + " NUMERIC not null,"
            + KEY_DEP_TIME + " NUMERIC not null);";

    public static final String TABLE_PREFIX = "create table if not exists ";
    public static final String WEEKDAY_TABLE_BUS_IN = "weekday_table_b_in";
    public static final String WEEKDAY_TABLE_BUS_OUT = "weekday_table_b_out";
    public static final String WEEKDAY_TABLE_4BUS_IN = "weekday_table_4b_in";
    public static final String WEEKDAY_TABLE_4BUS_OUT = "weekday_table_4b_out";

    public static final String WEEKDAY_TABLE_SUB_IN = "weekday_table_sub_in";
    public static final String WEEKDAY_TABLE_SUB_OUT = "weekday_table_sub_out";

    public static final String SATURDAY_TABLE_BUS_IN = "sat_table_bus_in";
    public static final String SATURDAY_TABLE_BUS_OUT = "sat_table_bus_out";
    public static final String SATURDAY_TABLE_4BUS_IN = "sat_table_4bus_in";
    public static final String SATURDAY_TABLE_4BUS_OUT = "sat_table_4bus_out";

    public static final String SATURDAY_TABLE_SUB_IN = "sat_table_sub_in";
    public static final String SATURDAY_TABLE_SUB_OUT = "sat_table_sub_out";

    public static final String SUNDAY_TABLE_BUS_IN = "sun_table_bus_in";
    public static final String SUNDAY_TABLE_BUS_OUT = "sun_table_bus_out";
    public static final String SUNDAY_TABLE_4BUS_IN = "sun_table_4bus_in";
    public static final String SUNDAY_TABLE_4BUS_OUT = "sun_table_4bus_out";

    public static final String SUNDAY_TABLE_SUB_IN = "sun_table_sub_in";
    public static final String SUNDAY_TABLE_SUB_OUT = "sun_table_sub_out";

    private static final String RTINDEX = "CREATE INDEX RTINDEX ON " + DB_ROUTE_TABLE + "("
            + KEY_ROUTE_ID + ");";
    private static final String STOP_IN_DEX = "CREATE UNIQUE INDEX STOP_IN_DEX ON " + STOPS_INB_TABLE + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_STOP_ORD + ");";
    private static final String STOP_OUT_DEX = "CREATE UNIQUE INDEX STOP_OUT_DEX ON " + STOPS_OUT_TABLE + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_STOP_ORD + ");";
    //these are created AFTER tables are loaded to speed through loading up these big tables
    public static final String WEEKDAY_DEX_B_IN = "CREATE UNIQUE INDEX WDAY_DEX_B_IN ON " + WEEKDAY_TABLE_BUS_IN + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String WEEKDAY_DEX_B_OUT = "CREATE UNIQUE INDEX WDAY_DEX_B_OUT ON " + WEEKDAY_TABLE_BUS_OUT + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String WEEKDAY_DEX_B4_IN = "CREATE UNIQUE INDEX WDAY_DEX_B_IN ON " + WEEKDAY_TABLE_4BUS_IN + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String WEEKDAY_DEX_B4_OUT = "CREATE UNIQUE INDEX WDAY_DEX_B_OUT ON " + WEEKDAY_TABLE_4BUS_OUT + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String WEEKDAY_DEX_SUB_IN = "CREATE UNIQUE INDEX WDAY_DEX_SUB_IN ON " + WEEKDAY_TABLE_SUB_IN + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String WEEKDAY_DEX_SUB_OUT = "CREATE UNIQUE INDEX WDAY_DEX_SUB_OUT ON " + WEEKDAY_TABLE_SUB_OUT + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";

    public static final String SATURDAY_DEX_B_IN = "CREATE UNIQUE INDEX SAT_DEX_INB ON " + SATURDAY_TABLE_BUS_IN + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String SATURDAY_DEX_B_OUT = "CREATE UNIQUE INDEX SAT_DEX_OUTB ON " + SATURDAY_TABLE_BUS_OUT + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String SATURDAY_DEX_4B_IN = "CREATE UNIQUE INDEX SAT_DEX_INB ON " + SATURDAY_TABLE_4BUS_IN + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String SATURDAY_DEX_4B_OUT = "CREATE UNIQUE INDEX SAT_DEX_OUTB ON " + SATURDAY_TABLE_4BUS_OUT + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String SATURDAY_DEX_SUB_IN = "CREATE UNIQUE INDEX SAT_DEX_IN ON " + SATURDAY_TABLE_SUB_IN + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String SATURDAY_DEX_SUB_OUT = "CREATE UNIQUE INDEX SAT_DEX_OUT ON " + SATURDAY_TABLE_SUB_OUT + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";

    public static final String SUN_DEX_B_IN = "CREATE UNIQUE INDEX SUN_DEX_INB ON " + SUNDAY_TABLE_BUS_IN + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String SUN_DEX_B_OUT = "CREATE UNIQUE INDEX SUN_DEX_OUTB ON " + SUNDAY_TABLE_BUS_OUT + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String SUN_DEX_B4_IN = "CREATE UNIQUE INDEX SUN_DEX_INB ON " + SUNDAY_TABLE_4BUS_IN + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String SUN_DEX_B4_OUT = "CREATE UNIQUE INDEX SUN_DEX_OUTB ON " + SUNDAY_TABLE_4BUS_OUT + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String SUN_DEX_SUB_IN = "CREATE UNIQUE INDEX SUN_DEX_IN ON " + SUNDAY_TABLE_SUB_IN + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";
    public static final String SUN_DEX_SUB_OUT = "CREATE UNIQUE INDEX SUN_DEX_OUT ON " + SUNDAY_TABLE_SUB_OUT + "("
            + KEY_ROUTE_ID + "," + KEY_STOPID + "," + KEY_ARR_TIME + ");";


    public static final String KEY_ALERTS= "alerts";
    public static final String KEY_ALERT_ID = "alert_id";
    public static final String KEY_EFFECT_NAME = "effect_name";
    public static final String KEY_EFFECT = "effect";
    public static final String KEY_CAUSE_NAME = "cause_name";
    public static final String KEY_CAUSE = "cause";
    public static final String KEY_HEADER_TEXT = "header_text";
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

    String CREATE_DB_TABLE_ALERTS  = TABLE_PREFIX + DB_ALERTS_TABLE + "("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_ALERT_ID + " INT not null,"
            + KEY_EFFECT_NAME + " TEXT not null,"
            + KEY_CAUSE_NAME + " TEXT not null,"
            + KEY_CAUSE + " TEXT not null,"
            + KEY_HEADER_TEXT + " TEXT not null,"
            + KEY_SHORT_HEADER_TEXT + " TEXT not null);"
            + KEY_DESCRIPTION_TEXT + " TEXT not null);"
            + KEY_SEVERITY + " TEXT not null);"
            + KEY_CREATED_DT + " TEXT not null);"
            + KEY_LAST_MODIFIED_DT + " TEXT not null);"
            + KEY_SERVICE_EFFECT_TEXT + " TEXT not null);"
            + KEY_TIMEFRAME_TEXT + " TEXT not null);"
            + KEY_ALERT_LIFECYCLE + " TEXT not null);"
            + KEY_EFFECT_PERIOD_START + " TEXT not null);"
            + KEY_EFFECT_PERIOD_END + " TEXT not null);";


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

        db.execSQL(TABLE_PREFIX + STOPS_INB_TABLE + STOPS_COLS);
        db.execSQL(TABLE_PREFIX + STOPS_OUT_TABLE + STOPS_COLS);
        db.execSQL(RTINDEX);
        db.execSQL(STOP_IN_DEX);
        db.execSQL(STOP_OUT_DEX);
        db.execSQL(TABLE_PREFIX + WEEKDAY_TABLE_BUS_IN + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + WEEKDAY_TABLE_BUS_OUT + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + WEEKDAY_TABLE_4BUS_IN + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + WEEKDAY_TABLE_4BUS_OUT + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + WEEKDAY_TABLE_SUB_IN + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + WEEKDAY_TABLE_SUB_OUT + SCHEDULE_COLS);

        db.execSQL(TABLE_PREFIX + SATURDAY_TABLE_BUS_IN + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + SATURDAY_TABLE_BUS_OUT + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + SATURDAY_TABLE_4BUS_IN + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + SATURDAY_TABLE_4BUS_OUT + SCHEDULE_COLS);

        db.execSQL(TABLE_PREFIX + SATURDAY_TABLE_SUB_IN + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + SATURDAY_TABLE_SUB_OUT + SCHEDULE_COLS);

        db.execSQL(TABLE_PREFIX + SUNDAY_TABLE_BUS_IN + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + SUNDAY_TABLE_BUS_OUT + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + SUNDAY_TABLE_4BUS_IN + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + SUNDAY_TABLE_4BUS_OUT + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + SUNDAY_TABLE_SUB_IN + SCHEDULE_COLS);
        db.execSQL(TABLE_PREFIX + SUNDAY_TABLE_SUB_OUT + SCHEDULE_COLS);
    }


    public DBHelper(Context context) {
        super(context, DBNAME, null, DB_VERSION);
    }

}