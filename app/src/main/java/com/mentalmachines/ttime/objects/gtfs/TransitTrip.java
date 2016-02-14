package com.mentalmachines.ttime.objects.gtfs;

import android.content.Context;
import android.util.Log;

import com.mentalmachines.ttime.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
	public String block_id;
    public String shape_id;
    public String wheelchair_accessible;


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

    public static void createTrips(Context ctx) {
        BufferedReader rawReader = new BufferedReader(new InputStreamReader(ctx.getResources().openRawResource(R.raw.trips)));
        String line = "";
        ArrayList<TransitTrip> tripCollection = new ArrayList<>();
        try {
            while((line = rawReader.readLine()) != null) {
                tripCollection.add(new TransitTrip(line));
            }
            tripMapList = new TransitTrip[tripCollection.size()];
            tripMapList = tripCollection.toArray(tripMapList);
            tripCollection.clear();
        } catch (IOException e) {
            Log.e(TAG, "Error reading GTFS");
            e.printStackTrace();
        }
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
