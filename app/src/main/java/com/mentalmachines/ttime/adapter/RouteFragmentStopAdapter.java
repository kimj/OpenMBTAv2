package com.mentalmachines.ttime.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.StopData;

/**
 * Created by emezias on 1/20/16.
 * This is a simple adapter to fill in the stop,
 * scheduled time and estimated next arrival
 */

public class RouteFragmentStopAdapter extends RecyclerView.Adapter<RouteFragmentStopAdapter.StopViewHolder> {
    public static final String TAG = "RouteFragmentStopAdapter";
    public Route mRoute;
    public int mDirectionId;
    final public boolean isOneWay;
    static String SCHED, ACTUAL, NODATA;

    //public RouteFragmentStopAdapter(String[] data, int resource) {
    public RouteFragmentStopAdapter(Route r, boolean inbound) {
        super();
        if(inbound) {
            mDirectionId = 1;
        } else {
            mDirectionId = 0;
        }

        mRoute = r;
        isOneWay = mRoute.mInboundStops == null || mRoute.mOutboundStops == null
                || mRoute.mInboundStops.size() == 0 || mRoute.mOutboundStops.size() == 0;
    }

    @Override
    public int getItemCount() {
        if(mRoute == null) {
            Log.e(TAG, "route object empty");
            return 0;
        }
        if(mDirectionId == 0) {
            return mRoute.mOutboundStops.size();
        } else {
            return mRoute.mInboundStops.size();
        }
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public StopViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
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
        StopData s = null;
        if(mDirectionId == 0 && position < mRoute.mOutboundStops.size()) {
            s = mRoute.mOutboundStops.get(position);
        }

        if(mDirectionId == 1 && position < mRoute.mInboundStops.size()) {
            s = mRoute.mInboundStops.get(position);
        }
        ((View) holder.mStopDescription.getTag()).setTag(s);
        //put the stop object on the view as a tag for onClick methods to use
        setView(holder, s, position);
    }

    void setView(StopViewHolder holder, StopData s, int position) {
        if(s == null) {
            Log.w(TAG, "null stop " + position);
            holder.mStopDescription.setText("");
            holder.mETA.setText("");
            holder.mAlertBtn.setVisibility(View.GONE);
        } else {
            if(s.stopAlert != null) {
                holder.mAlertBtn.setVisibility(View.VISIBLE);
            } else {
                holder.mAlertBtn.setVisibility(View.GONE);
            }
            holder.mAlertBtn.invalidate();

            if(!TextUtils.isEmpty(s.schedTimes)) {
                //s.schedTimes is not empty
                holder.mStopDescription.setText(s.stopName + "\n" + SCHED + " " + s.schedTimes);
            } else if(TextUtils.isEmpty(s.predicTimes)){
                //both are empty
                holder.mStopDescription.setText(s.stopName + "\n" + NODATA);
                holder.mETA.setText("");
            } else {
                holder.mStopDescription.setText(s.stopName);
            }

            if(!TextUtils.isEmpty(s.predicTimes)) {
                holder.mETA.setText(ACTUAL + " " + s.predicTimes);
            }
        }
    }

    public class StopViewHolder extends RecyclerView.ViewHolder {
        public final TextView mStopDescription;
        public final TextView mETA;
        public final ImageButton mAlertBtn;
        public final View mCompass;
        //set a tag on the parent view for the two buttons to read
        public StopViewHolder(View itemView) {
            super(itemView);
            mStopDescription = (TextView) itemView.findViewById(R.id.stop_desc);
            mETA = (TextView) itemView.findViewById(R.id.stop_eta);
            mCompass = itemView.findViewById(R.id.stop_mapbtn);
            mAlertBtn = (ImageButton) itemView.findViewById(R.id.stop_alert_btn);
            mStopDescription.setTag(itemView);
            if(SCHED == null) {
                final Context ctx = itemView.getContext();
                SCHED = ctx.getString(R.string.scheduled);
                ACTUAL = ctx.getString(R.string.actual);
                NODATA = ctx.getString(R.string.noSched);
            }
        }
    }
}
