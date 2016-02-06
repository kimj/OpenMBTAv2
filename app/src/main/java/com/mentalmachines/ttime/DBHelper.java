package com.mentalmachines.ttime;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

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
    public static final String KEY_ROUTE_TYPE = "route_type";
    public static final String ROUTE = "route";
    public static final String DB_ROUTE_TABLE = "route_table";

    String CREATE_DB_TABLE_ROUTE  = "create table if not exists " + DB_ROUTE_TABLE + "("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_ROUTE_MODE + " TEXT not null, "
            + KEY_ROUTE_ID + " TEXT not null, "
            + KEY_ROUTE_NAME + " TEXT not null);";

    //Stop keys, some duplication with routes
    public static final String KEY_DIR = "direction";
    public static final String KEY_DIR_NM = "direction_name";
    //Inbound, Outbound
    public static final String STOP = "stop";
    public static final String KEY_STOP_ORD = "stop_order";
    public static final String KEY_STOPID = "stop_id";
    public static final String KEY_STOPNM = "stop_name";
    public static final String KEY_STOPLT = "stop_lat";
    public static final String KEY_STOPLN = "stop_lon";

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

    public static final String DB_INB_TABLE = "inbound_table";
    public static final String DB_OUT_TABLE = "outbound_table";
    String CREATE_DB_TABLE_INBOUND  = "create table if not exists " + DB_INB_TABLE + "("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_ROUTE_ID + " TEXT not null,"
            + KEY_STOPID + " INT not null,"
            + KEY_STOP_ORD + " INT not null,"
            + KEY_STOPNM + " TEXT not null,"
            + KEY_STOPLT + " REAL not null,"
            + KEY_STOPLN + " REAL not null);";

    String CREATE_DB_TABLE_OUTBOUND  = "create table if not exists " + DB_OUT_TABLE + "("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_ROUTE_ID + " TEXT not null,"
            + KEY_STOPID + " INT not null,"
            + KEY_STOP_ORD + " INT not null,"
            + KEY_STOPNM + " TEXT not null,"
            + KEY_STOPLT + " NUMERIC not null,"
            + KEY_STOPLN + " NUMERIC not null);";

    public static final String DB_ALERTS_TABLE = "alerts";

    String CREATE_DB_TABLE_ALERTS  = "create table if not exists " + DB_ALERTS_TABLE + "("
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

    private static final String RTINDEX = "CREATE INDEX RTINDEX ON " + DB_ROUTE_TABLE + "("
            + "_id," + KEY_ROUTE_MODE + ");";
    private static final String STOP_IN_DEX = "CREATE UNIQUE INDEX STOP_IN_DEX ON " + DB_INB_TABLE + "("
            + "_id," + KEY_ROUTE_ID + "," + KEY_STOPID + ");";
    private static final String STOP_OUT_DEX = "CREATE UNIQUE INDEX STOP_OUT_DEX ON " + DB_OUT_TABLE + "("
            + "_id," + KEY_ROUTE_ID + "," + KEY_STOPID + ");";

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion
                + " to "
                + newVersion + ", which will destroy all old data");
        String dropAllTables = "DROP TABLE IF EXISTS " + DB_ROUTE_TABLE + ";";
        db.execSQL(dropAllTables);
        onCreate(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DB_TABLE_ROUTE);
        db.execSQL(CREATE_DB_TABLE_INBOUND);
        db.execSQL(CREATE_DB_TABLE_OUTBOUND);
        db.execSQL(RTINDEX);
        db.execSQL(STOP_IN_DEX);
        db.execSQL(STOP_OUT_DEX);
        db.execSQL(CREATE_DB_TABLE_ALERTS);
    }

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    public DBHelper(Context context) {
        super(context, DBNAME, null, DB_VERSION);
    }

    /**
     * This function runs in the bg and copies the db over to the root directory
     * @param ctxt
     * @throws IOException
     */
    static void copyDBFile(Context ctxt) throws IOException {
        //use as needed to verify data
        FileInputStream inStream = new FileInputStream("/data/data/com.mentalmachines.ttime/databases/" + DBNAME);
        final File outFile;
        if (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            outFile = new File(Environment.getExternalStorageDirectory(), "copy" + DBNAME);

        } else {
            outFile = new File("copy" + DBNAME);
        }
        //outFile.setReadOnly(false);
        //this line is failing on permissions

        FileOutputStream outStream = new FileOutputStream(outFile);
        int tmp = inStream.read(); //read one byte
        while(tmp != -1) {
            outStream.write(tmp); //write that byte
            tmp = inStream.read();
        }
        Log.d(TAG, "db file: " + outFile.getName());
        Toast.makeText(ctxt, "database copy to sdcard complete", Toast.LENGTH_SHORT).show();
    }

}