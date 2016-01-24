package com.mentalmachines.ttime;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

/**
 * Created by emezias on 1/23/16.
 * this class will show in the drawer and allow the user to pick their bus or train route
 * The list is built from the route tables
 * The first groups are the lines, with 2(orange) to 8(green) children
 * The next groups are bus - organized by bus number
 * The app will be grouping bus numbers by hundreds
 */
public class RouteExpandableAdapter extends BaseExpandableListAdapter {
    public static final String TAG = "RouteExpandableAdapter";
    public final String[] mGroupNames;

    public RouteExpandableAdapter(Context ctx, boolean busList) {
        if(busList) {
            mGroupNames = ctx.getResources().getStringArray(R.array.bus_group);
        } else {
            mGroupNames = ctx.getResources().getStringArray(R.array.line_group);
        }
    }

    @Override
    public int getGroupCount() {
        if(mGroupNames != null) {
            return mGroupNames.length;
        }
        return 0;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        switch (groupPosition) {
            case BLUE:
                return fakeBlue.length;
            case GREEN:
                return fakeGreen.length;
            case ORANGE:
                return fakeOrange.length;
            case RED:
                return fakeRed.length;
            case SILVER:
                return fakeSilver.length;
        }
        return 0;
    }

    @Override
    public Object getGroup(int i) {
        if(mGroupNames != null && i < mGroupNames.length) {
            return mGroupNames[i];
        }
        return null;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        switch (groupPosition) {
            case BLUE:
                return fakeBlue[childPosition];
            case GREEN:
                return fakeGreen[childPosition];
            case ORANGE:
                return fakeOrange[childPosition];
            case RED:
                return fakeRed[childPosition];
            case SILVER:
                return fakeSilver[childPosition];
        }
        return null;
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if(mGroupNames != null && groupPosition >= mGroupNames.length) {
            Log.w(TAG, "bad group number, array ready? " + (mGroupNames != null));
            return null;
        } else {
            if(convertView == null) {
                //inflate a new group view
                convertView = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.group_view, null);
            }
            ((TextView) convertView).setText(mGroupNames[groupPosition]);
        }
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
             boolean isLastChild, View convertView, ViewGroup parent) {
        switch (groupPosition) {
            case BLUE:
                if(childPosition > fakeBlue.length) {
                    return null;
                } else {
                    if(convertView == null) {
                        convertView = LayoutInflater.from(parent.getContext()).inflate(
                                R.layout.child_view, null);
                    }
                    ((TextView) convertView).setText(fakeBlue[childPosition]);
                    ((TextView) convertView).setTextColor(
                            parent.getContext().getResources().getColor(R.color.solidBlueline));
                }
                break;
            case GREEN:
                if(childPosition > fakeGreen.length) {
                    return null;
                } else {
                    if(convertView == null) {
                        convertView = LayoutInflater.from(parent.getContext()).inflate(
                                R.layout.child_view, null);
                    }
                    ((TextView) convertView).setText(fakeGreen[childPosition]);
                    ((TextView) convertView).setTextColor(
                            parent.getContext().getResources().getColor(R.color.solidGreenline));
                }
                break;
            case ORANGE:
                if(childPosition > fakeOrange.length) {
                    return null;
                } else {
                    if(convertView == null) {
                        convertView = LayoutInflater.from(parent.getContext()).inflate(
                                R.layout.child_view, null);
                    }
                    ((TextView) convertView).setText(fakeOrange[childPosition]);
                    ((TextView) convertView).setTextColor(
                            parent.getContext().getResources().getColor(R.color.solidOrangeline));
                }
                break;
            case RED:
                if(childPosition > fakeRed.length) {
                    return null;
                } else {
                    if(convertView == null) {
                        convertView = LayoutInflater.from(parent.getContext()).inflate(
                                R.layout.child_view, null);
                    }
                    ((TextView) convertView).setText(fakeRed[childPosition]);
                    ((TextView) convertView).setTextColor(
                            parent.getContext().getResources().getColor(R.color.solidRedline));
                }
                break;
            case SILVER:
                if(childPosition > fakeSilver.length) {
                    return null;
                } else {
                    if(convertView == null) {
                        convertView = LayoutInflater.from(parent.getContext()).inflate(
                                R.layout.child_view, null);
                    }
                    ((TextView) convertView).setText(fakeSilver[childPosition]);
                    ((TextView) convertView).setTextColor(
                            parent.getContext().getResources().getColor(R.color.solidSilverline));
                }
                break;
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    //These group position constants will be more dynamic
    static final int BLUE = 0;
    static final int GREEN = 1;
    static final int ORANGE = 2;
    static final int RED = 3;
    static final int SILVER = 4;

    String[] fakeBlue = {"Wonderland to Bowdoin (inbound)", "Bowdoin to Wonderland (outbound"};
    String[] fakeOrange = {"Forest Hills to Oak Grove (inbound)", "Oak Grove to Forest Hills (outbound"};
    String[] fakeGreen = {"Green A (inbound)", "Green A (outbound",
            "Green b (inbound)", "Green b (outbound",
            "Green c (inbound)", "Green c (outbound",
            "Green d (inbound)", "Green d (outbound"};
    String[] fakeRed = {"Alewife to BrainTree (inbound)",
            "Braintree to Alewife (outbound)",
            "Ashmont to Braintree (inbound)",
            "Braintree to Ashmont (outbound"};
    String[] fakeSilver = { "SL2 to South Station (inbound)",
            "SL2 to Airport (outbound)",
            "SL4 to South Station (inbound)",
            "SL4 to Ruggles (outbound)",
            "SL5 to Downtown Crossing (inbound)",
            "SL5 to Ruggles (outbound"};
}
