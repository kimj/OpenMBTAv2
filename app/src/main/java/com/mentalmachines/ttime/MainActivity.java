package com.mentalmachines.ttime;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mentalmachines.ttime.openmbtav2.R;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
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

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, SubwayFragment.newInstance(null, R.string.def_text))
                .commit();
        /*mTransitMethodNavigationDrawerFragment = (TransitMethodNavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.transit_method_navigation_drawer_fragment);
		mRouteSelectDrawerFragment = (RouteSelectNavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.route_select_navigation_drawer_fragment);*/

		
		// mTransitMethodDrawerList = (ListView) findViewById(R.id.transit_method_navigation_drawer_fragment);

		//String[] resources = getResources().getStringArray(R.array.transit_methods);
		// Set up the drawer.
		/*mTransitMethodNavigationDrawerFragment.setUp(R.id.transit_method_navigation_drawer_fragment,
				(DrawerLayout) findViewById(R.id.drawer_layout));*/
	}

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        final String[] fakeData = getResources().getStringArray(R.array.fake_data);
        Log.w(TAG, "no of stops? " + fakeData.length);
        switch(item.getItemId()) {
            case R.id.dr_blue:
                //((TextView)findViewById(R.id.title)).setText(R.string.nm_blue);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, SubwayFragment.newInstance(fakeData, R.string.nm_blue))
                        .commit();
                break;
            case R.id.dr_green:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, SubwayFragment.newInstance(fakeData, R.string.nm_green))
                        .commit();
                break;
            case R.id.dr_orange:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, SubwayFragment.newInstance(fakeData, R.string.nm_orange))
                        .commit();
                break;
            case R.id.dr_redline:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, SubwayFragment.newInstance(fakeData, R.string.nm_red))
                        .commit();
                break;
            case R.id.dr_silver:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, SubwayFragment.newInstance(fakeData, R.string.nm_silver))
                        .commit();
                break;
        }
        Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show();
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
                break;
            case R.id.menu_settings:
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

    /**
	 * A placeholder fragment containing a simple view.
	 */

	/*class TransitMethodDrawerClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Fragment fragment = null;  
			switch (position) {
			case 0:
				mRouteSelectDrawerFragment.loadBusRoutes();
				fragment = new BusFragment();
				break;
			case 1:
				// routeSelectNavigationDrawerFragment.loadSubwayRoutes();
				fragment = new SubwayFragment();
				break;
			case 2:
				// routeSelectNavigationDrawerFragment.loadCommuterRailRoutes();
				fragment = new CommuterRailFragment();
				break;
			case 3:
				// routeSelectNavigationDrawerFragment.loadBoatRoutes();
				fragment = new BoatFragment();
				break;

			default:
				break;
			}

			if (fragment != null) {
				FragmentManager fragmentManager = getSupportFragmentManager();
				fragmentManager.beginTransaction().replace(R.id.container, fragment).commit();

				mTransitMethodDrawerList.setItemChecked(position, true);
				mTransitMethodDrawerList.setSelection(position);
				getActionBar().setTitle(mTransitMethodNavigationDrawerItemTitles[position]);
				mTransitMethodNavigationDrawerLayout.closeDrawer(mTransitMethodDrawerList);

			} else {
				Log.e("MainActivity", "Error in creating fragment");
			}
		}
	}*/
}
