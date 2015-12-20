package com.mentalmachines.openmbta.openmbtav2;

import java.util.ArrayList;
import java.util.List;

import com.mentalmachines.openmbta.openmbtav2.objects.Route;
import com.mentalmachines.openmbta.openmbtav2.objects.RouteConfig;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class RouteSelectAdapter extends ArrayAdapter<Route>  {
	Context mContext;
	ArrayList<Route> routes;
	ListView mListView;
	int layoutResourceId;
	
	public RouteSelectAdapter(Context mContext, int layoutResourceId, ArrayList<Route> routes) {
        super(mContext, layoutResourceId);
        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
        this.routes = routes;
    }

	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout summaryLayout;
		Route route = getItem(position);

		if (convertView == null){
			summaryLayout = new RelativeLayout(getContext());
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.route_select_item, parent, false);
		}
		else {
			summaryLayout = (RelativeLayout) convertView;
		}
		
		TextView textViewRouteName = (TextView) convertView.findViewById(R.id.textview_route_name);
		textViewRouteName.setText(route.getTitle());
				return convertView;
	} 
}