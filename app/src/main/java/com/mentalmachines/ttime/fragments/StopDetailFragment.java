package com.mentalmachines.ttime.fragments;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.adapter.StopDetailAdapter;
import com.mentalmachines.ttime.objects.Favorite;
import com.mentalmachines.ttime.objects.StopData;
import com.mentalmachines.ttime.objects.StopList;
import com.mentalmachines.ttime.objects.Utils;
import com.mentalmachines.ttime.services.FavoritesAction;
import com.mentalmachines.ttime.services.StopService;

public class StopDetailFragment extends Fragment {
	/**
	 * A fragment representing a stop, favorite or detailed from a route
	 */
    public static final String TAG = "StopDetailFragment";
    public RecyclerView mList;
    public StopList mStopDetail;
    static ProgressDialog mProgress = null;

    //required empty constructor
    public StopDetailFragment() {}

    public static final StopDetailFragment newInstance(StopData stopToShow) {
        StopDetailFragment fragment = new StopDetailFragment();
        final Bundle args = new Bundle();
        args.putParcelable(StopService.TAG, stopToShow);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final Context ctx = getContext();
        final Intent tnt = new Intent(ctx, StopService.class);
        tnt.putExtra(StopService.TAG, getArguments().getParcelable(StopService.TAG));
        ctx.startService(tnt);
        //start the service, register the receiver
        LocalBroadcastManager.getInstance(ctx).registerReceiver(mDetailsUpdated,
                new IntentFilter(StopService.TAG));
        return inflater.inflate(R.layout.stops_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mList = (RecyclerView) view.findViewById(R.id.stopdetail_list);
        final SwipeRefreshLayout swipeViewGroup = (SwipeRefreshLayout)view.findViewById(R.id.stopdetail_swipe);
        swipeViewGroup.setOnRefreshListener(refreshList);
        swipeViewGroup.setColorSchemeColors(R.color.colorPrimary, R.color.colorPrimaryDark);
        //show a progress dialog when the list is empty and the user is waiting, refreshing doesn't work here
        mProgress = ProgressDialog.show(getContext(), "", getString(R.string.getting_data), true, true);
    }

    @Override
    public void onResume() {
        super.onResume();
        FavoritesAction.setFavoriteButton((FloatingActionButton) getActivity().findViewById(R.id.favorites_fab),
                Favorite.isStopFavorite(mStopDetail.mainStop.stopId));
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

            FavoritesAction.setFavoriteButton((FloatingActionButton) getActivity().findViewById(R.id.favorites_fab), Favorite.isStopFavorite(mStopDetail.mainStop.stopId));
            Log.d(TAG, mStopDetail.mainStop.stopName +
                    " data check, stop detail list size " + mStopDetail.mStopList.size());
            ((TextView)getView().findViewById(R.id.stopdetail_name)).setText(mStopDetail.mainStop.stopName);
            StopDetailFragment.this.setList();
        }
    };

}
