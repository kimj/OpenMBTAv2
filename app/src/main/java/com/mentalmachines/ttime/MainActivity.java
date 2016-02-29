package com.mentalmachines.ttime;


import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
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
import android.widget.TextView;
import android.widget.Toast;

import com.mentalmachines.ttime.adapter.CursorRouteAdapter;
import com.mentalmachines.ttime.adapter.CursorRouteAdapter.StopData;
import com.mentalmachines.ttime.adapter.RouteExpandableAdapter;
import com.mentalmachines.ttime.fragments.AlertsFragment;
import com.mentalmachines.ttime.fragments.RouteFragment;
import com.mentalmachines.ttime.services.GetMBTARequestService;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    public ExpandableListView mRouteList;
    //RouteFragment mRouteFragment;
    public String mRouteId;
    String mRouteName;
    int mRouteColor;
    ProgressDialog mPD = null;
    MenuItem mFavoritesAction;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        /*Log.d(TAG, "starting svc");
        startService(new Intent(this, CopyDBService.class));*/
		final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_in_out);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "action?", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mRouteList = (ExpandableListView) findViewById(R.id.routeNavList);
        mRouteList.addHeaderView(LayoutInflater.from(this).inflate(R.layout.buttons_listheader, null));
        //shows the subway lines and sets the background on the view as selected
        mRouteList.setAdapter(new RouteExpandableAdapter(this, false));
        mRouteList.expandGroup(0);
        //allow only one open group
        mRouteList.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            // Keep track of previous expanded group
            int previousGroup = -1;

            @Override
            public void onGroupExpand(int groupPosition) {
                // Collapse previous parent if expanded.
                if ((previousGroup != -1) && (groupPosition != previousGroup)) {
                    mRouteList.collapseGroup(previousGroup);
                }
                previousGroup = groupPosition;
            }
        });
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, RouteFragment.newInstance(null, getString(R.string.def_text)))
                .commit();
        /*mRouteFragment = RouteFragment.newInstance(null, getString(R.string.def_text),
                getResources().getColor(R.color.colorPrimary));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, mRouteFragment)
                .commit();*/
        /*mTransitMethodNavigationDrawerFragment = (TransitMethodNavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.transit_method_navigation_drawer_fragment);
		mRouteSelectDrawerFragment = (RouteSelectNavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.route_select_navigation_drawer_fragment);*/
		
		// mTransitMethodDrawerList = (ListView) findViewById(R.id.transit_method_navigation_drawer_fragment);
        LocalBroadcastManager.getInstance(this).registerReceiver(mScheduleReady, new IntentFilter(GetMBTARequestService.TAG));
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mScheduleReady);

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
                if(DBHelper.setFavorite(this, mRouteName)) {
                    mFavoritesAction.setChecked(true);
                    mFavoritesAction.setIcon(android.R.drawable.star_big_on);
                } else {
                    mFavoritesAction.setChecked(false);
                    mFavoritesAction.setIcon(android.R.drawable.star_big_off);
                }
                break;
            case R.id.menu_alerts:
                AlertsFragment alertsFragment = new AlertsFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, alertsFragment)
                        .commit();

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

    public void setList(View v) {
        //show either lines or bus expandable list view in the drawer
        switch(v.getId()) {
            case R.id.exp_bus:
                //mRouteList.setVisibility(View.VISIBLE);
                mRouteList.setAdapter(new RouteExpandableAdapter(this, true));
                mRouteList.setOnGroupExpandListener(null);
                break;
            case R.id.exp_lines:
                mRouteList.setAdapter(new RouteExpandableAdapter(this, false));
                mRouteList.expandGroup(0);
                mRouteList.setOnGroupCollapseListener(faveSubListener);
                break;
            case R.id.exp_favorite:
                final Cursor c = RouteExpandableAdapter.getFavorites(this);
                if(c.getCount() > 0) {
                    mRouteList.setVisibility(View.VISIBLE);
                    mRouteList.setAdapter(new RouteExpandableAdapter(this, c));
                    mRouteList.expandGroup(0);
                    mRouteList.setOnGroupCollapseListener(faveSubListener);
                } else {
                    /* Need to clear the adapter to display the empty view
                    final View emptyV = LayoutInflater.from(this).inflate(R.layout.group_view, null);
                    ((TextView) emptyV).setText(getString(R.string.no_favs));
                    ((TextView) emptyV).setTextColor(getResources().getColor(R.color.colorPrimary));
                    emptyV.setBackgroundResource(android.R.color.white);*/
                    //mRouteList.setVisibility(View.GONE);
                    final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    if (drawer.isDrawerOpen(GravityCompat.START)) {
                        Toast.makeText(this, getString(R.string.no_favs), Toast.LENGTH_SHORT).show();
                        drawer.closeDrawer(GravityCompat.START);
                    }


                }
        }
    }

    public void childClick(View v) {
        //click listener set on the child view in the nav drawer
        final String routeId = (String) v.getTag();
        final Intent tnt = new Intent(this, GetMBTARequestService.class);
        tnt.putExtra(GetMBTARequestService.TAG, routeId);
        startService(tnt);
        Log.i(TAG, "show route " + routeId);
        mRouteId = (String)v.getTag();
        mRouteName = ((TextView) v).getText().toString();
        if(Character.isDigit(mRouteName.charAt(0))) {
            setTitle(getString(R.string.bus_prefix) + mRouteName);
        } else {
            setTitle(mRouteName);
        }

        mRouteColor = v.getTag(R.layout.child_view) == null?
                getResources().getColor(R.color.solidBusYellow):(int)v.getTag(R.layout.child_view);
        //Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show();
        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
        findViewById(R.id.fab_in_out).setVisibility(View.VISIBLE);
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
        //click listener in the route fragment recycler view
        final StopData stop = (StopData) ((ViewGroup) v.getParent()).getTag();
        Log.d(TAG, "stop " + stop.stopName);
        //geo:0,0?q=lat,lng(label)
        //Uri uri = Uri.parse("geo:" + stop.stopLat + "," + stop.stopLong + "?z=16");
        final Uri uri = Uri.parse("geo:0,0?q=" + stop.stopLat + "," + stop.stopLong + "("
                + Uri.encode(stop.stopName) + ")");
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
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
            //this is always zero for favorites and subway
            if(!mRouteList.isGroupExpanded(i)) {
                mRouteList.expandGroup(i);
            }
        }

    };

    BroadcastReceiver mScheduleReady = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //the route times are ready. set up the adapter
            new CreateRouteFragment().execute();
            mPD = ProgressDialog.show(MainActivity.this, "", getString(R.string.loading), true, true);
            Log.d(TAG, "starting async task to get to route fragment");
        }
    };

    /**
     * This will be quick, just want to get the I/O off the main thread
     * All of the db operations are in the Adapter class
     * TO move SQL operations off main thread
     */
    public class CreateRouteFragment extends AsyncTask<Object, Void, CursorRouteAdapter> {

        @Override
        protected CursorRouteAdapter doInBackground(Object... params) {

            CursorRouteAdapter rte = new CursorRouteAdapter(          //default direction is inbound
                    MainActivity.this, mRouteId, 1);
            Log.d(TAG, "creating adapter " + rte.getItemCount());
            if(rte.getItemCount() == 0) {
                rte = new CursorRouteAdapter(          //one way, try outbound
                        MainActivity.this, mRouteId, 0);
            }
            return rte;
            //CursorRouteAdapter(Context ctx, String routeId, int routeColor, int direction)
        }

        @Override
        protected void onPostExecute(CursorRouteAdapter result) {
            super.onPostExecute(result);
            if(mPD != null && mPD.isShowing()) {
                mPD.dismiss();
            }
            if(isCancelled()) {
                Log.w(TAG, "Task cancelled");
                return;
            }
            //newInstance(CursorRouteAdapter listData, String title, int bgColor) {
            //mRouteFragment = RouteFragment.newInstance(result, mRouteId, mRouteColor);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, RouteFragment.newInstance(result, mRouteName))
                    .commit();
            mFavoritesAction.setVisible(true);
            mFavoritesAction.setCheckable(true);
            if(DBHelper.checkFavorite(MainActivity.this, mRouteName)) {
                mFavoritesAction.setIcon(android.R.drawable.star_big_on);
            } else {
                mFavoritesAction.setIcon(android.R.drawable.star_big_off);
            }
            mFavoritesAction.getIcon().invalidateSelf();
        }
    } //end task



}
