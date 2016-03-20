package com.mentalmachines.ttime.fragments;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.mentalmachines.ttime.TTimeApp;
import com.mentalmachines.ttime.adapter.SimpleStopAdapter;
import com.mentalmachines.ttime.objects.Route;
import com.mentalmachines.ttime.services.ScheduleService;

public class RouteFragment extends Fragment{
	/**
	 * A fragment representing a train or bus line
	 */
    private static final String TAG = "RouteFragment";

    public boolean mInbound = true;
    public RecyclerView mList;
    public SimpleStopAdapter mListAdapter;
    int mWidth = -1;

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
		final View rootView = inflater.inflate(R.layout.route_fragment, container, false);

        mList = (RecyclerView) rootView.findViewById(R.id.route_list);
        final SwipeRefreshLayout swipeViewGroup = (SwipeRefreshLayout) rootView.findViewById(R.id.route_swipe);
        swipeViewGroup.setOnRefreshListener(refreshList);
        swipeViewGroup.setColorSchemeColors(R.color.colorPrimary, R.color.colorPrimaryDark);
        //Floating Action button switches the display between inbound and outbound
		return rootView;
	}

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "route resume");

        if(!getArguments().containsKey(TAG)) {
            mList.setVisibility(View.GONE);
            mListAdapter = null;
            getView().findViewById(R.id.route_empty).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.fab_in_out).setVisibility(View.GONE);
            Log.w(TAG, "no stops");
        } else {
            //there is a route
            final Route r = getArguments().getParcelable(TAG);
            mList.setVisibility(View.VISIBLE);
            finishList(r);
            //TODO wire up inbound and outbound based on the time and the last time this fragment was shown
        }
    }

    SwipeRefreshLayout.OnRefreshListener refreshList = new SwipeRefreshLayout.OnRefreshListener() {

        @Override
        public void onRefresh() {
            Log.d(TAG, "on refresh");
            reloadTimes();
        }
    };

    public void resetRoute(Route r) {
        finishList(r);
        if(mInbound) {
            ObjectAnimator.ofFloat(mList, "translationX", -mWidth, 0).start();
        } else {
            ObjectAnimator.ofFloat(mList, "translationX", mWidth, 0).start();
        }
        getActivity().findViewById(R.id.fab_in_out).setEnabled(true);
        ((SwipeRefreshLayout)getActivity().findViewById(R.id.route_swipe)).setRefreshing(false);
    }

    public void finishList(Route r) {
        mListAdapter = new SimpleStopAdapter(r, mInbound);
        if(mListAdapter.isOneWay) {
            //this is a one way route
            Log.w(TAG, "one way route");
            getActivity().findViewById(R.id.fab_in_out).setVisibility(View.GONE);
        } else {
            getActivity().findViewById(R.id.fab_in_out).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.fab_in_out).setOnClickListener(fabListener);
            getActivity().findViewById(R.id.fab_in_out).setEnabled(true);
        }
        mList.setAdapter(mListAdapter);
    }

    public void reloadTimes() {
        final Context ctx = getContext();
        if(TTimeApp.checkNetwork(ctx)) {
            ((SwipeRefreshLayout)getActivity().findViewById(R.id.route_swipe)).setRefreshing(true);
            //the main activity broadcast receiver will reload the data into the adapter and list
            final Intent tnt = new Intent(ctx, ScheduleService.class);
            tnt.putExtra(DBHelper.KEY_ROUTE_NAME, mListAdapter.mRoute.name);
            tnt.putExtra(DBHelper.KEY_ROUTE_ID, mListAdapter.mRoute.id);
            ctx.startService(tnt);
        } else {
            Toast.makeText(ctx, "check network", Toast.LENGTH_SHORT).show();
            getActivity().findViewById(R.id.fab_in_out).setEnabled(true);
        }
    }


    View.OnClickListener fabListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            reloadTimes();
            view.setEnabled(false);
            //make sure the times are fresh when switching directions
            if(mWidth < 0) {
                mWidth = getView().getWidth();
            }
            mInbound = !mInbound;
            Log.d(TAG, "inbound direction?" + mInbound);
            if(mInbound) {
                ObjectAnimator.ofFloat(view, "rotation", 540f).start();
                //mItems = getArguments().getStringArray(IN_STOPS_LIST);
                ((FloatingActionButton)view).setImageResource(R.drawable.ic_menu_forward);
                ObjectAnimator.ofFloat(mList, "translationX", 0, 2 * mWidth).start();
            } else {
                ObjectAnimator.ofFloat(view, "rotation", -540f).start();
                //TODO -> call the schedule service to get the latest times
                ((FloatingActionButton)view).setImageResource(R.drawable.ic_menu_back);
                ObjectAnimator.ofFloat(mList, "translationX", 0, -mWidth).start();
            }
        }
    };

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
