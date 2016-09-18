package com.mentalmachines.ttime;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mentalmachines.ttime.adapter.StopDetailAdapter;
import com.mentalmachines.ttime.fragments.AlertDetailFragment;
import com.mentalmachines.ttime.objects.Favorite;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.objects.StopList;
import com.mentalmachines.ttime.objects.Utils;
import com.mentalmachines.ttime.services.StopService;

public class StopDetailActivity extends AppCompatActivity {
	/**
	 * An activity showing a stop or list of stops selected from a route
	 */
    private static final String TAG = "StopDetailActivity";
    public RecyclerView mList;
    public StopList mStopDetail;
    MenuItem mFavoritesAction;
    ProgressDialog mProgress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stops);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            final Bundle b = getIntent().getExtras();
            if(b != null && b.containsKey(Route.TAG)) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            } //else favorite stop
            setTitle(R.string.stopDetail);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mDetailsUpdated,
                new IntentFilter(StopService.TAG));
        mList = (RecyclerView) findViewById(R.id.route_list);
        final SwipeRefreshLayout swipeViewGroup = (SwipeRefreshLayout)findViewById(R.id.route_swipe);
        swipeViewGroup.setOnRefreshListener(refreshList);
        swipeViewGroup.setColorSchemeColors(R.color.colorPrimary, R.color.colorPrimaryDark);
        //show a progress dialog when the list is empty and the user is waiting, refreshing doesn't work here
        mProgress = ProgressDialog.show(this, "", getString(R.string.getting_data), true, true);
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
        getMenuInflater().inflate(R.menu.stopscreen, menu);
        if(menu != null) {
            mFavoritesAction = menu.getItem(0);
            mFavoritesAction.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            case R.id.menu_favorites:
                Log.i(TAG, "favorites change called");
                if(mStopDetail != null)
                new CheckFavorite(true).execute(mStopDetail.mainStop.stopId);
                break;
            case R.id.menu_alerts:
                //This piece will always create a new alert fragment
                Toast.makeText(this, "TO DO", Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_settings:
                Toast.makeText(this, R.string.action_settings, Toast.LENGTH_SHORT).show();
                break;
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
        Toast.makeText(this, "open alert stop detail" + alertId, Toast.LENGTH_SHORT).show();

        FragmentManager fm = getSupportFragmentManager();
        AlertDetailFragment alertsDetailFragment = new AlertDetailFragment();

        Bundle args = new Bundle();
        args.putString(DBHelper.KEY_ALERT_ID, alertId);
        alertsDetailFragment.setArguments(args);
        fm.beginTransaction().add(R.id.container, alertsDetailFragment).addToBackStack(null).commit();

    }

    void setList() {
        final StopData[] adapterList = new StopData[mStopDetail.mStopList.size()];
        mStopDetail.mStopList.toArray(adapterList);
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
        if(Utils.checkNetwork(this)) {
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
            new CheckFavorite(false).execute(mStopDetail.mainStop.stopId);
            Log.d(TAG, mStopDetail.mainStop.stopName +
                    " data check, stop detail list size " + mStopDetail.mStopList.size());
            ((TextView)findViewById(R.id.stopdetail_name)).setText(mStopDetail.mainStop.stopName);
            setList();
        }
    };

    /**
     *
     */
    class CheckFavorite extends AsyncTask<String, Void, Boolean> {
        boolean menuCall;

        /**
         * pushing the db I/O into a background thread
         * this sets the button's display, does not yet save the change
         * @param change, is this a menu click to change the favorite value or a call to read it
         */
        public CheckFavorite(boolean change) {
            menuCall = change;
        }

        @Override
        protected Boolean doInBackground(String[] params) {
            if(menuCall) {
               menuCall = Favorite.isStopFavorite(params[0]);
               if(menuCall) {
                   //remove favorite stop from table
                   Favorite.dropFavoriteStop(params[0]);
               } else {
                   //start Task to save stop to the faves table
                   /*final Intent tnt = SaveFavoriteSchedule.newInstance(
                           StopDetailActivity.this, mStopDetail.mainStop.stopId);
                   startService(tnt);*/
               }
                return !menuCall;
            }
            return Favorite.isStopFavorite(params[0]);
        }

        @Override
        protected void onPostExecute(Boolean o) {
            super.onPostExecute(o);
            if(o) {
                mFavoritesAction.setChecked(true);
                mFavoritesAction.setIcon(android.R.drawable.star_big_on);
            } else {
                mFavoritesAction.setChecked(false);
                mFavoritesAction.setIcon(android.R.drawable.star_big_off);
            }
        }
    }

}
