package com.mentalmachines.ttime.adapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.services.GetMBTARequestService;

import java.util.HashMap;

/**
 * Created by emezias on 1/20/16.
 * This is a simple adapter to fill in the stop,
 * scheduled time and estimated next arrival
 */

public class SimpleStopAdapter extends RecyclerView.Adapter<SimpleStopAdapter.StopViewHolder> {
    public static final String TAG = "SimpleStopAdapter";
    final public StopData[] mItems;
    final int mDirectionId, mTextColor;
    static HashMap<String, String> mTimeMap = null;
    boolean timesReady = false;

    //public SimpleStopAdapter(String[] data, int resource) {
    public SimpleStopAdapter(Context ctx, StopData[] data, int textColor, int direction) {
        super();
        mDirectionId = direction;
        mItems = data;
        mTextColor = textColor;
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
        if(mTextColor > 0) {
            ((TextView) view.findViewById(R.id.stop_desc)).setTextColor(mTextColor);
        }
        return new StopViewHolder(view);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
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
            ((View) holder.mStopDescription.getTag()).setTag(mItems[position]);
            holder.mStopDescription.setText(mItems[position].stopName);
            if(mItems[position].stopAlert != null) {
                holder.mAlertBtn.setVisibility(View.VISIBLE);
                holder.mAlertBtn.setTag(mItems[position].stopAlert);
            } else {
                holder.mAlertBtn.setVisibility(View.GONE);
            }
            //holder.mCompass.setTag(mItems[position]);
                   // TODO disappear this if there is no predictive data for the route
            if(mTimeMap != null) {
                holder.mETA.setText(mTimeMap.get(mItems[position].stopId));
                Log.i(TAG, "set text? " + mItems[position].stopId + ": " + mTimeMap.get(mItems[position].stopId) );
            } else {
                holder.mETA.setText("getting schedule data...");
            }

        }
    }

    public final static String[] mSchProjection = new String[] {
            DBHelper.KEY_STOPID, DBHelper.KEY_SCH_TIME, DBHelper.PRED_TIME, DBHelper.KEY_PREAWAY
    };


    /**
     * This method sets up the times to show in the main activity at each stop on a route
     * @param context
     * @param direction, inbound = 1 outbound = 0
     */
    public static void pullSchedule(Context context, String direction) {
        Log.i(TAG, "pull schedule");
        mTimeMap = null;
        final SQLiteDatabase db = new DBHelper(context).getReadableDatabase();
        final Cursor c = db.query(DBHelper.DB_TABLE_PREDICTION,
                mSchProjection,
                DBHelper.KEY_DIR_ID + " like "+ direction,
                null, null, null, DBHelper.KEY_STOPID);
        // use group by here? is that faster?
        if(c.moveToFirst()) {
            String stopId = "";
            final HashMap<String, String> tmp = new HashMap<>();
            final StringBuilder schTimes = new StringBuilder(0);
            final StringBuilder predTimes = new StringBuilder(0);
            do {
                if(stopId == c.getString(0)) {
                    if(c.getString(1) != null) {
                        schTimes.append(c.getString(1)).append(" ");
                    }
                    predTimes.append(c.getString(2)).append(" in ").append(c.getString(3)).append("  ");
                    Log.i(TAG, "schedule time data?" + c.getString(2) + " in " + c.getString(3));
                } else {
                    if(!stopId.isEmpty()) {
                        //first time through...
                        if(schTimes.length() > 20) {
                            tmp.put(stopId, schTimes.toString() + "/n" + predTimes.toString());
                        } else {
                            tmp.put(stopId, predTimes.toString());
                        }
                    }
                    stopId = c.getString(0);
                    Log.i(TAG, "reading times " + stopId);
                    schTimes.setLength(0);
                    schTimes.append("Schedule Times: ");
                    predTimes.setLength(0);
                    predTimes.append("Actual: ");
                    if(c.getString(1) != null) {
                        schTimes.append(c.getString(1)).append(" ");
                    }
                    predTimes.append(c.getString(2)).append(" in ").append(c.getString(3)).append("  ");
                    Log.i(TAG, "schedule time data?" + c.getString(2) + " in " + c.getString(3));
                }

            } while (c.moveToNext());
            //end while, hash map is build
            c.close();
            db.close();
            predTimes.setLength(0);
            schTimes.setLength(0);
            mTimeMap = tmp;
            Log.i(TAG, "schedule time data ready");
        } else {
            Log.w(TAG, "cursor cannot move to first");
        }
    }

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
    }

    /**
     * This method will help instantiate the route fragment from a db query of the route name
     * @param c - query reslut
     * @return the list of stop data to be displayed
     */
    public static StopData[] makeStopsList(Cursor c) {
        if(c.getCount() > 0 && c.moveToFirst()) {
            final StopData[] tmp = new StopData[c.getCount()];
            StopData data;
            for(int dex = 0; dex < tmp.length; dex++) {
                data = new StopData();
                data.stopName = c.getString(0);
                data.stopId = c.getString(1);
                data.stopLong = c.getString(2);
                data.stopLat = c.getString(3);
                data.stopAlert = c.getString(4);
                tmp[dex] = data;
                c.moveToNext();
            }
            return tmp;
        }
        Log.w(TAG, "bad cursor, no array");
        return null;
    }
}
