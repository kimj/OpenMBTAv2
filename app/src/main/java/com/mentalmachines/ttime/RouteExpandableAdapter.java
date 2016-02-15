package com.mentalmachines.ttime;

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
import java.util.Arrays;
import java.util.List;

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
    String[] mGroupNames;
    final boolean isBus;
    static boolean loadComplete = false;
    static SQLiteDatabase mDB;

    final static String[] mRouteProjection = new String[] {
            DBHelper.KEY_ROUTE_ID, DBHelper.KEY_ROUTE_NAME
    };
    final static String[] mLineProjection = new String[] {
            DBHelper.KEY_STOPNM, DBHelper.KEY_STOP_ORD
    };
    final static String routeSubwayWhereClause = DBHelper.KEY_ROUTE_NAME + " like ";
    final static String stopsSubwayWhereClause = DBHelper.KEY_ROUTE_ID + " like ";

    final static String modeWhereClause = DBHelper.KEY_ROUTE_MODE + " like ";

    //here is the actual data to display
    static BusData[][] mBusArrays;


    public RouteExpandableAdapter(Context ctx, boolean busList) {
        isBus = busList;
        if(busList) {
            mGroupNames = ctx.getResources().getStringArray(R.array.nav_groups);
        } else {
            //mGroupNames = ctx.getResources().getStringArray(R.array.line_group);
            if(mDB == null || !mDB.isOpen()) {
                mDB = new DBHelper(ctx).getReadableDatabase();
            }
            final Cursor c = mDB.query(DBHelper.DB_ROUTE_TABLE,
                    new String[] { DBHelper.KEY_ROUTE_NAME },
                    modeWhereClause + "'" + DBHelper.SUBWAY_MODE + "'",
                    null, null, null, null);
            if(c.moveToFirst()) {
                mGroupNames = new String[c.getCount()];
                for(int dex = 0; dex < mGroupNames.length; dex++) {
                    mGroupNames[dex] = c.getString(0);
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
        if(isBus && mGroupNames != null && groupPosition >= mGroupNames.length) {
            return null;
        } else {
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
            ((TextView)convertView).setText(mBusArrays[groupPosition][childPosition].routeName);
        } else {
            //is subway
            ((TextView)convertView).setText(mGroupNames[childPosition]);
            ((TextView) convertView).setTextColor(
                    parent.getContext().getResources().getColor(GroupTxtColor[groupPosition]));

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

        loadComplete = true;
        /*new Thread() {
            @Override
            public void run() {
                super.run();
                }
        }.start();*/

    }

    static String[] loadLineArray(String[] silverline, SQLiteDatabase db) {
        final List<String> tmp = new ArrayList<>();
        for(String routeId: silverline) {
            tmp.addAll(Arrays.asList(loadLineArray(routeId, db)));
        }
        final String[] returnList = new String[tmp.size()];
        tmp.toArray(returnList);
        tmp.clear();
        return returnList;
    }

    static String[] loadLineArray(String line, SQLiteDatabase db) {
        final ArrayList<String> tmp = new ArrayList<>();
        Cursor c = db.query(DBHelper.DB_ROUTE_TABLE, mRouteProjection,
                routeSubwayWhereClause + line, null,
                null, null, null, null);
        //get all the subway color routes then get the first and last stop
        if(c.getCount() > 0 && c.moveToFirst()) {
            do {
                tmp.add(c.getString(0));
            } while(c.moveToNext());
            c.close();
        }
        final String[] routeIds = new String[tmp.size()];
        tmp.toArray(routeIds);
        tmp.clear();
        String routeChild;
        for(String r: routeIds) {
            //get all of the inbound stops, use the first and last to describe the route
            c = db.query(DBHelper.STOPS_INB_TABLE, mLineProjection,
                    stopsSubwayWhereClause + "'" + r + "'",
                    null, null, null, null);
            if(c.getCount() > 0 && c.moveToFirst()) {
                routeChild = c.getString(0);
                c.moveToLast();
                tmp.add(routeChild + "-" + c.getString(0));
                c.close();
            }
            c = db.query(DBHelper.STOPS_OUT_TABLE, mLineProjection,
                    stopsSubwayWhereClause + "'" + r + "'",
                    null, null, null, null);
            if(c.getCount() > 0 && c.moveToFirst()) {
                routeChild = c.getString(0);
                c.moveToLast();
                tmp.add(routeChild + "-" + c.getString(0));
                c.close();
            }
        }

        final String[] returnList = new String[tmp.size()];
        tmp.toArray(returnList);
        tmp.clear();
        Log.d(TAG, "returning line array, size is " + returnList.length);
        return returnList;
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
