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
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.services.FullScheduleService;

import java.util.Calendar;

public class ShowScheduleActivity extends AppCompatActivity {
	/**
	 * An activity showing a stop or list of stops selected from a route
	 */
    public static final String TAG = "ShowScheduleActivity";
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
        mList = (RecyclerView) findViewById(R.id.sch_list);
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
        //tried reset service here...
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.schedule, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final ActionBar ab = getSupportActionBar();
        if(mProgress != null && mProgress.isShowing()) {
            mProgress.cancel();
        }
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_weekdays:
                if(!ab.getSubtitle().equals(getString(R.string.weekdays))) {
                    showSchedule(Calendar.TUESDAY, ab);
                }
                break;
            case R.id.menu_saturdays:
                if(!ab.getSubtitle().equals(getString(R.string.saturdays))) {
                    showSchedule(Calendar.SATURDAY, ab);
                }
                break;
            case R.id.menu_sunday:
                if(!ab.getSubtitle().equals(getString(R.string.sunday))) {
                    showSchedule(Calendar.SUNDAY, ab);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mScheduleReady);
    }

    void showSchedule(int scheduleDay, ActionBar ab) {
        switch(scheduleDay) {
            case Calendar.TUESDAY:
                mList.setAdapter(new ScheduleStopAdapter(FullScheduleService.sWeekdays));
                ab.setTitle(Route.readableName(this, FullScheduleService.sWeekdays.route.name));
                ab.setSubtitle(getString(R.string.weekdays));
                Log.d(TAG, "set new weekday adapter");
                break;
            case Calendar.SUNDAY:
                mList.setAdapter(new ScheduleStopAdapter(FullScheduleService.sSunday));
                ab.setTitle(Route.readableName(this, FullScheduleService.sSunday.route.name));
                ab.setSubtitle(getString(R.string.sunday));
                Log.d(TAG, "set new sunday adapter");
                break;
            case Calendar.SATURDAY:
                mList.setAdapter(new ScheduleStopAdapter(FullScheduleService.sSaturday));
                ab.setTitle(Route.readableName(this, FullScheduleService.sSaturday.route.name));
                ab.setSubtitle(getString(R.string.saturdays));
                Log.d(TAG, "set new satureday adapter");
                break;
        }
        //Is this a one way route? hide the FAB
        final Route route = ((ScheduleStopAdapter) mList.getAdapter()).mSchedule.route;
        if(route.mInboundStops == null || route.mOutboundStops == null
                || route.mInboundStops.size() == 0 || route.mOutboundStops.size() == 0) {
            findViewById(R.id.sch_switch).setVisibility(View.GONE);
        }
    }

    /*********click listeners ********/
    public void switchSchedule(View v) {
        ((ScheduleStopAdapter) mList.getAdapter()).switchDirection();
    }

    public void setList(View v) {
        Toast.makeText(ShowScheduleActivity.this, "change schedule...", Toast.LENGTH_SHORT).show();
    }
    /*********end listeners ********/

    BroadcastReceiver mScheduleReady = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "service completed");
            final Bundle b = intent.getExtras();

            if(b == null || b.getBoolean(FullScheduleService.TAG)) {
                Log.w(TAG, "error fetching schedule");
                Toast.makeText(ShowScheduleActivity.this,
                        getString(R.string.schedSvcErr), Toast.LENGTH_SHORT).show();
                return;
            }

            if(mList.getAdapter() == null) {
                showSchedule(b.getInt(TAG), getSupportActionBar());
            }
            //at least one schedule is not null, calling the service for the others
            // waiting for a return to make the next call, parallel processing
            final Intent svc = new Intent(context, FullScheduleService.class);
            if(FullScheduleService.sWeekdays == null) {
                svc.putExtra(FullScheduleService.TAG, Calendar.TUESDAY);
                context.startService(svc);
                return;
            }
            if(FullScheduleService.sSaturday == null) {
                svc.putExtra(FullScheduleService.TAG, Calendar.SATURDAY);
                context.startService(svc);
                return;
            }
            if(FullScheduleService.sSunday == null) {
                svc.putExtra(FullScheduleService.TAG, Calendar.SUNDAY);
                context.startService(svc);
                return;
            }

            //TODO, call the other days
            if(mProgress != null && mProgress.isShowing()) {
                mProgress.cancel();
            }
        }
    };


}
