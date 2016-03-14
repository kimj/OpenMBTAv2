package com.mentalmachines.ttime.fragments;

import android.support.v4.app.ListFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.mentalmachines.ttime.MainActivity;
import com.mentalmachines.ttime.TTimeApp;
import com.mentalmachines.ttime.adapter.AlertsAdapter;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.adapter.SimpleStopAdapter;
import com.mentalmachines.ttime.objects.Alert;

import java.util.ArrayList;

/**
 * Created by CaptofOuterSpace on 2/15/2016.
 */
public class AlertsFragment extends ListFragment {

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.alerts_fragment, container, false);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainActivity mainActivity = (MainActivity) getActivity();
        TTimeApp application = (TTimeApp) mainActivity.getApplication();

        alertsListView = getListView();

        if(alertsAdapter == null) {
            alertsListView.setVisibility(View.GONE);
            getActivity().findViewById(R.id.fab_in_out).setVisibility(View.GONE);
        } else {
            //there is a route
            alertsListView.setVisibility(View.VISIBLE);
            // alertsListView.setAdapter(alertsAdapter);
        }

        ArrayList<Alert> alerts =  new ArrayList<Alert>();
        AlertsAdapter alertsAdapter = new AlertsAdapter(getActivity(), R.layout.alert_item_layout, alerts);
        alerts = alertsAdapter.loadAllAlerts(application.sHelper.getReadableDatabase());
        alertsListView.setAdapter(alertsAdapter);
        alertsAdapter.notifyDataSetChanged();

        setListAdapter(alertsAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.alerts_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
}
