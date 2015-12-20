package com.mentalmachines.openmbta.openmbtav2;

import java.util.concurrent.ExecutionException;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import com.mentalmachines.openmbta.openmbtav2.objects.RouteConfig;
import com.mentalmachines.openmbta.openmbtav2.objects.RouteList;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BusFragment extends Fragment{
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";
	RouteList routeList;
	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static BusFragment newInstance(int sectionNumber) {
		BusFragment fragment = new BusFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	public BusFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container,
				false);
		return rootView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		/*((MainActivity) activity).onSectionAttached(getArguments().getInt(
				ARG_SECTION_NUMBER));*/
	}
}
	

