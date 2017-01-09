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

    public NearbyLVAdapter(Context ctx, StopData[] stopList) {
        super();
        items = stopList;
        mSchedule = ctx.getString(R.string.scheduled);
        mNoData = ctx.getString(R.string.stopEmptyString);
        mActual = ctx.getString(R.string.actual);
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

        if(convertView == null) {
            convertView = LayoutInflater.from(ctx)
                    .inflate(R.layout.nearby_stop, parent, false);
            convertView.setTag(convertView.findViewById(R.id.nr_prediction));
        }
        final StopData s = items[position];
        if (s == null) {
            return null;
        }

        ((TextView) convertView.findViewById(R.id.nr_name_route)).setText(s.stopName + "\n" + s.stopRouteDir);
        //alert header text gets set in the alert field instead of the alert id
        ((TextView) convertView.findViewById(R.id.nr_schedule)).setText(mSchedule + " " + Utils.nearbyTimes(mNoData, s));
        final TextView tv = (TextView) convertView.getTag();
        tv.setText(Utils.setPredictions(mActual, s));
        if (TextUtils.isEmpty(tv.getText().toString())) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setVisibility(View.VISIBLE);
        }

        final ImageButton alertBtn = (ImageButton) convertView.findViewById(R.id.nr_alert);
        if (TextUtils.isEmpty(s.stopAlert)) {
            alertBtn.setVisibility(View.GONE);
        } else {
            alertBtn.setTag(s.stopAlert);
            alertBtn.setVisibility(View.VISIBLE);
        }
        if (position == 0) {
            convertView.findViewById(R.id.divider).setVisibility(View.GONE);
            //alertBtn.setImageResource(android.R.drawable.ic_menu_directions);
        }
        return convertView;
    }


}
