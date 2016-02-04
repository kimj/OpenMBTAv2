package com.mentalmachines.ttime;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mentalmachines.ttime.objects.Route;

import java.util.ArrayList;

/**
 * Created by CaptofOuterSpace on 1/23/2016.
 */
public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_KEY_ROUTE_MODE = "mode";
    public static final String DB_KEY_ROUTE_ID = "route_id";
    public static final String DB_KEY_ROUTE_ROUTE_NAME = "route_name";

    private static final String DB_TABLE_ROUTE = "route";

    private static final String TAG = "DBAdapter";
    private static final int DB_VERSION = 1;

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
        String dropAllTables = "DROP TABLE IF EXISTS " + DB_TABLE_ROUTE + ";";
        db.execSQL(dropAllTables);
        onCreate(db);
    }
    String CREATE_DB_TABLE_ROUTE  = "create table if not exists " + DB_TABLE_ROUTE + "("
                + "id Integer AUTOINCREMENT PRIMARY KEY"
                + DB_KEY_ROUTE_MODE + "TEXT"
                + DB_KEY_ROUTE_ID + "INTEGER"
                + DB_KEY_ROUTE_ROUTE_NAME + "TEXT)";

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