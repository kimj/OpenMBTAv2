package com.mentalmachines.ttime;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

public class RouteFragment extends Fragment{
	/**
	 * A fragment representing a train or bus line
	 */
    private static final String IN_STOPS_LIST = "in";
    private static final String OUT_STOPS_LIST = "out";
    private static final String LINE_NAME = "line";
    private static final String TAG = "RouteFragment";
    private static final int SWIPE_VELOCITY = 150;

    boolean mInbound = true;
    RecyclerView mList;
    String[] mItems;

    ObjectAnimator moveLeft, moveLeft2, moveRight, moveRight2;

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
        animationSetup(rootView);
        final Bundle args = getArguments();

        ((TextView)rootView.findViewById(R.id.mc_title)).setText(args.getString(LINE_NAME));
        ((TextView)rootView.findViewById(R.id.mc_title)).setTextColor(args.getInt(TAG));
        /* TODO polish this
        final int d = setUpTitle(lineName, (TextView)rootView.findViewById(R.id.mc_title),
                (ImageView) rootView.findViewById(R.id.mc_icon));*/
        //now work the list
        final String[] listItems = args.getStringArray(IN_STOPS_LIST);
        //defaulting to inbound

		mList = (RecyclerView) rootView.findViewById(R.id.mc_routelist);

        if(listItems == null) {
			mList.setVisibility(View.GONE);
            Log.w(TAG, "no stops");
        } else {
			mList.setVisibility(View.VISIBLE);
			mList.setAdapter(new SimpleStopAdapter(listItems, args.getInt(TAG)));
            //TODO wire up inbound and outbound
        }
        final CheckBox cb = (CheckBox) rootView.findViewById(R.id.mc_favorite);
        if(listItems != null) {
            cb.setVisibility(View.VISIBLE);
            //read and set preference
            cb.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(
                    getArguments().getString(LINE_NAME), false));
            cb.setOnCheckedChangeListener(favListener);
        }

        //Floating Action button switches between inbound and outbound
        getActivity().findViewById(R.id.fab_in_out).setOnClickListener(fabListener);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gesture.onTouchEvent(event);
            }
        });
		return rootView;
	}

    View.OnClickListener fabListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            setAndRunAnimation(true);
        }
    };

    void setAndRunAnimation(boolean right) {
        mInbound = !mInbound;
        if(mInbound) {
            mItems = getArguments().getStringArray(IN_STOPS_LIST);
        } else {
            mItems = getArguments().getStringArray(OUT_STOPS_LIST);
        }
        if(right) {
            moveRight.start();
        } else {
            moveLeft.start();
        }
    }

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
            });


    /**
     * these animations run when switching between inbound and outbound
     * the "moveRight" go along with the direction of the FAB
     * the moveLeft are also setup for gesture detectors, swiping changes the list
     */
    void animationSetup(View screen) {
        final int width = screen.getWidth();
        moveRight = ObjectAnimator.ofFloat(screen, "translationX", 0, 2 * width);
        moveRight2 = ObjectAnimator.ofFloat(screen, "translationX", -width, 0);
        moveRight.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mList.setAdapter(new SimpleStopAdapter(mItems, getArguments().getInt(TAG)));
                moveRight2.start();
            }

        });
        moveLeft = ObjectAnimator.ofFloat(screen, "translationX", 0, -width);
        //instantiate the second animation here, instead of everytime the listener runs
        moveLeft2 = ObjectAnimator.ofFloat(screen, "translationX", width, 0);

        moveLeft.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //after animating current fragment offscreen, hide it
                mList.setAdapter(new SimpleStopAdapter(mItems, getArguments().getInt(TAG)));
                //now animate the new fragment on to the screen
                moveLeft2.start();
            }
        });
    }

    /**
     * Setup up the line name title textview
     * Return the icon resource needed by the Recycler View
     * @param titleResource - name string int resource
      *@param titleTV - the title text field  @return icon drawable resource
     */
    int setUpTitle(int titleResource, TextView titleTV, ImageView v) {
        titleTV.setText(titleResource);
        switch (titleResource) {
            case R.string.nm_blue:
                v.setImageResource(R.drawable.ic_blueline);
                //TODO, animate into view
                v.setVisibility(View.VISIBLE);
                return R.drawable.ic_blueline;
            case R.string.nm_green:
                v.setImageResource(R.drawable.ic_greenline);
                v.setVisibility(View.VISIBLE);
                return R.drawable.ic_greenline;
            case R.string.nm_orange:
                v.setImageResource(R.drawable.ic_orangeline);
                v.setVisibility(View.VISIBLE);
                return R.drawable.ic_orangeline;
            case R.string.nm_red:
                v.setImageResource(R.drawable.ic_redline);
                v.setVisibility(View.VISIBLE);
                return R.drawable.ic_redline;
            case R.string.nm_silver:
                v.setImageResource(R.drawable.ic_silverline);
                v.setVisibility(View.VISIBLE);
                return R.drawable.ic_silverline;
            default:
                //v.setVisibility(View.GONE); NO need
                return -1;
        }
    }

    final CompoundButton.OnCheckedChangeListener favListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putBoolean(getArguments().getString(LINE_NAME), b).commit();
        }
    };

}
