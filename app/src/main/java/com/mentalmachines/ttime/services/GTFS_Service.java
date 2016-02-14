package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.mentalmachines.ttime.objects.gtfs.TransitCalendar;
import com.mentalmachines.ttime.objects.gtfs.TransitStopTimes;
import com.mentalmachines.ttime.objects.gtfs.TransitTrip;

/**
 * Created by emezias on 2/5/16.
 */
public class GTFS_Service extends IntentService {
    public static final String TAG = "GTFS_Service";

    public GTFS_Service() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "start");

        TransitTrip.createTrips(this);
        Log.d(TAG, "transit trips read");
        TransitCalendar.parseCalendar(this);
        Log.d(TAG, "divided calendar");
        TransitStopTimes.createScheduleEntries(this);
        Log.d(TAG, "finished schedule");
        startService(new Intent(this, GetMBTARequestService.class));
        Log.d(TAG, "starting service to load routes and stops");
    }

}
