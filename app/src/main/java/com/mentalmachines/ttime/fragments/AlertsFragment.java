package com.mentalmachines.ttime.fragments;

import android.support.v4.app.ListFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.mentalmachines.ttime.AlertsAdapter;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.objects.Alert;

import java.util.ArrayList;

/**
 * Created by CaptofOuterSpace on 2/15/2016.
 */
public class AlertsFragment extends ListFragment {
    ListView alertsListView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.alerts_fragment, container, false);
        alertsListView = (ListView) v.findViewById(R.id.alertsListView);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ArrayList<Alert> alerts =  new ArrayList<Alert>();
        AlertsAdapter alertsAdapter = new AlertsAdapter(getActivity(), R.layout.alert_item_layout, alerts);
        /// alertsListView = (ListView) getView();
        alertsListView.setAdapter(alertsAdapter);
        alertsAdapter.notifyDataSetChanged();

        setListAdapter(alertsAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }
}
