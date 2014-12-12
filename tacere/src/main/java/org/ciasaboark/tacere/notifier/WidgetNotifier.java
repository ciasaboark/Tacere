/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.notifier;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import org.ciasaboark.tacere.widget.ActiveEventWidgetProvider;
import org.ciasaboark.tacere.widget.QuickSilenceTinyWidgetProvider;
import org.ciasaboark.tacere.widget.QuickSilenceWidgetProvider;

public class WidgetNotifier {
    public static final String TAG = "WidgetNotifier";
    private Context context;

    public WidgetNotifier(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        this.context = ctx;
    }

    public void updateAllWidgets() {
        updateTinyQuicksilenceWidgets();
        updateQuicksilenceWidgets();
        updateActiveEventWidgets();
    }

    private void updateTinyQuicksilenceWidgets() {
        Intent quicksilenceWidgetIntent = new Intent(context, QuickSilenceTinyWidgetProvider.class);
        quicksilenceWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, QuickSilenceTinyWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        quicksilenceWidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
        context.sendBroadcast(quicksilenceWidgetIntent);
    }

    private void updateQuicksilenceWidgets() {
        Intent quicksilenceWidgetIntent = new Intent(context, QuickSilenceWidgetProvider.class);
        quicksilenceWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, QuickSilenceWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        quicksilenceWidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
        context.sendBroadcast(quicksilenceWidgetIntent);
    }

    private void updateActiveEventWidgets() {
        Intent quicksilenceWidgetIntent = new Intent(context, ActiveEventWidgetProvider.class);
        quicksilenceWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, ActiveEventWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        quicksilenceWidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
        context.sendBroadcast(quicksilenceWidgetIntent);
    }
}
