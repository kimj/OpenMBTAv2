package com.mentalmachines.ttime;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.mentalmachines.ttime.objects.Utils;
import com.mentalmachines.ttime.services.DBCreateStopsRoutes;
import com.mentalmachines.ttime.services.GetMBTARequestService;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Application class made for sharing a few tools
 * Singleton database helper,device location and Google API client -> all found here
 * Created by emezias on 2/5/16.
 * this class to manage a single Google API client used across the app
 */
public class TTimeApp extends Application implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = "TTimeApp";
    public static DBHelper sHelper;
    //most variables are singletons for the app's activities to use and reuse
    private static Location sLastLocation = null;
    public static final int F15MINS = 900000;

    private static GoogleApiClient mLocationClient = null;
    private static LocationRequest mLocationRequest = null;
    private static LocationListener mLocationListener = null;
    private static boolean mlocationRequestRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        sHelper = new DBHelper(this);
        final SQLiteDatabase db = sHelper.getReadableDatabase();
        if(Utils.checkNetwork(this)) {
            if(DatabaseUtils.queryNumEntries(db, DBHelper.DB_ROUTE_TABLE) == 0) {
                Log.i(TAG, "no db");
                startService(new Intent(this, DBCreateStopsRoutes.class));
            } else {
                Log.i(TAG, "get alerts");
                startService(new Intent(this, GetMBTARequestService.class));
            }
        } else {
            Log.e(TAG, "network error, app services not started");
        }
        //Create an http cache
        try {
            HttpResponseCache.install(new File(getCacheDir(), "http"), (100 * 1024 * 1024)); // 100 MiB
        } catch (IOException e) {
            Log.e(TAG, "HTTP response cache installation failed", e);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if(mlocationRequestRunning) {
            //cancel requests!
            LocationServices.FusedLocationApi.removeLocationUpdates(mLocationClient, (LocationCallback) mLocationListener);
        }
        if(mLocationClient.isConnected()) {
            mLocationClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            final Location tmp = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);
            if(tmp != null && isLocationFresh(tmp)) {
                sLastLocation = tmp;
                Log.v(TAG, "sLastLocation set");
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(TAG));
            } else {
                requestLocation(this);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error");
            //TODO show error in Main, exit app
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG, "connection failed");
        if(mLocationListener != null && mLocationClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mLocationClient, mLocationListener);
        }
    }

    static boolean isLocationFresh(Location l) {
        return (System.currentTimeMillis() - l.getTime()) < F15MINS;
    }

    static void requestLocation(final TTimeApp ctx) {

        //make sure client is connected before running
        mlocationRequestRunning = true;
        sLastLocation = null;
        if(mLocationRequest == null) {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            mLocationRequest.setInterval(5000); //5s
            mLocationRequest.setFastestInterval(5000); //5
        }

        try {
            //the getListener will create a LocationCallback
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mLocationClient, mLocationRequest, getListener(ctx));
            Log.v(TAG, "request location updates");
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error");
            //error shows in Main Activity and exits app
        }

        // timeout to cancel the location updates
        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                // This is just to insure that location requests time out
                if(mlocationRequestRunning) {
                    //cancel requests!
                    LocationServices.FusedLocationApi.removeLocationUpdates(mLocationClient, mLocationListener);
                    mlocationRequestRunning = false;
                    try {
                        final Location tmp = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);
                        if(tmp != null) {
                            sLastLocation = tmp;
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "Location permission error");
                        //error shows in Main Activity and exits app
                    }
                    LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(TAG));
                    Log.v(TAG, "timeout broadcast");
                }
            }

        }, 60000); //cancel location request after 60s

    }

    static boolean isClientReady(TTimeApp ctx) {
        if(mLocationClient == null || !mLocationClient.isConnected()) {
            return false;
        }
        return true;
    }

    public static Location getPhoneLocation(TTimeApp ctx) {
        if(sLastLocation != null && isLocationFresh(sLastLocation)) {
            //fastest, first choice
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(TAG));
            return sLastLocation;
        }

        if(isClientReady(ctx)) {
            //re use the connected client to get a better read
            try {
                final Location tmp = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);
                if(tmp != null && isLocationFresh(tmp)) {
                    sLastLocation = tmp;
                    Log.v(TAG, "sLastLocation set");
                    return sLastLocation;
                } else {
                    requestLocation(ctx);
                    return null;
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission error");
                //TODO show error in Main, exit app
                return null;
            }
        } else {
            //first ever call for location!
            Log.w(TAG, "client not ready to request location");
            mLocationClient = new GoogleApiClient.Builder(ctx).addApi(LocationServices.API)
                    .addConnectionCallbacks(ctx)
                    .addOnConnectionFailedListener(ctx).build();
            mLocationClient.connect();
            return null;
        }
    }

    static LocationListener getListener(final Context ctx) {
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider
                LocationServices.FusedLocationApi.removeLocationUpdates(mLocationClient, this);
                //timer task will insure that the updates are cancelled when there is no fix
                if(location == null) {
                    Log.w(TAG, "null location returned");
                    return;
                } else if(!isLocationFresh(location)) {
                    Log.w(TAG, "location returned is not fresh, no broadcast");
                    sLastLocation = location;
                    return;
                }
                sLastLocation = location;
                mlocationRequestRunning = false;
                Log.v(TAG, "got new location, sending local broadcast");
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(TAG));
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };
        return mLocationListener;
    }
}
