package com.mentalmachines.ttime.fragments;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.adapter.RouteStopAdapter;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.objects.Utils;
import com.mentalmachines.ttime.services.FavoritesAction;
import com.mentalmachines.ttime.services.GetTimesForRoute;

public class RouteFragment extends Fragment {
	/**
	 * A fragment representing a train or bus line
     * It is displayed in the MainActivity and shows a list of stops to the user
     * There is a FAB to switch directions
	 */
    public static final String TAG = "RouteFragment";

    public boolean mInbound = true;
    public RecyclerView mList;
    public RouteStopAdapter mListAdapter;
    public SwipeRefreshLayout mSwipeLayout;

	/**
	 * Returns a new instance of this fragment
     * sets the route stops and route name
	 */
	public static RouteFragment newInstance(Route route) {
        if(route == null) {
            return new RouteFragment();
        }
		RouteFragment fragment = new RouteFragment();
		Bundle args = new Bundle();
        args.putParcelable(TAG, route);
		fragment.setArguments(args);
		return fragment;
	}

	public RouteFragment() { }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        Log.d(TAG, "create view");
        //LocalBroadcastManager.getInstance(getContext()).registerReceiver(mTimesReady, new IntentFilter(GetTimesForRoute.TAG));
        return inflater.inflate(R.layout.route_vp, container, false);
	}

    @Override
    public void onViewCreated(View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        mList = (RecyclerView) rootView.findViewById(R.id.route_list);
        mSwipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.route_swipe);
        mSwipeLayout.setOnRefreshListener(refreshList);
        mSwipeLayout.setColorSchemeColors(R.color.colorPrimary, R.color.colorPrimaryDark);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "route resume");

        if(!getArguments().containsKey(TAG)) {
            mList.setVisibility(View.GONE);
            mListAdapter = null;
            getView().findViewById(R.id.route_empty).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.favorites_fab).setVisibility(View.GONE);
            Log.w(TAG, "no stops");
        } else {
            //there is a route
            final Route r = getArguments().getParcelable(TAG);
            mList.setVisibility(View.VISIBLE);
            finishList(r);
            //TODO wire up inbound and outbound based on the time and the last time this fragment was shown
        }
    }

    /*@Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mTimesReady);
    }*/

    SwipeRefreshLayout.OnRefreshListener refreshList = new SwipeRefreshLayout.OnRefreshListener() {

        @Override
        public void onRefresh() {
            Log.d(TAG, "on refresh");
            reloadTimes();
        }
    };

    public void resetRoute(Route r) {
        finishList(r);
        final int width = getView().getWidth();
        if(mInbound) {
            ObjectAnimator.ofFloat(mList, "translationX", -width, 0).start();
        } else {
            ObjectAnimator.ofFloat(mList, "translationX", width, 0).start();
        }
        getActivity().findViewById(R.id.favorites_fab).setEnabled(true);
        mSwipeLayout.setRefreshing(false);
    }

    public void finishList(Route r) {
        mListAdapter = new RouteStopAdapter(r, mInbound);
        if(mListAdapter.isOneWay) {
            //this is a one way route
            Log.w(TAG, "one way route");
            getView().findViewById(R.id.route_swipe_row).setVisibility(View.GONE);
        } else {
            final View v = getView();
            v.findViewById(R.id.route_swipe_row).setVisibility(View.VISIBLE);
            v.findViewById(R.id.route_leftButton).setOnClickListener(dirClick);
            v.findViewById(R.id.route_rightButton).setOnClickListener(dirClick);
        }
        mList.setAdapter(mListAdapter);
        new FavoritesAction((FloatingActionButton) getActivity().findViewById(R.id.favorites_fab)).
                execute(getContext(), false, r.id);
        mSwipeLayout.setRefreshing(false);
    }

    public void reloadTimes() {
        final Context ctx = getContext();
        if(Utils.checkNetwork(ctx)) {
            //mSwipeLayout.setRefreshing(true);
            //the main activity broadcast receiver will reload the data into the adapter and list
            final Intent tnt = new Intent(ctx, GetTimesForRoute.class);
            tnt.putExtra(DBHelper.KEY_ROUTE_NAME, mListAdapter.mRoute.name);
            tnt.putExtra(DBHelper.KEY_ROUTE_ID, mListAdapter.mRoute.id);
            ctx.startService(tnt);
            Log.d(TAG, "call times from route fragment");
        } else {
            Toast.makeText(ctx, "check network", Toast.LENGTH_SHORT).show();
            getActivity().findViewById(R.id.favorites_fab).setEnabled(true);
        }
    }

    View.OnClickListener dirClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            reloadTimes();
            Log.d(TAG, "click listener");
            getView().findViewById(R.id.route_swipe_row).setClickable(false);
            //make sure the times are fresh when switching directions
            final int width = getView().getWidth();
            if(view.getId() == R.id.route_leftButton) {
                //inbound, default
                mInbound = true;
                view.setSelected(true);
                getView().findViewById(R.id.route_rightButton).setSelected(false);
                ObjectAnimator.ofFloat(view, "rotation", 360f).start();
                //mItems = getArguments().getStringArray(IN_STOPS_LIST);
                ObjectAnimator.ofFloat(mList, "translationX", 0, 2 * width).start();
            } else {
                mInbound = false;
                getView().findViewById(R.id.route_leftButton).setSelected(false);
                view.setSelected(true);
                ObjectAnimator.ofFloat(view, "rotation", -360f).start();
                //TODO -> call the schedule service to get the latest times
                ObjectAnimator.ofFloat(mList, "translationX", 0, -width).start();
            }
            Log.d(TAG, "inbound direction?" + mInbound);
            getView().findViewById(R.id.route_swipe_row).setClickable(false);
            //TODO set row as class variable
        }
    };

    /**
     * This receiver gets the Route object back from the GetTimesForRoute IntentService class
     * Either creates a new RouteFragment or resets the data in the recycler view of the fragment
     */
    /*BroadcastReceiver mTimesReady = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //the route times are ready. set up the adapter
            Log.d(TAG, "times returned");

            //quick callbacks error check, is everything here
            if(getActivity() == null || !isAdded()) {
                return;
            }
            final Bundle b = intent.getExtras();
            if(b == null) {
                //Error check, service returns data
                Log.w(TAG, "no data returned");
                return;
            }

            if((b.containsKey(TAG) && b.getParcelable(RouteFragment.TAG) == null) ||
                    (b.containsKey(GetTimesForRoute.TAG) && b.getBoolean(GetTimesForRoute.TAG))) {
                //error conditions, no route object, true boolean
                Snackbar.make(getActivity().findViewById(R.id.container),
                        getString(R.string.schedSvcErr), Snackbar.LENGTH_LONG).show();

            }
            //exception from predictions but route and schedule times are good
            final Route r = b.getParcelable(TAG);
            if(r == null) {
                Log.i(TAG, "bad route!");
            }

            final String title = getActivity().getTitle().toString();
            if(Route.readableName(context, r.name).equals(title)) {
                //route fragment is already up! Reset route includes animation TODO
                Log.d(TAG, "reset Route");
                resetRoute(r);
            } else {
                getActivity().setTitle(Route.readableName(context, r.name));
            }
            if(!viewCreated) {
                FavoritesAction.setFavoriteButton((FloatingActionButton)getActivity().findViewById(R.id.favorites_fab),
                        Favorite.isFavoriteRoute(r.id));
                viewCreated = true;
            }

            mSwipeLayout.setRefreshing(false);
        }
    };*/

    /*  REMINDER
    gesture detector does not play nicely with a scrolling list
    RecyclerView has it's own left/right item animations that are not easy to override

    final GestureDetector gesture = new GestureDetector(getActivity(),
            new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent start, MotionEvent finish, float velocityX, float velocityY) {
                    //Log.d(TAG,"on fling");
                    super.onFling(start, finish, velocityX, velocityY);
                    if (Math.abs(velocityX) < SWIPE_VELOCITY) {
                        return false;
                    }
                    if(start.getRawX() < finish.getRawX()) {
                        //swipe is going from left to right
                        setAndRunAnimation(true);
                    } else {
                        //swipe is from right to left
                        setAndRunAnimation(false);
                    }
                    return true;
                }
            });*/

}
