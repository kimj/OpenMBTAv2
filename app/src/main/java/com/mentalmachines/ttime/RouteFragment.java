package com.mentalmachines.ttime;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
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

    boolean mInbound = true;
    RecyclerView mList;
    String[] mItems;
    AnimatorSet moveLeft, moveRight;

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
            swipeListHelper.attachToRecyclerView(mList);
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
                moveRight.start();
            } else {
                ObjectAnimator.ofFloat(view, "rotation", -540f).start();
                mItems = getArguments().getStringArray(OUT_STOPS_LIST);
                ((FloatingActionButton)view).setImageResource(R.drawable.ic_menu_back);
                moveLeft.start();
            }

        }
    };

    void setAndRunAnimation(boolean right) {

    }

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
                .with(ObjectAnimator.ofFloat(mList, "alpha", 1f, 0f))
                .before(ObjectAnimator.ofFloat(mList, "alpha", 0f, 1f))
                .with(ObjectAnimator.ofFloat(mList, "translationX", -width, 0));
        moveRight.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) { }

            @Override
            public void onAnimationEnd(Animator animation) {
                mList.setAdapter(new SimpleStopAdapter(mItems, getArguments().getInt(TAG)));
            }

            @Override
            public void onAnimationCancel(Animator animator) { }

            @Override
            public void onAnimationRepeat(Animator animator) { }
        });

        moveLeft = new AnimatorSet();
        moveLeft.play(ObjectAnimator.ofFloat(mList, "translationX", 0, -width))
                .with(ObjectAnimator.ofFloat(mList, "alpha", 1f, 0f))
                .before(ObjectAnimator.ofFloat(mList, "translationX", width, 0))
                .with(ObjectAnimator.ofFloat(mList, "alpha", 0f, 1f));

        moveLeft.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) { }

            @Override
            public void onAnimationEnd(Animator animation) {
                mList.setAdapter(new SimpleStopAdapter(mItems, getArguments().getInt(TAG)));
            }

            @Override
            public void onAnimationCancel(Animator animator) { }

            @Override
            public void onAnimationRepeat(Animator animator) { }
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

    /**
     * this touch code is not acting on the list item
     it is triggering an animation and changes out the underlying list
     */
    ItemTouchHelper swipeListHelper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(0, 0) {

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            if(direction == ItemTouchHelper.LEFT) {
                moveLeft.start();
            } else {
                moveRight.start();
            }
        }
    });

}
