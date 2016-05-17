package com.mentalmachines.ttime.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.adapter.AlertsAdapter;
import com.mentalmachines.ttime.objects.Alert;

import java.util.ArrayList;

/**
 * Created by CaptofOuterSpace on 2/15/2016.
 */
public class AlertsFragment extends Fragment {

    private static final String LINE_NAME = "line";
    public static final String TAG = "AlertsFragment";

    ListView alertsListView;
    ArrayList<Alert> alerts;
    AlertsAdapter alertsAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static AlertsFragment newInstance(AlertsAdapter alertsAdapter, String title) {
        AlertsFragment fragment = new AlertsFragment();
        Bundle args = new Bundle();
        args.putString(LINE_NAME, title);
        fragment.setArguments(args);
        alertsAdapter = alertsAdapter;
        return fragment;
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

        final FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

        Bundle arguments = getArguments();
        String alertId = "";
        if (arguments != null){
            alertId = arguments.getString("alertId");
        }
        alertsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Alert alert = alerts.get(position);
                AlertDetailFragment alertDetailFragment = new AlertDetailFragment();
                Bundle arguments = new Bundle();
                arguments.putString("alertId", alert.alert_id);
                alertDetailFragment.setArguments(arguments);
                fragmentManager.beginTransaction().add(R.id.container, alertDetailFragment).addToBackStack(null).commit();
            }
        });

        if (alertId != null || alertId != ""){
            alerts = DBHelper.getAlertsByStopAlertId(alertId);
        }  else {
            alerts = DBHelper.getAllAlerts() ;
        }

        AlertsAdapter alertsAdapter = new AlertsAdapter(getActivity(), R.layout.alert_item_layout, alerts);
        alertsListView.setAdapter(alertsAdapter);
    }

    public void updateAlertsListView(String alertId){
        ArrayList<Alert> alerts = new ArrayList<>();
        if (alertId != null){
            // alerts = DBHelper.getAlertByRouteId(alertId);
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
