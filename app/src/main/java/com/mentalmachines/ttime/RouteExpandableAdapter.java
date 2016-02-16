package com.mentalmachines.ttime;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

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
    String[] mGroupNames, mLineIds;
    final boolean isBus;
    static SQLiteDatabase mDB;

    final static String[] mRouteProjection = new String[] {
            DBHelper.KEY_ROUTE_ID, DBHelper.KEY_ROUTE_NAME
    };
    public final static String[] mStopProjection = new String[] {
            DBHelper.KEY_STOPNM, DBHelper.KEY_STOP_ORD
    };
    final static String stopsSubwayWhereClause = DBHelper.KEY_ROUTE_ID + " like ";
    final static String modeWhereClause = DBHelper.KEY_ROUTE_MODE + " like ";

    //here is the actual data to display, along with mGroupNames
    static BusData[][] mBusArrays;


    public RouteExpandableAdapter(Activity ctx, boolean busList) {
        isBus = busList;
        if(busList) {
            mGroupNames = ctx.getResources().getStringArray(R.array.nav_groups);
            ctx.findViewById(R.id.exp_bus).setBackgroundResource(R.color.bluelineBG);
            ctx.findViewById(R.id.exp_lines).setBackgroundResource(android.R.color.transparent);
        } else {
            ctx.findViewById(R.id.exp_bus).setBackgroundResource(android.R.color.transparent);
            ctx.findViewById(R.id.exp_lines).setBackgroundResource(R.color.bluelineBG);
            if(mDB == null || !mDB.isOpen()) {
                mDB = new DBHelper(ctx).getReadableDatabase();
            }
            final Cursor c = mDB.query(DBHelper.DB_ROUTE_TABLE,
                    mRouteProjection,
                    modeWhereClause + "'" + DBHelper.SUBWAY_MODE + "'",
                    null, null, null, null);
            if(c.moveToFirst()) {
                mGroupNames = new String[c.getCount()];
                mLineIds = new String[c.getCount()];
                for(int dex = 0; dex < mGroupNames.length; dex++) {
                    mGroupNames[dex] = c.getString(1);
                    mLineIds[dex] = c.getString(0);
                    c.moveToNext();
                }
                c.close();

            }
        }
    }

    @Override
    public int getGroupCount() {
        if(mGroupNames == null) {
            return 0;
        }
        if(isBus) {
            return mGroupNames.length;
        }
        //The lines are all children in group 0
        return 1;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if(isBus) {
            return mBusArrays[groupPosition].length;
        }
        //now work with the lines, subway mode or silverline
        return mGroupNames.length;
    }

    @Override
    public Object getGroup(int i) {
        if(isBus && mGroupNames != null && i < mGroupNames.length) {
            return mGroupNames[i];
        }
        return DBHelper.SUBWAY_MODE;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        if(isBus) {
            return mBusArrays[groupPosition][childPosition];
        }
        return mGroupNames[childPosition];
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
        if(convertView == null) {
            //inflate a new group view
            convertView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.group_view, null);
        }

        if(!isBus) {
            ((TextView) convertView).setText(DBHelper.SUBWAY_MODE);
            ((TextView) convertView).setTextColor(
                    parent.getContext().getResources().getColor(GroupTxtColor[groupPosition]));
            convertView.setBackgroundColor(
                    parent.getContext().getResources().getColor(GroupColorBG[groupPosition]));

        } else {
            ((TextView) convertView).setText(mGroupNames[groupPosition]);
            convertView.setBackgroundColor(
                    parent.getContext().getResources().getColor(GroupColorBG[2]));
        }
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
             boolean isLastChild, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.child_view, null);
        }
        if(isBus) {
            final BusData tmp = mBusArrays[groupPosition][childPosition];
            ((TextView)convertView).setText(tmp.routeName);
            convertView.setTag(tmp.routeId);
        } else {
            //is subway
            final int color = parent.getContext().getResources().getColor(GroupTxtColor[groupPosition]);
            ((TextView)convertView).setText(mGroupNames[childPosition]);
            ((TextView) convertView).setTextColor(color);
            convertView.setTag(mLineIds[childPosition]);
            convertView.setTag(R.layout.child_view, color);
        }
        //??convertView.setTag(groupPosition);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public static void initBusList(final Context ctx) {
        if(mDB == null || !mDB.isOpen()) {
            mDB = new DBHelper(ctx).getReadableDatabase();
        }
        final ArrayList<BusData> masterBusList = new ArrayList<>();
        final BusData[][] busGroups = new BusData[7][];

        final Cursor c = mDB.query(DBHelper.DB_ROUTE_TABLE, mRouteProjection,
                modeWhereClause + "'" + DBHelper.BUS_MODE + "'",
                null, null, null, null);
        Log.i(TAG, "cursor size? " + c.getCount());
        BusData bData;
        if(c.moveToFirst()) {
            do {
                bData = new BusData();
                bData.routeId = c.getString(0);
                bData.routeName = c.getString(1);
                masterBusList.add(bData);
            } while(c.moveToNext());
            c.close();
        }
        Log.i(TAG, "master list size? " + masterBusList.size());
        for(int dex=0; dex < 7; dex++) {
            Log.i(TAG, "Loading bus group " + dex);
            busGroups[dex] = loadBusArray(dex, masterBusList);
        }
        mBusArrays = busGroups;
    }

    static BusData[] loadBusArray(int groupPosition, ArrayList<BusData> busList) {

        final ArrayList<BusData> tmp = new ArrayList<>();
        switch(groupPosition) {
            case 0:
                for(BusData bus: busList) {
                    if (bus.routeId.matches("[0-9]+") && bus.routeId.length() <= 2 &&
                            Integer.valueOf(bus.routeId) < 51) {
                        tmp.add(bus);
                        //mBusList.remove(bus);
                    }
                }
                break;
            case 1:
                for(BusData bus: busList) {
                    if (bus.routeId.matches("[0-9]+") && bus.routeId.length() <= 2 &&
                            Integer.valueOf(bus.routeId) > 50) {
                        tmp.add(bus);
                        //mBusList.remove(bus);
                    }
                }
                break;
            case 2:
                for(BusData bus: busList) {
                    if (bus.routeId.matches("[0-9]+") && bus.routeId.length() == 3 &&
                            Integer.valueOf(bus.routeId) < 200) {
                        tmp.add(bus);
                        //mBusList.remove(bus);
                    }
                }
                break;
            case 3:
                for(BusData bus: busList) {
                    if (bus.routeId.matches("[0-9]+") && bus.routeId.length() == 3 &&
                            Integer.valueOf(bus.routeId) > 199 && Integer.valueOf(bus.routeId) < 400) {
                        tmp.add(bus);
                        //mBusList.remove(bus);
                    }
                }
                break;
            case 4:
                for(BusData bus: busList) {
                    if (bus.routeId.matches("[0-9]+") && bus.routeId.length() == 3 &&
                            Integer.valueOf(bus.routeId) > 399 && Integer.valueOf(bus.routeId) < 600) {
                        tmp.add(bus);
                        //mBusList.remove(bus);
                    }
                }
                break;
            case 5:
                //this section may need to exclude the Silver line
                for(BusData bus: busList) {
                    if (bus.routeId.matches("[0-9]+") && bus.routeId.length() == 3 &&
                            Integer.valueOf(bus.routeId) > 599 && Integer.valueOf(bus.routeId) <= 751) {
                        tmp.add(bus);
                        //mBusList.remove(bus);
                    }
                }
                break;
            case 6:
                for(BusData bus: busList) {
                    if(bus.routeId.length() > 3) {
                        tmp.add(bus);
                        //mBusList.remove(bus);
                    }
                }
                break;
        }
        BusData[] childNames = new BusData[tmp.size()];
        childNames = tmp.toArray(childNames);
        tmp.clear();
        Log.d(TAG, "returning line array, size is " + childNames.length);
        return childNames;
    }

    public static class BusData {
        String routeId;
        String routeName;
    }

    //Quick access colors, ordered by the lines
    public static final int[] GroupColorBG = { R.color.bluelineBG, R.color.greenlineBG, R.color.busYellowBG,
            R.color.orangelineBG, R.color.redlineBG, R.color.silverlineBG };
    public static final int[] GroupTxtColor = { R.color.solidBlueline, R.color.solidGreenline, R.color.solidBusYellow,
            R.color.solidOrangeline, R.color.solidRedline, android.R.color.darker_gray };

}
