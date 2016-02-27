package com.mentalmachines.ttime.objects.gtfs;

import android.content.Context;

import com.mentalmachines.ttime.DBHelper;

import java.util.ArrayList;

public class TransitTrip {
    public static final String TAG = "TransitTrip";
    static TransitTrip[] tripMapList;

    public String route_id;
    public String service_id;
    public String trip_id;
    public String trip_headsign;
    public String trip_short_name;
    public String direction_id;
    /**
     * "direction_id": "0",
     "direction_name": "Southbound",
     "direction_id": "0",
     "direction_name": "Outbound",
     */
    public String block_id;
    public String shape_id;
    public String wheelchair_accessible;
    public boolean isBus;


    public TransitTrip(String str){
        String[] temp = str.split(",");

        this.route_id = temp[0];
        this.service_id = temp[1];
        this.trip_id = temp[2];
        this.trip_headsign = temp[3];
        this.trip_short_name = temp[4];
        this.direction_id = temp[5];
        this.block_id = temp[6];
        this.shape_id = temp[7];
        this.wheelchair_accessible = temp[8];

    }

    final static String[] projection = new String[] { DBHelper.KEY_ROUTE_MODE };
    final static String whereClause = DBHelper.KEY_ROUTE_ID + " like ";
    public static void createTrips(Context ctx) {
        /*final SQLiteDatabase mDB = new DBHelper(ctx).getWritableDatabase();
        BufferedReader rawReader = new BufferedReader(new InputStreamReader(ctx.getResources().openRawResource(R.raw.trips)));
        String line = "";
        ArrayList<TransitTrip> tripCollection = new ArrayList<>();
        TransitTrip tmp;
        Cursor c;
        try {
            while((line = rawReader.readLine()) != null) {
                tmp = new TransitTrip(line);
                //set the trip mode, only add trips that are subway and bus
                c = mDB.query(DBHelper.DB_ROUTE_TABLE, projection, whereClause + tmp.route_id, null, null, null, null);
                if(c.getCount() > 0) {
                    c.moveToFirst();
                    if(c.getString(0).equals(DBHelper.SUBWAY_MODE)){
                        tmp.isBus = false;
                    } else {
                        tmp.isBus = true;
                    }

                    tripCollection.add(tmp);
                }
                c.close();
            }
            tripMapList = new TransitTrip[tripCollection.size()];
            tripMapList = tripCollection.toArray(tripMapList);
            tripCollection.clear();
        } catch (IOException e) {
            Log.e(TAG, "Error reading GTFS");
            e.printStackTrace();
        }*/
    }

    public static TransitTrip[] returnTripsWithScheduleIds(String[] calList) {
        final ArrayList<TransitTrip> tmp = new ArrayList<>();
        for(TransitTrip t: tripMapList) {
            for(String tripid: calList) {
                if(tripid.equals(t.service_id) && !tmp.contains(t)) {
                    tmp.add(t);
                }
            }

        }
        TransitTrip[] returnList = new TransitTrip[tmp.size()];
        returnList = tmp.toArray(returnList);
        tmp.clear();

        return returnList;
    }
}