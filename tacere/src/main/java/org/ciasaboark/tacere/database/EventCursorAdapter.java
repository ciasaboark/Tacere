/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.database;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CursorAdapter;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.event.EventInstance;

import java.util.ArrayList;
import java.util.List;

public class EventCursorAdapter extends CursorAdapter {
    private static final String TAG = "EventCursorAdapter";
    private final LayoutInflater layoutInflator;
    private final DatabaseInterface databaseInterface;
    private final Context context;
    private EventInstance thisEvent;
    private List<Integer> animatedViews = new ArrayList<Integer>();
    private View view;


    public EventCursorAdapter(Context ctx, Cursor c) {
        super(ctx, c);
        this.databaseInterface = DatabaseInterface.getInstance(ctx);
        this.layoutInflator = LayoutInflater.from(ctx);
        this.context = ctx;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup group) {
        return layoutInflator.inflate(R.layout.list_item_event, group, false);
    }


    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        this.view = view;
        int id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
        try {
            thisEvent = databaseInterface.getEvent(id);
            EventListItemInflator eventListItemInflator = new EventListItemInflator(context, view, thisEvent);
            eventListItemInflator.inflateView();


            if (!animatedViews.contains(id)) {
                Animation animation = AnimationUtils.loadAnimation(context, R.anim.up_from_bottom);
                view.startAnimation(animation);
                animatedViews.add(id);
            }

        } catch (NoSuchEventInstanceException e) {
            Log.w(TAG, "unable to get calendar event to build listview: " + e.getMessage());
        }
    }
}