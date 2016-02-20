package com.mentalmachines.ttime;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class RouteFragment extends Fragment{
	/**
	 * A fragment representing a train or bus line
	 */
    private static final String IN_STOPS_LIST = "in";
    private static final String OUT_STOPS_LIST = "out";
    private static final String LINE_NAME = "line";
    private static final String TAG = "RouteFragment";

    boolean mInbound = true;
    RecyclerView mList;
    String[] mItems;
    AnimatorSet moveLeft, moveRight, moveR2, moveL2;

	/**
	 * Returns a new instance of this fragment
     * sets the route stops and route name
	 */
	public static RouteFragment newInstance(String[] instops, String[] outstops, String title, int bgColor) {
		RouteFragment fragment = new RouteFragment();
		Bundle args = new Bundle();
		args.putStringArray(IN_STOPS_LIST, instops);
        args.putStringArray(OUT_STOPS_LIST, outstops);
        args.putString(LINE_NAME, title);
        args.putInt(TAG, bgColor);
		fragment.setArguments(args);
		return fragment;
	}

	public RouteFragment() { }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.content_main, container, false);

        final Bundle args = getArguments();
        final TextView titleTV = (TextView)rootView.findViewById(R.id.mc_title);
        mItems = args.getStringArray(IN_STOPS_LIST);

		mList = (RecyclerView) rootView.findViewById(R.id.mc_routelist);

        if(mItems == null) {
			mList.setVisibility(View.GONE);
            Log.w(TAG, "no stops");
            titleTV.setText(args.getString(LINE_NAME));
            titleTV.setTextColor(args.getInt(TAG));
        } else {
            final CheckBox cb = (CheckBox) rootView.findViewById(R.id.mc_favorite);
            cb.setVisibility(View.VISIBLE);
            //read and set preference
            cb.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(
                    getArguments().getString(LINE_NAME), false));
            cb.setOnCheckedChangeListener(favListener);
            //defaulting to inbound
            titleTV.setText(args.getString(LINE_NAME));
            titleTV.setTextColor(args.getInt(TAG));
			mList.setVisibility(View.VISIBLE);
			mList.setAdapter(new SimpleStopAdapter(mItems, args.getInt(TAG)));

            //TODO wire up inbound and outbound based on time/previous display
        }
        //Floating Action button switches the display between inbound and outbound
        getActivity().findViewById(R.id.fab_in_out).setOnClickListener(fabListener);
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
                mItems = getArguments().getStringArray(IN_STOPS_LIST);
                ((FloatingActionButton)view).setImageResource(R.drawable.ic_menu_forward);
                ((TextView)getActivity().findViewById(R.id.mc_title)).setText(
                        getArguments().getString(LINE_NAME));
                moveRight.start();
            } else {
                ObjectAnimator.ofFloat(view, "rotation", -540f).start();
                mItems = getArguments().getStringArray(OUT_STOPS_LIST);
                ((FloatingActionButton)view).setImageResource(R.drawable.ic_menu_back);
                ((TextView)getActivity().findViewById(R.id.mc_title)).setText(
                        getArguments().getString(LINE_NAME));
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
                mList.setAdapter(new SimpleStopAdapter(mItems, getArguments().getInt(TAG)));
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
                mList.setAdapter(new SimpleStopAdapter(mItems, getArguments().getInt(TAG)));
                moveL2.start();
            }

            @Override
            public void onAnimationCancel(Animator animator) { }

            @Override
            public void onAnimationRepeat(Animator animator) { }
        });
    }

    final CompoundButton.OnCheckedChangeListener favListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putBoolean(getArguments().getString(LINE_NAME), b).commit();
            DBHelper.handleFavorite(getContext(), getArguments().getString(LINE_NAME), b);
            if(getActivity().findViewById(R.id.exp_favorite).getTag() != null) {
                getActivity().findViewById(R.id.exp_favorite).callOnClick();
                //problem with zero?
            }
        }
    };


}
