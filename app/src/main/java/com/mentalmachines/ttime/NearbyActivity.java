package com.mentalmachines.ttime;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.mentalmachines.ttime.adapter.NearbyLVAdapter;
import com.mentalmachines.ttime.fragments.AlertDetailFragment;
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.services.StopService;

import java.util.ArrayList;

public class NearbyActivity extends AppCompatActivity {
	/**
	 * An activity showing a stop or list of stops selected from a route
	 */
    private static final String TAG = "NearbyActivity";
    public static final Double BOSTON_LAT = 42.3601;
    public static final Double BOSTON_LNG = -71.0589;

    public ListView mStopList;
    ProgressDialog mProgress = null;
    //private GoogleMap mMap;
    private Location mLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.nr_toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        /*LocalBroadcastManager.getInstance(this).registerReceiver(mDetailsUpdated,
                new IntentFilter(StopService.TAG));*/
        mStopList = (ListView) findViewById(R.id.nr_stoplist);
        final SwipeRefreshLayout swipeViewGroup = (SwipeRefreshLayout)findViewById(R.id.nr_swipe);
        swipeViewGroup.setOnRefreshListener(refreshList);
        swipeViewGroup.setColorSchemeColors(R.color.colorPrimary, R.color.colorPrimaryDark);
        //show a progress dialog when the list is empty and the user is waiting, refreshing doesn't work here
        mProgress = ProgressDialog.show(this, "", getString(R.string.getting_data), true, true);

        mLocation = TTimeApp.getPhoneLocation((TTimeApp)getApplication());
        if(mLocation == null) {
            LocalBroadcastManager.getInstance(this).registerReceiver(gotLocation,
                    new IntentFilter(TTimeApp.TAG));
        } else {
            new PopMap().execute();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mProgress != null && mProgress.isShowing()) {
            mProgress.cancel();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mDetailsUpdated);
    }

    //very much like the Main activity, TODO open alert activity to the alert in the tag
    public void openAlerts(View v) {
        //click listener in the t_stop layout
        final String alertId = (String) v.getTag();
        Log.d(TAG, "open alert " + alertId);
        Toast.makeText(this, "open alert stop detail" + alertId, Toast.LENGTH_SHORT).show();

        FragmentManager fm = getSupportFragmentManager();
        AlertDetailFragment alertsDetailFragment = new AlertDetailFragment();

        Bundle args = new Bundle();
        args.putString(DBHelper.KEY_ALERT_ID, alertId);
        alertsDetailFragment.setArguments(args);
        fm.beginTransaction().add(R.id.container, alertsDetailFragment).addToBackStack(null).commit();

    }


    SwipeRefreshLayout.OnRefreshListener refreshList = new SwipeRefreshLayout.OnRefreshListener() {

        @Override
        public void onRefresh() {
            Log.d(TAG, "on refresh");
            //reloadTimes();
        }
    };

    BroadcastReceiver gotLocation = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mLocation = TTimeApp.getPhoneLocation((TTimeApp)getApplication());
            if(mLocation == null) {
                Log.e(TAG, "no location after TTime broadcast");
                //TODO show error to user
                if(mProgress != null && mProgress.isShowing()) mProgress.cancel();
                Toast.makeText(context, "Cannot find your location", Toast.LENGTH_LONG).show();
                finish();
            } else {
                //TODO use location data to create list
                new PopMap().execute();
            }
        }
    };

    /*@Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng boston = new LatLng(BOSTON_LAT, BOSTON_LNG);

        //mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                boston, 13));

        mMap.addMarker(new MarkerOptions()
                .title(getString(R.string.boston))
                .position(boston));
    }

    /*public void reloadTimes() {
        Log.d(TAG, "reload times");
        if(Utils.checkNetwork(this)) {
            ((SwipeRefreshLayout)findViewById(R.id.route_swipe)).setRefreshing(true);
            //the main activity broadcast receiver will reload the data into the adapter and list
            final Intent tnt = new Intent(this, StopService.class);
            tnt.putExtra(StopService.TAG, mStopDetail.mainStop);
            startService(tnt);

        } else {
            Toast.makeText(this, getString(R.string.chkNet), Toast.LENGTH_SHORT).show();
        }
    }*/

    /*BroadcastReceiver mDetailsUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "service completed");
            if(mProgress != null && mProgress.isShowing()) {
                mProgress.cancel();
            }
            if(intent.getExtras() == null) {
                //error broadcast, no extras
                Toast.makeText(NearbyActivity.this,
                        getString(R.string.schedSvcErr), Toast.LENGTH_SHORT).show();
                return;
            }
            mStopDetail = intent.getParcelableExtra(StopService.TAG);
            new PopMap(false).execute(mStopDetail.mainStop.stopId);
            Log.d(TAG, mStopDetail.mainStop.stopName +
                    " data check, stop detail list size " + mStopDetail.mStopList.size());
            setList();
        }
    };*/

    /**
     *
     */
    class PopMap extends AsyncTask<Void, Void, StopData[]> {

        /**
         * pushing the db I/O into a background thread
         */
        @Override
        protected StopData[] doInBackground(Void... voids) {
            if(mLocation == null) {
                Log.e(TAG, "location require to display activity");
                return null;
            }
            ArrayList<StopData> stopData = StopService.createStopList(mLocation);
            final StopData[] adapterList = new StopData[stopData.size()];
            stopData.toArray(adapterList);
            return adapterList;
        }

        @Override
        protected void onPostExecute(StopData[] stopDatas) {
            super.onPostExecute(stopDatas);
            if(mProgress != null && mProgress.isShowing()) mProgress.cancel();
            mStopList.setAdapter(new NearbyLVAdapter(stopDatas));
        }
    }

}
