package com.mentalmachines.ttime.services;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.MenuItem;

import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.data.model.Favorite;

import java.lang.ref.WeakReference;

/**
 * Created by emezias on 9/6/16.
 * This task will update favorites, as needed
 * Pass in the menu item to the constructor and the context, change boolean and stopid to the execute
 */
public class CheckFavorite extends AsyncTask<Object, Void, Boolean> {
    public static final String TAG = "CheckFavorite";

    WeakReference<MenuItem> mFavoritesAction;

    /**
     * pushing the db I/O into a background thread
     * this sets the button's display, does not yet save the change
     * @param actionBarStar, is this a menu click to change the favorite value or a call to read it
     */
    public CheckFavorite(MenuItem actionBarStar) {
        mFavoritesAction = new WeakReference<MenuItem>(actionBarStar);
    }

    @Override
    protected Boolean doInBackground(Object[] params) {
        Log.i(TAG, "favorites doInBackground");
        final Context ctx = (Context) params[0];
        boolean menuCall = (boolean) params[1];
        String stopId = (String) params[2];
        if(menuCall) {
            menuCall = Favorite.isStopFavorite(stopId); //stop id
            if(menuCall) {
                //remove favorite stop from table
                Favorite.dropFavoriteStop(stopId);
            } else {
                //start Intent service to save stop to the faves table
                final Intent tnt = SaveFavorites.newInstance(ctx, stopId);
                ctx.startService(tnt);
            }
            return !menuCall;
        }
        return Favorite.isStopFavorite(stopId);
    }

    @Override
    protected void onPostExecute(Boolean o) {
        super.onPostExecute(o);
        if(!isCancelled() && mFavoritesAction.get() != null) {
            final MenuItem star = mFavoritesAction.get();
            if(o) {
                star.setChecked(true);
                star.setIcon(R.drawable.ic_star_light);
            } else {
                star.setChecked(false);
                star.setIcon(R.drawable.ic_star_border_light);
            }
            star.setVisible(false);
            star.setVisible(true);
            Log.i(TAG, "favorites change " + o);
        } else {
            Log.w(TAG, "dropping favorites change");
        }

    }
}
