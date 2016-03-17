package com.mentalmachines.ttime.objects;

import java.util.ArrayList;

public class Schedule {

	public Route route;
    /**
     * These array lists are parallel to the Inbound/Outbound stops of the route
     */
    public ArrayList<StopTimes> TripsInbound;
    public ArrayList<StopTimes> TripsOutbound;

    public Schedule(Route r) {
        route = r;
        TripsInbound = new ArrayList<>(r.mInboundStops.size());
        TripsOutbound = new ArrayList<>(r.mOutboundStops.size());
    }

    /**
     * Every time the T will depart from a specific stop on a route
     * Three schedules for every route, Weekday, Saturday and Sunday
     * The TripsInbound and outbound lists will have a stop times object for every stop on the route
     */
    public static class StopTimes {
        public String stopId;

        public ArrayList<Integer> hours = new ArrayList<>();
        public ArrayList<Integer> minutes = new ArrayList<>();
    }
}