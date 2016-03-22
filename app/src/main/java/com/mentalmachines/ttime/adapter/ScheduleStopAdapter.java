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

import java.util.ArrayList;

/**
 * Created by emezias on 1/20/16.
 * This is a schedule adapter to show the full day of times at each stop of a route
 * Inbound and Outbound are combined (to start)
 */

public class ScheduleStopAdapter extends RecyclerView.Adapter<ScheduleStopAdapter.ScheduleViewHolder> {
    public static final String TAG = "SimpleStopAdapter";
    public Schedule mSchedule;
    StringBuilder builder = new StringBuilder(8);

    public ScheduleStopAdapter(Schedule s) {
        super();
        mSchedule = s;
        Log.d(TAG, "size checks? " + mSchedule.TripsInbound.length + ":" + mSchedule.TripsOutbound.length);
        Log.d(TAG, "route checks? " + mSchedule.route.mInboundStops.size() + ":" + mSchedule.route.mOutboundStops.size());
    }

    @Override
    public int getItemCount() {
        if(mSchedule == null) {
            Log.e(TAG, "schedule object empty");
            return 0;
        }
        return mSchedule.route.mInboundStops.size() + mSchedule.route.mOutboundStops.size();
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
        if(mSchedule == null) {
            s = null;
        } else if(position < mSchedule.TripsInbound.length) {
            s = mSchedule.TripsInbound[position];
        } else {
            position -= mSchedule.TripsInbound.length;
            s = mSchedule.TripsOutbound[position];
        }
        if(s == null) {
            Log.e(TAG, "null stopTimes! " + position);
            return;
        }
        if(s.morning != null && s.morning.size() > 0) {
            holder.mMorning.setText(enumerateTimes(s.morning));
        } else {
            holder.mMorning.setText("");
        }

        if(s.amPeak != null && s.amPeak.size() > 0) {
            holder.mAMPeak.setText(enumerateTimes(s.amPeak));
        } else {
            holder.mAMPeak.setText("");
        }

        if(s.midday != null && s.midday.size() > 0) {
            holder.mMidday.setText(enumerateTimes(s.midday));
        } else {
            holder.mMidday.setText("");
        }

        if(s.pmPeak != null && s.pmPeak.size() > 0) {
            holder.mPMPeak.setText(enumerateTimes(s.pmPeak));
        } else {
            holder.mPMPeak.setText("");
        }

        if(s.night != null && s.night.size() > 0) {
            holder.mNight.setText(enumerateTimes(s.night));
        } else {
            holder.mNight.setText("");
        }
    }

    String enumerateTimes(ArrayList<String> interval) {
        builder.setLength(0);
        for(int dex = 0; dex < interval.size()-1; dex++) {
            builder.append(interval.get(dex)).append(", ");
        }
        builder.append(interval.get(interval.size()-1));
        return builder.toString();
    }

    public class ScheduleViewHolder extends RecyclerView.ViewHolder {
        public final TextView mMorning, mAMPeak, mMidday, mPMPeak, mNight;
        //set any tags?
        public ScheduleViewHolder(View itemView) {
            super(itemView);
            mMorning = (TextView) itemView.findViewById(R.id.sch_morning);
            mAMPeak = (TextView) itemView.findViewById(R.id.sch_ampeak);
            mMidday = (TextView) itemView.findViewById(R.id.sch_midday);
            mPMPeak = (TextView) itemView.findViewById(R.id.sch_pmpeak);
            mNight = (TextView) itemView.findViewById(R.id.sch_night);
        }
    }
}
