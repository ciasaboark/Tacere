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
import android.util.Log;
import android.widget.RemoteViews;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.MainActivity;
import org.ciasaboark.tacere.activity.ProUpgradeActivity;
import org.ciasaboark.tacere.billing.Authenticator;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.EventManager;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.manager.ServiceStateManager;
import org.ciasaboark.tacere.widget.activity.WidgetPopupRinger;

public class ActiveEventWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "ActiveEventWidgetProvider";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ServiceStateManager serviceStateManager = ServiceStateManager.getInstance(context);
        EventInstance activeEvent = serviceStateManager.getActiveEvent();

        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews;
            Authenticator authenticator = new Authenticator(context);
            if (!authenticator.isAuthenticated()) {
                //widgets are for pro version only
                remoteViews = new RemoteViews(context.getPackageName(),
                        R.layout.widget_active_event_pro);
                Intent proUpgradeIntent = new Intent(context, ProUpgradeActivity.class);
                PendingIntent upgradePendingIntent = PendingIntent.getActivity(context, 0,
                        proUpgradeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.upgrade_button, upgradePendingIntent);
                remoteViews.setImageViewResource(R.id.ringer_icon, R.drawable.shopping_cart);
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
                        activeEvent.getLocalEffectiveEndDate() + " " +
                        activeEvent.getLocalEffectiveEndTime());

                Intent selectRingerIntent = new Intent(context,
                        org.ciasaboark.tacere.widget.activity.WidgetPopupRinger.class);
                selectRingerIntent.putExtra(WidgetPopupRinger.EVENT_ID, activeEvent.getId());

                PendingIntent pendingRingerIntent = PendingIntent.getActivity(context, 15,
                        selectRingerIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.ringer_icon, pendingRingerIntent);

                EventManager eventManager = new EventManager(context, activeEvent);
                RingerType ringerType = eventManager.getBestRinger();
                switch (ringerType) {
                    case NORMAL:
                        remoteViews.setImageViewResource(R.id.ringer_icon, R.drawable.ic_state_normal);
                        break;
                    case VIBRATE:
                        remoteViews.setImageViewResource(R.id.ringer_icon, R.drawable.ic_state_vibrate);
                        break;
                    case SILENT:
                        remoteViews.setImageViewResource(R.id.ringer_icon, R.drawable.ic_state_silent);
                        break;
                    case IGNORE:
                        remoteViews.setImageViewResource(R.id.ringer_icon, R.drawable.ic_state_ignore);
                        break;
                    default:
                        Log.e(TAG, "error getting ringer type for event " + activeEvent.getId());
                }

            }
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.app_icon, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        // This is how you get your changes.
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
    }


}
