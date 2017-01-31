package com.mentalmachines.ttime;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.mentalmachines.ttime.adapter.NavDrawerAdapter;
import com.mentalmachines.ttime.fragments.AlertsFragment;
import com.mentalmachines.ttime.fragments.RouteFragment;
import com.mentalmachines.ttime.fragments.StopFragment;
import com.mentalmachines.ttime.objects.Favorite;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.objects.StopList;
import com.mentalmachines.ttime.objects.Utils;
import com.mentalmachines.ttime.services.CheckFavorite;
import com.mentalmachines.ttime.services.GetScheduleService;
import com.mentalmachines.ttime.services.GetTimesForRoute;
import com.mentalmachines.ttime.services.NavDrawerTask;
import com.mentalmachines.ttime.services.SaveFavorites;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    public static final String TAG = "MainActivity";
    SharedPreferences mPrefs;
    ExpandableListView mDrawerList;
    public FloatingActionButton mFab;
    int mCurrentSelection = -1;
    //This is set to the expandable adapter mode or to -1 when Alerts are showing
    //TODO create a timeout for late night, intent service can hang
    ProgressDialog mPD = null;
    public MenuItem mFavoritesAction;
    boolean noFaves = true;
    Fragment mFragment;
    public Toolbar mToolbar;

	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.activity_main);
        new CheckFavesTable().execute();
        //this task sets the noFaves boolean
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mDrawerList = (ExpandableListView) findViewById(R.id.routeNavList);
        final View header = LayoutInflater.from(this).inflate(R.layout.buttons_listheader, null);
        mDrawerList.addHeaderView(header);
        //shows the subway lines and sets the background on the view as selected
        mDrawerList.setOnGroupCollapseListener(faveSubListener);
        final Intent tnt = new Intent(this, NavDrawerTask.class);
        if(noFaves) {
            Log.i(TAG, "no favorites");
            mCurrentSelection = NavDrawerAdapter.SUBWAY;
            tnt.putExtra(NavDrawerTask.TAG, DBHelper.SUBWAY_MODE);
            findViewById(R.id.exp_lines).setBackgroundColor(getResources().getColor(R.color.silverlineBG));
        } else {
            mCurrentSelection = NavDrawerAdapter.FAVE;
            findViewById(R.id.exp_favorite).setBackgroundColor(getResources().getColor(R.color.silverlineBG));
        }
        final LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(this);
        mgr.registerReceiver(mNavDataReady, new IntentFilter(NavDrawerTask.TAG));
        startService(tnt);
        Log.d(TAG, "starting drawer adapter service");

        mgr.registerReceiver(mTimesReady, new IntentFilter(GetTimesForRoute.TAG));
        mFab = (FloatingActionButton) findViewById(R.id.fab_in_out);
        //read extra, show alerts - if appropriate
        final Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(NearbyActivity.TAG)) {
            mFragment = Utils.fragmentChange(this, null, AlertsFragment.TAG, null);
            //TODO handle the case where the mFragment is an AlertsFragment
            //now a little clean up for a switch from Route to Alerts
            mFab.setVisibility(View.GONE);
            mToolbar.setTitle(getString(R.string.action_alerts));
            mCurrentSelection = -1;
            Log.d(TAG, "reset selection here! " + mCurrentSelection);
            if(mDrawerList.getTag() != null) {
                ((View)mDrawerList.getTag()).setSelected(false);
                mDrawerList.setTag(null);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTimesReady);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mNavDataReady);
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if(menu != null) {
            mFavoritesAction = menu.getItem(0);
            mFavoritesAction.setVisible(false);
        }
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
            /* TODO move route name, map button and favorite selection up to action bar
            case R.id.menu_map:
                //map menu from the action bar will display the route
                break;*/
            case R.id.menu_schedule:
                //TODO clean up NPE
                if(((RouteFragment)mFragment).mListAdapter == null &&
                        ((RouteFragment)mFragment).mListAdapter.getItemCount() == 0) {
                    //show error and return
                    Toast.makeText(this, "Need a route to show the schedule", Toast.LENGTH_SHORT).show();
                    return super.onOptionsItemSelected(item);
                }
                final Calendar c = Calendar.getInstance();

                Intent svc = new Intent(this, GetScheduleService.class);
                if(c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ||
                        c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                    svc.putExtra(GetScheduleService.TAG, c.get(Calendar.DAY_OF_WEEK));
                } else {
                    svc.putExtra(GetScheduleService.TAG, Calendar.TUESDAY);
                }
                svc.putExtra(DBHelper.KEY_ROUTE_ID, ((RouteFragment)mFragment).mListAdapter.mRoute);
                startService(svc);

                final Intent act  = new Intent(this, ScheduleActivity.class);
                act.putExtra(DBHelper.KEY_ROUTE_ID, ((RouteFragment)mFragment).mListAdapter.mRoute);
                //Toast.makeText(this, "Starting service, activity", Toast.LENGTH_SHORT).show();
                startActivity(act);
                break;
            case R.id.menu_favorites:
                if(mFragment instanceof StopFragment) {
                    if(((StopFragment)mFragment).mStopDetail != null) {
                        new CheckFavorite(mFavoritesAction).execute(
                                this, false, ((StopFragment)mFragment).mStopDetail.mainStop.stopId);
                    } else {
                        Log.w(TAG, "don't have stop id to set favorite");
                    }
                    return true;
                } else {
                    final Route r = ((RouteFragment) mFragment).mListAdapter.mRoute;
                    //TODO move setFavorite to bg thread
                    if(Favorite.setFavorite(r.name, r.id)) {
                        mFavoritesAction.setChecked(true);
                        mFavoritesAction.setIcon(R.drawable.ic_star_light);
                        noFaves = false;
                        startService(SaveFavorites.newInstance(this, r));
                        Log.v(TAG, "saving favorite route schedule: " + r.name);
                    } else {
                        mFavoritesAction.setChecked(false);
                        mFavoritesAction.setIcon(R.drawable.ic_star_border_light);
                        noFaves = true;
                    }
                    return true;
                }
            case R.id.menu_alerts:
                //This piece will always create a new alert fragment
                mFragment = Utils.fragmentChange(this, mFragment, AlertsFragment.TAG, null);
                //TODO handle the case where the mFragment is an AlertsFragment
                //now a little clean up for a switch from Route to Alerts
                mFab.setVisibility(View.GONE);
                mToolbar.setTitle(getString(R.string.action_alerts));
                mFavoritesAction.setVisible(false);
                mCurrentSelection = -1;
                Log.d(TAG, "reset selection here! " + mCurrentSelection);
                if(mDrawerList.getTag() != null) {
                    ((View)mDrawerList.getTag()).setSelected(false);
                    mDrawerList.setTag(null);
                }
                break;
            case R.id.menu_nearby:
                //Toast.makeText(this, R.string.action_settings, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, NearbyActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
	}

    @Override
    public void onBackPressed() {
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        //Log.d(TAG, getSupportFragmentManager().getBackStackEntryCount() + " bs count");
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            // to avoid looping below on initScreen
            super.onBackPressed();
            finish();
        } else {
            super.onBackPressed();
            mFragment = getSupportFragmentManager().findFragmentById(R.id.container);
            if (mFragment != null) {
                mFragment.onResume();
            } else {
                //like onCreate...
                initScreen();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mPD != null && mPD.isShowing()) {
            mPD.dismiss();
        }

        //just in case
        if(findViewById(R.id.route_swipe) != null) {
            if(((SwipeRefreshLayout)findViewById(R.id.route_swipe)).isRefreshing()) {
                ((SwipeRefreshLayout)findViewById(R.id.route_swipe)).setRefreshing(false);
            }
        }
        //persist the view
        //save the route ID in order to call the route prediction times again when user gets back
        if(mCurrentSelection > 0) {
            if (mDrawerList.getTag() != null) {
                View v = (View) mDrawerList.getTag();
                final String routeId = (String) v.getTag();
                mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                mPrefs.edit().putString(DBHelper.KEY_ROUTE_ID, routeId).commit();
            } else if(mFragment != null && mFragment instanceof RouteFragment && ((RouteFragment)mFragment).mListAdapter != null &&
                    ((RouteFragment)mFragment).mListAdapter.mRoute != null) {
                mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                mPrefs.edit().putString(DBHelper.KEY_ROUTE_ID,
                        ((RouteFragment) mFragment).mListAdapter.mRoute.id).commit();
            }
        } //ELSE do something about alerts that are onscreen
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mFragment == null) {
            initScreen();
        } else {
            /*if(mFragment.isHidden()) {
                getSupportFragmentManager().beginTransaction().show(mFragment).commit();
                Log.d(TAG, "showing hidden fragment?");
                //TODO this seems wrong
            }*/
            if(mCurrentSelection > 0 && mFragment instanceof RouteFragment) {
                //this is a route fragment, get the latest times
                ((RouteFragment)mFragment).reloadTimes();
                new CheckFavesTable().execute();
            }
        }
    }

    void initScreen() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        //no fragment showing, no preference from last route shown, display toast
        if(!mPrefs.contains(DBHelper.KEY_ROUTE_ID)) {
            Toast.makeText(this, getString(R.string.def_text), Toast.LENGTH_SHORT).show();
        } else {
            final String mRouteId = mPrefs.getString(DBHelper.KEY_ROUTE_ID, "");
            getTimesFromService(mRouteId, "");
        }
    }

    void getTimesFromService(String routeId, String routeName) {
        if (!Utils.checkNetwork(this)) {
            Toast.makeText(this, getString(R.string.chkNet), Toast.LENGTH_SHORT).show();
            return;
        } else if(TextUtils.isEmpty(routeId)) {
            Toast.makeText(this, getString(R.string.def_text), Toast.LENGTH_SHORT).show();
            return;
        }
        mPD = ProgressDialog.show(this, "", getString(R.string.getting_data), true, true);
        final Intent tnt = new Intent(this, GetTimesForRoute.class);
        if(!TextUtils.isEmpty(routeName)) {
            tnt.putExtra(DBHelper.KEY_ROUTE_NAME, routeName);
        }
        tnt.putExtra(DBHelper.KEY_ROUTE_ID, routeId);
        startService(tnt);
        Log.i(TAG, "starting get times service with id: " + routeId);
    }

    /**
     * This is a click listener on the 3 tabs in the nav drawer
     * @param v
     */
    public void setList(View v) {
        //show either subway, favorites or bus expandable list view in the drawer
        v.setBackgroundColor(getResources().getColor(R.color.silverlineBG));
        switch(v.getId()) {
            case R.id.exp_bus:
                final Intent tnt = new Intent(this, NavDrawerTask.class);
                mCurrentSelection = NavDrawerAdapter.BUS;
                tnt.putExtra(NavDrawerTask.TAG, DBHelper.BUS_MODE);
                startService(tnt);
                findViewById(R.id.exp_lines).setBackgroundColor(Color.TRANSPARENT);
                findViewById(R.id.exp_favorite).setBackgroundColor(Color.TRANSPARENT);
                break;
            case R.id.exp_lines:
                final Intent svc = new Intent(this, NavDrawerTask.class);
                svc.putExtra(NavDrawerTask.TAG, DBHelper.SUBWAY_MODE);
                startService(svc);
                mCurrentSelection = NavDrawerAdapter.SUBWAY;
                findViewById(R.id.exp_bus).setBackgroundColor(Color.TRANSPARENT);
                findViewById(R.id.exp_favorite).setBackgroundColor(Color.TRANSPARENT);
                break;
            case R.id.exp_favorite:
                findViewById(R.id.exp_bus).setBackgroundColor(Color.TRANSPARENT);
                findViewById(R.id.exp_lines).setBackgroundColor(Color.TRANSPARENT);
                //boolean is tracking changes based on favorite button clicks in the Route fragment
                if(noFaves) {
                    Log.i(TAG, "no favorites");
                    final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    if (drawer.isDrawerOpen(GravityCompat.START)) {
                        Toast.makeText(this, getString(R.string.no_favs), Toast.LENGTH_SHORT).show();
                        drawer.closeDrawer(GravityCompat.START);
                    }
                    findViewById(R.id.exp_favorite).setBackgroundColor(Color.TRANSPARENT);
                } else {
                    mCurrentSelection = NavDrawerAdapter.FAVE;
                    //no extras for Favorites service
                    startService(new Intent(this, NavDrawerTask.class));
                }

        } //end switch
    }

    /**
     * This is a click listener on the nav drawer. Chooses a route, shows it in a RouteFragment
     * @param v
     */
    public void childClick(View v) {
        //click listener set on the child view in the nav drawer
        if(v.getTag() instanceof StopData) {
            //favorite stop
            showStopDetail((StopData) v.getTag());
            Log.i(TAG, "show stop click");
        } else {
            final String routeId = (String) v.getTag();
            Log.i(TAG, "show route " + routeId);
            mPrefs.edit().putString(DBHelper.KEY_ROUTE_ID, routeId).apply();
            //final String routeName = (String) v.getTag(R.layout.child_view);
            v.setSelected(true);
            if(mDrawerList.getTag() != null) {
                ((View)mDrawerList.getTag()).setSelected(false);
            }
            if(Utils.checkNetwork(this)) {
                getTimesFromService(routeId, (String) v.getTag(R.layout.child_view));
                Log.i(TAG, "starting get times service with id: " + routeId);
            } else {
                Toast.makeText(this, "check network", Toast.LENGTH_SHORT).show();
            }
            mDrawerList.setTag(v);
            //Fab is to switch between inbound and outbound
            mFab.setVisibility(View.VISIBLE);
        }
        //Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show();
        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
    }

    /* Showing Route between two Places
        This click listener is set on the map button next to the route name */
    /*public void mapRoute(View v) {
        StringBuilder s = new StringBuilder().append("http://maps.google.com/maps?saddr=");
        StopData stop = ((RouteStopAdapter)mRouteFragment.mList.getAdapter()).mItems[0];
        s.append(stop.stopLat).append(",").append(stop.stopLong).append("&daddr=");
        Log.d(TAG, "first stop " + stop.stopName);
        stop = ((RouteStopAdapter)mRouteFragment.mList.getAdapter()).mItems[
                ((RouteStopAdapter)mRouteFragment.mList.getAdapter()).mItems.length-1];
        s.append(stop.stopLat).append(",").append(stop.stopLong);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s.toString()));
        //intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        startActivity(intent);
        Log.d(TAG, "stop " + stop.stopName);
        *//*StopData stop = ((RouteStopAdapter)mRouteFragment.mList.getAdapter()).mItems[0];
        StringBuilder s = new StringBuilder().append("geo:=")
                .append(stop.stopLat).append(",").append(stop.stopLong).append("?q=");
        stop = ((RouteStopAdapter)mRouteFragment.mList.getAdapter()).mItems[
                ((RouteStopAdapter)mRouteFragment.mList.getAdapter()).mItems.length-1];
        s.append(stop.stopLat).append(",").append(stop.stopLong)*//*;
        startActivity(new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse(s.toString())));
    }*/

    /**
     * This method is set in the layout
     * It finds the stop on a map, the Stop Data is set as a tag on the view
     * @param v
     */
    public void openMenu(View v) {
        PopupMenu popup = new PopupMenu(v.getContext(), v);
        popup.inflate(R.menu.stop_popup);
        popup.setOnMenuItemClickListener(this);
        popup.show();
        //click listener in the route fragment recycler view
        findViewById(R.id.container).setTag(((ViewGroup) v.getParent()).getTag());
    }

    //TBD, open inside the alert
    public void openAlerts(View v) {
        //click listener in the route fragment recycler view
        final StopData stop = (StopData) ((ViewGroup) v.getParent()).getTag();
        showStopAlert(stop);
        //geo:0,0?q=lat,lng(label)
        //Uri uri = Uri.parse("geo:" + stop.stopLat + "," + stop.stopLong + "?z=16");
        //old code trying to show stop on the map
    }

    void showStopAlert(StopData stop) {
        Toast.makeText(this, "open alert main activity" + stop.stopAlert, Toast.LENGTH_SHORT).show();
        mFragment = Utils.fragmentChange(this, mFragment, AlertsFragment.TAG, stop.stopAlert);
    }

    /**
     * The favorites and subway have only one group
     * That group is not allowed to collapse
     */
    ExpandableListView.OnGroupCollapseListener faveSubListener = new ExpandableListView.OnGroupCollapseListener() {

        @Override
        public void onGroupCollapse(int i) {
        //group is always zero for favorites and subway, allow only one open group
            if(mCurrentSelection == NavDrawerAdapter.BUS) {
                return;
            } else {
                if(!mDrawerList.isGroupExpanded(i)) {
                    mDrawerList.expandGroup(i);
                }
            } //don't collapse favorites or subways
        }

    };

    /**
     * This receiver gets the Route object back from the GetTimesForRoute IntentService class
     * Either creates a new RouteFragment or resets the data in the recycler view of the fragment
     */
    BroadcastReceiver mTimesReady = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //the route times are ready. set up the adapter
            if(mPD != null && mPD.isShowing()) {
                mPD.dismiss();
            }
            Log.d(TAG, "times returned");

            //quick callbacks error check, is everything here
            if(MainActivity.this.isFinishing()) {
                return;
            }
            final Bundle b = intent.getExtras();
            if(b == null) {
                //Error check, service returns data
                return;
            }

            if((b.containsKey(TAG) && b.getParcelable(TAG) == null) ||
                    (b.containsKey(GetTimesForRoute.TAG) && b.getBoolean(GetTimesForRoute.TAG))) {
                //error conditions, no route object, true boolean
                Snackbar.make(findViewById(R.id.container),
                        getString(R.string.schedSvcErr), Snackbar.LENGTH_LONG).show();

            }
            //exception from predictions but route and schedule times are good
            if(b.containsKey(TAG) && b.getParcelable(TAG) == null) {
                return;
            }
            final Route r = intent.getExtras().getParcelable(TAG);
            Log.i(TAG, "good route!");

            final String title = getSupportActionBar().getTitle().toString();
            if(mFragment !=null && mFragment instanceof RouteFragment && Route.readableName(context, r.name).equals(title)) {
                //route fragment is already up! Reset route includes animation
                Log.d(TAG, "reset Route");
                ((RouteFragment) mFragment).resetRoute(r);
            } else {
                mFavoritesAction.setVisible(true);
                mFavoritesAction.setCheckable(true);
                if(Favorite.checkFavoriteRoute(r.name)) {
                    mFavoritesAction.setChecked(true);
                } else {
                    mFavoritesAction.setChecked(false);
                }
                mToolbar.setTitle(Route.readableName(context, r.name));
                mFragment = Utils.fragmentChange(MainActivity.this, mFragment, RouteFragment.TAG, r);
            }

            if(Favorite.checkFavoriteRoute(r.name)) {
                mFavoritesAction.setIcon(R.drawable.ic_star_light);
                Log.d(TAG, "is a favorite");
            } else {
                Log.d(TAG, "not favorited");
                mFavoritesAction.setIcon(R.drawable.ic_star_border_light);
            }
            mFavoritesAction.getIcon().invalidateSelf();
        }
    };

    /**
     * This receiver sets the adapter based on the drawer header view selection
     * The possible values are subway, bus or favorites -> favorites is the default
     */
    BroadcastReceiver mNavDataReady = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Bundle b = intent.getExtras();
            if(b == null) {
                Log.e(TAG, "Error reading routes and stops");
                //TODO report error to user?
                return;
            }
            else {
                //show the right list after the I/O is done in the bg
                switch (mCurrentSelection) {
                    case NavDrawerAdapter.FAVE:
                        final StopList faveStops = intent.getParcelableExtra(DBHelper.STOP);
                        final StopData[] vals;
                        if(faveStops != null && faveStops.mStopList != null && faveStops.mStopList.size() > 0) {
                            vals = new StopData[faveStops.mStopList.size()];
                            faveStops.mStopList.toArray(vals);
                        } else {
                            vals = null;
                        }
                        final NavDrawerAdapter adapter = new NavDrawerAdapter(
                                b.getStringArray(DBHelper.KEY_ROUTE_NAME),
                                b.getStringArray(DBHelper.KEY_ROUTE_ID),
                                vals);
                        mDrawerList.setAdapter(adapter);
                        switch(adapter.getGroupCount()) {
                            case 2:
                                mDrawerList.expandGroup(1);
                            case 1:
                                mDrawerList.expandGroup(0);
                        }

                        break;
                    case NavDrawerAdapter.BUS:
                        mDrawerList.setAdapter(new NavDrawerAdapter(
                                b.getStringArray(DBHelper.KEY_ROUTE_NAME),
                                b.getStringArray(DBHelper.KEY_ROUTE_ID),
                                context));
                        break;
                    case NavDrawerAdapter.SUBWAY:
                        mDrawerList.setAdapter(new NavDrawerAdapter(
                                b.getStringArray(DBHelper.KEY_ROUTE_NAME),
                                b.getStringArray(DBHelper.KEY_ROUTE_ID)));
                        mDrawerList.expandGroup(0);
                        break;
                }
                /*if(mCurrentSelection == NavDrawerAdapter.SUBWAY) {
                    mDrawerList.expandGroup(0);
                    //here the list has only one group
                } else if(mCurrentSelection == NavDrawerAdapter.FAVE) {
                    switch(((NavDrawerAdapter)mDrawerList.getAdapter()).getGroupCount()) {
                        case 2:
                            mDrawerList.expandGroup(1);
                        case 1:
                            mDrawerList.expandGroup(0);
                    }
                }*/
            }
        }
    };

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final StopData stop = (StopData) findViewById(R.id.container).getTag();
        switch (menuItem.getItemId()) {
            case R.id.stop_fav:
                LocalBroadcastManager.getInstance(this).registerReceiver(
                        new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                if(!intent.getBooleanExtra(SaveFavorites.TAG, false)) {
                                    Log.e(TAG, "error setting stop as favorite");
                                    if(MainActivity.this.hasWindowFocus()) {
                                        Toast.makeText(MainActivity.this, "error setting stop as favorite", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(this);
                            }
                        }, new IntentFilter(NavDrawerTask.TAG));
                startService(SaveFavorites.newInstance(this, stop.stopId));
                //favorites service
                break;
            case R.id.stop_detail:
                showStopDetail(stop);
                break;
            case R.id.stop_location:
                //Log.d(TAG, "stop " + stop.stopName);
                //geo:0,0?q=lat,lng(label)
                //Uri uri = Uri.parse("geo:" + stop.stopLat + "," + stop.stopLong + "?z=16");
                final Uri uri = Uri.parse("geo:0,0?q=" + stop.stopLat + "," + stop.stopLong + "("
                        + Uri.encode(stop.stopName) + ")");
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                break;
            case R.id.stop_alert:
                showStopAlert(stop);
                break;
        }
        return false;
    }

    void showStopDetail(StopData stop) {
        //mFragment must be route fragment
        if(mFragment instanceof RouteFragment) {
            final String mainStopRoute = Route.readableName(this, ((RouteFragment)mFragment).mListAdapter.mRoute.name) + " " +
                    (((RouteFragment)mFragment).mListAdapter.mDirectionId == 0? getString(R.string.outbound): getString(R.string.inbound));
            /*if(stop.schedTimes == null) {
                stop.schedTimes = mainStopRoute;
            } else {
                stop.schedTimes = mainStopRoute + stop.schedTimes;
            }

            TODO what's this again?*/
        }
        Log.i(TAG, "show stop detail " + stop.readableStopName());
        mFragment = Utils.fragmentChange(this, mFragment, StopFragment.TAG, stop);
    }

    class CheckFavesTable extends AsyncTask <Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
            if(DatabaseUtils.queryNumEntries(db, DBHelper.FAVS_TABLE) == 0 &&
                    DatabaseUtils.queryNumEntries(db, DBHelper.FAVESTOPS_TABLE) == 0) {
                Log.i(TAG, "no favorites");
                noFaves = true;
            } else {
                noFaves = false;
            }
            return null;
        }
    }

    /*public void switchFragment(String fragmentTag){
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();


        if(fm.findFragmentByTag() != null) {
            RouteFragment frag = (RouteFragment) mgr.findFragmentByTag(fragmentTah);
            if(mFragment == frag) {
                ((RouteFragment) mFragment).resetRoute(r);
            } else if(mFragment == null) {
                ft.show(frag).commit();
                mFragment = frag;
            } else {
                ft.hide(mFragment).show(frag).commit();
                frag.resetRoute(r);
                mFragment = frag;
            }
        }
    }*/
}
