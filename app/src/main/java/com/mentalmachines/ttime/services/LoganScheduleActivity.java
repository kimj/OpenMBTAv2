package com.mentalmachines.ttime.services;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.adapter.LoganScheduleAdapter;
import com.mentalmachines.ttime.fragments.RouteFragment;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.ScheduleLogan;
import com.mentalmachines.ttime.objects.Utils;

import java.util.Calendar;

public class LoganScheduleActivity extends AppCompatActivity {
	/**
	 * An activity showing a stop or list of stops selected from a route
	 */
    public static final String TAG = "LoganScheduleActivity";
    ProgressDialog mProgress = null;
    public RecyclerView mList;
    public Route mRoute;
    boolean weekdayDone = false, saturdayDone = false, sundayDone = false;

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
                new IntentFilter(LoganScheduleSvc.TAG));
        mList = (RecyclerView) findViewById(R.id.sch_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //final Bundle b = getIntent().getExtras();
        /*if(b != null && b.containsKey(DBHelper.KEY_ROUTE_ID)) {
            mRoute = b.getParcelable(DBHelper.KEY_ROUTE_ID);
            Log.d(TAG, "has route extra " + mRoute.name);
            TODO cleanup
        }*/
        mProgress = ProgressDialog.show(this, "", getString(R.string.getting_data), true, true);
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

                ab.setTitle(Route.readableName(this, mRoute.name));
                ab.setSubtitle(getString(R.string.weekdays));
                if(!weekdayDone) {
                    mProgress = ProgressDialog.show(this, "", getString(R.string.getting_data), true, true);
                    Intent svc = new Intent(this, LoganScheduleSvc.class);
                    svc.putExtra(LoganScheduleSvc.TAG, Calendar.TUESDAY);
                    svc.putExtra(DBHelper.KEY_ROUTE_ID, mRoute);
                    startService(svc);
                    Log.d(TAG, "get weekday");
                } else {
                    Log.d(TAG, "set new weekday adapter");
                    mList.setAdapter(new LoganScheduleAdapter(scheduleDay, mRoute));
                }
                break;
            case Calendar.SUNDAY:
                ab.setTitle(Route.readableName(this, mRoute.name));
                ab.setSubtitle(getString(R.string.sunday));

                if(!sundayDone) {
                    mProgress = ProgressDialog.show(this, "", getString(R.string.getting_data), true, true);
                    Intent svc = new Intent(this, LoganScheduleSvc.class);
                    svc.putExtra(LoganScheduleSvc.TAG, Calendar.SUNDAY);
                    svc.putExtra(DBHelper.KEY_ROUTE_ID, mRoute);
                    startService(svc);
                } else {
                    mList.setAdapter(new LoganScheduleAdapter(scheduleDay, mRoute));
                    Log.d(TAG, "set new sunday adapter");
                }
                break;
            case Calendar.SATURDAY:
                if(!saturdayDone) {
                    mProgress = ProgressDialog.show(this, "", getString(R.string.getting_data), true, true);
                    Intent svc = new Intent(this, LoganScheduleSvc.class);
                    svc.putExtra(LoganScheduleSvc.TAG, Calendar.SATURDAY);
                    svc.putExtra(DBHelper.KEY_ROUTE_ID, mRoute);
                    startService(svc);
                } else {
                    Log.d(TAG, "set new saturday adapter");
                    mList.setAdapter(new LoganScheduleAdapter(scheduleDay, mRoute));
                }
                ab.setTitle(Route.readableName(this, mRoute.name));
                ab.setSubtitle(getString(R.string.saturdays));
                break;
        }
        //Is this a one way route? hide the FAB
        if(mRoute.mInboundStops == null || mRoute.mOutboundStops == null
                || mRoute.mInboundStops.size() == 0 || mRoute.mOutboundStops.size() == 0) {
            findViewById(R.id.sch_switch).setVisibility(View.GONE);
        }
    }

    /*********click listeners ********/
    public void switchSchedule(View view) {
        //return isInbound
        final int width = Utils.getScreenWidth(this);
        Log.d(TAG, "FAB click in schedule screen");
        final AnimatorSet set = new AnimatorSet();
        if(((LoganScheduleAdapter) mList.getAdapter()).switchDirection()) {
            ObjectAnimator.ofFloat(view, "rotation", 540f).start();
            //mItems = getArguments().getStringArray(IN_STOPS_LIST);
            ((FloatingActionButton)view).setImageResource(R.drawable.ic_menu_forward);
            set.play(ObjectAnimator.ofFloat(mList, "translationX", 0, 2 * width))
                    .before(ObjectAnimator.ofFloat(mList, "translationX", -width, 0));
        } else {
            ObjectAnimator.ofFloat(view, "rotation", -540f).start();
            //TODO -> call the schedule service to get the latest times
            ((FloatingActionButton)view).setImageResource(R.drawable.ic_menu_back);

            set.play(ObjectAnimator.ofFloat(mList, "translationX", 0, -width))
                    .before(ObjectAnimator.ofFloat(mList, "translationX", width, 0));
        }
        set.start();
    }

    public void setList(View v) {
        Toast.makeText(LoganScheduleActivity.this, "change schedule...", Toast.LENGTH_SHORT).show();
    }
    /*********end listeners ********/

    BroadcastReceiver mScheduleReady = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "service completed");
            final Bundle b = intent.getExtras();

            if(b == null || b.getBoolean(LoganScheduleSvc.TAG)) {
                Log.w(TAG, "error fetching schedule");
                Toast.makeText(LoganScheduleActivity.this,
                        getString(R.string.schedSvcErr), Toast.LENGTH_SHORT).show();

            }
            mRoute = b.getParcelable(DBHelper.KEY_ROUTE_ID);
            if(mProgress != null && mProgress.isShowing()) {
                mProgress.cancel();
            }
            if(mRoute == null) {
                Log.e(TAG, "fatal error fetching schedule, no route");
                return;
            }

            final int scheduleDay = b.getInt(TAG);
            Log.d(TAG, "day " + scheduleDay + " route sent from schedule svc " + mRoute.name);
            showSchedule(scheduleDay, getSupportActionBar());

            switch(scheduleDay) {
                case Calendar.TUESDAY:
                    weekdayDone = true;
                    break;
                case Calendar.SATURDAY:
                    saturdayDone = true;
                    break;
                case Calendar.SUNDAY:
                    sundayDone = true;
                    break;
            }
        }
    };


}
