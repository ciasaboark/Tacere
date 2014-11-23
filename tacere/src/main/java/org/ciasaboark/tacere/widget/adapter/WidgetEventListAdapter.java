/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.widget.adapter;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.Columns;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventInstanceException;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.view.EventListItem;

import java.util.ArrayList;

public class WidgetEventListAdapter implements RemoteViewsFactory {
    private ArrayList<EventInstance> listItemList = new ArrayList<EventInstance>();
    private Context context = null;
    private int appWidgetId;

    public WidgetEventListAdapter(Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        populateListItem();
    }

    private void populateListItem() {
        DatabaseInterface databaseInterface = DatabaseInterface.getInstance(context);
        Cursor eventsCursor = databaseInterface.getEventCursor();
        while (eventsCursor.moveToNext()) {
            long instanceId = eventsCursor.getLong(eventsCursor.getColumnIndex(Columns._ID));
            try {
                EventInstance event = databaseInterface.getEvent(instanceId);
                listItemList.add(event);
            } catch (NoSuchEventInstanceException e) {
                //TODO
            }
        }

    }


    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return listItemList.size();
    }

    /*
    *Similar to getView of Adapter where instead of View
    *we return RemoteViews
    *
    */
    @Override
    public RemoteViews getViewAt(int position) {
        final RemoteViews remoteView = new RemoteViews(
                context.getPackageName(), R.layout.widget_event_list_row);
        EventListItem listItem = new EventListItem(context, null);
        listItem.setEvent(listItemList.get(position));
        listItem.refresh();
        listItem.measure(500, 500);
        listItem.layout(0, 0, 500, 500);
        Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        listItem.draw(new Canvas(bitmap));
        remoteView.setImageViewBitmap(R.id.widget_event_list_row_imageview, bitmap);
        return remoteView;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
