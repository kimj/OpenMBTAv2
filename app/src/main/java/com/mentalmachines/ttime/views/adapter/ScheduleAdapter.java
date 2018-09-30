package com.mentalmachines.ttime.views.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.data.model.Route;
import com.mentalmachines.ttime.data.model.StopData;
import com.mentalmachines.ttime.Utils;
import com.mentalmachines.ttime.services.GetScheduleService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

/**
 * Created by emezias on 1/20/16.
 * This is a schedule adapter to show the full day of times at each stop of a route
 * Schedule data is held in a hashmap of longs (stop objects)
 */

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {
    public static final String TAG = "ScheduleAdapter";
    final Route mRoute;
    final int mScheduleDay;
    private boolean mIsInbound = true;
    Calendar mCal = Calendar.getInstance();
    final long midnight;

    //Constructor creates inbound by default, can switch to outbound
    public ScheduleAdapter(int scheduleDay, Route route) {
        super();
        mScheduleDay = scheduleDay;
        mRoute = route;
        //The day never shows in the adapter - TODO, skip
        mCal = GetScheduleService.setDay(mCal, scheduleDay);
        midnight = mCal.getTimeInMillis();
        for(StopData data: mRoute.mOutboundStops){
            Log.d(TAG, "out count: " + data.getScheduleArray(mScheduleDay).size());
        }
        for(StopData data: mRoute.mInboundStops){
            Log.d(TAG, "inbound: " + data.getScheduleArray(mScheduleDay).size());
        }
    }

    @Override
    public int getItemCount() {
        if(mRoute == null) {
            Log.e(TAG, "route object empty");
            return 0;
        }
        if(mIsInbound) {
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
    public ScheduleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sched_stop, parent, false);
        return new ScheduleViewHolder(view);
    }

    /**
     * Map a stop in mItems to a recycler view entry on the list
     * The mStopDescription view has a tag holding the parent view group
     * @param holder, viewholder, required for recycler view
     * @param position, the position in the list
     */
    @Override
    public void onBindViewHolder(final ScheduleViewHolder holder, int position) {
        StopData s;
        ArrayList<Long> timestamps = null;
        if(mRoute == null) {
            s = null;
        } else if(mIsInbound) {
            //inbound is showing
            if(position < mRoute.mInboundStops.size()) {
                s = mRoute.mInboundStops.get(position);
            } else {
                Log.w(TAG, "position out of bounds");
                return;
            }
        } else {
            //outbound stops showing
            if(position < mRoute.mOutboundStops.size()) {
                s = mRoute.mOutboundStops.get(position);
            } else {
                Log.w(TAG, "position out of bounds");
                return;
            }
        }

        if(s == null) {
            Log.e(TAG, "Error finding stop in route: " + position);
            return;
        }
        timestamps = s.getScheduleArray(mScheduleDay);
        Collections.sort(timestamps);
        holder.mDir.setText(s.stopName);
        Log.w(TAG, "loading times for stop:" + s.stopName);
        enumerateTimes(timestamps, holder);
    }

    void enumerateTimes(ArrayList<Long> stopTimes, ScheduleViewHolder holder) {
        if(stopTimes.size() == 0) {
            holder.mMorning.setText("");
            holder.mAMPeak.setText("");
            holder.mMidday.setText("");
            holder.mPMPeak.setText("");
            holder.mNight.setText("");
            Log.w(TAG, "empty array, no times");
            return;
        }
        final int sz = stopTimes.size();
        final StringBuilder builder = new StringBuilder(sz);
        Collections.sort(stopTimes);
        long timestamp, limit;
        int dex, curr_hr = -1;
        //TODO - is this necessary? The day never shows
        mCal = GetScheduleService.setDay(mCal, mScheduleDay);
        mCal.set(Calendar.HOUR, 6);
        mCal.set(Calendar.MINUTE, 30);
        mCal.set(Calendar.AM_PM, Calendar.AM);
        limit = mCal.getTimeInMillis();
        Log.d(TAG, "setting morning limit " + Utils.timeFormat.format(mCal.getTime()));

        for(dex = 0; dex < sz; dex++) {
            timestamp = stopTimes.get(dex);
            mCal.setTimeInMillis(timestamp);
            if(dex == 0) {
                Log.d(TAG, sz + " first in array " + Utils.timeFormat.format(mCal.getTime()));
            }
            if(timestamp <= limit) {
                if (curr_hr != mCal.get(Calendar.HOUR_OF_DAY)) {
                    curr_hr = mCal.get(Calendar.HOUR_OF_DAY);
                    builder.append("\n").append(Utils.timeFormat.format(mCal.getTime()));
                } else {
                    builder.append(",  ").append(Utils.timeFormat.format(mCal.getTime()));
                }
            } else {
                Log.d(TAG, "> limit " + Utils.timeFormat.format(mCal.getTime()));
                break;
            }
        }

        holder.mMorning.setText(builder.toString());
        builder.setLength(0);
        mCal.set(Calendar.HOUR, 9);
        mCal.set(Calendar.MINUTE, 0);
        mCal.set(Calendar.AM_PM, Calendar.AM);
        limit = mCal.getTimeInMillis();

        for(; dex < sz; dex++) {
            timestamp = stopTimes.get(dex);
            mCal.setTimeInMillis(timestamp);
            if(builder.length() == 0) {
                curr_hr = mCal.get(Calendar.HOUR_OF_DAY);
                builder.append(Utils.timeFormat.format(mCal.getTime()));
            } else if (timestamp <= limit) {
                if (curr_hr != mCal.get(Calendar.HOUR_OF_DAY)) {
                    curr_hr = mCal.get(Calendar.HOUR_OF_DAY);
                    builder.append("\n").append(Utils.timeFormat.format(mCal.getTime()));
                } else {
                    builder.append(",  ").append(Utils.timeFormat.format(mCal.getTime()));
                }
            } else {
                Log.d(TAG, "> limit " + Utils.timeFormat.format(mCal.getTime()));
                break;
            }
        }
        holder.mAMPeak.setText(builder.toString());
        builder.setLength(0);
        mCal.set(Calendar.HOUR, 3);
        mCal.set(Calendar.MINUTE, 30);
        mCal.set(Calendar.AM_PM, Calendar.PM);
        limit = mCal.getTimeInMillis();

        for(; dex < sz; dex++) {
            timestamp = stopTimes.get(dex);
            mCal.setTimeInMillis(timestamp);
            if(builder.length() == 0) {
                curr_hr = mCal.get(Calendar.HOUR_OF_DAY);
                builder.append(Utils.timeFormat.format(mCal.getTime()));
            } else if (timestamp <= limit) {
                if (curr_hr != mCal.get(Calendar.HOUR_OF_DAY)) {
                    curr_hr = mCal.get(Calendar.HOUR_OF_DAY);
                    builder.append("\n").append(Utils.timeFormat.format(mCal.getTime()));
                } else {
                    builder.append(",  ").append(Utils.timeFormat.format(mCal.getTime()));
                }
            } else {
                Log.d(TAG, "> midday limit " + Utils.timeFormat.format(mCal.getTime()));
                break;
            }
        }
        holder.mMidday.setText(builder.toString());
        builder.setLength(0);
        mCal.set(Calendar.HOUR, 6);
        mCal.set(Calendar.MINUTE, 30);
        mCal.set(Calendar.AM_PM, Calendar.PM);
        limit = mCal.getTimeInMillis();

        for(; dex < sz; dex++) {
            timestamp = stopTimes.get(dex);
            mCal.setTimeInMillis(timestamp);
            if(builder.length() == 0) {
                curr_hr = mCal.get(Calendar.HOUR_OF_DAY);
                builder.append(Utils.timeFormat.format(mCal.getTime()));
            } else if (timestamp <= limit) {
                if (curr_hr != mCal.get(Calendar.HOUR_OF_DAY)) {
                    curr_hr = mCal.get(Calendar.HOUR_OF_DAY);
                    builder.append("\n").append(Utils.timeFormat.format(mCal.getTime()));
                } else {
                    builder.append(",  ").append(Utils.timeFormat.format(mCal.getTime()));
                }
            } else {
                Log.d(TAG, "> pmpeak limit " + Utils.timeFormat.format(mCal.getTime()));
                break;
            }
        }
        holder.mPMPeak.setText(builder.toString());
        builder.setLength(0);
        mCal.set(Calendar.HOUR, 11);
        mCal.set(Calendar.MINUTE, 59);
        mCal.set(Calendar.AM_PM, Calendar.PM);
        mCal.set(Calendar.SECOND, 59);
        limit = mCal.getTimeInMillis();

        for(; dex < sz; dex++) {
            timestamp = stopTimes.get(dex);
            mCal.setTimeInMillis(timestamp);
            if(builder.length() == 0) {
                curr_hr = mCal.get(Calendar.HOUR_OF_DAY);
                builder.append(Utils.timeFormat.format(mCal.getTime()));
            } else if (timestamp <= limit) {
                if(builder.length() == 0) {
                    builder.append(Utils.timeFormat.format(mCal.getTime()));
                } else if(curr_hr != mCal.get(Calendar.HOUR_OF_DAY)) {
                    curr_hr = mCal.get(Calendar.HOUR_OF_DAY);
                    builder.append("\n").append(Utils.timeFormat.format(mCal.getTime()));
                } else {
                    builder.append(",  ").append(Utils.timeFormat.format(mCal.getTime()));
                }
            } else {
                Log.d(TAG, "> night limit " + Utils.timeFormat.format(mCal.getTime()));
                break;
            }
        }
        holder.mNight.setText(builder.toString());
        builder.setLength(0);
    }

    int runLoop(int index, int curr_hr, int sz,
                      ArrayList<Long> stopTimes, long limit, StringBuilder builder) {
        long timestamp;
        for(; index < sz; index++) {
            timestamp = stopTimes.get(index);
            mCal.setTimeInMillis(timestamp);
            if (timestamp <= limit) {
                if(builder.length() == 0) {
                    curr_hr = mCal.get(Calendar.HOUR_OF_DAY);
                    builder.append(Utils.timeFormat.format(mCal.getTime()));
                } else if(curr_hr != mCal.get(Calendar.HOUR_OF_DAY)) {
                    curr_hr = mCal.get(Calendar.HOUR_OF_DAY);
                    builder.append("\n").append(Utils.timeFormat.format(mCal.getTime()));
                } else {
                    builder.append(",  ").append(Utils.timeFormat.format(mCal.getTime()));
                }
            } else {
                Log.d(TAG, "> night limit " + Utils.timeFormat.format(mCal.getTime()));
                break;
            }
        }
        return index;
    }

    public class ScheduleViewHolder extends RecyclerView.ViewHolder {
        public final TextView mDir, mMorning, mAMPeak, mMidday, mPMPeak, mNight;
        //set any tags?
        public ScheduleViewHolder(View itemView) {
            super(itemView);
            mDir = (TextView) itemView.findViewById(R.id.sch_direction);
            mMorning = (TextView) itemView.findViewById(R.id.sch_morning);
            mAMPeak = (TextView) itemView.findViewById(R.id.sch_ampeak);
            mMidday = (TextView) itemView.findViewById(R.id.sch_midday);
            mPMPeak = (TextView) itemView.findViewById(R.id.sch_pmpeak);
            mNight = (TextView) itemView.findViewById(R.id.sch_night);
        }
    }

    public boolean switchDirection() {
        Log.d(TAG, "switch direction");
        mIsInbound = !mIsInbound;
        notifyDataSetChanged();
        return mIsInbound;
    }
}
