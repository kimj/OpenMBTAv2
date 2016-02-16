package com.mentalmachines.ttime;

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
    final String[] mItems;
    final int mTextColor;
    //final int drawableResource;

    //public SimpleStopAdapter(String[] data, int resource) {
    public SimpleStopAdapter(String[] data, int textColor) {
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
            holder.mStopDescription.setText(mItems[position]
                    + "\nNext scheduled times: ? and ?");
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

}
