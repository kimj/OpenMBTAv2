package com.mentalmachines.ttime.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.objects.StopData;

/**
 * Created by emezias on 5/14/16
 * This adapter is just like the stop detail adapter but is for ListView instead of Recycler View
 * This is a simple adapter to fill in the stop,
 * scheduled time and estimated next arrival
 */

public class NearbyLVAdapter extends BaseAdapter {
    public static final String TAG = "StopDetailAdapter";
    final StopData[] items;

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
        if(convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.t_stop, parent, false);
        }
        final StopData s = items[position];
        ((TextView) convertView.findViewById(R.id.stop_route)).setText(s.stopName);
        //alert header text gets set in the alert field instead of the alert id
        if(TextUtils.isEmpty(s.schedTimes)) {
            ((TextView) convertView.findViewById(R.id.stop_desc)).setText("");
        } else {
            ((TextView) convertView.findViewById(R.id.stop_desc)).setText(s.schedTimes);

        }
        if(TextUtils.isEmpty(s.predicTimes)) {
            ((TextView) convertView.findViewById(R.id.stop_eta)).setText("");
        } else {
            ((TextView) convertView.findViewById(R.id.stop_eta)).setText(s.predicTimes);
        }

        //TODO create new layout
        final View alertBtn = convertView.findViewById(R.id.stop_alert_btn);
        if(s.stopAlert != null) {
            alertBtn.setVisibility(View.VISIBLE);
            alertBtn.setTag(s.stopAlert);
        } else {
            alertBtn.setVisibility(View.GONE);
            alertBtn.invalidate();
        }
        return convertView;
    }


}
