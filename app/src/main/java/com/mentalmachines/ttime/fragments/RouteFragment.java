package com.mentalmachines.ttime.fragments;

import android.animation.Animator;
import android.animation.AnimatorSet;
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
import com.mentalmachines.ttime.services.ScheduleService;

public class RouteFragment extends Fragment{
	/**
	 * A fragment representing a train or bus line
	 */
    private static final String LINE_NAME = "line";
    private static final String TAG = "RouteFragment";

    boolean mInbound = true;
    public RecyclerView mList;
    public static SimpleStopAdapter mListAdapter;
    AnimatorSet moveLeft, moveRight, moveR2, moveL2;

	/**
	 * Returns a new instance of this fragment
     * sets the route stops and route name
	 */
	public static RouteFragment newInstance(SimpleStopAdapter listData, String title) {
		RouteFragment fragment = new RouteFragment();
		Bundle args = new Bundle();
        args.putString(LINE_NAME, title);
		fragment.setArguments(args);
        mListAdapter = listData;
		return fragment;
	}

	public RouteFragment() { }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View rootView = inflater.inflate(R.layout.route_fragment, container, false);

        mList = (RecyclerView) rootView.findViewById(R.id.route_list);
        if(mListAdapter == null) {
			mList.setVisibility(View.GONE);
            getActivity().findViewById(R.id.fab_in_out).setVisibility(View.GONE);
            Log.w(TAG, "no stops");
        } else {
            //there is a route
			mList.setVisibility(View.VISIBLE);
            if(mListAdapter.isOneWay) {
                //this is a one way route
                Log.w(TAG, "one way route");
                getActivity().findViewById(R.id.fab_in_out).setVisibility(View.GONE);
            } else {
               //getActivity().findViewById(R.id.fab_in_out).setVisibility(View.VISIBLE);
                getActivity().findViewById(R.id.fab_in_out).setOnClickListener(fabListener);
            }
            mList.setAdapter(mListAdapter);
            //TODO wire up inbound and outbound based on the time and the last time this fragment was shown
            final SwipeRefreshLayout swipeViewGroup = (SwipeRefreshLayout) rootView.findViewById(R.id.route_swipe);
            swipeViewGroup.setOnRefreshListener(refreshList);
            swipeViewGroup.setColorSchemeColors(R.color.colorPrimary, R.color.solidSilverline, R.color.colorPrimaryDark);
        }
        //Floating Action button switches the display between inbound and outbound
		return rootView;
	}

    SwipeRefreshLayout.OnRefreshListener refreshList = new SwipeRefreshLayout.OnRefreshListener() {

        @Override
        public void onRefresh() {
            Log.d(TAG, "on refresh");
            reloadTimes();
        }
    };

    public void reloadTimes() {
        final Context ctx = getContext();
        if(TTimeApp.checkNetwork(ctx)) {
            //the main activity broadcast receiver will reload the data into the adapter and list
            final Intent tnt = new Intent(ctx, ScheduleService.class);
            tnt.putExtra(DBHelper.KEY_ROUTE_NAME, mListAdapter.mRoute.name);
            tnt.putExtra(DBHelper.KEY_ROUTE_ID, mListAdapter.mRoute.id);
            ctx.startService(tnt);
        } else {
            Toast.makeText(ctx, "check network", Toast.LENGTH_SHORT).show();
        }
    }


    View.OnClickListener fabListener = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        reloadTimes();
        //make sure the times are fresh when switching directions
        if(moveRight == null) {
            animationSetup(getView());
        }
        mInbound = !mInbound;
        if(mInbound) {
            ObjectAnimator.ofFloat(view, "rotation", 540f).start();
            //mItems = getArguments().getStringArray(IN_STOPS_LIST);
            ((FloatingActionButton)view).setImageResource(R.drawable.ic_menu_forward);
            mListAdapter.changeDirection(1);
            moveRight.start();
        } else {
            ObjectAnimator.ofFloat(view, "rotation", -540f).start();
            //TODO -> call the schedule service to get the latest times
            ((FloatingActionButton)view).setImageResource(R.drawable.ic_menu_back);
            mListAdapter.changeDirection(0);
            moveLeft.start();
        }
        }
    };

    /**
     * these animations run when switching between inbound and outbound
     * the "moveRight" go along with the direction of the FAB
     * the moveLeft are also setup for gesture detectors, swiping changes the list
     */
    void animationSetup(final View screen) {

        final int width = screen.getWidth();
        //Log.d(TAG, "setting up animations " + width);
        moveRight = new AnimatorSet();
        moveRight.play(ObjectAnimator.ofFloat(mList, "translationX", 0, 2 * width))
                .with(ObjectAnimator.ofFloat(mList, "alpha", 1f, 0f));
        moveR2 = new AnimatorSet();
        moveR2.play(ObjectAnimator.ofFloat(mList, "alpha", 0f, 1f))
                .with(ObjectAnimator.ofFloat(mList, "translationX", -width, 0));
        moveRight.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) { }

            @Override
            public void onAnimationEnd(Animator animation) {
                //move right, IN_STOPS_LIST
                //SimpleStopAdapter.pullSchedule(getContext(), 1+"");
                mList.setAdapter(mListAdapter);
                moveR2.start();
            }

            @Override
            public void onAnimationCancel(Animator animator) { }

            @Override
            public void onAnimationRepeat(Animator animator) { }
        });

        moveLeft = new AnimatorSet();
        moveLeft.play(ObjectAnimator.ofFloat(mList, "translationX", 0, -width))
                .with(ObjectAnimator.ofFloat(mList, "alpha", 1f, 0f));
        moveL2 = new AnimatorSet();
        moveL2.play(ObjectAnimator.ofFloat(mList, "translationX", width, 0))
                .with(ObjectAnimator.ofFloat(mList, "alpha", 0f, 1f));

        moveLeft.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) { }

            @Override
            public void onAnimationEnd(Animator animation) {
                //SimpleStopAdapter.pullSchedule(getContext(), 0+"");
                mList.setAdapter(mListAdapter);
                moveL2.start();
            }

            @Override
            public void onAnimationCancel(Animator animator) { }

            @Override
            public void onAnimationRepeat(Animator animator) { }
        });
    }

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
