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
import android.os.Vibrator;
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
	private int bufferMinutes;
	private int lookaheadDays;
	
	private static final long TEN_SECONDS = 10000;
	private DatabaseInterface DBIface = DatabaseInterface.get(this);
	
	public PollService () {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		/* PollService looks for four different types of intents, one to start a
		 * quick silence request, one to stop an ongoing quick silence request,
		 * one to start the service (if not already started), and a normal request
		 * to check for active events.  The type of intent is determined by mapping
		 * the extra info "type" to one of "quickSilent", "cancelQuickSilence",
		 * "activityRestart".  Normal request either have no extra info, or are tagged
		 * "normal"
		 */
		
		Log.d(TAG, "Pollservice waking");
		readSettings();
		Context context = getApplicationContext();
		
		//pull extra info (if any) from the incoming intent
		String requestType = "";
		int duration = DefPrefs.REFRESH_INTERVAL;
		if (intent.getExtras() != null) {
			//for some reason intents that have no extras are still getting through this check
			String t = intent.getStringExtra("type");
			if (t != null) {
				requestType = t;
			}
			duration = intent.getIntExtra("duration", DefPrefs.REFRESH_INTERVAL);
		}
		
		if (requestType.equals("firstWake")) {
			//since the state is saved in a SharedPreferences file it can become out of sync
			//+ if the device restarts
			setServiceState("notActive");
			scheduleAlarmAt(System.currentTimeMillis(), "normal");
			
			//update(n) will also remove all events not found in the next n days, so we
			//+ need to keep this in sync with the users preferences.
			DBIface.update(lookaheadDays);
			DBIface.pruneEventsBefore(System.currentTimeMillis() - CalEvent.MILLISECONDS_IN_MINUTE * (long)bufferMinutes);
		} else if (requestType.equals("quickSilent")) {
			//This is a bit of a hack. The state of the service is stored in the shared preferences.
			//+ This will help the logic later on during the normal requests
			setServiceState("quickSilent");
			
			//when the quick silence duration is over the device should wake regardless
			//+ of the user settings
			long wakeAt = System.currentTimeMillis() + CalEvent.MILLISECONDS_IN_MINUTE * duration;
			scheduleAlarmAt(wakeAt, "cancel");
			
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
				sb.append(hrs + " hr ");
			}
			
			sb.append(min + " min. Touch to cancel");
			
			//FLAG_CANCEL_CURRENT is required to make sure that the extras are including in the new pending intent
			PendingIntent pendIntent = PendingIntent.getService(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(getApplicationContext())
				.setContentTitle("Tacere: Quick Silence")
				.setContentText(sb.toString())
				.setTicker("Quick Silence activating")
				.setSmallIcon(R.drawable.small_mono)
				.setAutoCancel(true)
				.setOngoing(true)
				.setContentIntent(pendIntent);

			NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			nm.cancel(DefPrefs.NOTIFICATION_ID);
			nm.notify(DefPrefs.NOTIFICATION_ID, notBuilder.build());
		} else if (requestType.equals("cancelQuickSilence")) {
			setServiceState("notActive");
			restoreVolumes();
			cancelNotification();			
			vibrate();
			//schedule an immediate normal wakeup
			scheduleAlarmAt(System.currentTimeMillis(), "normal");
		} else if (requestType.equals("activityRestart") || requestType.equals("providerChanged")) {
			//update(n) will also remove all events not found in the next n days, so we
			//+ need to keep this in sync with the users preferences.
			DBIface.update(lookaheadDays);
			DBIface.pruneEventsBefore(System.currentTimeMillis() - CalEvent.MILLISECONDS_IN_MINUTE * (long)bufferMinutes);
			String serviceState = getServiceState();
			//schedule an immediate wakeup only if we aren't in a quick silence period
			if (!serviceState.equals("quickSilent")) {
				scheduleAlarmAt(System.currentTimeMillis(), "normal");
			}
		} else  if (isActivated) {  //this is a normal request and the service is marked to be active
			DBIface.pruneEventsBefore(System.currentTimeMillis() - CalEvent.MILLISECONDS_IN_MINUTE * (long)bufferMinutes);
			CalEvent nextEvent = nextMatchingEvent();
			String serviceState = getServiceState();
			if (nextEvent != null) {
				//now that we know that there is an event in the next 24 hrs that matches
				//+ the users criteria we need to see if this event is currently active
				long now = System.currentTimeMillis();
				long begin = nextEvent.getBegin() - (CalEvent.MILLISECONDS_IN_MINUTE * (long)bufferMinutes);
				long end = nextEvent.getEnd() + (CalEvent.MILLISECONDS_IN_MINUTE * (long)bufferMinutes);
				if (begin <= now && end >= now) {
					//this event is active
					
					//a ringer type of 0 means we should use the default setting
					int ringType = nextEvent.getRingerType();
					if (ringType == 0) {
						ringType = ringerType;
					}
					silenceRinger(ringType);
					
					//clicking the notification should take the user to the app
					Intent notificationIntent = new Intent(context, MainActivity.class);
					notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
					notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	
					//FLAG_CANCEL_CURRENT is required to make sure that the extras are including in the new pending intent
					PendingIntent pendIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
					NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(getApplicationContext())
						.setContentTitle("Tacere: Event active")
						.setContentText(nextEvent.toString())
						.setSmallIcon(R.drawable.small_mono)
						.setAutoCancel(false)
						.setOnlyAlertOnce(true)
						.setOngoing(true)
						.setContentIntent(pendIntent);
					
					if (serviceState.equals("notActive")) {
						//only adjust the media and alarm volumes at the beginning of each event
						silenceVolumes();
						
						//we only want to vibrate at the beginning of a silent period, and only
						//+ if the ringer type is silent or vibrate
						if (ringType == CalEvent.RINGER_TYPE_SILENT || ringType == CalEvent.RINGER_TYPE_VIBRATE ) {
							vibrate();
						}
						
						//the ticker text should only be shown the first time the notification is created,
						//+ not on each update
						notBuilder.setTicker("Tacere Silencing for " + nextEvent.toString());
						
						setServiceState("eventActive");
					}
	
					NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
					nm.notify(DefPrefs.NOTIFICATION_ID, notBuilder.build());
					
					//sleep until this event ends.  The extra seconds are tacked on to make sure
					//+ that this event will be removed during the next database prune
					long wakeAt = nextEvent.getEnd() + CalEvent.MILLISECONDS_IN_MINUTE * (long)bufferMinutes + TEN_SECONDS;
					scheduleAlarmAt(wakeAt, "normal");
				} else {
					//there is at least one event in the next 24 hours but it isn't active now
					//cancel notification, restore volumes, schedule a new wake request
					if (serviceState.equals("eventActive")) {
						//vibrate to signal that the event has ended
						vibrate();
						setServiceState("notActive");			
						cancelNotification();
						restoreVolumes();
					}
					
					//sleep until the next event starts, shifted a few seconds to avoid collisions 
					long wakeAt = nextEvent.getBegin() - CalEvent.MILLISECONDS_IN_MINUTE * (long)bufferMinutes + TEN_SECONDS;
					scheduleAlarmAt(wakeAt, "normal");
				}
			} else {
				//there is no event in the next 24 hours
				if (serviceState.equals("eventActive")) {
					vibrate();
					setServiceState("notActive");
					cancelNotification();
					restoreVolumes();
				}
				
				//sleep for the next 24 hours (or until the calendar changes)
				long wakeAt = System.currentTimeMillis() + CalEvent.MILLISECONDS_IN_DAY;
				scheduleAlarmAt(wakeAt, "normal");
			}
		} else {
			//the service is marked not to start
			if (getServiceState().equals("eventActive")) {
				vibrate();
				setServiceState("notActive");
				cancelNotification();
				restoreVolumes();
			}
			silenceRinger(CalEvent.RINGER_TYPE_NORMAL);
			cancelAlarm();
			shutdown();
		}
	}

	private void cancelNotification() {
		NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(DefPrefs.NOTIFICATION_ID);
	}
	
	private void scheduleAlarmAt(long time, String type) {
		Intent i = new Intent(this, PollService.class);
		if (type.equals("cancel")) {
			i.putExtra("type", "cancelQuickSilent");
		}
		
		if (time < 0) {
			throw new IllegalArgumentException("PollService:scheduleAlarmAt not given valid time");
		}
		
		PendingIntent pintent = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarm.set(AlarmManager.RTC_WAKEUP, time, pintent);
	}
	
	private void vibrate() {
		Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		long[] pattern = {0, 500, 200, 500};
		vibrator.vibrate(pattern, -1);
	}
	
	
	//check all events in the database to see if they match the user's criteria
	//+ for silencing.  Returns the first event that matches.  Does not check
	//+ whether that event is active
	private CalEvent nextMatchingEvent() {
		CalEvent result = null;
		
		Cursor cursor = DBIface.getCursor(EventProvider.BEGIN);
		
		if (cursor.moveToFirst()) {
			do {
				int id = cursor.getInt(cursor.getColumnIndex(EventProvider._ID));
				CalEvent e = DBIface.getEvent(id);
				
				//if the event is marked as busy (but is not an all day event)
				//+ then we need no further tests
				boolean busy_notAllDay = false;
				if (!e.getIsFreeTime() && !e.getIsAllDay()) {
					busy_notAllDay = true;
				}
				
				//all day events
				boolean allDay = false;
				if (silenceAllDay && e.getIsAllDay()) {
					allDay = true;
				}
				
				//events marked as 'free' or 'available'
				boolean free_notAllDay = false;
				if (silenceFreeTime && e.getIsFreeTime() && !e.getIsAllDay()) {
					free_notAllDay = true;
				}
				
				if (busy_notAllDay || allDay || free_notAllDay) {
					result = e;
					break;
				}
			} while (cursor.moveToNext());
		}
		
		cursor.close();
		
		return result;
	}
	
	private void cancelAlarm() {
		Intent i = new Intent(this, PollService.class);
		PendingIntent pintent = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pintent);
	}
	
	private void readSettings() {
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		isActivated = preferences.getBoolean("isActivated", DefPrefs.IS_ACTIVATED);
		silenceFreeTime = preferences.getBoolean("silenceFreeTime",DefPrefs.SILENCE_FREE_TIME);
		ringerType = preferences.getInt("ringerType", DefPrefs.RINGER_TYPE);
		adjustMedia = preferences.getBoolean("adjustMedia", DefPrefs.ADJUST_MEDIA);
		mediaVolume = preferences.getInt("mediaVolume", DefPrefs.MEDIA_VOLUME);
		adjustAlarm = preferences.getBoolean("adjustAlarm", DefPrefs.ADJUST_ALARM);
		alarmVolume = preferences.getInt("alarmVolume", DefPrefs.ALARM_VOLUME);
		silenceAllDay = preferences.getBoolean("silenceAllDay", DefPrefs.SILENCE_ALL_DAY);
		bufferMinutes = preferences.getInt("bufferMinutes", DefPrefs.BUFFER_MINUTES);
		lookaheadDays = preferences.getInt("lookaheadDays", DefPrefs.LOOKAHEAD_DAYS);
	}
	
	private void restoreVolumes() {
		AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		
		if (adjustMedia) {
			audio.setStreamVolume(AudioManager.STREAM_MUSIC, DefPrefs.MEDIA_VOLUME, 0);
		}

		if (adjustAlarm) {
			audio.setStreamVolume(AudioManager.STREAM_ALARM, DefPrefs.ALARM_VOLUME, 0);
		}
	}
	
	private void silenceRinger(int ringType) {
		AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		switch (ringType) {
			case CalEvent.RINGER_TYPE_NORMAL:
				audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
				break;
			case CalEvent.RINGER_TYPE_VIBRATE:
				audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
				break;
			case CalEvent.RINGER_TYPE_SILENT:
				audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
				break;
		}
	}
	
	private void silenceVolumes() {
		//change media volume, and alarm volume
		AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		if (adjustMedia) {
			audio.setStreamVolume(AudioManager.STREAM_MUSIC, mediaVolume, 0);
		}

		if (adjustAlarm) {
			audio.setStreamVolume(AudioManager.STREAM_ALARM, alarmVolume, 0);
		}

	}
	
	private void setServiceState(String state) {
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		if (state != null) {
			editor.putString("serviceState", state);
		} else {
			Log.e(TAG, "Trying to set state to null value");
		}
		editor.commit();
	}
	
	private String getServiceState() {
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		return preferences.getString("serviceState", "notActive");
	}
	
	
	private void shutdown() {
		stopSelf();
	}

	

}