package com.mentalmachines.ttime;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
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
import android.widget.Toast;

import com.mentalmachines.ttime.services.GetMBTARequestService;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        Log.d(TAG, "starting svc");
        startService(new Intent(this, GetMBTARequestService.class));

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

        final ExpandableListView routeList = (ExpandableListView) findViewById(R.id.routeNavList);
        routeList.addHeaderView(LayoutInflater.from(this).inflate(R.layout.buttons_listheader, null));
        routeList.setAdapter(new RouteExpandableAdapter(this, false));
        //allow only one open group
        routeList.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            // Keep track of previous expanded group
            int previousGroup = -1;

            @Override
            public void onGroupExpand(int groupPosition) {
                // Collapse previous parent if expanded.
                if ((previousGroup != -1) && (groupPosition != previousGroup)) {
                    routeList.collapseGroup(previousGroup);
                }
                previousGroup = groupPosition;
            }
        });
        //NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        //navigationView.setNavigationItemSelectedListener(this);
        //no more menu? using exp list view
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, RouteFragment.newInstance(null, R.string.def_text,
                        getResources().getColor(android.R.color.transparent)))
                .commit();
        /*mTransitMethodNavigationDrawerFragment = (TransitMethodNavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.transit_method_navigation_drawer_fragment);
		mRouteSelectDrawerFragment = (RouteSelectNavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.route_select_navigation_drawer_fragment);*/

		
		// mTransitMethodDrawerList = (ListView) findViewById(R.id.transit_method_navigation_drawer_fragment);

	}

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        final String[] fakeData = getResources().getStringArray(R.array.fake_data);
        Log.w(TAG, "no of stops? " + fakeData.length);
        switch(item.getItemId()) {
            case R.id.dr_blue:
                //((TextView)findViewById(R.id.title)).setText(R.string.nm_blue);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, RouteFragment.newInstance(fakeData, R.string.nm_blue,
                                getResources().getColor(R.color.bluelineBG)))
                        .commit();
                break;
            case R.id.dr_green:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, RouteFragment.newInstance(fakeData, R.string.nm_green,
                                getResources().getColor(R.color.greenlineBG)))
                        .commit();
                break;
            case R.id.dr_orange:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, RouteFragment.newInstance(fakeData, R.string.nm_orange,
                                getResources().getColor(R.color.orangelineBG)))
                        .commit();
                break;
            case R.id.dr_redline:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, RouteFragment.newInstance(fakeData, R.string.nm_red,
                                getResources().getColor(R.color.redlineBG)))
                        .commit();
                break;
            case R.id.dr_silver:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, RouteFragment.newInstance(fakeData, R.string.nm_silver,
                                getResources().getColor(R.color.silverlineBG)))
                        .commit();
                break;
        }
        //Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
            case R.id.menu_alerts:
                Toast.makeText(this, R.string.action_alerts, Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_settings:
                Toast.makeText(this, R.string.action_settings, Toast.LENGTH_SHORT).show();
                break;
        }

		return super.onOptionsItemSelected(item);
	}

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void setList(View v) {
        //show either lines or bus expandable list view in the drawer

    }

    public void childClick(View v) {
        //click listener set on the child view
        final String[] fakeData = getResources().getStringArray(R.array.fake_data);
        Log.w(TAG, "no of stops? " + fakeData.length);
        final int route = (int) v.getTag();
        switch(route) {
            case RouteExpandableAdapter.BLUE:
                //((TextView)findViewById(R.id.title)).setText(R.string.nm_blue);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, RouteFragment.newInstance(fakeData, R.string.nm_blue,
                                getResources().getColor(R.color.bluelineBG)))
                        .commit();
                break;
            case RouteExpandableAdapter.GREEN:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, RouteFragment.newInstance(fakeData, R.string.nm_green,
                                getResources().getColor(R.color.greenlineBG)))
                        .commit();
                break;
            case RouteExpandableAdapter.ORANGE:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, RouteFragment.newInstance(fakeData, R.string.nm_orange,
                                getResources().getColor(R.color.orangelineBG)))
                        .commit();
                break;
            case RouteExpandableAdapter.RED:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, RouteFragment.newInstance(fakeData, R.string.nm_red,
                                getResources().getColor(R.color.redlineBG)))
                        .commit();
                break;
            case RouteExpandableAdapter.SILVER:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, RouteFragment.newInstance(fakeData, R.string.nm_silver,
                                getResources().getColor(R.color.silverlineBG)))
                        .commit();
                break;
        }
        //Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show();
        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_in_out);
        fab.setVisibility(View.VISIBLE);
        fab.setBackgroundResource(RouteExpandableAdapter.GroupTxtColor[route]);
    }
}
