package com.mentalmachines.ttime.objects;

import java.util.ArrayList;

public class Schedule{

	public Route route;
    /**
     * These array lists are parallel to the Inbound/Outbound stops of the route
     */
    public StopTimes[] TripsInbound;
    public StopTimes[] TripsOutbound;

    public Schedule(Route r) {
        route = r;
        TripsInbound = new StopTimes[r.mInboundStops.size()];
        TripsOutbound = new StopTimes[r.mOutboundStops.size()];
    }

    /**
     * Every time the T will depart from a specific stop on a route
     * Three schedules for every route, Weekday, Saturday and Sunday
     * The TripsInbound and outbound lists will have a stop times object for every stop on the route
     */
    public static class StopTimes {
        public String stopId;

        public ArrayList<String> morning = new ArrayList<>();
        public ArrayList<String> amPeak = new ArrayList<>();
        public ArrayList<String> midday = new ArrayList<>();
        public ArrayList<String> pmPeak = new ArrayList<>();
        public ArrayList<String> night = new ArrayList<>();
    }

    /**
     * Creator required for class implementing the parcelable interface.

    public static final Parcelable.Creator<Schedule> CREATOR = new Creator<Schedule>() {

        @Override
        public Schedule createFromParcel(Parcel parcel) {
            final Route route = parcel.readTypedObject(Route.CREATOR);
            Schedule schedule = new Schedule(route);

            return schedule;
        }

        @Override
        public Schedule[] newArray(int size) {
            return new Schedule[size];
        }
    };  */
}