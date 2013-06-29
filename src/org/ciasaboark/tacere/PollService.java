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
import android.media.AudioManager;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

public class PollService extends IntentService {
	private static final String TAG = "PollService";
	
	//Values that should be read from the settings
	//TODO find out a good way to handle default values here
	private boolean isActivated;
	private boolean silenceFreeTime;
	private int ringerType;
	private boolean adjustMedia;
	private int mediaVolume;
	private boolean adjustAlarm;
	private int alarmVolume;
	private int quickSilenceMinutes;
	private int quickSilenceHours;
	private int refreshInterval;
	
	private Handler handler = new Handler();
	
	public PollService () {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "Received an intent: " + intent);
		readSettings();
		
		String requestType = "";
		int duration = DefPrefs.refreshInterval * 60 * 1000;
		if (intent.getExtras() != null) {
			//for some reason intents that should have no extras are still getting through this check
			String t = intent.getStringExtra("type");
			if (t != null) {
				requestType = t;
			}
			duration = intent.getIntExtra("duration", DefPrefs.refreshInterval * 60 * 1000);
		}
		
		//is this a quick silence request?
		if (requestType.equals("quicksilent")) {
			Log.d(TAG, "service was sent a quick silence request for " + duration + " ms");
			scheduleAlarm(duration, 1);
		} else {
			//this is a normal wake up
			//clear any notifications
			NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			nm.cancel(DefPrefs.QUICK_N_ID);
			if (isActivated) {
				//TODO expand this with a real check
				boolean eventActive = false;
				if (!eventActive) {
					//restore the volumes then sleep
					Log.d(TAG, "No event was active, restoring volumes");
					restoreVolumes();
				}
				Log.d(TAG, "Service is marked to start, registering with AlarmManager");
				scheduleAlarm((int)(refreshInterval * DateUtils.MINUTE_IN_MILLIS), 0);		//there should be no chance of overflow here
				//do stuff here
			} else {
				//the service should not run
				Log.d(TAG, "Service is not supposed to start, shutting down");
				stopService();
			}
		}
	}
	
	//type is one of 0: RTC, 1: RTC_WAKEUP
	private void scheduleAlarm(final int duration, int type) {
		Intent i = new Intent(this, PollService.class);
		PendingIntent pintent = PendingIntent.getService(this, 0, i, 0);

		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		
		//Using the RTC clock _should_ keep the service from waking the device every time a
		//+ pending intent should fire. The pending intents should not trigger until some other
		//+ process wakes the device.  RTC_WAKEUP is needed to restore from the quick silence
		//+ feature.
		if (type == 1) {
			alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + duration, pintent);
		} else {
			alarm.set(AlarmManager.RTC, System.currentTimeMillis() + duration, pintent);
		}
		
		//Creating a toast from within an IntentService can leave toast visible indefinitely.  Posting the
		//+ toast from a new thread solves this problem.
		handler.post(new Runnable() {
						
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(),"PollService scheduling for restart in " + duration + " ms", Toast.LENGTH_SHORT).show();
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
		quickSilenceHours = preferences.getInt("quickSilenceHours", DefPrefs.quickSilenceHours);
		refreshInterval = preferences.getInt("refreshInterval", DefPrefs.refreshInterval);
		
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
	
	private void shutdown() {
		//TODO
		Log.d(TAG, "PollService:shutdown() called");
		stopSelf();
	}

	

}