package com.mentalmachines.ttime.objects;

import java.util.ArrayList;

/**
 * Created by CaptofOuterSpace on 1/23/2016.
 */
public class Stops {
    ArrayList<Stop> stops  = new ArrayList<Stop>();

    class Stop {
        int route;
        int stop_order;
        int stop_id;
        String stop_name;
        double stop_lat;
        double stop_lon;
    }

}
