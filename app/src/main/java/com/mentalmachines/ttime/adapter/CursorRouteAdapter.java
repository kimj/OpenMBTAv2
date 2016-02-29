package com.mentalmachines.ttime.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.R;

/**
 * Created by emezias on 1/20/16.
 * This is a cursor adapter to fill in the stop details along with the
 * scheduled time and estimated next arrival
 * This is to be instantiated in the bg (Async Task?) to keep the database I/O off the main thread
 */

public class CursorRouteAdapter extends RecyclerView.Adapter<CursorRouteAdapter.StopViewHolder> {
    public static final String TAG = "CursorRouteAdapter";
    //final public Cursor stopNameCursor, stopTimesCursor;
    final StopData[] mItems;
    final int mDirectionId;
    final public boolean isOneWay;

    //public SimpleStopAdapter(String[] data, int resource) {
    public CursorRouteAdapter(final Context ctx, String routeId, int direction) {
        super();
        final Cursor stopNameCursor, stopTimes;
/*
        Log.d(TAG, "starting svc");
        ctx.startService(new Intent(ctx, CopyDBService.class));
*/
        final SQLiteDatabase mDB = new DBHelper(ctx).getReadableDatabase();
        mDirectionId = direction;
        //remember to mark 1 way routes on the action bar
        if(direction == 1) {
            stopNameCursor = mDB.query(DBHelper.STOPS_INB_TABLE, mStopProjection,
                    RouteExpandableAdapter.stopsSubwayWhereClause + "'" + routeId + "'",
                    null, null, null, DBHelper.KEY_STOP_ORD + " ASC");
            isOneWay = setOneWay(mDB, DBHelper.STOPS_OUT_TABLE, routeId);

        } else {
            stopNameCursor = mDB.query(DBHelper.STOPS_OUT_TABLE, mStopProjection,
                    RouteExpandableAdapter.stopsSubwayWhereClause + "'" + routeId + "'",
                    null, null, null, DBHelper.KEY_STOP_ORD + " ASC");
            isOneWay = setOneWay(mDB, DBHelper.STOPS_INB_TABLE, routeId);
        }
        stopTimes = mDB.query(DBHelper.DB_TABLE_PREDICTION,
                mSchProjection,
                //DBHelper.KEY_DIR_ID + " like " + direction,
                null, null, null, null, DBHelper.KEY_STOPID + " ASC");
        mItems = new StopData[stopNameCursor.getCount()];
        int dex = 0;
        if(stopNameCursor.moveToFirst()) {
            do {
                mItems[dex++] = makeStop(stopNameCursor, stopTimes);
            } while(stopNameCursor.moveToNext());
        }
        Log.d(TAG, "creating adapter, times for this route?" + stopTimes.getCount() + " stops "+ stopNameCursor.getCount());
        stopNameCursor.close();
        stopTimes.close();
        mDB.close();
    }

    @Override
    public int getItemCount() {
        if(mItems == null) {
            return 0;
        } else {
            return mItems.length;
        }
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public StopViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.t_stop, parent, false);
        return new StopViewHolder(view);
    }


    /**
     * Map a stop in mItems to a recycler view entry on the list
     * The mStopDescription view has a tag holding the parent view group
     * @param holder, viewholder, required for recycler view
     * @param position, the position in the list
     */
    @Override
    public void onBindViewHolder(final StopViewHolder holder, int position) {
        if(mItems == null || mItems.length < position) {
            Log.w(TAG, "bad position sent to adapter " + position);
            holder.mStopDescription.setText("");
        } else {
            //setting a tag on the parent view for the two buttons to read
            StopData item = mItems[position];
            ((View) holder.mStopDescription.getTag()).setTag(item);
            holder.mStopDescription.setText(item.stopName);
            if(item.stopAlert != null) {
                holder.mAlertBtn.setVisibility(View.VISIBLE);
                holder.mAlertBtn.setTag(item.stopAlert);
            } else {
                holder.mAlertBtn.setVisibility(View.GONE);
            }
            //holder.mCompass.setTag(mItems[position]);
                   // TODO disappear this if there is no predictive data for the route
            if(item.predicTimes != null) {
                holder.mETA.setText(item.predicTimes);
            } else {
                holder.mETA.setText("");
            }

        }
    }

    boolean setOneWay(SQLiteDatabase mDB, String DBTable, String routeId) {
        if(DatabaseUtils.queryNumEntries(
                mDB, DBTable, RouteExpandableAdapter.stopsSubwayWhereClause + "'" + routeId + "'",
                null) == 0) {
            //This is a one way route
            return true;
        } else {
            return false;
        }
    }

    public final static String[] mSchProjection = new String[] {
            DBHelper.KEY_STOPID, DBHelper.KEY_SCH_TIME, DBHelper.PRED_TIME, DBHelper.KEY_PREAWAY, DBHelper.KEY_DIR_ID
    };

    public class StopViewHolder extends RecyclerView.ViewHolder {
        public final TextView mStopDescription;
        public final TextView mETA;
        public final ImageButton mAlertBtn;
        public final View mCompass;

        public StopViewHolder(View itemView) {
            super(itemView);
            mStopDescription = (TextView) itemView.findViewById(R.id.stop_desc);
            mETA = (TextView) itemView.findViewById(R.id.stop_eta);
            mCompass = itemView.findViewById(R.id.stop_mapbtn);
            mAlertBtn = (ImageButton) itemView.findViewById(R.id.stop_alert_btn);
            mStopDescription.setTag(itemView);
        }
    }

    public final static String[] mStopProjection = new String[] {
            DBHelper.KEY_STOPNM, DBHelper.KEY_STOPID, DBHelper.KEY_STOPLN, DBHelper.KEY_STOPLT, DBHelper.KEY_ALERT_ID
    };

    public static class StopData {
        public String stopName; //to display
        public String stopId; //to check for alerts
        public String stopLat, stopLong; //to open Map
        public String stopAlert = null;
        public String predicTimes;
    }

    /**
     * This method will help instantiate the route fragment from a db query of the route name
     * @return the list of stop data to be displayed
     */
    public StopData makeStop(Cursor stopNameCursor, Cursor stopTimes) {
        final StopData data = new StopData();
        data.stopName = stopNameCursor.getString(0);
        data.stopId = stopNameCursor.getString(1);
        data.stopLong = stopNameCursor.getString(2);
        data.stopLat = stopNameCursor.getString(3);
        data.stopAlert = stopNameCursor.getString(4);
        data.predicTimes = getTimes(data.stopId, stopTimes);
        //Log.d(TAG, data.stopId + " predictimes? " + data.predicTimes);
        return data;
    }

    public String getTimes(String stopId, Cursor stopTimesCursor) {
        if(stopTimesCursor.moveToFirst()) {
            String value;
            StringBuilder str = new StringBuilder("Timing:\n");
            do {
                if(stopTimesCursor.getString(0).equals(stopId)
                        && mDirectionId == stopTimesCursor.getInt(4)) {
                    value = stopTimesCursor.getString(1);
                    if(value != null && !value.isEmpty()) {
                        str.append(value).append("(scheduled)\n");
                    }
                    str.append(stopTimesCursor.getString(2));
                    value = stopTimesCursor.getString(3);
                    if(value != null && !value.isEmpty()) {
                        str.append(" in ").append(value).append("  ");
                    }
                    str.append("\n");
                }

            } while(stopTimesCursor.moveToNext());
            return str.toString();
        }
        Log.e(TAG, "no data in times cursor: " + stopId);
        return "times not available";
    }
}
