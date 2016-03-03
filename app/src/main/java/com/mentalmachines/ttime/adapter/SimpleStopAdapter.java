package com.mentalmachines.ttime.adapter;

import android.support.v7.widget.RecyclerView;
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

public class SimpleStopAdapter extends RecyclerView.Adapter<SimpleStopAdapter.StopViewHolder> {
    public static final String TAG = "SimpleStopAdapter";
    public static Route mRoute;
    public int mDirectionId;
    final public boolean isOneWay;

    //public SimpleStopAdapter(String[] data, int resource) {
    public SimpleStopAdapter(Route r, int direction) {
        super();
        mDirectionId = direction;
        mRoute = r;
        if(mRoute.mInboundStops == null || mRoute.mOutboundStops == null
                || mRoute.mInboundStops.size() == 0 || mRoute.mOutboundStops.size() == 0) {
            isOneWay = true;
        } else {
            isOneWay = false;
        }
    }

    /**
     * Reset list, invalidate, redraw the recycler view
     * @param dir
     */
    public void changeDirection(int dir) {
        mDirectionId = dir;
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
        StopData s;
        if(mRoute == null) {
            s = null;
        } else if(mDirectionId == 0 && position < mRoute.mOutboundStops.size()) {
            s = mRoute.mOutboundStops.get(position);
        } else if(mDirectionId == 1 && position < mRoute.mInboundStops.size()) {
            s = mRoute.mInboundStops.get(position);
        } else {
            s = null;
        }
        ((View) holder.mStopDescription.getTag()).setTag(s);
        setView(holder, s);
    }

    void setView(StopViewHolder holder, StopData s) {
        if(s == null) {
            holder.mStopDescription.setText("");
            holder.mETA.setText("");
            holder.mAlertBtn.setVisibility(View.GONE);
        } else {
            holder.mStopDescription.setText(s.stopName);
            if(s.schedTimes == null || s.schedTimes.isEmpty()) {
                holder.mETA.setText(s.predicTimes);
            } else {
                holder.mETA.setText(s.schedTimes + "\n" + s.predicTimes);
            }
            if(s.stopAlert != null) {
                holder.mAlertBtn.setVisibility(View.VISIBLE);
                holder.mAlertBtn.setTag(s.stopAlert);
            } else {
                holder.mAlertBtn.setVisibility(View.GONE);
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
        }
    }
}
