package com.mentalmachines.ttime;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.mentalmachines.ttime.adapter.NearbyLVAdapter;
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.services.StopService;

import java.util.ArrayList;

public class NearbyActivity extends AppCompatActivity {
    public static final String TAG = "NearbyActivity";
    /*public static final Double BOSTON_LAT = 42.3601;
    public static final Double BOSTON_LNG = -71.0589;*/

    public ListView mStopList;
    SwipeRefreshLayout mRefreshLayout;
    private Location mLocation;
    ProgressDialog mPD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.nr_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.action_nearby));
        }
        mStopList = (ListView) findViewById(R.id.nr_stoplist);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.nr_swipe);
        mRefreshLayout.setOnRefreshListener(refreshList);
        mRefreshLayout.setColorSchemeColors(R.color.colorPrimary, R.color.colorPrimaryDark);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRefreshLayout.isRefreshing()) {
            mRefreshLayout.setRefreshing(false);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(gotLocation);
        }
    }

    void setupList() {
        mRefreshLayout.setEnabled(false);
        mLocation = TTimeApp.getPhoneLocation((TTimeApp) getApplication());
        if (mLocation == null) {
            Log.d(TAG, "wait for broadcast location");
            LocalBroadcastManager.getInstance(this).registerReceiver(gotLocation,
                    new IntentFilter(TTimeApp.TAG));
            mRefreshLayout.setRefreshing(true);
        } else {
            new PopMap().execute();
            Log.d(TAG, "calling pop map");
        }
    }

    // TODO open specific alert

    /**
     * return to the main activity and show the alerts fragment
     * @param v, the alert button with the specific alert id on it
     */
    public void openAlerts(View v) {
        //click listener in the layout
        final String alertId = (String) v.getTag();
        Log.d(TAG, "open alert " + alertId);
        Toast.makeText(this, "open alert stop detail" + alertId, Toast.LENGTH_SHORT).show();

        final Intent tnt = new Intent(this, MainActivity.class);
        tnt.putExtra(TAG, alertId);
        startActivity(tnt);
    }

    public void mapIt(View v) {
        //click listener in the layout
        final StopData stop = (StopData) ((View) v.getParent()).getTag(R.layout.nearby_stop);
        Uri gmmIntentUri;
        Intent mapIntent = null;
        switch (v.getId()) {
            case R.id.nr_map:
                // Creates an Intent that will load a map of the stop
                gmmIntentUri = Uri.parse("geo:0,0?q=" + stop.stopLat + "," + stop.stopLong + "(" + stop.stopName + ")");
                //geo:0,0?q=latitude,longitude(label)
                mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                break;
            case R.id.nr_directions:
                //Uri gmmIntentUri = Uri.parse("google.navigation:q=Taronga+Zoo,+Sydney+Australia&mode=b");
                // Creates an Intent that will load a map of the stop
                gmmIntentUri = Uri.parse("google.navigation:q=" + stop.stopLat + "," + stop.stopLong + "&mode=w");
                //geo:0,0?q=latitude,longitude(label)
                mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                break;
        }

        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    SwipeRefreshLayout.OnRefreshListener refreshList = new SwipeRefreshLayout.OnRefreshListener() {

        @Override
        public void onRefresh() {
            Log.d(TAG, "on refresh");
            setupList();
        }
    };

    BroadcastReceiver gotLocation = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mLocation = TTimeApp.getPhoneLocation((TTimeApp) getApplication());

            if (mLocation == null) {
                Log.e(TAG, "no location after TTime broadcast?");
                //show error to user?
                Toast.makeText(context, "Cannot find your location", Toast.LENGTH_LONG).show();
                //finish();
                if (mRefreshLayout.isRefreshing()) mRefreshLayout.setRefreshing(false);
            } else {
                // use location data to create list
                new PopMap().execute();
                Log.d(TAG, "got broadcast, create list with location");
                LocalBroadcastManager.getInstance(context).unregisterReceiver(gotLocation);
            }
        }
    };

    class PopMap extends AsyncTask<Void, Void, StopData[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mPD = ProgressDialog.show(NearbyActivity.this, "", getString(R.string.getting_data), true, true);
        }

        /**
         * pushing the db I/O into a background thread
         */
        @Override
        protected StopData[] doInBackground(Void... voids) {
            if(mLocation == null) {
                Log.e(TAG, "location require to display activity");
                return null;
            } else {
                Log.i(TAG, "got location");
            }
            ArrayList<StopData> stopData = StopService.createNearbyStopList(NearbyActivity.this, mLocation);
            final StopData[] adapterList = new StopData[stopData.size()];
            stopData.toArray(adapterList);
            return adapterList;
        }

        @Override
        protected void onPostExecute(StopData[] stopDatas) {
            super.onPostExecute(stopDatas);
            if (mRefreshLayout.isRefreshing()) mRefreshLayout.setRefreshing(false);
            mStopList.setAdapter(new NearbyLVAdapter(getApplicationContext(), stopDatas));
            mPD.dismiss();
            mRefreshLayout.setEnabled(true);
        }
    }

}
