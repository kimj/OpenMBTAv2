package com.mentalmachines.openmbta.openmbtav2;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.mentalmachines.openmbta.openmbtav2.objects.RouteConfig;
import com.mentalmachines.openmbta.openmbtav2.objects.RouteList;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.util.concurrent.ExecutionException;

public class SubwayFragment extends Fragment{
	/**
	 * A fragment representing the a train line
	 */
	private static final String STOPS_LIST = "stops";
    private static final String LINE_NAME = "line";
    private static final String TAG = "SubwayFragment";

	/**
	 * Returns a new instance of this fragment
     * sets the route stops and route name
	 */
	public static SubwayFragment newInstance(String[] stops, String title) {
		SubwayFragment fragment = new SubwayFragment();
		Bundle args = new Bundle();
		args.putStringArray(STOPS_LIST, stops);
        args.putString(LINE_NAME, title);
		fragment.setArguments(args);
		return fragment;
	}

	public SubwayFragment() { }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.content_main, container, false);
        final String lineName = getArguments().getString(LINE_NAME);
        rootView.setTag(lineName);
        ((TextView)rootView.findViewById(R.id.mc_title)).setText(lineName);
        //now work the list
        final String[] listItems = getArguments().getStringArray(STOPS_LIST);
        //final String[] listItems = getResources().getStringArray(R.array.fake_data);
		final RecyclerView rView = (RecyclerView) rootView.findViewById(R.id.mc_routelist);
        if(listItems == null) {
			rView.setVisibility(View.GONE);
            Log.w(TAG, "no stops");
        } else {
			rView.setVisibility(View.VISIBLE);
			rView.setAdapter(new SimpleStopAdapter(listItems));
        }
        final CheckBox cb = (CheckBox) rootView.findViewById(R.id.mc_favorite);
        if(listItems != null) {
            cb.setVisibility(View.VISIBLE);
            //read and set preference
            cb.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(lineName, false));
            cb.setOnCheckedChangeListener(favListener);
        }

		return rootView;
	}

    final CompoundButton.OnCheckedChangeListener favListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putBoolean((String)getView().getTag(), b).commit();
        }
    };

	/*@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((MainActivity) activity).onSectionAttached(getArguments().getInt(
				STOPS_LIST));
	}*/
	
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
