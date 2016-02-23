package com.mentalmachines.ttime.objects;

/**
 * Created by CaptofOuterSpace on 2/5/2016.
 * There is a gap here, it would help to show the list of affected stops
 * The stop table only reports the latest, any stop might be affected by more than one alert
 * This level of detail is not essential to the first release
 */
public class Alert {
        public int alert_id;
        public String effect_name;
        public String effect;
        public String cause_name;
        public String cause;
        public String header_text;
        public String short_header_text;
        public String description_text;
        public String severity;
        public String created_dt;
        public String last_modified_dt;
        public String service_effect_text;
        public String timeframe_text;
        public String alert_lifecycle;
        public String effect_start;
        public String effect_end;
}



