package com.mentalmachines.ttime.views;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.views.adapter.StopDetailAdapter;
import com.mentalmachines.ttime.data.model.StopData;
import com.mentalmachines.ttime.data.model.StopList;
import com.mentalmachines.ttime.Utils;
import com.mentalmachines.ttime.services.CheckFavorite;
import com.mentalmachines.ttime.services.StopService;

public class StopFragment extends Fragment {
	/**
	 * A fragment representing a stop, favorite or detailed from a route
	 */
    public static final String TAG = "StopFragment";
    public RecyclerView mList;
    public StopData mStop;
    public StopList mStopDetail;
    static ProgressDialog mProgress = null;

    //required empty constructor
    public StopFragment() {}

    public static final StopFragment newInstance(StopData stopToShow) {
        StopFragment fragment = new StopFragment();
        final Bundle args = new Bundle();
        args.putParcelable(StopService.TAG, stopToShow);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Add your menu entries here
        super.onCreateOptionsMenu(menu, inflater);
        menu.removeItem(R.id.menu_nearby);
        menu.removeItem(R.id.menu_favorites);
        menu.removeItem(R.id.menu_schedule);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final Context ctx = getContext();
        final Intent tnt = new Intent(ctx, StopService.class);
        mStop = getArguments().getParcelable(StopService.TAG);
        tnt.putExtra(StopService.TAG, mStop);
        ctx.startService(tnt);
        //start the service, register the receiver
        LocalBroadcastManager.getInstance(ctx).registerReceiver(mDetailsUpdated,
                new IntentFilter(StopService.TAG));
        return inflater.inflate(R.layout.stops_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mStop.stopName.contains("-")) {
            ((MainActivity)getActivity()).mToolbar.setTitle(mStop.stopName.substring(0, (mStop.stopName.indexOf("-") -1)));
        } else {
            ((MainActivity)getActivity()).mToolbar.setTitle(mStop.stopName);
        }
        mList = (RecyclerView) view.findViewById(R.id.stopdetail_list);
        final SwipeRefreshLayout swipeViewGroup = (SwipeRefreshLayout)view.findViewById(R.id.stopdetail_swipe);
        swipeViewGroup.setOnRefreshListener(refreshList);
        swipeViewGroup.setColorSchemeColors(R.color.colorPrimary, R.color.colorPrimaryDark);
        //show a progress dialog when the list is empty and the user is waiting, refreshing doesn't work here
        mProgress = ProgressDialog.show(getContext(), "", getString(R.string.getting_data), true, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mProgress != null && mProgress.isShowing()) {
            mProgress.cancel();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mDetailsUpdated);
    }

    void setList() {
        final StopData[] adapterList = new StopData[mStopDetail.mStopList.size()];
        mStopDetail.mStopList.toArray(adapterList);
        mList.setAdapter(new StopDetailAdapter(adapterList));
        Log.d(TAG, "set new adapter");
        ((SwipeRefreshLayout)getView().findViewById(R.id.stopdetail_swipe)).setRefreshing(false);
    }


    SwipeRefreshLayout.OnRefreshListener refreshList = new SwipeRefreshLayout.OnRefreshListener() {

        @Override
        public void onRefresh() {
            Log.d(TAG, "on refresh");
            reloadTimes();
        }
    };

    public void reloadTimes() {
        Log.d(TAG, "reload times");
        if(Utils.checkNetwork(getContext())) {
            ((SwipeRefreshLayout)getView().findViewById(R.id.stopdetail_swipe)).setRefreshing(true);
            //the main activity broadcast receiver will reload the data into the adapter and list
            final Intent tnt = new Intent(getContext(), StopService.class);
            tnt.putExtra(StopService.TAG, mStopDetail.mainStop);
            //alt - use stop data from arguments
            getActivity().startService(tnt);

        } else {
            Toast.makeText(getContext(), getString(R.string.chkNet), Toast.LENGTH_SHORT).show();
        }
    }

    BroadcastReceiver mDetailsUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "service completed");
            if(mProgress != null && mProgress.isShowing()) {
                mProgress.cancel();
            }
            if(intent.getExtras() == null) {
                //error broadcast, no extras
                Toast.makeText(context, getString(R.string.schedSvcErr), Toast.LENGTH_SHORT).show();
                return;
            }
            mStopDetail = intent.getParcelableExtra(StopService.TAG);
            Log.i(TAG, "favorites change called");

            new CheckFavorite(((MainActivity)getActivity()).mFavoritesAction).execute(
                    getContext(), false, mStopDetail.mainStop.stopId);
            Log.d(TAG, mStopDetail.mainStop.stopName +
                    " data check, stop detail list size " + mStopDetail.mStopList.size());
            //((TextView)getView().findViewById(R.id.stopdetail_name)).setText(mStopDetail.mainStop.stopName);
            StopFragment.this.setList();
        }
    };

}
