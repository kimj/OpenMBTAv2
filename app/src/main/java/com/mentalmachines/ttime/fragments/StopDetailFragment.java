package com.mentalmachines.ttime.fragments;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.mentalmachines.ttime.services.SaveFavorites;
import com.mentalmachines.ttime.services.StopService;

public class StopDetailFragment extends Fragment {
	/**
	 * A fragment representing a stop, favorite or detailed from a route
	 */
    private static final String TAG = "StopDetailFragment";
    public RecyclerView mList;
    static StopList mStopDetail;
    static ProgressDialog mProgress = null;

    //required empty constructor
    public StopDetailFragment() {}

    public static final StopDetailFragment newInstance(Context ctx, StopData stopToShow) {
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
        return inflater.inflate(R.layout.activity_stops, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mList = (RecyclerView) view.findViewById(R.id.route_list);
        final SwipeRefreshLayout swipeViewGroup = (SwipeRefreshLayout)view.findViewById(R.id.route_swipe);
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
        ((SwipeRefreshLayout)getView().findViewById(R.id.route_swipe)).setRefreshing(false);
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
            ((SwipeRefreshLayout)getView().findViewById(R.id.route_swipe)).setRefreshing(true);
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
            new CheckFavorite(false).execute(mStopDetail.mainStop.stopId);
            Log.d(TAG, mStopDetail.mainStop.stopName +
                    " data check, stop detail list size " + mStopDetail.mStopList.size());
            ((TextView)getView().findViewById(R.id.stopdetail_name)).setText(mStopDetail.mainStop.stopName);
            StopDetailFragment.this.setList();
        }
    };

    /**
     * TODO move to activity
     */
    class CheckFavorite extends AsyncTask<String, Void, Boolean> {
        boolean menuCall;

        /**
         * pushing the db I/O into a background thread
         * this sets the button's display, does not yet save the change
         * @param change, is this a menu click to change the favorite value or a call to read it
         */
        public CheckFavorite(boolean change) {
            menuCall = change;
        }

        @Override
        protected Boolean doInBackground(String[] params) {
            if(menuCall) {
                menuCall = Favorite.isStopFavorite(params[0]);
                if(menuCall) {
                    //remove favorite stop from table
                    Favorite.dropFavoriteStop(params[0]);
                } else {
                    //start Intent service to save stop to the faves table
                    final Intent tnt = SaveFavorites.newInstance(
                            getContext(), mStopDetail.mainStop.stopId);
                    getContext().startService(tnt);
                }
                return !menuCall;
            }
            return Favorite.isStopFavorite(params[0]);
        }

        /*@Override
        protected void onPostExecute(Boolean o) {
            super.onPostExecute(o);
            if(o) {
                mFavoritesAction.setChecked(true);
                mFavoritesAction.setIcon(android.R.drawable.star_big_on);
            } else {
                mFavoritesAction.setChecked(false);
                mFavoritesAction.setIcon(android.R.drawable.star_big_off);
            }
        }*/
    }
}
