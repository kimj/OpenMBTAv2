package com.mentalmachines.ttime;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mentalmachines.ttime.SimpleStopAdapter.StopData;
import com.mentalmachines.ttime.fragments.AlertsFragment;
import com.mentalmachines.ttime.fragments.RouteFragment;
import com.mentalmachines.ttime.services.CopyDBService;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    public ExpandableListView mRouteList;
    SQLiteDatabase mDB;
    //String mSelectedRouteId;
    RouteFragment mRouteFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        Log.d(TAG, "starting svc");
        startService(new Intent(this, CopyDBService.class));
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
        mRouteFragment = RouteFragment.newInstance(null, null, getString(R.string.def_text),
                getResources().getColor(R.color.colorPrimary));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, mRouteFragment)
                .commit();
        /*mTransitMethodNavigationDrawerFragment = (TransitMethodNavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.transit_method_navigation_drawer_fragment);
		mRouteSelectDrawerFragment = (RouteSelectNavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.route_select_navigation_drawer_fragment);*/
		
		// mTransitMethodDrawerList = (ListView) findViewById(R.id.transit_method_navigation_drawer_fragment);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
            /* TODO move route name, map button and favorite selection up to action bar
            case R.id.menu_favorites:
                break;
            case R.id.menu_map:
                //map menu from the action bar will display the route
                break;*/
            case R.id.menu_alerts:
                startActivity(new Intent(this, AlertsFragment.class));
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
                        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
                    }


                }
        }
    }

    public void childClick(View v) {
        //click listener set on the child view in the nav drawer
        if(mDB == null || !mDB.isOpen()) {
            mDB = new DBHelper(this).getReadableDatabase();
        }
        final String routeId = (String) v.getTag();
        Log.i(TAG, "show route " + routeId);
        Cursor c = mDB.query(DBHelper.STOPS_INB_TABLE, SimpleStopAdapter.mStopProjection,
                RouteExpandableAdapter.stopsSubwayWhereClause + "'" + routeId + "'",
                null, null, null, DBHelper.KEY_STOP_ORD + " ASC");
        StopData[] inStops = SimpleStopAdapter.makeStopsList(c);

        c.close();
        c = mDB.query(DBHelper.STOPS_OUT_TABLE, SimpleStopAdapter.mStopProjection,
                RouteExpandableAdapter.stopsSubwayWhereClause + "'" + routeId + "'",
                null, null, null, DBHelper.KEY_STOP_ORD + " ASC");
        StopData[] outStops = SimpleStopAdapter.makeStopsList(c);

        c.close();
        //9701, 9702 and 9703 are one way routes
        mRouteFragment = RouteFragment.newInstance(inStops, outStops,
                ((TextView)v).getText().toString(),
                v.getTag(R.layout.child_view) == null?
                        getResources().getColor(R.color.solidBusYellow):(int)v.getTag(R.layout.child_view));
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.container, mRouteFragment)
            .commit();

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

    public void openMap(View v) {
        //click listener in the route fragment recycler view
        final StopData stop = (StopData) v.getTag();
        Log.d(TAG, "stop " + stop.stopName);
        //geo:0,0?q=lat,lng(label)
        //Uri uri = Uri.parse("geo:" + stop.stopLat + "," + stop.stopLong + "?z=16");
        Uri uri = Uri.parse("geo:0,0?q=" + stop.stopLat + "," + stop.stopLong + "("
                + Uri.encode(stop.stopName) + ")");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    ExpandableListView.OnGroupCollapseListener faveSubListener = new ExpandableListView.OnGroupCollapseListener() {

        @Override
        public void onGroupCollapse(int i) {
            //this is always zero for favorites and subway
            if(!mRouteList.isGroupExpanded(i)) {
                mRouteList.expandGroup(i);
            }
        }

    };

}
