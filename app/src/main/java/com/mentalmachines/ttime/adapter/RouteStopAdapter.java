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
import com.mentalmachines.ttime.objects.Utils;

/**
 * Created by emezias on 1/20/16.
 * This is a simple adapter to fill in the stop,
 * scheduled time and estimated next arrival
 */

public class RouteStopAdapter extends RecyclerView.Adapter<RouteStopAdapter.StopViewHolder> {
    public static final String TAG = "RouteStopAdapter";
    public Route mRoute;
    public int mDirectionId;
    final public boolean isOneWay;
    static String SCHED, ACTUAL, NODATA;

    //public RouteStopAdapter(String[] data, int resource) {
    public RouteStopAdapter(Route r, boolean inbound) {
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
        NearbyLVAdapter.colorView(parent.getResources(), view.findViewById(R.id.divider), mRoute.name);
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
        if (position == 0) holder.mDivider.setVisibility(View.GONE);
        else holder.mDivider.setVisibility(View.VISIBLE);
    }

    void setView(StopViewHolder holder, StopData s, int position) {
        if(s == null) {
            Log.w(TAG, "null stop " + position);
            holder.mStopDescription.setText("");
            holder.mETA.setText("");
            holder.mStopDetail.setVisibility(View.GONE);
        } else {
            if(TextUtils.isEmpty(s.stopAlert)) {
                holder.mStopDetail.setImageResource(android.R.drawable.ic_menu_compass);
            } else {
                holder.mStopDetail.setImageResource(R.drawable.ic_alert_light);
            }
            holder.mStopDescription.setText(s.stopName + "\n" + SCHED + " "
                    + Utils.trimStopTimes(NODATA, s));
            holder.mETA.setText(Utils.setPredictions(ACTUAL, s));

        }
    }

    public class StopViewHolder extends RecyclerView.ViewHolder {
        public final TextView mStopDescription;
        public final TextView mETA;
        public final ImageButton mStopDetail;
        public final View mDivider;
        //public final View mCompass;
        //set a tag on the parent view for the two buttons to read
        public StopViewHolder(View itemView) {
            super(itemView);
            mDivider = itemView.findViewById(R.id.divider);
            mStopDescription = (TextView) itemView.findViewById(R.id.stop_desc);
            mETA = (TextView) itemView.findViewById(R.id.stop_eta);
            //mCompass = itemView.findViewById(R.id.stop_detail_btn);
            mStopDetail = (ImageButton) itemView.findViewById(R.id.stop_detail_btn);
            mStopDescription.setTag(itemView);
            if(SCHED == null) {
                final Context ctx = itemView.getContext();
                SCHED = ctx.getString(R.string.scheduled);
                ACTUAL = ctx.getString(R.string.actual);
                NODATA = ctx.getString(R.string.stopEmptyString);
            }
        }
    }
}
