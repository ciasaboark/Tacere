/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/


/* Provides access to a homescreen widget, currently disabled
 * within the manifest
 */
package org.ciasaboark.tacere;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class QuickSilenceProvider extends AppWidgetProvider {

  private static final String TAG = "QuickSilenceProvider";

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

    // Get all ids
    ComponentName thisWidget = new ComponentName(context, QuickSilenceProvider.class);
    int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
    
    //read quicksilence preferences
    SharedPreferences preferences = context.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
    int quickSilenceMinutes = preferences.getInt("quickSilenceMinutes", DefPrefs.QUICK_SILENCE_MINUTES);
	int quickSilenceHours = preferences.getInt("quickSilenceHours", DefPrefs.QUICK_SILENCE_HOURS);
	
	String quicksilenceText = context.getResources().getString(R.string.widget_duration);
	String hrs = "";
	if (quickSilenceHours > 0) {
		hrs = String.valueOf(quickSilenceHours) + " hours "; 
	}
	String length = String.format(quicksilenceText, hrs, quickSilenceMinutes);
    
    for (int widgetId : allWidgetIds) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.quicksilence_widget_layout);
		remoteViews.setTextViewText(R.id.widget_duration, length);
		
		//the length of time for the pollService to sleep in minutes
		int duration = 60 * quickSilenceHours + quickSilenceMinutes;
		
		//an intent to send to PollService immediately
		Intent i = new Intent(context, PollService.class);
		i.putExtra("type", "quickSilent");
		i.putExtra("duration", duration);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
		appWidgetManager.updateAppWidget(widgetId, remoteViews);
    }
  }
} 