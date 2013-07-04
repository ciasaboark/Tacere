/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere;


import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.util.Log;

public class PollService extends IntentService {
	private static final String TAG = "PollService";
	
	//Values that should be read from the settings
	//TODO find out a good way to handle default values here
	private boolean isActivated;
	private boolean silenceFreeTime;
	private boolean silenceAllDay;
	private int ringerType;
	private boolean adjustMedia;
	private int mediaVolume;
	private boolean adjustAlarm;
	private int alarmVolume;
	private int quickSilenceMinutes;
	private int quickSilenceHours;
	private int refreshInterval;
	private int bufferMinutes;
	private boolean wakeDevice;
	
	private Handler handler = new Handler();
	
	public PollService () {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		/*
		 * -read settings
		 * -pull extra info from incoming intent
		 * if quick silent
		 * 		schedule a new alarm
		 * 		update/create notification
		 * 		silence ringer
		 * 		schedule a quick silent wakeup request for the duration
		 * else if this is a quick silent cancel request
		 * 		change the ringer type to the user preference
		 * 		issue an immediate wakeup request
		 * 		remove the notification
		 * else if this is an activity wakeup request
		 * 		if there is no pending intent
		 * 			schedue a new immediate wakeup request
		 * else
		 * 		remove the notification
		 * 		check for a current event
		 * 		if an event is current
		 * 			change phone ringer to user pref
		 * 			schedule a wakeup request for user pref duration
		 * 		else
		 * 			restore ringer volume
		 * 			schedule a wakeup request for the pref duration
		 */
		
		Log.i(TAG, "Received an intent: " + intent);
		readSettings();
		Context context = getApplicationContext();
		
		//pull extra info (if any) from the incoming intent
		String requestType = "";
		int duration = DefPrefs.refreshInterval;
		if (intent.getExtras() != null) {
			//for some reason intents that have no extras are still getting through this check
			String t = intent.getStringExtra("type");
			if (t != null) {
				requestType = t;
			}
			duration = intent.getIntExtra("duration", DefPrefs.refreshInterval);
		}
		
		if (requestType.equals("quickSilent")) {
			Log.d(TAG, "quickSilent intent received");
			//when the quick silence duration is over the device should wake
			scheduleAlarm(duration, true, "cancel");
			
			AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
			
			Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
			long[] pattern = {0, 500, 200, 500};
			vibrator.vibrate(pattern, -1);
			
			//an intent to attach to the notification
			Intent notificationIntent = new Intent(context, PollService.class);
			notificationIntent.putExtra("type", "cancelQuickSilence");
			
			int hrs = duration / 60;
			int min = duration % 60;
			StringBuilder sb = new StringBuilder("Silencing for ");
			if (hrs > 0) {
				sb.append(hrs + " hours, ");
			}
			
			sb.append(min + " minutes. Touch to cancel");
			
			//FLAG_CANCEL_CURRENT is required to make sure that the extras are including in the new pending intent
			PendingIntent pendIntent = PendingIntent.getService(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			Notification.Builder notBuilder = new Notification.Builder(getApplicationContext())
				.setContentTitle("Quick Silence")
				.setContentText(sb.toString())
				.setTicker("Quick Silence activating")
				.setSmallIcon(R.drawable.small_mono)
				.setAutoCancel(true)
				.setContentIntent(pendIntent);

			NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			nm.cancel(DefPrefs.QUICK_N_ID);
			nm.notify(DefPrefs.QUICK_N_ID, notBuilder.build());
		} else if (requestType.equals("cancelQuickSilence")) {
			Log.d(TAG, "cancelQuickSilence intent received");
			AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			switch (ringerType) {
				case 1:
					audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
					break;
				case 2:
					audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
					break;
				case 3:
					audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
					break;
			}
			
			NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			nm.cancel(DefPrefs.QUICK_N_ID);
			
			//schedule an immediate normal wakeup
			scheduleAlarm(0, true, "normal");
		} else if (requestType.equals("activityRestart")) {
			Log.d(TAG, "activityRestart intent received");
			Intent i = new Intent(context, PollService.class);
			PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
			if (pi == null) {
				Log.d(TAG, "no pending intent was found, scheduling a wakeup");
				scheduleAlarm(0, true, "normal");
			}
		} else {
			//normal requests should only run if the service is marked as active
			if (isActivated) {
				String eventTitle = activeEvent();
				if (eventTitle != null) {
					//an event matched
					AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
					switch (ringerType) {
						case 1:
							audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
							break;
						case 2:
							audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
							break;
						case 3:
							audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
							break;
					}
					if (adjustMedia) {
						audio.setStreamVolume(AudioManager.STREAM_MUSIC, mediaVolume, 0);
					}
					if (adjustAlarm) {
						audio.setStreamVolume(AudioManager.STREAM_ALARM, alarmVolume, 0);
					}
					
					//display a notification
					Intent notificationIntent = new Intent(context, PollService.class);
					notificationIntent.putExtra("type", "normal");
					
					//FLAG_CANCEL_CURRENT is required to make sure that the extras are including in the new pending intent
					PendingIntent pendIntent = PendingIntent.getService(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
					Notification.Builder notBuilder = new Notification.Builder(getApplicationContext())
						.setContentTitle("Tacere: Event active")
						.setContentText("Silencing for " + eventTitle)
						.setTicker("Tacere Silencing")
						.setSmallIcon(R.drawable.small_mono)
						.setAutoCancel(false)
						.setOnlyAlertOnce(true)
						.setOngoing(true)
						.setContentIntent(pendIntent);
	
					NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
					nm.cancel(DefPrefs.QUICK_N_ID);
					nm.notify(DefPrefs.QUICK_N_ID, notBuilder.build());
					
					scheduleAlarm(refreshInterval, wakeDevice, "normal");
				}
			} else {
				//the phone should not be silenced
				
				//restore all volumes
				AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
				audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
				
				if (adjustMedia) {
					audio.setStreamVolume(AudioManager.STREAM_MUSIC, DefPrefs.mediaVolume, 0);
				}
				if (adjustAlarm) {
					audio.setStreamVolume(AudioManager.STREAM_ALARM, DefPrefs.alarmVolume, 0);
				}
				
				//cancel any active notifications
				NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
				nm.cancel(DefPrefs.QUICK_N_ID);
				
				scheduleAlarm(refreshInterval, wakeDevice, "normal");
			}
		}
	}
	
	private void scheduleAlarm(final int duration, boolean wakeOnAlarm, String type) {
		Intent i = new Intent(this, PollService.class);
		if (type.equals("cancel")) {
			i.putExtra("type", "cancelQuickSilent");
		}
		
		PendingIntent pintent = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		
		//Using the RTC clock _should_ keep the service from waking the device every time a
		//+ pending intent should fire. The pending intents should not trigger until some other
		//+ process wakes the device.  RTC_WAKEUP is needed to restore from the quick silence
		//+ feature.
		if (wakeOnAlarm) {
			alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 60 * duration, pintent);
		} else {
			alarm.set(AlarmManager.RTC, System.currentTimeMillis() + 1000 * 60 * duration, pintent);
		}
		
		//Creating a toast from within an IntentService can leave toast visible indefinitely.  Posting the
		//+ toast from a new thread solves this problem.
		handler.post(new Runnable() {
						
						@Override
						public void run() {
							//Toast.makeText(getApplicationContext(),"PollService scheduling for restart in " + duration + " min", Toast.LENGTH_SHORT).show();
						}
					});
	}
	
	private String activeEvent() {
		Log.d(TAG, "eventActive() called");
		String result = null;

		long begin = System.currentTimeMillis() - 1000 *60 * bufferMinutes;
		long end = System.currentTimeMillis() + 1000 * 60 * bufferMinutes;
    
    	String[] projection = new String[]{"title", "dtstart", "dtend", "allDay", "availability"};
    	Cursor cursor = Instances.query(getContentResolver(), projection, begin, end);
    	
    	if (cursor.moveToFirst()) {
    		int eventNum = 0;
    		
    		int col_title = cursor.getColumnIndex(projection[0]);
    		int col_begin = cursor.getColumnIndex(projection[1]);
    		int col_end = cursor.getColumnIndex(projection[2]);
    		int col_allDay = cursor.getColumnIndex(projection[3]);
    		int col_availability = cursor.getColumnIndex(projection[4]);
    		
    		do {
    			String event_title = cursor.getString(col_title);
    			String event_begin = cursor.getString(col_begin);
    			String event_end = cursor.getString(col_end);
    			String event_isAllDay = cursor.getString(col_allDay);
    			String event_availability = cursor.getString(col_availability);
    			
    			Log.d(TAG, "Event:" + eventNum + ", title: '" + event_title + "', begin:" +
    						event_begin + ", end:" + event_end + ", allDay:" + event_isAllDay +
    						", availability:" + event_availability);
    			
    			//if the event is marked as busy (but is not an all day event)
    			//+ then we need no further tests
    			if (event_availability.equals("0") && event_isAllDay.equals("0")) {
    				result = event_title;
    				break;
    			}
    			
    			//all day events
    			if (	silenceAllDay && event_isAllDay.equals("1")) {
    				result = event_title;
    				break;
    			}
    			
    			//events marked as 'free' or 'available'
    			if (silenceFreeTime && event_availability.equals("1")) {
    				result = event_title;
    				break;
    			}

    			eventNum++;
    		} while (cursor.moveToNext());
    	}
	
		return result;
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
		refreshInterval = preferences.getInt("refreshInterval", DefPrefs.refreshInterval);
		silenceAllDay = preferences.getBoolean("silenceAllDay", DefPrefs.silenceAllDay);
		refreshInterval = preferences.getInt("refreshInterval", DefPrefs.refreshInterval);
		bufferMinutes = preferences.getInt("bufferMinutes", DefPrefs.bufferMinutes);
		wakeDevice = preferences.getBoolean("wakeDevice", DefPrefs.wakeDevice);
		
		Log.d(TAG, "PollService:readSettings() called");
	}
	
	private void restoreVolumes() {
		//TODO
		Log.d(TAG, "PollService:restoreVolumes() called");
		AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		audio.setStreamVolume(AudioManager.STREAM_ALARM, DefPrefs.alarmVolume, 0);
		audio.setStreamVolume(AudioManager.STREAM_MUSIC, DefPrefs.mediaVolume, 0);
	}
	
	private void silenceVolumes() {
		//TODO change ringer state, media volume, and alarm volume
		Log.d(TAG, "silenceVolumes() called");
	}
	private void shutdown() {
		//TODO
		Log.d(TAG, "PollService:shutdown() called");
		stopSelf();
	}

	

}