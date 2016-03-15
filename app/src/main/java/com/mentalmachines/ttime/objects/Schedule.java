package com.mentalmachines.ttime.objects;

import java.util.ArrayList;

public class Schedule {

	public Route route;
    /**
     * These array lists are parallel to the Inbound/Outbound stops of the route
     */
    public ArrayList<StopTimes> TripsInbound = new ArrayList<>();
    public ArrayList<StopTimes> TripsOutbound = new ArrayList<>();

    /**
     * Every time the T will depart from a specific stop on a route
     * Three schedules for every route, Weekday, Saturday and Sunday
     */
    public static class StopTimes {
        public String tripSign;
        public String stopId;

        public ArrayList<Integer> hours = new ArrayList<>();
        public ArrayList<Integer> minutes = new ArrayList<>();
    }
}