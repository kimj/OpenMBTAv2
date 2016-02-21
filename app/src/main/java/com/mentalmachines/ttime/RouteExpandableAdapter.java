package com.mentalmachines.ttime;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
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
    String[] mGroupNames;
    public String[] mLineIds;
    public final int mMode;
    static SQLiteDatabase mDB;

    final static String[] mRouteProjection = new String[] {
            DBHelper.KEY_ROUTE_ID, DBHelper.KEY_ROUTE_NAME
    };
    public final static String[] mStopProjection = new String[] {
            DBHelper.KEY_STOPNM, DBHelper.KEY_STOP_ORD
    };
    final static String[] mFavIDProjection = new String[] {
            DBHelper.KEY_ROUTE_ID
    };
    final static String[] mFavProjection = new String[] {
            DBHelper.KEY_ROUTE_NAME
    };
    final static String stopsSubwayWhereClause = DBHelper.KEY_ROUTE_ID + " like ";
    final static String modeWhereClause = DBHelper.KEY_ROUTE_MODE + " like ";
    final static String routeWhereClause = DBHelper.KEY_ROUTE_NAME + " like ";
    //here is the actual data to display, along with mGroupNames
    static BusData[][] mBusArrays;

    public RouteExpandableAdapter(Activity ctx, Cursor c) {
        //this is the favorites list
        mMode = FAVE;
        setSelectedBtn(R.id.exp_favorite, ctx);
        final int sz = c.getCount();
        if(sz > 0 && c.moveToFirst()) {
            mGroupNames = DBHelper.makeArrayFromCursor(c, 0);
            mLineIds = new String[sz];

            int dex = 0;
            for(String route: mGroupNames) {
                c = mDB.query(DBHelper.DB_ROUTE_TABLE,
                        mFavIDProjection,
                        routeWhereClause + "'" + route + "'",
                        null, null, null, null);
                if(c.getCount() > 0 && c.moveToFirst()) {
                    mLineIds[dex] = c.getString(0);
                } else {
                    Log.w(TAG, "no route id for: " + route);
                }
                dex++;
            }
            c.close();
        } else {
            //no favorites
            mGroupNames = new String[] { ctx.getString(R.string.no_favs)};
            mLineIds = null;
        }

    }

    public RouteExpandableAdapter(Activity ctx, boolean busList) {

        if(busList) {
            mMode = BUS;
            mGroupNames = ctx.getResources().getStringArray(R.array.nav_groups);
            setSelectedBtn(R.id.exp_bus, ctx);
        } else {
            mMode = SUBWAY;
            setSelectedBtn(R.id.exp_lines, ctx);
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
        if(mMode == 1) {
            return mGroupNames.length;
        }
        //The lines are all children in group 0
        return 1;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if(mMode == BUS && mBusArrays != null) {
            return mBusArrays[groupPosition].length;
        } else if(mGroupNames != null) {
            return mGroupNames.length;
        } else return 0;
        //if the db is initializing, these can be null

    }

    @Override
    public Object getGroup(int i) {
        if(mMode == BUS && mGroupNames != null && i < mGroupNames.length) {
            return mGroupNames[i];
        } else if(mMode == FAVE) {
            //return mLineIds[i];
            Log.w(TAG, "favorite group");
        }
        return DBHelper.SUBWAY_MODE;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        if(mMode == BUS) {
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
            ((TextView) convertView).setText(mGroupNames[groupPosition]);
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
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
             boolean isLastChild, View convertView, ViewGroup parent) {
        final Context ctx = parent.getContext();
        if(convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(
                    R.layout.child_view, null);
        }
        if(mMode == BUS) {
            final BusData tmp = mBusArrays[groupPosition][childPosition];
            ((TextView)convertView).setText(tmp.routeName);
            convertView.setTag(tmp.routeId);
        } else if(mMode == SUBWAY) {
            final int color = getBgColor(ctx, mGroupNames[childPosition]);
            ((TextView)convertView).setText(mGroupNames[childPosition]);
            ((TextView) convertView).setTextColor(Color.BLACK);
            convertView.setBackgroundColor(color);
            convertView.setTag(mLineIds[childPosition]);
            convertView.setTag(R.layout.child_view, color);
        } else {
            //must be favorites with a valid mLineids array
            ((TextView)convertView).setText(mGroupNames[childPosition]);
            ((TextView) convertView).setTextColor(ctx.getResources().getColor(R.color.colorPrimary));
            convertView.setTag(mLineIds[childPosition]);
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
        final BusData[][] busGroups = new BusData[8][];

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
        for(int dex=0; dex < busGroups.length; dex++) {
            Log.i(TAG, "Loading bus group " + dex);
            busGroups[dex] = loadBusArray(dex, masterBusList);
        }
        mBusArrays = busGroups;
        Log.i(TAG, "master list size? " + masterBusList.size());
        masterBusList.clear();
    }

    static BusData[] loadBusArray(int groupPosition, ArrayList<BusData> busList) {

        final ArrayList<BusData> tmp = new ArrayList<>();
        switch(groupPosition) {
            case 0:
                for(BusData bus: busList) {
                    if (bus.routeName.contains("Silver")) {
                        tmp.add(bus);
                    }
                }
                break;
            case 1:
                for(BusData bus: busList) {
                    if (bus.routeId.matches("[0-9]+") && bus.routeId.length() <= 2 &&
                            Integer.valueOf(bus.routeId) < 51) {
                        tmp.add(bus);
                    }
                }
                break;
            case 2:
                for(BusData bus: busList) {
                    if (bus.routeId.matches("[0-9]+") && bus.routeId.length() <= 2 &&
                            Integer.valueOf(bus.routeId) > 50) {
                        tmp.add(bus);
                    }
                }
                break;
            case 3:
                for(BusData bus: busList) {
                    if (bus.routeId.matches("[0-9]+") && bus.routeId.length() == 3 &&
                            Integer.valueOf(bus.routeId) < 200) {
                        tmp.add(bus);
                    }
                }
                break;
            case 4:
                for(BusData bus: busList) {
                    if (bus.routeId.matches("[0-9]+") && bus.routeId.length() == 3 &&
                            Integer.valueOf(bus.routeId) > 199 && Integer.valueOf(bus.routeId) < 400) {
                        tmp.add(bus);
                    }
                }
                break;
            case 5:
                for(BusData bus: busList) {
                    if (bus.routeId.matches("[0-9]+") && bus.routeId.length() == 3 &&
                            Integer.valueOf(bus.routeId) > 399 && Integer.valueOf(bus.routeId) < 600) {
                        tmp.add(bus);
                    }
                }
                break;
            case 6:
                //this section has to exclude the Silver line which is case 1
                for(BusData bus: busList) {
                    if(!bus.routeName.contains("Silver") && bus.routeId.matches("[0-9]+") && bus.routeId.length() == 3 &&
                            Integer.valueOf(bus.routeId) > 599 && Integer.valueOf(bus.routeId) <= 751) {
                        tmp.add(bus);
                    }
                }
                break;
            case 7:
                for(BusData bus: busList) {
                    if(bus.routeId.length() > 3) {
                        tmp.add(bus);
                    }
                }
                break;
        }
        for(BusData bus: tmp) {
            busList.remove(bus);
        }
        BusData[] childNames = new BusData[tmp.size()];
        childNames = tmp.toArray(childNames);
        tmp.clear();
        Log.d(TAG, "returning next array, size is " + childNames.length);
        return childNames;
    }

    void setSelectedBtn(int buttonId, Activity ctx) {
        View v = ctx.findViewById(buttonId);
        v.setBackgroundResource(R.color.silverlineBG);
        v.setTag(true);
        if(R.id.exp_lines != buttonId) {
            v = ctx.findViewById(R.id.exp_lines);
            v.setBackgroundResource(android.R.color.transparent);
            v.setTag(null);
        }
        if(R.id.exp_bus != buttonId) {
            v = ctx.findViewById(R.id.exp_bus);
            v.setBackgroundResource(android.R.color.transparent);
            v.setTag(null);
        }
        if(R.id.exp_favorite != buttonId) {
            v = ctx.findViewById(R.id.exp_favorite);
            v.setBackgroundResource(android.R.color.transparent);
            v.setTag(null);
        }
    }

    public static class BusData {
        String routeId;
        String routeName;
    }

    public static Cursor getFavorites(Context ctx) {
        if(mDB == null || !mDB.isOpen()) {
            mDB = new DBHelper(ctx).getReadableDatabase();
        }
        //select all, table only has one column
        return mDB.query(DBHelper.FAVS_TABLE,
                mFavProjection, null, null, null, null, null);
    }

    public static final int SUBWAY = 0;
    public static final int BUS = 1;
    public static final int FAVE = 2;

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

    //Quick access colors, ordered by the lines
    public static final int[] GroupColorBG = { R.color.bluelineBG, R.color.greenlineBG, R.color.busYellowBG,
            R.color.orangelineBG, R.color.redlineBG, R.color.silverlineBG };
    public static final int[] GroupTxtColor = { R.color.solidBlueline, R.color.solidGreenline, R.color.solidBusYellow,
            R.color.solidOrangeline, R.color.solidRedline, android.R.color.darker_gray };

}
