/*
 * Created by Jonathan Nelson
 *
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license. For details see the COPYING file.
 */


package org.ciasaboark.tacere.manager;

import org.ciasaboark.tacere.activity.CalEvent;
import org.ciasaboark.tacere.prefs.Prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;

public class RingerStateManager {
	private Context context;
	private Prefs prefs;
	
	public RingerStateManager(Context ctx) {
		this.context = ctx;
		this.prefs = new Prefs(ctx);
	}

	/**
	 * Store the current ringer state (vibrate, silent, or normal). Ringer state is stored into
	 * shared preferences
	 */
	public void storeRingerState() {
		SharedPreferences preferences = context.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		// TODO this may return a state indicating that a call is ongoing, this should stop
		// processing and wait for the call to end before adjusting volumes
		int curRinger = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE))
				.getRingerMode();
		preferences.edit().putInt("curRinger", curRinger).commit();
	}

	/**
	 * Remove stored ringer state from preferences
	 */
	public void clearStoredRingerState() {
		SharedPreferences preferences = context.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		preferences.edit().remove("curRinger").commit();
	}

	/**
	 * Retrieves the current stored phone ringer from preferences
	 * 
	 * @return the stored ringer state. Returned value can be compared to CalEvent.RINGER types
	 */
	public int getStoredRingerState() {
		SharedPreferences preferences = context.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		// TODO this may return a state indicating that a call is ongoing, this should stop
		// processing and wait for the call to end before adjusting volumes
		return preferences.getInt("curRinger", CalEvent.RINGER.UNDEFINED);
	}
	
	public void setPhoneRinger(int ringerType) {
		AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

		switch (ringerType) {
			case CalEvent.RINGER.NORMAL:
				audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
				break;
			case CalEvent.RINGER.VIBRATE:
				audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
				break;
			case CalEvent.RINGER.SILENT:
				audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
				break;
			case CalEvent.RINGER.IGNORE:
				audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
				break;
		}
	}
}
