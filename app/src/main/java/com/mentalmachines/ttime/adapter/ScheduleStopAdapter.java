package com.mentalmachines.ttime.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.objects.Schedule;
import com.mentalmachines.ttime.objects.Schedule.StopTimes;
import com.mentalmachines.ttime.services.SaveScheduleService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

/**
 * Created by emezias on 1/20/16.
 * This is a schedule adapter to show the full day of times at each stop of a route
 * Inbound and Outbound are combined (to start)
 */

public class ScheduleStopAdapter extends RecyclerView.Adapter<ScheduleStopAdapter.ScheduleViewHolder> {
    public static final String TAG = "ScheduleStopAdapter";
    public Schedule mSchedule;
    private boolean isInbound = true;
    StringBuilder builder = new StringBuilder(8);

    //Constructor creates inbound by default, can switch to outbound
    public ScheduleStopAdapter(Schedule s) {
        super();
        mSchedule = s;
        if(mSchedule.route == null) {
            //debug?
            Log.e(TAG, "route must be set on the Schedule");
            return;
        }
        if(mSchedule.route != null && mSchedule.route.mInboundStops == null) {
            isInbound = false;
            if(mSchedule.route.mOutboundStops == null) {
                Log.e(TAG, "route has no stops!");
                mSchedule.route.mOutboundStops = new ArrayList<>(0);
            }
        }
        //Log.d(TAG, "size checks? " + mSchedule.TripsInbound.length + ":" + mSchedule.TripsOutbound.length);
        //Log.d(TAG, "route checks? " + mSchedule.route.mInboundStops.size() + ":" + mSchedule.route.mOutboundStops.size());
    }

    @Override
    public int getItemCount() {
        if(mSchedule == null) {
            Log.w(TAG, "schedule object empty");
            return 0;
        }
        if(isInbound) {
            return mSchedule.route.mInboundStops.size();
        }
        return mSchedule.route.mOutboundStops.size();
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
        StopTimes s;
        String name ="";
        if(mSchedule == null) {
            s = null;
        } else if(isInbound) {
            //inbound is showing
            if(position < mSchedule.TripsInbound.length) {
                s = mSchedule.TripsInbound[position];
                name = mSchedule.route.mInboundStops.get(position).stopName;
            } else {
                Log.w(TAG, "position out of bounds");
                return;
            }
        } else {
            //outbound stops showing
            if(position < mSchedule.TripsOutbound.length) {
                s = mSchedule.TripsOutbound[position];
                name = mSchedule.route.mOutboundStops.get(position).stopName;
            } else {
                Log.w(TAG, "position out of bounds");
                return;
            }
        }

        if(s == null) {
            Log.e(TAG, "null stopTimes! " + position);
            return;
        }
        Log.i(TAG, "stop times: " + s.morning.size() +":" + s.amPeak.size() + ":" + s.midday.size() + ":" + s.pmPeak.size() + ":" + s.night.size());
        holder.mDir.setText(name);
        if(s.morning.size() > 0) {
            holder.mMorning.setText(enumerateTimes(s.morning));
        } else {
            holder.mMorning.setText("");
        }

        if(s.amPeak.size() > 0) {
            holder.mAMPeak.setText(enumerateTimes(s.amPeak));
        } else {
            holder.mAMPeak.setText("");
        }

        if(s.midday.size() > 0) {
            holder.mMidday.setText(enumerateTimes(s.midday));
        } else {
            holder.mMidday.setText("");
        }

        if(s.pmPeak.size() > 0) {
            holder.mPMPeak.setText(enumerateTimes(s.pmPeak));
        } else {
            holder.mPMPeak.setText("");
        }

        if(s.night.size() > 0) {
            holder.mNight.setText(enumerateTimes(s.night));
        } else {
            holder.mNight.setText("");
        }
    }

    public boolean switchDirection() {
        isInbound = !isInbound;
        notifyDataSetChanged();
        return isInbound;
    }

    final Calendar cal = Calendar.getInstance();

    String enumerateTimes(ArrayList<Long> interval) {
        int hour, next;
        String tmp;
        Collections.sort(interval);
        builder.setLength(0);
        cal.setTimeInMillis(interval.get(0));
        tmp = SaveScheduleService.timeFormat.format(cal.getTime());
        hour = Integer.valueOf(tmp.substring(0, tmp.indexOf(":")));
        builder.append(tmp);
        for(int dex = 1; dex < interval.size(); dex++) {
            cal.setTimeInMillis(interval.get(dex));
            tmp = SaveScheduleService.timeFormat.format(cal.getTime());
            next = Integer.valueOf(tmp.substring(0, tmp.indexOf(":")));
            //start a new line with an hour change
            if(next != hour) {
                hour = next;
                builder.append("\n").append(tmp);
            } else {
                builder.append(",  ").append(tmp);
            }
        }
        return builder.toString();
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
}
