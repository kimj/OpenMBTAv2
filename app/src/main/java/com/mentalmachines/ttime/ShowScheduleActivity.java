package com.mentalmachines.ttime;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mentalmachines.ttime.adapter.ScheduleStopAdapter;
import com.mentalmachines.ttime.services.FullScheduleService;

public class ShowScheduleActivity extends AppCompatActivity {
	/**
	 * An activity showing a stop or list of stops selected from a route
	 */
    private static final String TAG = "ShowScheduleActivity";
    ProgressDialog mProgress = null;
    public RecyclerView mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        //Schedule Service is launched before start Activity
        LocalBroadcastManager.getInstance(this).registerReceiver(mScheduleReady,
                new IntentFilter(FullScheduleService.TAG));
        mList = (RecyclerView) findViewById(R.id.times_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.schedule, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_weekdays:
                break;
            case R.id.menu_saturdays:
                break;
            case R.id.menu_sunday:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mScheduleReady);
    }

    public void setList(View v) {
        Toast.makeText(ShowScheduleActivity.this, "change schedule...", Toast.LENGTH_SHORT).show();
    }

    BroadcastReceiver mScheduleReady = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "service completed");
            final boolean hasError = intent.getExtras().getBoolean(FullScheduleService.TAG);
            if(hasError) {
                Log.w(TAG, "error fetching schedule");
                Toast.makeText(ShowScheduleActivity.this,
                        getString(R.string.schedSvcErr), Toast.LENGTH_SHORT).show();
                return;
            }
            mList.setAdapter(new ScheduleStopAdapter(FullScheduleService.sWeekdays));
            final ActionBar ab = getSupportActionBar();
            ab.setTitle(FullScheduleService.sWeekdays.route.name);
            ab.setSubtitle(getString(R.string.weekdays));
            Log.d(TAG, "set new weekday adapter");
            if(mProgress != null && mProgress.isShowing()) {
                mProgress.cancel();
            }
        }
    };


}
