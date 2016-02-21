package com.mentalmachines.ttime;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by emezias on 1/20/16.
 * This is a simple adapter to fill in the stop,
 * scheduled time and estimated next arrival
 */

public class SimpleStopAdapter extends RecyclerView.Adapter<SimpleStopAdapter.StopViewHolder> {
    public static final String TAG = "SimpleStopAdapter";
    final StopData[] mItems;
    final int mTextColor;
    static Drawable alertDrawable;
    //final int drawableResource;

    //public SimpleStopAdapter(String[] data, int resource) {
    public SimpleStopAdapter(StopData[] data, int textColor) {
        super();
        mItems = data;
        mTextColor = textColor;
        //drawableResource = resource = -1;
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
        if(alertDrawable == null) {
            alertDrawable = parent.getContext().getResources().getDrawable(android.R.drawable.ic_dialog_alert);
            alertDrawable.setBounds(0, 0, alertDrawable.getIntrinsicWidth(), alertDrawable.getIntrinsicHeight());
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.t_stop, parent, false);
        if(mTextColor > 0) {
            ((TextView) view.findViewById(R.id.stop_desc)).setTextColor(mTextColor);
        }

        return new StopViewHolder(view);
    }

    //all text is dummy data to format the display on the screen
    @Override
    public void onBindViewHolder(final StopViewHolder holder, int position) {
        if(mItems == null || mItems.length < position) {
            Log.w(TAG, "bad position sent to adapter " + position);
            holder.mStopDescription.setText("");
        } else {
            holder.mStopDescription.setText(mItems[position].stopName);
            if(mItems[position].stopAlert != null) {
                holder.mStopDescription.setCompoundDrawables(
                    alertDrawable, null, null, null );
            }
                   // + "\nNext scheduled times: ? and ?");
            holder.mETA.setText("Actual time: ?, in ? minutes and ? in ? minutes");
        }
    }

    public class StopViewHolder extends RecyclerView.ViewHolder {
        public final TextView mStopDescription;
        public final TextView mETA;

        public StopViewHolder(View itemView) {
            super(itemView);
            mStopDescription = (TextView) itemView.findViewById(R.id.stop_desc);
            mETA = (TextView) itemView.findViewById(R.id.stop_eta);
        }
    }

    public final static String[] mStopProjection = new String[] {
            DBHelper.KEY_STOPNM, DBHelper.KEY_STOPID, DBHelper.KEY_STOPLN, DBHelper.KEY_STOPLT, DBHelper.KEY_ALERT_ID
    };

    public static class StopData {
        String stopName; //to display
        String stopId; //to check for alerts
        String stopLat, stopLong; //to open Map
        String stopAlert = null;
    }

    /**
     * This method will help instantiate the route fragment from a db query of the route name
     * @param c - query reslut
     * @return the list of stop data to be displayed
     */
    public static StopData[] makeStopsList(Cursor c) {
        if(c.getCount() > 0 && c.moveToFirst()) {
            final StopData[] tmp = new StopData[c.getCount()];
            for(int dex = 0; dex < tmp.length; dex++) {
                tmp[dex].stopName = c.getString(0);
                tmp[dex].stopId = c.getString(1);
                tmp[dex].stopLong = c.getString(2);
                tmp[dex].stopLat = c.getString(3);
                tmp[dex].stopAlert = c.getString(4);
                c.moveToNext();
            }
            return tmp;
        }
        Log.w(TAG, "bad cursor, no array");
        return null;
    }
}
