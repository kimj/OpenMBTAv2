package com.mentalmachines.ttime.fragments;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.adapter.SimpleStopAdapter;

public class RouteFragment extends Fragment{
	/**
	 * A fragment representing a train or bus line
	 */
    private static final String LINE_NAME = "line";
    private static final String TAG = "RouteFragment";

    boolean mInbound = true;
    public RecyclerView mList;
    static SimpleStopAdapter mListAdapter;
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
        final Bundle args = getArguments();

		mList = (RecyclerView) rootView.findViewById(R.id.mc_routelist);

        if(mListAdapter == null) {
			mList.setVisibility(View.GONE);
            getActivity().findViewById(R.id.fab_in_out).setVisibility(View.GONE);
            Log.w(TAG, "no stops");
            //titleTV.setText(args.getString(LINE_NAME));
            //todo
        } else {
            //getActivity().setTitle(args.getString(LINE_NAME));
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
            //TODO wire up inbound and outbound based on time/previous display
        }
        //Floating Action button switches the display between inbound and outbound

		return rootView;
	}

    View.OnClickListener fabListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
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

    /*  The gesture detector does not play nicely with a scrolling list

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


    /**
     * these animations run when switching between inbound and outbound
     * the "moveRight" go along with the direction of the FAB
     * the moveLeft are also setup for gesture detectors, swiping changes the list
     */
    void animationSetup(final View screen) {

        final int width = screen.getWidth();
        Log.d(TAG, "setting up animations " + width);
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

    /**
     * This will be a quick call to update the times when the list direction changes

    public class ChangeList extends AsyncTask<Object, Void, SimpleStopAdapter> {
        final boolean right;

        public ChangeList(boolean b) {
            right = b;
        }

        @Override
        protected SimpleStopAdapter doInBackground(Object... params) {
            final Bundle args = RouteFragment.this.getArguments();
            return new CursorRouteAdapter(
                RouteFragment.this.getActivity(),
                    ((MainActivity) getActivity()).mRouteId,
                mInbound? 1: 0);
            //CursorRouteAdapter(Context ctx, String routeId, int routeColor, int direction)
        }

        @Override
        protected void onPostExecute(SimpleStopAdapter result) {
            super.onPostExecute(result);
            //newInstance(CursorRouteAdapter listData, String title, int bgColor) {
            if(isCancelled() || getActivity() == null) return;
            mListAdapter = result;
            if(right) {
                moveRight.start();
            } else {
                moveLeft.start();
            }
        }
    } */

}
