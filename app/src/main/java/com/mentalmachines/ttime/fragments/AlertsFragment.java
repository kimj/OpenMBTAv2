package com.mentalmachines.ttime.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private static final String TAG = "AlertsFragment";

    ListView alertsListView;
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

        alertsListView = (ListView) view.findViewById(R.id.listViewAlerts);
        ArrayList<Alert> alerts =  DBHelper.getAllAlerts();
        Log.i(TAG, "alerts to show: " + alerts.size());
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
