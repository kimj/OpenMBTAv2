package com.mentalmachines.ttime.objects;

import android.database.Cursor;

import com.mentalmachines.ttime.DBHelper;

/**
 * Created by CaptofOuterSpace on 2/5/2016.
 * There is a gap here, it would help to show the list of affected stops
 * The stop table only reports the latest, any stop might be affected by more than one alert
 * This level of detail is not essential to the first release
 */
public class Alert {
    public String alert_id; //e.g.115683-1
    public int effect_name;
    public String effect;
    public String cause;
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

    public Alert() {}

    public Alert(Cursor alertsCursor) {
        this.alert_id = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_ALERT_ID));
        this.effect_name = alertsCursor.getInt(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_NAME));
        this.effect = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT));
        this.cause = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_CAUSE));
        this.description_text = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_DESCRIPTION_TEXT));
        this.short_header_text= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_SHORT_HEADER_TEXT));
        this.severity= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_SEVERITY));
        this.created_dt= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_CREATED_DT));
        this.last_modified_dt= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_LAST_MODIFIED_DT));
        this.timeframe_text= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_TIMEFRAME_TEXT));
        this.alert_lifecycle= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_ALERT_LIFECYCLE));
        this.effect_start = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_PERIOD_START));
        this.effect_end= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_PERIOD_END));
    }

    public static class StopAlertData {
        public String stopID;
        public String stopName;
        public String[] routeNames;
        public Alert[] alertsForStop;
    }
}



