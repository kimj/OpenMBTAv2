package com.mentalmachines.ttime.views;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mentalmachines.ttime.data.DBHelper;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.views.adapter.AlertsAdapter;
import com.mentalmachines.ttime.data.model.Alert;

import java.util.ArrayList;

/**
 * Created by CaptofOuterSpace on 2/15/2016.
 */
public class AlertsFragment extends Fragment {

    public static final String TAG = "AlertsFragment";

    ListView alertsListView;
    ArrayList<Alert> alerts;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public AlertsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.alerts_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //TODO need to get the latest data every time this fragment shows
        //startService(new Intent(this, GetMBTARequestService.class));
        //can start the service in the MainActivity before calling the fragment
        alertsListView = (ListView) view.findViewById(R.id.listViewAlerts);
        alerts =  DBHelper.getAllAlerts();
        alertsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Alert alert = alerts.get(position);
                ((MainActivity)getActivity()).showStopAlert(alert.alert_id);
            }
        });

        AlertsAdapter alertsAdapter = new AlertsAdapter(getActivity(), R.layout.alert_item_layout, alerts);
        alertsListView.setAdapter(alertsAdapter);
    }

    public void updateAlertsListView(String alertId){

        if (alertId != null){
            alerts = DBHelper.getAlertsByStopAlertId(alertId);
        }  else {
            alerts =  DBHelper.getAllAlerts();
        }
        AlertsAdapter alertsAdapter = new AlertsAdapter(getActivity(), R.layout.alert_item_layout, alerts);
        alertsListView.setAdapter(alertsAdapter);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.alerts_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
}
