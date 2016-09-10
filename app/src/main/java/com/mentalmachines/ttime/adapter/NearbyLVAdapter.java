package com.mentalmachines.ttime.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.objects.Utils;

/**
 * Created by emezias on 5/14/16
 * This adapter is just like the stop detail adapter but is for ListView instead of Recycler View
 * This is a simple adapter to fill in the stop,
 * scheduled time and estimated next arrival
 */

public class NearbyLVAdapter extends BaseAdapter {
    public static final String TAG = "StopDetailAdapter";
    final StopData[] items;
    String mNoData, mActual, mSchedule = null;

    public NearbyLVAdapter(StopData[] stopList) {
        super();
        items = stopList;

    }

    @Override
    public int getCount() {
        if(items == null) {
            return 0;
        }
        return items.length;
    }

    @Override
    public Object getItem(int position) {
        if(items == null || position >= items.length) {
            return null;
        }
        return items[position];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(items == null || position >= items.length) {
            return null;
        }
        final Context ctx = parent.getContext();
        if(mSchedule == null) {
            mSchedule = ctx.getString(R.string.scheduled);
            mNoData = ctx.getString(R.string.stopEmptyString);
            mActual = ctx.getString(R.string.actual);
        }
        if(convertView == null) {
            convertView = LayoutInflater.from(ctx)
                    .inflate(R.layout.t_stop, parent, false);
        }
        final StopData s = items[position];
        ((TextView) convertView.findViewById(R.id.stop_route)).setText(s.stopName);
        //alert header text gets set in the alert field instead of the alert id
        ((TextView) convertView.findViewById(R.id.stop_desc)).setText(mSchedule + " " + Utils.trimStopTimes(mNoData, s));
        ((TextView) convertView.findViewById(R.id.stop_eta)).setText(Utils.setPredictions(mActual, s));

        //TODO create new layout
        final ImageButton alertBtn = (ImageButton) convertView.findViewById(R.id.stop_detail_btn);
        alertBtn.setTag(s.stopAlert);
        if(TextUtils.isEmpty(s.stopAlert)) {
            alertBtn.setImageResource(R.drawable.btn_stop_alert);
        } else {
            alertBtn.setImageResource(R.drawable.btn_stop_detail);
        }
        return convertView;
    }


}
