/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.database;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.ciasaboark.tacere.widget.ActiveEventWidgetProvider;

/**
 * Created by Jonathan Nelson on 8/11/14.
 */
public class DataSetManager {
    public static final String BROADCAST_MESSAGE_KEY = "data set changed";
    public static final String SOURCE_KEY = "source";
    public static final String ROW_CHANGED = "rowChanged";
    private static final String TAG = "DataSetManager";

    private Context context;
    private Object source;

    public DataSetManager(Object src, Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        if (src == null) {
            throw new IllegalArgumentException("source object cannot be null");
        }
        context = ctx;
        source = src;
    }

    public void broadcastDataSetChangedForId(long id) {
        Bundle args = new Bundle();
        args.putLong(ROW_CHANGED, id);
        broadcastDataSetChangedMessage(args);
    }

    private void broadcastDataSetChangedMessage(Bundle args) {
        assert args != null;

        Log.d(TAG, "Broadcasting data set changed message on behalf of " + source.getClass());
        Intent intent = new Intent(BROADCAST_MESSAGE_KEY);
        String sourceClass = source.getClass().getName();
        args.putString(SOURCE_KEY, sourceClass);
        intent.putExtras(args);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        Intent activeEventWidgetIntent = new Intent(context, ActiveEventWidgetProvider.class);
        activeEventWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context,
                ActiveEventWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        activeEventWidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
        context.sendBroadcast(activeEventWidgetIntent);
    }

    public void broadcastDataSetChangedMessage() {
        broadcastDataSetChangedMessage(new Bundle());
    }


}
