package com.mentalmachines.ttime.objects;

import java.util.ArrayList;

public class Route{
	static ArrayList<RouteData> bus = new ArrayList<RouteData>();
	static ArrayList<RouteData> bus_hide = new ArrayList<RouteData>();
	static ArrayList<RouteData> subway = new ArrayList<RouteData>();

	public class RouteData{
		public String route_id, route_name, mode_name;
	}
}