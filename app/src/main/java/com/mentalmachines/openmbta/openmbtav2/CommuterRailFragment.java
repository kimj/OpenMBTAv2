package com.mentalmachines.openmbta.openmbtav2;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mentalmachines.openmbta.openmbtav2.objects.RouteConfig;
import com.mentalmachines.openmbta.openmbtav2.objects.RouteList;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.util.concurrent.ExecutionException;

public class CommuterRailFragment extends Fragment{
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";
	RouteList routeList;
	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static CommuterRailFragment newInstance(int sectionNumber) {
		CommuterRailFragment fragment = new CommuterRailFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	public CommuterRailFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.content_main, container,
				false);
		getRouteList();
		getRouteConfig(1);
		return rootView;
	}

	
	public void getRouteList(){
		String jsonResponse = null;
		Uri.Builder builder = new Uri.Builder();
		Serializer serializer = new Persister();
		RouteList routeList;
		// http://realtime.mbta.com/developer/api/v2/stopsbylocation?api_key=wX9NwuHnZU2ToO7GmGR9
		// uw&lat=42.346961&lon=-71.076640&format=json
		
		Integer latitude = null, longitude = null;
		String apiKey = null;
		builder.scheme("http").authority("realtime.mbta.com").appendPath("developer");
		builder.appendPath("api").appendPath("v2").appendPath("stopsbylocation");
		builder.appendQueryParameter("api_key", apiKey).appendQueryParameter("lat", latitude.toString());
		builder.appendQueryParameter("lon", longitude.toString()).appendQueryParameter("format", "json");
		String URL = builder.toString();
		try {
			jsonResponse = new HttpRequestTask().execute(URL, "", "").get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			routeList = serializer.read(RouteList.class, jsonResponse);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void getRouteConfig(Integer routeId){
		String xmlResponse = null;
		Uri.Builder builder = new Uri.Builder();
		Serializer serializer = new Persister();
		RouteConfig routeConfig;
		
		builder.scheme("http").authority("webservices.nextbus.com").appendPath("service");
		builder.appendPath("publicXMLFeed").appendQueryParameter("command", "routeConfig").appendQueryParameter("a", "mbta");
		builder.appendQueryParameter("r", routeId.toString());
		String URL = builder.toString();
		try {
			xmlResponse = new HttpRequestTask().execute(URL, "", "").get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			routeConfig = serializer.read(RouteConfig.class, xmlResponse);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.i("OpenMBTAv2", xmlResponse);
	}
}
