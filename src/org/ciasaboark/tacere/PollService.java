/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere;


import java.util.Calendar;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract.Instances;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

public class PollService extends IntentService {
	private static final String TAG = "PollService";
	
	//Values that should be read from the settings
	//TODO find out a good way to handle default values here
	private boolean isActivated = true;
	private boolean silenceFreeTime;
	private int ringerType;
	private boolean adjustMedia;
	private int mediaVolume;
	private boolean adjustAlarm;
	private int alarmVolume;
	private int quickSilenceMinutes;
	private int refreshInterval;
	
	private Handler handler = new Handler();
	
	public PollService () {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "Received an intent: " + intent);
		readSettings();
		if (isActivated) {
			final String[] INSTANCE_PROJECTION = new String[] {
			    Instances.EVENT_ID,      // 0
			    Instances.BEGIN,         // 1
			    Instances.TITLE          // 2
			  };
			  
			// The indices for the projection array above.
			final int PROJECTION_ID_INDEX = 0;
			final int PROJECTION_BEGIN_INDEX = 1;
			final int PROJECTION_TITLE_INDEX = 2;
			

			// Specify the date range you want to search for recurring
			// event instances
			//Calendar beginTime = Calendar.getInstance();
			long startMillis = Calendar.getInstance().getTimeInMillis() - DateUtils.DAY_IN_MILLIS;
			//Calendar endTime = Calendar.getInstance();
			long endMillis = Calendar.getInstance().getTimeInMillis() + DateUtils.DAY_IN_MILLIS;
			  
			Cursor cur = null;
			ContentResolver cr = getContentResolver();

			// The ID of the recurring event whose instances you are searching
			// for in the Instances table
			String selection = Instances.EVENT_ID + " = ?";
			String[] selectionArgs = new String[] {"207"};

			// Construct the query with the desired date range.
			Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
			ContentUris.appendId(builder, startMillis);
			ContentUris.appendId(builder, endMillis);

			// Submit the query
			cur =  cr.query(builder.build(), 
			    INSTANCE_PROJECTION, 
			    selection, 
			    selectionArgs, 
			    null);
			   
			while (cur.moveToNext()) {
			    String title = null;
			    long eventID = 0;
			    long beginVal = 0;    
			    
			    // Get the field values
			    eventID = cur.getLong(PROJECTION_ID_INDEX);
			    beginVal = cur.getLong(PROJECTION_BEGIN_INDEX);
			    title = cur.getString(PROJECTION_TITLE_INDEX);
			              
			    // Do something with the values. 
			    Log.i(TAG, "Event:  " + title); 
			    Calendar calendar = Calendar.getInstance();
			    calendar.setTimeInMillis(beginVal);  
			    //DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
			    //Log.i(TAG, "Date: " + formatter.format(calendar.getTime()));    
			    }
			
			Log.d(TAG, "Service is marked to start, registering with AlarmManager");
			scheduleAlarm();
			//do stuff here
		} else {
			Log.d(TAG, "Service is not supposed to start, shutting down");
			stopService();
		}
	}
	
	private void scheduleAlarm() {
		Intent i = new Intent(this, PollService.class);
		PendingIntent pintent = PendingIntent.getService(this, 0, i, 0);

		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		
		//Using the RTC clock _should_ keep the service from waking the device every time a
		//+ pending intent should fire. The pending intents should not trigger until some other
		//+ process wakes the device
		alarm.set(AlarmManager.RTC, System.currentTimeMillis() + DateUtils.MINUTE_IN_MILLIS * refreshInterval, pintent);
		
		//Creating a toast from within an IntentService can leave toast visible indefinitely.  Posting the
		//+ toast from a new thread solves this problem.
		handler.post(new Runnable() {
						
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(),"PollService scheduling for restart", Toast.LENGTH_SHORT).show();
						}
					});
	}
	
	private void cancelAlarm() {
		Log.d(TAG, "PollService:cancelAlarm() called");
		Intent i = new Intent(this, PollService.class);
		PendingIntent pintent = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pintent);
	}
	
	private void stopService() {
		Log.d(TAG, "PollService:stopService() called");
		cancelAlarm();
		restoreVolumes();
		shutdown();
	}
	
	private void readSettings() {
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		isActivated = preferences.getBoolean("isActivated", DefPrefs.isActivated);
		silenceFreeTime = preferences.getBoolean("silenceFreeTime",DefPrefs.silenceFreeTime);
		ringerType = preferences.getInt("ringerType", DefPrefs.ringerType);
		adjustMedia = preferences.getBoolean("adjustMedia", DefPrefs.adjustMedia);
		mediaVolume = preferences.getInt("mediaVolume", DefPrefs.mediaVolume);
		adjustAlarm = preferences.getBoolean("adjustAlarm", DefPrefs.adjustAlarm);
		alarmVolume = preferences.getInt("alarmVolume", DefPrefs.alarmVolume);
		quickSilenceMinutes = preferences.getInt("quickSilenceMinutes", DefPrefs.quickSilenceMinutes);
		refreshInterval = preferences.getInt("refreshInterval", DefPrefs.refreshInterval);
		
		Log.d(TAG, "PollService:readSettings() called");
	}
	
	private void restoreVolumes() {
		//TODO
		Log.d(TAG, "PollService:restoreVolumes() called");
	}
	
	private void shutdown() {
		//TODO
		Log.d(TAG, "PollService:shutdown() called");
		stopSelf();
	}

	

}