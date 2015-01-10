/*
 * Copyright (c) 2015 Jonathan Nelson
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
import org.ciasaboark.tacere.activity.ProUpgradeActivity;
import org.ciasaboark.tacere.billing.Authenticator;
import org.ciasaboark.tacere.manager.ServiceStateManager;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;

import java.text.DateFormat;
import java.util.Date;

public class QuickSilenceTinyWidgetProvider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
        ServiceStateManager serviceStateManager = ServiceStateManager.getInstance(context);
        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews;

            Authenticator authenticator = new Authenticator(context);
            if (!authenticator.isAuthenticated()) {
                remoteViews = new RemoteViews(context.getPackageName(),
                        R.layout.widget_quicksilence_tiny_pro);
                Intent proUpgradeIntent = new Intent(context, ProUpgradeActivity.class);
                PendingIntent upgradePendingIntent = PendingIntent.getActivity(context, 0,
                        proUpgradeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.widget_quicksilence_fab,
                        upgradePendingIntent);
            } else {

                if (serviceStateManager.isQuicksilenceActive()) {
                    remoteViews = new RemoteViews(context.getPackageName(),
                            R.layout.widget_quicksilence_tiny_active);
                    long endTimeStamp = serviceStateManager.getEndTimeStamp();
                    DateFormat dateFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);
                    Date date = new Date(endTimeStamp);
                    String formattedDate = dateFormatter.format(date);
                    remoteViews.setTextViewText(R.id.widget_quicksilence_text,
                            "Silencing until " + formattedDate);
                    Intent intent = new Intent(context, EventSilencerService.class);
                    intent.putExtra(EventSilencerService.WAKE_REASON,
                            RequestTypes.CANCEL_QUICKSILENCE.value);
                    PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    remoteViews.setOnClickPendingIntent(R.id.widget_quicksilence_fab, pendingIntent);
                } else {
                    remoteViews = new RemoteViews(context.getPackageName(),
                            R.layout.widget_quicksilence_tiny_inactive);
                    Intent intent = new Intent(context, EventSilencerService.class);
                    intent.putExtra(EventSilencerService.WAKE_REASON, RequestTypes.QUICKSILENCE.value);
                    Prefs prefs = new Prefs(context);
                    int duration = 60 * prefs.getQuickSilenceHours() + prefs.getQuicksilenceMinutes();
                    intent.putExtra(EventSilencerService.QUICKSILENCE_DURATION, duration);
                    PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    remoteViews.setOnClickPendingIntent(R.id.widget_quicksilence_fab, pendingIntent);
                }


            }

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}
