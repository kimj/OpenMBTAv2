package com.mentalmachines.ttime;

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
import android.widget.ImageView;
import android.widget.TextView;

public class RouteFragment extends Fragment{
	/**
	 * A fragment representing the a train line
	 */
	private static final String STOPS_LIST = "stops";
    private static final String LINE_NAME = "line";
    private static final String TAG = "RouteFragment";

	/**
	 * Returns a new instance of this fragment
     * sets the route stops and route name
	 */
	public static RouteFragment newInstance(String[] stops, int title, int bgColor) {
		RouteFragment fragment = new RouteFragment();
		Bundle args = new Bundle();
		args.putStringArray(STOPS_LIST, stops);
        args.putInt(LINE_NAME, title);
        args.putInt(TAG, bgColor);
		fragment.setArguments(args);
		return fragment;
	}

	public RouteFragment() { }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.content_main, container, false);
        final Bundle args = getArguments();
        final int lineName = args.getInt(LINE_NAME);
        rootView.setTag(getString(lineName));
        final int d = setUpTitle(lineName, args.getInt(TAG), (TextView)rootView.findViewById(R.id.mc_title),
                (ImageView) rootView.findViewById(R.id.mc_icon));
        //now work the list
        final String[] listItems = args.getStringArray(STOPS_LIST);
        //final String[] listItems = getResources().getStringArray(R.array.fake_data);
		final RecyclerView rView = (RecyclerView) rootView.findViewById(R.id.mc_routelist);
        if(listItems == null) {
			rView.setVisibility(View.GONE);
            Log.w(TAG, "no stops");
        } else {
			rView.setVisibility(View.VISIBLE);
			rView.setAdapter(new SimpleStopAdapter(listItems, d));
        }
        final CheckBox cb = (CheckBox) rootView.findViewById(R.id.mc_favorite);
        if(listItems != null) {
            cb.setVisibility(View.VISIBLE);
            //read and set preference
            cb.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(
                    getString(lineName), false));
            cb.setOnCheckedChangeListener(favListener);
        }

		return rootView;
	}

    /**
     * Setup up the line name title textview
     * Return the icon resource needed by the Recycler View
     * @param titleResource - name string int resource
     * @param bgColor - yellow for a bus or the line color
      *@param titleTV - the title text field  @return icon drawable resource
     */
    int setUpTitle(int titleResource, int bgColor, TextView titleTV, ImageView v) {
        titleTV.setText(titleResource);
        titleTV.setBackgroundColor(bgColor);
        switch (titleResource) {
            case R.string.nm_blue:
                v.setImageResource(R.drawable.ic_blueline);
                //TODO, animate into view
                v.setVisibility(View.VISIBLE);
                return R.drawable.ic_blueline;
            case R.string.nm_green:
                v.setImageResource(R.drawable.ic_greenline);
                v.setVisibility(View.VISIBLE);
                return R.drawable.ic_greenline;
            case R.string.nm_orange:
                v.setImageResource(R.drawable.ic_orangeline);
                v.setVisibility(View.VISIBLE);
                return R.drawable.ic_orangeline;
            case R.string.nm_red:
                v.setImageResource(R.drawable.ic_redline);
                v.setVisibility(View.VISIBLE);
                return R.drawable.ic_redline;
            case R.string.nm_silver:
                v.setImageResource(R.drawable.ic_silverline);
                v.setVisibility(View.VISIBLE);
                return R.drawable.ic_silverline;
            default:
                //v.setVisibility(View.GONE); NO need
                return -1;
        }
    }

    final CompoundButton.OnCheckedChangeListener favListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putBoolean((String)getView().getTag(), b).commit();
        }
    };

}
