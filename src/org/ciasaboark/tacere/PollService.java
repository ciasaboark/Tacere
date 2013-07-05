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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.CalendarContract.Instances;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PollService extends IntentService {
	private static final String TAG = "PollService";
	
	//Values that should be read from the settings
	private boolean isActivated;
	private boolean silenceFreeTime;
	private boolean silenceAllDay;
	private int ringerType;
	private boolean adjustMedia;
	private int mediaVolume;
	private boolean adjustAlarm;
	private int alarmVolume;
	private int refreshInterval;
	private int bufferMinutes;
	private boolean wakeDevice;
	private boolean ongoing;
	
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
			
			SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("ongoing", false);
			editor.commit();
			
			//when the quick silence duration is over the device should wake
			scheduleAlarm(duration, true, "cancel");
			
			AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
			
			vibrate();
			
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
			NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(getApplicationContext())
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
			
			vibrate();
			
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
		} else  if (isActivated) {  //this is a normal request and the service is marked to be active
			CalEvent event = activeEvent();
			if (!event.isBlank()) {
				//an event matched
				silenceVolumes();
				
				//clicking the notification should take the user to the app
				Intent notificationIntent = new Intent(context, MainActivity.class);
				notificationIntent.putExtra("type", "normal");

				//FLAG_CANCEL_CURRENT is required to make sure that the extras are including in the new pending intent
				PendingIntent pendIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
				NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(getApplicationContext())
					.setContentTitle("Tacere: Event active")
					.setContentText(event.toString())
					.setSmallIcon(R.drawable.small_mono)
					.setAutoCancel(false)
					.setOnlyAlertOnce(true)
					.setOngoing(true)
					.setContentIntent(pendIntent);
				
				if (!ongoing) {
					//we only want to vibrate at the beginning of a silent period
					vibrate();
					
					//the ticker text should only be shown the first time the notification is created,
					//+ not on each update
					notBuilder.setTicker("Tacere Silencing for " + event.toString());
					
					SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = preferences.edit();
					editor.putBoolean("ongoing", true);
					editor.commit();
				}

				NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
				//nm.cancel(DefPrefs.QUICK_N_ID);
				nm.notify(DefPrefs.QUICK_N_ID, notBuilder.build());
				
				scheduleAlarm(refreshInterval, wakeDevice, "normal");
			} else { //there was no active event
				//cancel notification, restore volumes, schedule a new wake request
				if (ongoing) {
					//vibrate to signal that the event has ended
					vibrate();
				}
				
				SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putBoolean("ongoing", false);
				editor.commit();
				
				cancelNotification();
				restoreVolumes();
				scheduleAlarm(refreshInterval, wakeDevice, "normal");

			}
		} else {	//this was a normal request but the service is not marked to be active
			//restore all volumes, clear any ongoing notifications then shutdown the service
			if (ongoing) {
				//vibrate to signal that the event has ended
				vibrate();
			}
			
			SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("ongoing", false);
			editor.commit();
			
			restoreVolumes();
			cancelNotification();
			
			stopService();
		}
	}

	private void cancelNotification() {
		NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(DefPrefs.QUICK_N_ID);
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
	
	private void vibrate() {
		Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		long[] pattern = {0, 500, 200, 500};
		vibrator.vibrate(pattern, -1);
	}
	
	private CalEvent activeEvent() {
		Log.d(TAG, "activeEvent() called");
		CalEvent result = new CalEvent();

		long begin = System.currentTimeMillis() - 1000 * 60 * bufferMinutes;
		long end = System.currentTimeMillis() + 1000 * 60 * bufferMinutes;
    
    	String[] projection = new String[]{"title", "begin", "end", "allDay", "availability", "duration"};
    	Cursor cursor = Instances.query(getContentResolver(), projection, begin, end);
    	
    	if (cursor.moveToFirst()) {
    		int eventNum = 0;
    		
    		int col_title = cursor.getColumnIndex(projection[0]);
    		int col_begin = cursor.getColumnIndex(projection[1]);
    		int col_end = cursor.getColumnIndex(projection[2]);
    		int col_allDay = cursor.getColumnIndex(projection[3]);
    		int col_availability = cursor.getColumnIndex(projection[4]);
    		int col_duration = cursor.getColumnIndex(projection[5]);
    		
    		do {
    			String event_title = cursor.getString(col_title);
    			String event_begin = cursor.getString(col_begin);
    			String event_end = cursor.getString(col_end);
    			String event_isAllDay = cursor.getString(col_allDay);
    			String event_availability = cursor.getString(col_availability);
    			String event_duration = cursor.getString(col_duration);
    			
    			Log.d(TAG, "Event:" + eventNum + ", title: '" + event_title + "', begin:" +
    						event_begin + ", end:" + event_end + ", allDay:" + event_isAllDay +
    						", availability:" + event_availability + ", duration:" + event_duration);
    			
    			//if the event is marked as busy (but is not an all day event)
    			//+ then we need no further tests
    			boolean busy_notAllDay = false;
    			if (event_availability.equals("0") && event_isAllDay.equals("0")) {
    				busy_notAllDay = true;
    			}
    			
    			//all day events
    			boolean allDay = false;
    			if (silenceAllDay && event_isAllDay.equals("1")) {
    				allDay = true;
    			}
    			
    			//events marked as 'free' or 'available'
    			boolean free_notAllDay = false;
    			if (silenceFreeTime && event_availability.equals("1") && event_isAllDay.equals("0")) {
    				free_notAllDay = true;
    			}
    			
    			if (busy_notAllDay || allDay || free_notAllDay) {
    				result.setTitle(event_title);
    				result.setBegin(Long.valueOf(event_begin));
    				
    				//recurring events can sometimes have null values for the end time
    				//+ We might be able to salvage a proper end time from the duration info
    				if (event_end == null) {
    					//duration is stored as RFC2445 format, this block only matches against
    					//+ values such as 'PXXXXS' where XXXX is the seconds of the duration
    					if (event_duration.startsWith("P") && event_duration.endsWith("S")) {
    						String dur_seconds = event_duration.substring(1, event_duration.length() - 1);
    						long endTime = Long.valueOf(event_begin) + 1000 * 60 * Long.valueOf(dur_seconds);
    						result.setEnd(endTime);
    					} else {
    						Log.e(TAG, "could not generate a proper end time for event " + event_title + " setting end time to 5 minutes from now");
    						result.setEnd(Long.valueOf(event_begin) + 1000 * 60 * 5);
    					}
    				} else {
    					result.setEnd(Long.valueOf(event_end));
    				}
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
		ongoing = preferences.getBoolean("ongoing", false);
		
		Log.d(TAG, "PollService:readSettings() called");
	}
	
	private void restoreVolumes() {
		Log.d(TAG, "PollService:restoreVolumes() called");
		AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		
		if (adjustMedia) {
			audio.setStreamVolume(AudioManager.STREAM_MUSIC, DefPrefs.mediaVolume, 0);
		}

		if (adjustAlarm) {
			audio.setStreamVolume(AudioManager.STREAM_ALARM, DefPrefs.alarmVolume, 0);
		}
	}
	
	private void silenceVolumes() {
		//change ringer state, media volume, and alarm volume
		Log.d(TAG, "silenceVolumes() called");
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

	}
	private void shutdown() {
		Log.d(TAG, "PollService:shutdown() called");
		stopSelf();
	}

	

}