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
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.objects.Utils;

/**
 * Created by emezias on 1/20/16.
 * This is a simple adapter to fill in the stop,
 * scheduled time and estimated next arrival
 */

public class StopDetailAdapter extends RecyclerView.Adapter<StopDetailAdapter.StopViewHolder> {
    public static final String TAG = "StopDetailAdapter";
    final StopData[] items;

    public StopDetailAdapter(StopData[] stopList) {
        super();
        items = stopList;
    }


    @Override
    public int getItemCount() {
        if(items == null) return 0;
        return items.length;
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
        if(items == null || position >= items.length) {
            holder.mStopDescription.setText("");
            holder.mETA.setText("");
            Log.w(TAG, "bad stop position: " + position);
        } else {
            final StopData s = items[position];
            final Context ctx = holder.mStopRoutename.getContext();
            holder.mStopRoutename.setText(s.stopName);
            //alert header text gets set in the alert field instead of the alert id
            if(TextUtils.isEmpty(s.schedTimes)) {
                holder.mStopDescription.setText("");
            } else {
                holder.mStopDescription.setText(Utils.trimStopTimes(ctx, s.schedTimes));
            }
            if(TextUtils.isEmpty(s.predicTimes)) {
                holder.mETA.setText("");
            } else {
                holder.mETA.setText(Utils.trimStopTimes(ctx, s.predicTimes));
            }

            //TODO create new layout
            if(!TextUtils.isEmpty(s.stopAlert)) {
                holder.mAlertBtn.setVisibility(View.VISIBLE);
                holder.mAlertBtn.setTag(s.stopAlert);
            } else {
                holder.mAlertBtn.setVisibility(View.GONE);
                holder.mAlertBtn.invalidate();
            }
        }
    }

    public class StopViewHolder extends RecyclerView.ViewHolder {
        public final TextView mStopRoutename;
        public final TextView mStopDescription;
        public final TextView mETA;
        public final ImageButton mAlertBtn;
        //public final View mCompass;
        //set a tag on the parent view for the two buttons to read
        public StopViewHolder(View itemView) {
            super(itemView);
            mStopRoutename = (TextView) itemView.findViewById(R.id.stop_route);
            mStopDescription = (TextView) itemView.findViewById(R.id.stop_desc);
            mETA = (TextView) itemView.findViewById(R.id.stop_eta);
            mAlertBtn = (ImageButton) itemView.findViewById(R.id.stop_detail_btn);
            mAlertBtn.setImageResource(R.drawable.btn_stop_alert);
        }
    }
}
