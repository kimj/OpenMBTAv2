package com.mentalmachines.ttime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.mentalmachines.ttime.adapter.StopFragmentAdapter;
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.objects.StopList;
import com.mentalmachines.ttime.services.StopService;

public class StopDetailActivity extends AppCompatActivity {
	/**
	 * An activity showing a stop or list of stops selected from a route
	 */
    private static final String TAG = "StopDetailActivity";
    public RecyclerView mList;
    public StopList mStopDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stops);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        LocalBroadcastManager.getInstance(this).registerReceiver(mDetailsUpdated,
                new IntentFilter(StopService.TAG));
        mList = (RecyclerView) findViewById(R.id.route_list);
        final SwipeRefreshLayout swipeViewGroup = (SwipeRefreshLayout)findViewById(R.id.route_swipe);
        swipeViewGroup.setOnRefreshListener(refreshList);
        swipeViewGroup.setColorSchemeColors(R.color.colorPrimary, R.color.colorPrimaryDark);
        swipeViewGroup.setRefreshing(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDetailsUpdated);
    }

    void setList() {
        setTitle(mStopDetail.mainStop.stopName);
        final StopData[] adapterList = new StopData[mStopDetail.mStopList.size()];
        mStopDetail.mStopList.toArray(adapterList);
        mList.setAdapter(new StopFragmentAdapter(adapterList));
        ((SwipeRefreshLayout)findViewById(R.id.route_swipe)).setRefreshing(false);
    }


    SwipeRefreshLayout.OnRefreshListener refreshList = new SwipeRefreshLayout.OnRefreshListener() {

        @Override
        public void onRefresh() {
            Log.d(TAG, "on refresh");
            reloadTimes();
        }
    };

    public void reloadTimes() {
        if(TTimeApp.checkNetwork(this)) {
            ((SwipeRefreshLayout)findViewById(R.id.route_swipe)).setRefreshing(true);
            //the main activity broadcast receiver will reload the data into the adapter and list
            final Intent tnt = new Intent(this, StopService.class);
            tnt.putExtra(StopService.TAG, mStopDetail.mainStop);
            startService(tnt);
        } else {
            Toast.makeText(this, "check network", Toast.LENGTH_SHORT).show();
        }
    }

    BroadcastReceiver mDetailsUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "service completed");
            mStopDetail = intent.getParcelableExtra(StopService.TAG);
            setList();
        }
    };

}
