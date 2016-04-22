package com.mentalmachines.ttime;

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

import com.mentalmachines.ttime.adapter.ScheduleStopAdapter;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.Schedule;
import com.mentalmachines.ttime.objects.Utils;
import com.mentalmachines.ttime.services.ShowScheduleService;

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
                new IntentFilter(ShowScheduleService.TAG));
        mList = (RecyclerView) findViewById(R.id.sch_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                setScheduleIntoList(ShowScheduleService.sWeekdays, ab);
                ab.setSubtitle(getString(R.string.weekdays));
                Log.d(TAG, "set new weekday adapter");
                break;
            case Calendar.SUNDAY:
                setScheduleIntoList(ShowScheduleService.sSunday, ab);
                ab.setSubtitle(getString(R.string.sunday));
                Log.d(TAG, "set new sunday adapter");
                break;
            case Calendar.SATURDAY:
                setScheduleIntoList(ShowScheduleService.sSaturday, ab);
                ab.setSubtitle(getString(R.string.saturdays));
                Log.d(TAG, "set new saturday adapter");
                break;
        }
        //Is this a one way route? hide the FAB
        final Route route = ((ScheduleStopAdapter) mList.getAdapter()).mSchedule.route;
        if(route.mInboundStops == null || route.mOutboundStops == null
                || route.mInboundStops.size() == 0 || route.mOutboundStops.size() == 0) {
            findViewById(R.id.sch_switch).setVisibility(View.GONE);
        }
    }

    void setScheduleIntoList(Schedule s, ActionBar ab) {
        if(!s.scheduleIsLoaded()) {
            mProgress = ProgressDialog.show(this, "", getString(R.string.getting_data), true, true);
            Log.w(TAG, "schedules still loading");
            return;
        }
        mList.setAdapter(new ScheduleStopAdapter(s));
        ab.setTitle(Route.readableName(this, s.route.name));
        Log.d(TAG, "set new satureday adapter");
    }

    /*********click listeners ********/
    public void switchSchedule(View view) {
        //return isInbound
        final int width = Utils.getScreenWidth(this);
        Log.d(TAG, "FAB click in schedule screen");
        final AnimatorSet set = new AnimatorSet();
        if(((ScheduleStopAdapter) mList.getAdapter()).switchDirection()) {
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
        Toast.makeText(ShowScheduleActivity.this, "change schedule...", Toast.LENGTH_SHORT).show();
    }
    /*********end listeners ********/

    BroadcastReceiver mScheduleReady = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "service completed");
            final Bundle b = intent.getExtras();

            if(b == null || b.getBoolean(ShowScheduleService.TAG)) {
                Log.w(TAG, "error fetching schedule");
                Toast.makeText(ShowScheduleActivity.this,
                        getString(R.string.schedSvcErr), Toast.LENGTH_SHORT).show();
                return;
            }

            if(mList.getAdapter() == null) {
                //service returns the schedule day
                showSchedule(b.getInt(TAG), getSupportActionBar());
                //should be enough time while that draws to finish parsing the other days...
                //TODO manage this better, error check schedule object for null
            }
            //at least one schedule is not null, calling the service for the others
            // waiting for a return to make the next call, parallel processing
            final Intent svc = new Intent(context, ShowScheduleService.class);
            if(!ShowScheduleService.sWeekdays.scheduleIsLoaded()) {
                svc.putExtra(ShowScheduleService.TAG, Calendar.TUESDAY);
                context.startService(svc);
            } else if(!ShowScheduleService.sSaturday.scheduleIsLoaded()) {
                svc.putExtra(ShowScheduleService.TAG, Calendar.SATURDAY);
                context.startService(svc);
            } else if(!ShowScheduleService.sSunday.scheduleIsLoaded()) {
                svc.putExtra(ShowScheduleService.TAG, Calendar.SUNDAY);
                context.startService(svc);
            }

            if(mProgress != null && mProgress.isShowing()) {
                mProgress.cancel();
            }
        }
    };


}
