package com.mentalmachines.ttime.objects;

import java.util.ArrayList;

/**
 * Created by CaptofOuterSpace on 1/23/2016.
 */
public class RoutesByStop {
    int stop_id;
    String stop_name;
    ArrayList<Mode> mode;

    class Mode {
        int route_type;
        String mode_name;
        ArrayList<Route> route;

        class Route{
            int route_id;
            String route_name;
        }
    }
}


