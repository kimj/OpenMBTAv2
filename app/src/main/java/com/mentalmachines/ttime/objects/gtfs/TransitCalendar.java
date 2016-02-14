package com.mentalmachines.ttime.objects.gtfs;

import android.content.Context;
import android.util.Log;

import com.mentalmachines.ttime.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class TransitCalendar {
	public static final String TAG = "TransitCalendar";

    public static TransitTrip[] weekdays, saturdays, sundays, fridays;
	//"service_id","monday","tuesday","wednesday","thursday","friday","saturday","sunday","start_date","end_date"
	public String service_id;
	public String monday;
	public String tuesday;
	public String wednesday;
	public String thursday;
	public String friday;
	public String saturday;
	public String sunday;
	public String start_date;
	public String end_date;

	public TransitCalendar(String str){
		String[] temp = str.split(",");
        
		this.service_id = temp[0];
        this.monday = temp[1];
        this.tuesday = temp[2];
        this.wednesday = temp[3];
        this.thursday = temp[4];
        this.friday = temp[5];
        this.saturday = temp[6];
        this.sunday = temp[7];
        this.start_date = temp[8];
        if(temp.length > 9) this.end_date = temp[9];
		
	}

	public static void parseCalendar(Context ctx) {
		BufferedReader rawReader = new BufferedReader(new InputStreamReader(ctx.getResources().openRawResource(R.raw.calendar)));
		String line = "";
        TransitCalendar tc;
        ArrayList<String> week = new ArrayList<>();
        ArrayList<String> sat = new ArrayList<>();
        ArrayList<String> sun = new ArrayList<>();
        ArrayList<String> fri = new ArrayList<>();
		try {
			while((line = rawReader.readLine()) != null) {
				tc = new TransitCalendar(line);
                if(tc.monday.equals("1") && !week.contains(tc.service_id)) {
                    week.add(tc.service_id);
                }
                if(tc.saturday.equals("1") && !sat.contains(tc.service_id)) {
                    sat.add(tc.service_id);
                }
                if(tc.sunday.equals("1") && !sun.contains(tc.service_id)) {
                    sun.add(tc.service_id);
                }
                if(tc.friday.equals("1") && !fri.contains(tc.service_id) && !week.contains(tc.service_id)) {
                    fri.add(tc.service_id);
                }
			}
		} catch (IOException e) {
			Log.e(TAG, "Error reading GTFS");
			e.printStackTrace();
		}
        String[] tmp = new String[fri.size()];
        tmp = fri.toArray(tmp);
        fridays = TransitTrip.returnTripsWithScheduleIds(tmp);

        tmp = new String[sat.size()];
        tmp = sat.toArray(tmp);
        saturdays = TransitTrip.returnTripsWithScheduleIds(tmp);

        tmp = new String[sun.size()];
        tmp = sun.toArray(tmp);
        sundays = TransitTrip.returnTripsWithScheduleIds(tmp);

        tmp = new String[week.size()];
        tmp = week.toArray(tmp);
        weekdays = TransitTrip.returnTripsWithScheduleIds(tmp);
	}
}
