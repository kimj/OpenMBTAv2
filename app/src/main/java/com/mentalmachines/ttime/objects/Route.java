package com.mentalmachines.ttime.objects;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.adapter.RouteExpandableAdapter;

import java.util.ArrayList;

public class Route implements Parcelable {
    public static final String TAG = "Route";
    public int type;
    //public String mode_name;
    //skipping
    public String id;
    public String name;
    public ArrayList<StopData> mInboundStops, mOutboundStops;

    public final static String[] mStopProjection = new String[]{
            DBHelper.KEY_STOPNM, DBHelper.KEY_STOPID,
            DBHelper.KEY_STOPLN, DBHelper.KEY_STOPLT, DBHelper.KEY_ALERT_ID
    };

    public Route() { }

    /**
     * method creates the stop list shown in the route fragment and adapters
     * db operations, keep off main thread
     *
     * @param ctx, needed to build the database
     */
    public void setStops(Context ctx) {
        final SQLiteDatabase mDB = new DBHelper(ctx).getReadableDatabase();
        Cursor stopNameCursor = mDB.query(DBHelper.STOPS_INB_TABLE, mStopProjection,
                RouteExpandableAdapter.stopsSubwayWhereClause + "'" + id + "'",
                null, null, null, DBHelper.KEY_STOP_ORD + " ASC");
        mInboundStops = makeStops(stopNameCursor);
        Log.d(TAG, "inbound stop count for route: " + name + " " + stopNameCursor.getCount());
        //direction == 1) {
        stopNameCursor = mDB.query(DBHelper.STOPS_OUT_TABLE, mStopProjection,
                RouteExpandableAdapter.stopsSubwayWhereClause + "'" + id + "'",
                null, null, null, DBHelper.KEY_STOP_ORD + " ASC");
        mOutboundStops = makeStops(stopNameCursor);
        Log.d(TAG, "outbound stop count for route: " + name + " " + stopNameCursor.getCount());
        if (!stopNameCursor.isClosed()) {
            stopNameCursor.close();
        }
        mDB.close();
    }

    /**
     * This method will help instantiate the route fragment from a db query of the route name
     *
     * @return the list of stop data to be displayed
     */
    public ArrayList<StopData> makeStops(Cursor stopNameCursor) {
        if (stopNameCursor.getCount() > 0 && stopNameCursor.moveToFirst()) {
            ArrayList<StopData> stopList = new ArrayList<>(stopNameCursor.getCount());
            StopData data;
            do {
                data = new StopData();
                data.stopName = stopNameCursor.getString(0);
                data.stopId = stopNameCursor.getString(1);
                data.stopLong = stopNameCursor.getString(2);
                data.stopLat = stopNameCursor.getString(3);
                data.stopAlert = stopNameCursor.getString(4);
                stopList.add(data);
                //Log.d(TAG, data.stopId + " predictimes? " + data.predicTimes);
            } while (stopNameCursor.moveToNext());
            return stopList;
        }
        Log.w(TAG, "empty cursor for route: " + name);
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(type);
        //public String mode_name;
        //using integer mode type, not name
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeTypedList(mInboundStops);
        //Log.d(TAG, "array size?" + mOutboundStops.size());
        parcel.writeTypedList(mOutboundStops);
    }

    /**
     * Creator required for class implementing the parcelable interface.
     */
    public static final Parcelable.Creator<Route> CREATOR = new Creator<Route>() {

        @Override
        public Route createFromParcel(Parcel parcel) {
            Route r = new Route();
            r.type = parcel.readInt();
            r.id = parcel.readString();
            r.name = parcel.readString();
            r.mInboundStops = parcel.createTypedArrayList(StopData.CREATOR);
            r.mOutboundStops = parcel.createTypedArrayList(StopData.CREATOR);
            return r;
        }

        @Override
        public Route[] newArray(int size) {
            return new Route[size];
        }
    };
}
