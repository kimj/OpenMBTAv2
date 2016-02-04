package com.mentalmachines.ttime.objects;

import java.util.ArrayList;

/**
 * Created by CaptofOuterSpace on 1/23/2016.
 */
public class Stops {
    ArrayList<Stop> stops  = new ArrayList<Stop>();

    class Stop {
        String stop_order;
        String stop_id;
        String stop_name;
        String stop_lat;
        String stop_lon;
    }
}
