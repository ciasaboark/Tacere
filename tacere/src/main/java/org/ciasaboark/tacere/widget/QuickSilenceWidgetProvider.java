/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.manager.ServiceStateManager;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;

public class QuickSilenceWidgetProvider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
        ServiceStateManager serviceStateManager = ServiceStateManager.getInstance(context);
        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_quicksilence);
            Intent intent = new Intent(context, EventSilencerService.class);
            if (serviceStateManager.isQuicksilenceActive()) {
                remoteViews.setImageViewResource(R.id.widget_quicksilence_icon, R.drawable.fab_normal);
                intent.putExtra(EventSilencerService.WAKE_REASON, RequestTypes.CANCEL_QUICKSILENCE);
            } else {
                remoteViews.setImageViewResource(R.id.widget_quicksilence_icon, R.drawable.fab_silent);
                intent.putExtra(EventSilencerService.WAKE_REASON, RequestTypes.QUICKSILENCE);
                Prefs prefs = new Prefs(context);
                int duration = 60 * prefs.getQuickSilenceHours() + prefs.getQuicksilenceMinutes();
                intent.putExtra(EventSilencerService.QUICKSILENCE_DURATION, duration);
            }
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_quicksilence_root, pendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.widget_quicksilence_icon, pendingIntent);

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}
