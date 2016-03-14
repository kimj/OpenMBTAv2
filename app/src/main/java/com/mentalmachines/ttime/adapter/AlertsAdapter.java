package com.mentalmachines.ttime.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.objects.Alert;

import java.nio.channels.AlreadyConnectedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlertsAdapter extends ArrayAdapter<Alert> {
    public static final String TAG = "AlertsAdapter";
    static boolean loadComplete = false;
    static SQLiteDatabase mDB;
    Context context;
    private static LayoutInflater inflater = null;
    ArrayList<Alert> alerts;

    static class ViewHolder{
        ImageView imageViewSeverityIcon;
        TextView textViewSeverityText;
        TextView textViewServiceEffectText;
        TextView textViewDescriptionText;
        TextView textViewEffectPeriods;
        TextView textViewAlertLifecycle;
        TextView textViewAffectedServices;
    }

    /*public AlertsAdapter(Context context, ArrayList<Alert> alerts, ViewGroup parent){
        // super (context, android.R.layout.simple_list_item_1, )
        this.context = context
    }*/
    final static String[] mAlertProjection = new String[]{
            DBHelper.KEY_ALERT_ID,
            DBHelper.KEY_EFFECT_NAME, DBHelper.KEY_EFFECT,
            DBHelper.KEY_CAUSE, DBHelper.KEY_SHORT_HEADER_TEXT,
            DBHelper.KEY_DESCRIPTION_TEXT, DBHelper.KEY_SEVERITY,
            DBHelper.KEY_CREATED_DT, DBHelper.KEY_LAST_MODIFIED_DT,
            // DBHelper.KEY_SERVICE_EFFECT_TEXT,
            DBHelper.KEY_TIMEFRAME_TEXT,
            DBHelper.KEY_ALERT_LIFECYCLE, DBHelper.KEY_EFFECT_PERIOD_START,
            DBHelper.KEY_EFFECT_PERIOD_END


            // DBHelper.KEY_EFFECT_PERIODS
    };

    public AlertsAdapter(Context context, int resource, ArrayList<Alert> alerts) {
        super(context, resource, alerts);
    }

    static String[] loadAlertArray(String[] silverline, SQLiteDatabase db) {
        final List<String> tmp = new ArrayList<>();
        for (String routeId : silverline) {
            tmp.addAll(Arrays.asList(loadAlertsArray(routeId, db)));
        }
        final String[] returnList = new String[tmp.size()];
        tmp.toArray(returnList);
        tmp.clear();
        return returnList;
    }

    public ArrayList<Alert> loadAllAlerts(SQLiteDatabase db) {
        final ArrayList<Alert> alerts = new ArrayList<>();

        Boolean b = db.isOpen();
        /*Cursor alertsCursor = db.query(DBHelper.DB_ALERTS_TABLE, mAlertProjection,
                null, null, null, null, null, null);*/
        Cursor alertsCursor = db.rawQuery("SELECT * FROM alerts", null);
        if (alertsCursor.getCount() > 0 && alertsCursor.moveToFirst()) {
            do {
                Alert a = new Alert();
                a.alert_id = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_ALERT_ID));
                a.effect_name = alertsCursor.getInt(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_NAME));
                a.effect = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT));
                a.cause = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_CAUSE));
                a.description_text = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_DESCRIPTION_TEXT));
                a.short_header_text= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_SHORT_HEADER_TEXT));                a.severity = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_SEVERITY));
                a.severity= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_SEVERITY));
                a.created_dt= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_CREATED_DT));
                a.last_modified_dt= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_LAST_MODIFIED_DT));
                a.timeframe_text= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_TIMEFRAME_TEXT));
                a.alert_lifecycle= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_ALERT_LIFECYCLE));
                a.effect_start = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_PERIOD_START));
                a.effect_end= alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_PERIOD_END));
                alerts.add(a);
            } while (alertsCursor.moveToNext());
            alertsCursor.close();
        }
        return alerts;
    }

    static String[] loadAlertsArray(String route, SQLiteDatabase db) {
        String alertsWhereClause = "";
        final ArrayList<Alert> alerts = new ArrayList<>();

        Cursor alertsCursor = db.query(DBHelper.DB_ALERTS_TABLE, mAlertProjection,
                alertsWhereClause + route, null,
                null, null, null, null);
        //get all the subway color routes then get the first and last stop
        if (alertsCursor.getCount() > 0 && alertsCursor.moveToFirst()) {
            do {
                Alert a = new Alert();
                a.description_text = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_DESCRIPTION_TEXT));
                a.effect_name = alertsCursor.getInt(alertsCursor.getColumnIndex(DBHelper.KEY_EFFECT_NAME));
                a.cause = alertsCursor.getString(alertsCursor.getColumnIndex(DBHelper.KEY_CAUSE));
                alerts.add(a);
            } while (alertsCursor.moveToNext());
            alertsCursor.close();
        }
        final String[] routeIds = new String[alerts.size()];
        alerts.toArray(routeIds);
        alerts.clear();
        String routeChild;

        final String[] returnList = new String[alerts.size()];
        alerts.toArray(returnList);
        alerts.clear();
        Log.d(TAG, "returning line array, size is " + returnList.length);
        return returnList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout alertLayout;
        Alert alert = getItem(position);

        if (convertView == null){
            alertLayout = new LinearLayout(getContext());
            inflater = (LayoutInflater) context.getSystemService((Context.LAYOUT_INFLATER_SERVICE));
            convertView = inflater.inflate(R.layout.alert_item_layout, parent, false);
        } else {
            alertLayout = (LinearLayout) convertView;
        }

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.imageViewSeverityIcon = (ImageView) convertView.findViewById(R.id.imageViewSeverityIcon);
        viewHolder.textViewSeverityText = (TextView) convertView.findViewById(R.id.textViewSeverityText);
        viewHolder.textViewServiceEffectText = (TextView) convertView.findViewById(R.id.textViewServiceEffectText);;
        viewHolder.textViewDescriptionText = (TextView) convertView.findViewById(R.id.textViewDescriptionText);
        viewHolder.textViewEffectPeriods = (TextView) convertView.findViewById(R.id.textViewEffectPeriods);
        viewHolder.textViewAlertLifecycle = (TextView) convertView.findViewById(R.id.textViewAlertLifecycle);
        viewHolder.textViewAffectedServices = (TextView) convertView.findViewById(R.id.textViewAffectedServices);
        convertView.setTag(viewHolder);

        viewHolder.imageViewSeverityIcon.setImageResource(getSeverityIcon(alert.severity));
        viewHolder.textViewSeverityText.setText(alert.severity);
        viewHolder.textViewServiceEffectText.setText(alert.service_effect_text);
        viewHolder.textViewDescriptionText.setText(alert.description_text);
        viewHolder.textViewEffectPeriods.setText(alert.effect_start + " - " + alert.effect_end);
        viewHolder.textViewAlertLifecycle.setText(alert.alert_lifecycle);
        // viewHolder.textViewAffectedServices.setText(alert);
        return convertView;
    }

    private int getSeverityIcon(String severityText){
        String resourceName = "";
        resourceName = "ic_alert_severity_" + severityText + ".png";
        int id = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
        return id;
    }
}
