package com.mentalmachines.ttime;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mentalmachines.ttime.objects.Alert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlertsAdapter extends ArrayAdapter<Alert> {
    public static final String TAG = "AlertsAdapter";
    static boolean loadComplete = false;
    static SQLiteDatabase mDB;

    final static String[] mAlertProjection = new String[]{
            DBHelper.KEY_ALERT_ID, DBHelper.KEY_DESCRIPTION_TEXT
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
        View alertItemView = inflater.inflate(R.layout.alert_item_layout, parent, false);
        ImageView imageViewSeverityIcon = (ImageView) alertItemView.findViewById(R.id.imageViewSeverityIcon);
        TextView textViewSeverityText = (TextView) alertItemView.findViewById(R.id.textViewSeverityText);
        TextView textViewServiceEffectText = (TextView) alertItemView.findViewById(R.id.textViewServiceEffectText);
        TextView textViewDescriptionText = (TextView) alertItemView.findViewById(R.id.textViewDescriptionText);
        TextView textViewEffectPeriods = (TextView) alertItemView.findViewById(R.id.textViewEffectPeriods);
        TextView textViewAlertLifecycle = (TextView) alertItemView.findViewById(R.id.textViewAlertLifecycle);
        TextView textViewAffectedServices = (TextView) alertItemView.findViewById(R.id.textViewAffectedServices);

        // imageViewSeverityIcon.setText("");
        textViewSeverityText.setText("");
        textViewServiceEffectText.setText("");
        textViewDescriptionText.setText("");
        textViewEffectPeriods.setText("");
        textViewAlertLifecycle.setText("");
        textViewAffectedServices.setText("");

        return alertItemView;
    }
}
