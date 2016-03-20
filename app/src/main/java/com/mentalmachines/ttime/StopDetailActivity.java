package com.mentalmachines.ttime;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mentalmachines.ttime.adapter.StopDetailAdapter;
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
    ProgressDialog mProgress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stops);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mDetailsUpdated,
                new IntentFilter(StopService.TAG));
        mList = (RecyclerView) findViewById(R.id.route_list);
        final SwipeRefreshLayout swipeViewGroup = (SwipeRefreshLayout)findViewById(R.id.route_swipe);
        swipeViewGroup.setOnRefreshListener(refreshList);
        swipeViewGroup.setColorSchemeColors(R.color.colorPrimary, R.color.colorPrimaryDark);
        //show a progress dialog when the list is empty and the user is waiting, refreshing doesn't work here
        mProgress = ProgressDialog.show(this, "", getString(R.string.loading), true, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mProgress != null && mProgress.isShowing()) {
            mProgress.cancel();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDetailsUpdated);
    }

    //very much like the Main activity, TODO open alert activity to the alert in the tag
    public void openAlerts(View v) {
        //click listener in the t_stop layout
        final String alertId = (String) v.getTag();
        Log.d(TAG, "open alert " + alertId);
        Toast.makeText(this, "open alert " + alertId, Toast.LENGTH_SHORT).show();
    }

    void setList() {
        if(mStopDetail.mainStop.stopName.contains(" - ")) {
            setTitle(mStopDetail.mainStop.stopName.substring(0,
                    mStopDetail.mainStop.stopName.indexOf(" -")));
        } else {
            setTitle(mStopDetail.mainStop.stopName);
        }

        StopData[] adapterList = new StopData[mStopDetail.mStopList.size()];
        adapterList = mStopDetail.mStopList.toArray(adapterList);
        mList.setAdapter(new StopDetailAdapter(adapterList));
        Log.d(TAG, "set new adapter");
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
        Log.d(TAG, "reload times");
        if(TTimeApp.checkNetwork(this)) {
            ((SwipeRefreshLayout)findViewById(R.id.route_swipe)).setRefreshing(true);
            //the main activity broadcast receiver will reload the data into the adapter and list
            final Intent tnt = new Intent(this, StopService.class);
            tnt.putExtra(StopService.TAG, mStopDetail.mainStop);
            startService(tnt);

        } else {
            Toast.makeText(this, getString(R.string.chkNet), Toast.LENGTH_SHORT).show();
        }
    }

    BroadcastReceiver mDetailsUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "service completed");
            if(mProgress != null && mProgress.isShowing()) {
                mProgress.cancel();
            }
            if(intent.getExtras() == null) {
                //error broadcast, no extras
                Toast.makeText(StopDetailActivity.this,
                        getString(R.string.schedSvcErr), Toast.LENGTH_SHORT).show();
                return;
            }
            mStopDetail = intent.getParcelableExtra(StopService.TAG);
            Log.d(TAG, mStopDetail.mainStop.stopName +
                    " data check, stop detail list size " + mStopDetail.mStopList.size());
            setList();
        }
    };

}
