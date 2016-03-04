package com.mentalmachines.ttime.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.R;

import java.util.ArrayList;

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

    public static final int SUBWAY = 0;
    public static final int BUS = 1;
    public static final int FAVE = 2;

    public final int mMode;
    public final String[] rtNames, rtIds, busGroups;
    int[][] busChildren = null;

    public RouteExpandableAdapter(String[] names, String[] ids, int mode) {
        mMode = mode;
        rtNames = names;
        rtIds = ids;
        busGroups = null;
    }

    public RouteExpandableAdapter(String[] names, String[] ids, Context ctx) {
        mMode = BUS;
        rtNames = names;
        rtIds = ids;
        busGroups = ctx.getResources().getStringArray(R.array.nav_groups);
        busChildren = new int[busGroups.length][];
    }

    @Override
    public int getGroupCount() {
        if(mMode != BUS) {
            return 1;
        }
        return busGroups.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if(mMode == BUS && rtNames != null) {
            if(busChildren[groupPosition] == null) {
                busChildren[groupPosition] = getChildren(groupPosition);
            }
            return busChildren[groupPosition].length;
        } else if(rtNames != null) {
            return rtNames.length;
        } else return 0;
        //if the db is initializing, these can be null

    }

    @Override
    public Object getGroup(int i) {
        return i;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        if(mMode == BUS) {
            return busGroups[groupPosition];
        }
        return rtNames[childPosition];
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
        final Context ctx =  parent.getContext();
        if(convertView == null) {
            //inflate a new group view
            convertView = LayoutInflater.from(ctx).inflate(R.layout.group_view, null);
        }
        //Group position zero has the silver line bg, merges selected button into the list
        if(mMode == BUS) {
            if(groupPosition == 0) {
                ((TextView) convertView).setTextColor(
                        ctx.getResources().getColor(R.color.solidSilverline));
                convertView.setBackgroundResource(R.color.silverlineBG);
            } else {
                ((TextView) convertView).setTextColor(
                        ctx.getResources().getColor(R.color.solidBusYellow));
                convertView.setBackgroundResource(android.R.color.white);
            }
            ((TextView) convertView).setText(busGroups[groupPosition]);
        } else {
            ((TextView) convertView).setTextColor(ctx.getResources().getColor(R.color.colorPrimary));
            convertView.setBackgroundResource(R.color.silverlineBG);
            if(mMode == SUBWAY) {
                ((TextView) convertView).setText(DBHelper.SUBWAY_MODE);
            } else {
                //Favorites mode
                ((TextView) convertView).setText(ctx.getString(R.string.favorites));
            }
        }
        if(mMode == SUBWAY || mMode == FAVE) {
            //this bit will make sure that the group is always expanded
            ((ExpandableListView)parent).expandGroup(groupPosition);
        }
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
             boolean isLastChild, View convertView, ViewGroup parent) {
        //the tags are used to open the routeFragment
        final Context ctx = parent.getContext();
        if(convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(
                    R.layout.child_view, null);
        }
        if(mMode == BUS) {
            if(busChildren[groupPosition] == null) {
                busChildren[groupPosition] = getChildren(groupPosition);
            }
            ((TextView)convertView).setText(rtNames[busChildren[groupPosition][childPosition]]);
            convertView.setTag(rtIds[busChildren[groupPosition][childPosition]]);
            //Log.d(TAG, "tagging child view " + tmp.routeId);
        } else {
            ((TextView)convertView).setText(rtNames[childPosition]);
            convertView.setTag(rtIds[childPosition]);
            if(mMode == SUBWAY) {
                final int color = getBgColor(ctx, rtNames[childPosition]);
                ((TextView) convertView).setTextColor(Color.BLACK);
                convertView.setBackgroundColor(color);
                convertView.setTag(R.layout.child_view, color);
            } else {
                //must be favorites
                ((TextView) convertView).setText(rtNames[childPosition]);
                ((TextView) convertView).setTextColor(ctx.getResources().getColor(R.color.colorPrimary));

            }
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    int[] getChildren(int grpIndex) {
        final ArrayList<Integer> tmp = new ArrayList<>();
        int dex = 0;
        switch(grpIndex) {
            case 0:
                for(String bus: rtNames) {
                    if(bus.contains("Silver")) {
                        tmp.add(dex);
                    }
                    dex++;
                }
                break;
            case 1:
                for(String bus: rtIds) {
                    if (bus.matches("[0-9]+") && bus.length() <= 2 &&
                            Integer.valueOf(bus) < 51) {
                        tmp.add(dex);
                    }
                    dex++;
                }
                break;
            case 2:
                for(String bus: rtIds) {
                    if (bus.matches("[0-9]+") && bus.length() <= 2 &&
                            Integer.valueOf(bus) > 50) {
                        tmp.add(dex);
                    }
                    dex++;
                }
                break;
            case 3:
                for(String bus: rtIds) {
                    if (bus.matches("[0-9]+") && bus.length() == 3 &&
                            Integer.valueOf(bus) < 200) {
                        tmp.add(dex);
                    }
                    dex++;
                }
                break;
            case 4:
                for(String bus: rtIds) {
                    if (bus.matches("[0-9]+") && bus.length() == 3 &&
                            Integer.valueOf(bus) > 199 && Integer.valueOf(bus) < 400) {
                        tmp.add(dex);
                    }
                    dex++;
                }
                break;
            case 5:
                for(String bus: rtIds) {
                    if (bus.matches("[0-9]+") && bus.length() == 3 &&
                            Integer.valueOf(bus) > 399 && Integer.valueOf(bus) < 600) {
                        tmp.add(dex);
                    }
                    dex++;
                }
                break;
            case 6:
                //this section has to exclude the Silver line which is case 1
                for(String bus: rtIds) {
                    if(bus.matches("[0-9]+") && bus.length() == 3 && !rtNames[dex].contains("Silver") &&
                            Integer.valueOf(bus) > 599 && Integer.valueOf(bus) <= 751) {
                        tmp.add(dex);
                    }
                    dex++;
                }
                break;
            case 7:
                for(String bus: rtIds) {
                    if(bus.length() > 3) {
                        tmp.add(dex);
                    }
                    dex++;
                }
                break;
        } //end switch
        int[] childIndices = new int[tmp.size()];
        for(dex = 0; dex < childIndices.length; dex++) {
            childIndices[dex] = tmp.get(dex);
        }
        tmp.clear();
        Log.d(TAG, "returning next array, size is " + childIndices.length);
        return childIndices;
    }

    public static int getBgColor(Context ctx, String route) {
        if(route.contains("Green")) {
            return ctx.getResources().getColor(R.color.greenlineBG);
        } else if(route.contains("Blue")) {
            return ctx.getResources().getColor(R.color.bluelineBG);
        } else if(route.contains("Orange")) {
            return ctx.getResources().getColor(R.color.orangelineBG);
        } else if(route.contains("Red")) {
            return ctx.getResources().getColor(R.color.redlineBG);
        } /* See if this is needed...
        else if(route.contains("Silver")) {
            return ctx.getResources().getColor(R.color.silverlineBG);
        }*/
        return ctx.getResources().getColor(R.color.busYellowBG);
    }

}
