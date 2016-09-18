package com.mentalmachines.ttime.services;

import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;

import com.mentalmachines.ttime.objects.Favorite;
import com.mentalmachines.ttime.objects.Route;

import java.lang.ref.WeakReference;

/**
 * Created by emezias on 9/6/16.
 * This task will update favorites, as needed
 * Pass in the menu item to the constructor and the context, change boolean and stopid to the execute
 */
public class FavoritesAction extends AsyncTask<Object, Void, Boolean> {
    public static final String TAG = "FavoritesAction";

    final WeakReference<FloatingActionButton> mFavoritesAction;

    /**
     * pushing the db I/O into a background thread
     * this sets the button's display, does not yet save the change
     * @param favoriteButton, is this a menu click to change the favorite value or a call to read it
     */
    public FavoritesAction(FloatingActionButton favoriteButton) {
        mFavoritesAction = new WeakReference<FloatingActionButton>(favoriteButton);
    }

    @Override
    protected Boolean doInBackground(Object[] params) {
        Log.i(TAG, "favorites doInBackground");
        final Context ctx = (Context) params[0];
        boolean isStop = (boolean) params[1];
        String favoriteID = (String) params[2];
        if(isStop) {
            isStop = Favorite.isStopFavorite(favoriteID); //stop id
            if(isStop) {
                //remove favorite stop from table
                Favorite.dropFavoriteStop(favoriteID);
            } else {
                Favorite.setFavoriteStop(favoriteID);
            }
            return Favorite.isStopFavorite(favoriteID);
            //this will set the appearance of the favorites fab button
        } else {
            //route needs to be set as favorite
            isStop = Favorite.isStopFavorite(favoriteID);
            if(isStop) {
                //drop route as a favorite
                Favorite.dropFavoriteRoute(favoriteID);
                //TODO check if schedule is dropped
            } else {
                final String name = Route.getRouteName(ctx, favoriteID);
                Favorite.setFavoriteRoute(name, favoriteID);
            }
            return Favorite.isFavoriteRoute(favoriteID);
        }

    }

    @Override
    protected void onPostExecute(Boolean o) {
        super.onPostExecute(o);
        if(!isCancelled() && mFavoritesAction.get() != null) {
            setFavoriteButton(mFavoritesAction.get(), o);
            Log.i(TAG, "favorites change " + o);
        } else {
            Log.w(TAG, "failed favorites change");
        }
    }

    public static void setFavoriteButton(FloatingActionButton star, boolean isFavorite) {
        if(isFavorite) {
            //star.setSelected(true);
            star.setImageResource(android.R.drawable.star_big_on);
        } else {
            //star.setSelected(false);
            star.setImageResource(android.R.drawable.star_big_off);
        }
        star.invalidate();
        Log.i(TAG, "favorites change " + isFavorite);
    }
}
