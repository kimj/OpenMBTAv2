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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.mentalmachines.ttime.adapter.RouteExpandableAdapter;
import com.mentalmachines.ttime.fragments.AlertsFragment;
import com.mentalmachines.ttime.fragments.RouteFragment;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.services.NavDrawerTask;
import com.mentalmachines.ttime.services.ScheduleService;
import com.mentalmachines.ttime.services.StopService;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    public static final String TAG = "MainActivity";
    SharedPreferences mPrefs;
    ExpandableListView mDrawerList;
    int currentSelection = -1;
    //This is set to the expandable adapter mode or to -1 when Alerts are showing
    //TODO create a timeout for late night, intent service can hang
    ProgressDialog mPD = null;
    MenuItem mFavoritesAction;
    boolean noFaves = true;
    Fragment mFragment;

	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.activity_main);
        new CheckFavesTable().execute();
        //this task sets the noFaves boolean

        /*Log.d(TAG, "starting svc");
        startService(new Intent(this, CopyDBService.class));*/
		final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mDrawerList = (ExpandableListView) findViewById(R.id.routeNavList);
        mDrawerList.addHeaderView(LayoutInflater.from(this).inflate(R.layout.buttons_listheader, null));
        //shows the subway lines and sets the background on the view as selected
        mDrawerList.setOnGroupCollapseListener(faveSubListener);
        final Intent tnt = new Intent(this, NavDrawerTask.class);
        if(noFaves) {
            Log.i(TAG, "no favorites");
            currentSelection = RouteExpandableAdapter.SUBWAY;
            tnt.putExtra(NavDrawerTask.TAG, DBHelper.SUBWAY_MODE);
            findViewById(R.id.exp_lines).setBackgroundColor(getResources().getColor(R.color.silverlineBG));
        } else {
            currentSelection = RouteExpandableAdapter.FAVE;
            findViewById(R.id.exp_favorite).setBackgroundColor(getResources().getColor(R.color.silverlineBG));
        }
        final LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(this);
        mgr.registerReceiver(mNavDataReady, new IntentFilter(NavDrawerTask.TAG));
        startService(tnt);
        Log.d(TAG, "starting service");

        /*mTransitMethodNavigationDrawerFragment = (TransitMethodNavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.transit_method_navigation_drawer_fragment);
		mRouteSelectDrawerFragment = (RouteSelectNavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.route_select_navigation_drawer_fragment);*/
		
		// mTransitMethodDrawerList = (ListView) findViewById(R.id.transit_method_navigation_drawer_fragment);
        mgr.registerReceiver(mScheduleReady, new IntentFilter(ScheduleService.TAG));
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mScheduleReady);
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
            case R.id.menu_favorites:
                final Route r = ((RouteFragment) mFragment).mListAdapter.mRoute;
                if(DBHelper.setFavorite(r.name, r.id)) {
                    mFavoritesAction.setChecked(true);
                    mFavoritesAction.setIcon(android.R.drawable.star_big_on);
                    noFaves = false;
                } else {
                    mFavoritesAction.setChecked(false);
                    mFavoritesAction.setIcon(android.R.drawable.star_big_off);
                    noFaves = true;
                }
                break;
            case R.id.menu_alerts:
                //This piece will always create a new alert fragment
                if(mFragment != null) {
                    //hide the existing fragment, alert or route frgament
                    AlertsFragment alertsFragment = new AlertsFragment();
                    getSupportFragmentManager().beginTransaction().hide(mFragment)
                            .add(R.id.container, alertsFragment)
                            .commit();
                    mFragment = alertsFragment;
                } else {
                    mFragment = new AlertsFragment();
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.container, mFragment)
                            .commit();
                }
                //now a little clean up for a switch from Route to Alerts
                if(currentSelection > 0) {
                    findViewById(R.id.fab_in_out).setVisibility(View.GONE);
                    setTitle(getString(R.string.action_alerts));
                    mFavoritesAction.setVisible(false);
                }
                currentSelection = -1;
                Log.d(TAG, "reset selection here! " + currentSelection);
                if(mDrawerList.getTag() != null) {
                    ((View)mDrawerList.getTag()).setSelected(false);
                    mDrawerList.setTag(null);
                }
                break;
            case R.id.menu_settings:
                Toast.makeText(this, R.string.action_settings, Toast.LENGTH_SHORT).show();
                break;
        }

        return super.onOptionsItemSelected(item);
	}

    @Override
    public void onBackPressed() {
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
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
        if(currentSelection > 0) {
            //save the route ID in order to call the route prediction times again when user gets back
            mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            mPrefs.edit().putString(DBHelper.KEY_ROUTE_ID,
                    ((RouteFragment) mFragment).mListAdapter.mRoute.id).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mFragment == null) {
            mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            //no fragment showing, no preference from last route shown, display toast
            if(!mPrefs.contains(DBHelper.KEY_ROUTE_ID)) {
                Toast.makeText(this, getString(R.string.def_text), Toast.LENGTH_SHORT).show();
            } else {
                final String mRouteId = mPrefs.getString(DBHelper.KEY_ROUTE_ID, "");
                if(TTimeApp.checkNetwork(this)) {
                    mPD = ProgressDialog.show(MainActivity.this, "", getString(R.string.loading), true, true);
                    final Intent tnt = new Intent(MainActivity.this, ScheduleService.class);
                    tnt.putExtra(DBHelper.KEY_ROUTE_ID, mRouteId);
                    startService(tnt);
                    Log.i(TAG, "starting schedule service with id: " + mRouteId);
                    //DBUG, just checking
                    /*final Intent tnt2 = new Intent(MainActivity.this, ScheduleService.class);
                    tnt2.putExtra(DBHelper.KEY_ROUTE_ID, mRouteId);
                    tnt2.putExtra(ScheduleService.TAG, true);
                    startService(tnt2);
                    Log.i(TAG, "starting schedule service, testing new parse");*/
                } else {
                    Toast.makeText(this, "check network", Toast.LENGTH_SHORT).show();
                }
            }
        }
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
                currentSelection = RouteExpandableAdapter.BUS;
                tnt.putExtra(NavDrawerTask.TAG, DBHelper.BUS_MODE);
                startService(tnt);
                findViewById(R.id.exp_lines).setBackgroundColor(Color.TRANSPARENT);
                findViewById(R.id.exp_favorite).setBackgroundColor(Color.TRANSPARENT);
                break;
            case R.id.exp_lines:
                final Intent svc = new Intent(this, NavDrawerTask.class);
                svc.putExtra(NavDrawerTask.TAG, DBHelper.SUBWAY_MODE);
                startService(svc);
                currentSelection = RouteExpandableAdapter.SUBWAY;
                findViewById(R.id.exp_bus).setBackgroundColor(Color.TRANSPARENT);
                findViewById(R.id.exp_favorite).setBackgroundColor(Color.TRANSPARENT);
                break;
            case R.id.exp_favorite:
                //boolean is tracking changes based on favorite button clicks in the Route fragment
                if(noFaves) {
                    Log.i(TAG, "no favorites");
                    final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    if (drawer.isDrawerOpen(GravityCompat.START)) {
                        Toast.makeText(this, getString(R.string.no_favs), Toast.LENGTH_SHORT).show();
                        drawer.closeDrawer(GravityCompat.START);
                    }
                } else {
                    findViewById(R.id.exp_bus).setBackgroundColor(Color.TRANSPARENT);
                    findViewById(R.id.exp_lines).setBackgroundColor(Color.TRANSPARENT);
                    currentSelection = RouteExpandableAdapter.FAVE;
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
        final String routeId = (String) v.getTag();
        Log.i(TAG, "show route " + routeId);
        final String mRouteId = (String)v.getTag();
        final String mRouteName = (String) v.getTag(R.layout.child_view);
        setTitle(Route.readableName(this, mRouteName));
        v.setSelected(true);
        if(mDrawerList.getTag() != null) {
            ((View)mDrawerList.getTag()).setSelected(false);
        }
        //Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show();
        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
        findViewById(R.id.fab_in_out).setVisibility(View.VISIBLE);
        if(TTimeApp.checkNetwork(this)) {
            mPD = ProgressDialog.show(MainActivity.this, "", getString(R.string.loading), true, true);
            final Intent tnt = new Intent(MainActivity.this, ScheduleService.class);
            tnt.putExtra(DBHelper.KEY_ROUTE_NAME, mRouteName);
            tnt.putExtra(DBHelper.KEY_ROUTE_ID, mRouteId);
            startService(tnt);
            Log.i(TAG, "starting schedule service with id: " + mRouteId);
        } else {
            Toast.makeText(this, "check network", Toast.LENGTH_SHORT).show();
        }
        mDrawerList.setTag(v);
        //Fab is to switch between inbound and outbound
    }

    /* Showing Route between two Places
        This click listener is set on the map button next to the route name */
    /*public void mapRoute(View v) {
        StringBuilder s = new StringBuilder().append("http://maps.google.com/maps?saddr=");
        StopData stop = ((SimpleStopAdapter)mRouteFragment.mList.getAdapter()).mItems[0];
        s.append(stop.stopLat).append(",").append(stop.stopLong).append("&daddr=");
        Log.d(TAG, "first stop " + stop.stopName);
        stop = ((SimpleStopAdapter)mRouteFragment.mList.getAdapter()).mItems[
                ((SimpleStopAdapter)mRouteFragment.mList.getAdapter()).mItems.length-1];
        s.append(stop.stopLat).append(",").append(stop.stopLong);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s.toString()));
        //intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        startActivity(intent);
        Log.d(TAG, "stop " + stop.stopName);
        *//*StopData stop = ((SimpleStopAdapter)mRouteFragment.mList.getAdapter()).mItems[0];
        StringBuilder s = new StringBuilder().append("geo:=")
                .append(stop.stopLat).append(",").append(stop.stopLong).append("?q=");
        stop = ((SimpleStopAdapter)mRouteFragment.mList.getAdapter()).mItems[
                ((SimpleStopAdapter)mRouteFragment.mList.getAdapter()).mItems.length-1];
        s.append(stop.stopLat).append(",").append(stop.stopLong)*//*;
        startActivity(new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse(s.toString())));
    }*/

    /**
     * This method is set in the layout
     * It finds the stop on a map, the Stop Data is set as a tag on the view
     * @param v
     */
    public void openMap(View v) {
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
        Log.d(TAG, "open alert " + stop.stopAlert);
        Toast.makeText(this, "open alert " + stop.stopAlert, Toast.LENGTH_SHORT).show();
        //geo:0,0?q=lat,lng(label)
        //Uri uri = Uri.parse("geo:" + stop.stopLat + "," + stop.stopLong + "?z=16");
    }

    /**
     * The favorites and subway have only one group
     * That group is not allowed to collapse
     */
    ExpandableListView.OnGroupCollapseListener faveSubListener = new ExpandableListView.OnGroupCollapseListener() {

        @Override
        public void onGroupCollapse(int i) {
        //group is always zero for favorites and subway, allow only one open group
            if(currentSelection == RouteExpandableAdapter.BUS) {
                return;
            } else {
                if(!mDrawerList.isGroupExpanded(i)) {
                    mDrawerList.expandGroup(i);
                }
            } //don't collapse favorites or subways
        }

    };

    /**
     * This receiver gets the Route object back from the ScheduleService IntentService class
     * Either creates a new RouteFragment or resets the data in the recycler view of the fragment
     */
    BroadcastReceiver mScheduleReady = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //the route times are ready. set up the adapter
            if(mPD != null && mPD.isShowing()) {
                mPD.dismiss();
            }
            Log.d(TAG, "schedule ready");
            //quick callback error check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if(MainActivity.this.isDestroyed() || MainActivity.this.isFinishing()) {
                    return;
                }
            } else if(MainActivity.this.isFinishing()) {
                return;
            }
            final Route r = intent.getExtras().getParcelable(ScheduleService.TAG);

            //DEBUGGGING
            /*if(r.mInboundStops != null && r.mInboundStops.get(0) != null) {
                final Intent tnt = new Intent(getBaseContext(), StopService.class);
                tnt.putExtra(StopService.TAG, r.mInboundStops.get(0));
                getBaseContext().startService(tnt);
                Log.d(TAG, "debug, start svc: " + r.mInboundStops.get(0).stopName);
            }*/
            //CREATING LIST DETAIL PAGE
            final String rName;
            if(mFragment != null && currentSelection > 0) {
                rName = ((RouteFragment) mFragment).mListAdapter.mRoute.name;
            } else {
                rName = "";
            }
            if(mFavoritesAction.isVisible() && r.name.equals(rName)) {
                //route fragment is already up!
                Log.d(TAG, "reset Route");
                ((RouteFragment) mFragment).mListAdapter.resetRoute(r);
                ((SwipeRefreshLayout)findViewById(R.id.route_swipe)).setRefreshing(false);
                if(!((RouteFragment) mFragment).mListAdapter.isOneWay) {
                    //this is a one way route
                    Log.d(TAG, "stting button viz");
                    findViewById(R.id.fab_in_out).setVisibility(View.VISIBLE);
                }
            } else {
                mFavoritesAction.setVisible(true);
                mFavoritesAction.setCheckable(true);
                if(DBHelper.checkFavorite(r.name)) {
                    mFavoritesAction.setChecked(true);
                } else {
                    mFavoritesAction.setChecked(false);
                }
                setTitle(Route.readableName(context, r.name));

                final FragmentManager mgr = getSupportFragmentManager();
                if(mgr.findFragmentByTag(r.name) != null) {
                    Log.d(TAG, "fragment is showing");
                    RouteFragment frag = (RouteFragment) mgr.findFragmentByTag(r.name);
                    if(mFragment == null) {
                        mgr.beginTransaction().show(frag).commit();
                    } else {
                        mgr.beginTransaction().hide(mFragment).show(frag).commit();
                    }
                    mFragment = frag;
                    mFragment.onResume();
                } else {
                    Log.d(TAG, "new fragment");
                    if(mFragment == null) {
                        mFragment = RouteFragment.newInstance(r);
                        mgr.beginTransaction().add(R.id.container, mFragment, r.name).commit();
                    } else {
                        RouteFragment frag = RouteFragment.newInstance(r);
                        mgr.beginTransaction().hide(mFragment).add(R.id.container, frag, r.name).commit();
                        mFragment = frag;
                    }
                }

            }

            if(DBHelper.checkFavorite(r.name)) {
                mFavoritesAction.setIcon(android.R.drawable.star_big_on);
                Log.d(TAG, "is a favorite");
            } else {
                Log.d(TAG, "not favorited");
                mFavoritesAction.setIcon(android.R.drawable.star_big_off);
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
                switch (currentSelection) {
                    case RouteExpandableAdapter.FAVE:
                        mDrawerList.setAdapter(new RouteExpandableAdapter(
                                b.getStringArray(DBHelper.KEY_ROUTE_NAME),
                                b.getStringArray(DBHelper.KEY_ROUTE_ID),
                                RouteExpandableAdapter.FAVE));
                        mDrawerList.expandGroup(0);
                        break;
                    case RouteExpandableAdapter.BUS:
                        mDrawerList.setAdapter(new RouteExpandableAdapter(
                                b.getStringArray(DBHelper.KEY_ROUTE_NAME),
                                b.getStringArray(DBHelper.KEY_ROUTE_ID),
                                context));
                        break;
                    case RouteExpandableAdapter.SUBWAY:
                        mDrawerList.setAdapter(new RouteExpandableAdapter(
                                b.getStringArray(DBHelper.KEY_ROUTE_NAME),
                                b.getStringArray(DBHelper.KEY_ROUTE_ID),
                                RouteExpandableAdapter.SUBWAY));
                        mDrawerList.expandGroup(0);
                        break;
                }

                if(currentSelection != RouteExpandableAdapter.BUS) {
                    mDrawerList.expandGroup(0);
                    //here the list has only one group
                }
            }
        }
    };

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final StopData stop = (StopData) findViewById(R.id.container).getTag();
        switch (menuItem.getItemId()) {
            case R.id.stop_detail:
                Intent tnt = new Intent(this, StopService.class);
                tnt.putExtra(StopService.TAG, stop);
                startActivity(new Intent(this, StopDetailActivity.class));
                startService(tnt);
                //stop detail registers a receiver to get the stopDetail object it needs
                break;
            case R.id.stop_location:
                //Log.d(TAG, "stop " + stop.stopName);
                //geo:0,0?q=lat,lng(label)
                //Uri uri = Uri.parse("geo:" + stop.stopLat + "," + stop.stopLong + "?z=16");
                final Uri uri = Uri.parse("geo:0,0?q=" + stop.stopLat + "," + stop.stopLong + "("
                        + Uri.encode(stop.stopName) + ")");
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                break;
        }
        return false;
    }

    class CheckFavesTable extends AsyncTask <Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            final SQLiteDatabase db = TTimeApp.sHelper.getReadableDatabase();
            if(DatabaseUtils.queryNumEntries(db, DBHelper.FAVS_TABLE) == 0) {
                Log.i(TAG, "no favorites");
                noFaves = true;
            } else {
                noFaves = false;
            }
            return null;
        }
    }
}
