/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.Columns;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.manager.ServiceStateManager;
import org.ciasaboark.tacere.view.EventListItem;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

public class EventCursorAdapter extends CursorAdapter {
    private static final String TAG = "EventCursorAdapter";
    private final LayoutInflater layoutInflator;
    private final DatabaseInterface databaseInterface;
    private final Context context;
    private List<Long> animatedViews = new ArrayList<Long>();
    private boolean hasTodayHeaderBeenDrawn = false;
    private boolean hasFutureHeaderBeenDrawn = false;


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
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final long id = cursor.getLong(cursor.getColumnIndex(Columns._ID));
        final long begin = cursor.getLong(cursor.getColumnIndex(Columns.BEGIN));
        EventListItem eventListItem = (EventListItem) view;
        eventListItem.setEventById(id);

        ServiceStateManager serviceStateManager = ServiceStateManager.getInstance(context);
        eventListItem.setIsActiveEvent(serviceStateManager.getActiveEventId() == id);

        java.util.Calendar calendarDate = new GregorianCalendar();
        calendarDate.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendarDate.set(java.util.Calendar.MINUTE, 0);
        calendarDate.set(java.util.Calendar.SECOND, 0);
        calendarDate.set(java.util.Calendar.MILLISECOND, 0);
        calendarDate.add(java.util.Calendar.DAY_OF_MONTH, 1);
        long tomorrowMidnight = calendarDate.getTimeInMillis();
        if (begin >= tomorrowMidnight) {
            eventListItem.setIsFutureEvent(true);
        }

        if (!hasTodayHeaderBeenDrawn && !eventListItem.isFutureEvent()) {
            eventListItem.setHeaderType(EventListItem.HeaderType.TODAY);
            hasTodayHeaderBeenDrawn = true;
        } else if (!hasFutureHeaderBeenDrawn && eventListItem.isFutureEvent()) {
            eventListItem.setHeaderType(EventListItem.HeaderType.LATER);
            hasFutureHeaderBeenDrawn = true;
        }
        eventListItem.refresh();


//        if (!animatedViews.contains(id)) {
//            Animation animation = AnimationUtils.loadAnimation(context, R.anim.abc_fade_in);
//            view.startAnimation(animation);
//            animatedViews.add(id);
//        }

    }
}