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
import android.os.Bundle;
import android.widget.RemoteViews;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.MainActivity;
import org.ciasaboark.tacere.activity.ProUpgradeActivity;
import org.ciasaboark.tacere.billing.Authenticator;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.manager.ServiceStateManager;
import org.ciasaboark.tacere.widget.activity.WidgetPopupRinger;

public class ActiveEventWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ServiceStateManager serviceStateManager = ServiceStateManager.getInstance(context);
        EventInstance activeEvent = serviceStateManager.getActiveEvent();

        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews;
            Authenticator authenticator = new Authenticator(context);
            if (!authenticator.isAuthenticated()) {
                //widgets are for pro version only
                remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_active_event_pro);
                Intent proUpgradeIntent = new Intent(context, ProUpgradeActivity.class);
                PendingIntent upgradePendingIntent = PendingIntent.getActivity(context, 0, proUpgradeIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.upgrade_button, upgradePendingIntent);
                remoteViews.setImageViewResource(R.id.silence_icon, R.drawable.shopping_cart);
            } else if (activeEvent == null) {
                remoteViews = new RemoteViews(context.getPackageName(),
                        R.layout.widget_active_event_none);
            } else {
                remoteViews = new RemoteViews(context.getPackageName(),
                        R.layout.widget_active_event);
                remoteViews.setTextViewText(R.id.event_title, activeEvent.getTitle());
                String location = activeEvent.getLocation();
                if (location.equals("")) {
                    location = "No location set";
                }
                remoteViews.setTextViewText(R.id.event_location, location);
                DatabaseInterface databaseInterface = DatabaseInterface.getInstance(context);
                remoteViews.setTextViewText(R.id.event_calendar,
                        databaseInterface.getCalendarNameForId(activeEvent.getCalendarId()));
                remoteViews.setImageViewResource(R.id.ringerState, R.drawable.ic_launcher);
                remoteViews.setTextViewText(R.id.event_end, "Ends: " +
                        activeEvent.getLocalEndDate() + " " + activeEvent.getLocalEndTime());

                Intent selectRingerIntent = new Intent(context,
                        org.ciasaboark.tacere.widget.activity.WidgetPopupRinger.class);
                selectRingerIntent.putExtra(WidgetPopupRinger.EVENT_ID, activeEvent.getId());

                PendingIntent pendingRingerIntent = PendingIntent.getActivity(context, 15,
                        selectRingerIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                remoteViews.setImageViewResource(R.id.silence_icon, R.drawable.ic_state_ignore);
                remoteViews.setOnClickPendingIntent(R.id.silence_icon, pendingRingerIntent);

            }
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.app_icon, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        // This is how you get your changes.
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
    }


}
