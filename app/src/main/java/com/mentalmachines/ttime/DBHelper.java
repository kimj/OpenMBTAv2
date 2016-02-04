package com.mentalmachines.ttime;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by CaptofOuterSpace on 1/23/2016.
 */
public class DBHelper extends SQLiteOpenHelper {
    public static final String TAG = "DBAdapter";
    public static final int DB_VERSION = 1;

    public static final String KEY_ROUTE_MODE = "mode";
    public static final String KEY_ROUTE_MODE_NM = "mode_name";
    public static final String KEY_ROUTE_ID = "route_id";
    public static final String KEY_ROUTE_NAME = "route_name";
    public static final String KEY_ROUTE_TYPE = "route_type";
    public static final String ROUTE = "route";
    public static final String DB_ROUTE_TABLE = "route_table";


    private SQLiteDatabase mackeyrmsDatabase;
    private static Context context= null;


    public void initializeDatabase(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(CREATE_DB_TABLE_ROUTE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion
                + " to "
                + newVersion + ", which will destroy all old data");
        String dropAllTables = "DROP TABLE IF EXISTS " + DB_ROUTE_TABLE + ";";
        db.execSQL(dropAllTables);
        onCreate(db);
    }
    String CREATE_DB_TABLE_ROUTE  = "create table if not exists " + DB_ROUTE_TABLE + "("
                + "id Integer AUTOINCREMENT PRIMARY KEY"
                + KEY_ROUTE_MODE + "TEXT"
                + KEY_ROUTE_ID + "INTEGER"
                + KEY_ROUTE_NAME + "TEXT)";

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onOpen(SQLiteDatabase db) { super.onOpen(db); }

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

/*    public ArrayList<Route> getRoutes(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor routesCursor = db.query(DB_TABLE_ROUTE, new String[]{
                        KEY_NOTE_ACTION, KEY_NOTE_ALT_HTML, KEY_NOTE_BODY, KEY_NOTE_COMMENTS, KEY_NOTE_CONTACT_ID },
                null, null, null, null, null);

        return routesCursor;
    }

    public long insertRoute(Route route)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NOTE_ACTION, route.getAction());

        return db.insert(DB_TABLE_ROUTE, null, initialValues);

    }*/
}