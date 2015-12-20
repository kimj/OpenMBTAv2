package com.mentalmachines.openmbta.openmbtav2;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

public class MainActivity extends ActionBarActivity implements
		TransitMethodNavigationDrawerFragment.NavigationDrawerCallbacks, 
		RouteSelectNavigationDrawerFragment.NavigationDrawerCallbacks{

	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private TransitMethodNavigationDrawerFragment mTransitMethodNavigationDrawerFragment	;
	public RouteSelectNavigationDrawerFragment mRouteSelectDrawerFragment;

	ListView mTransitMethodDrawerList;
	private DrawerLayout mTransitMethodNavigationDrawerLayout;
	private String[] mTransitMethodNavigationDrawerItemTitles;
	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTransitMethodNavigationDrawerFragment = (TransitMethodNavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.transit_method_navigation_drawer_fragment);
		mRouteSelectDrawerFragment = (RouteSelectNavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.route_select_navigation_drawer_fragment);
		mTitle = getTitle();
		
		// mTransitMethodDrawerList = (ListView) findViewById(R.id.transit_method_navigation_drawer_fragment);

		mTransitMethodNavigationDrawerItemTitles = getResources().getStringArray(R.array.transit_methods);
		// Set up the drawer.
		mTransitMethodNavigationDrawerFragment.setUp(R.id.transit_method_navigation_drawer_fragment,
				(DrawerLayout) findViewById(R.id.drawer_layout));
	}


	/*@Override
	protected void onCreateView(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTransitMethodDrawerList = (ListView) findViewById(R.id.transit_method_navigation_drawer_fragment);
	}*/

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager
				.beginTransaction()
				.replace(R.id.container,
						BusFragment.newInstance(position + 1)).commit();
	}

	public void onSectionAttached(int number) {
		switch (number) {
		case 1:
			mTitle = getString(R.string.title_section1);
			break;
		case 2:
			mTitle = getString(R.string.title_section2);
			break;
		case 3:
			mTitle = getString(R.string.title_section3);
			break;
		}
	}

	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mTransitMethodNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void getRouteSelectDrawerFragment(){

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
