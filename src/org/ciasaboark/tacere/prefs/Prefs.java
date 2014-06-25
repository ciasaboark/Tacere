/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 * 
 * Released under the BSD license. For details see the COPYING file.
 */

package org.ciasaboark.tacere.prefs;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Holds default preferences used to control the service and to populate the preferences activities.
 * 
 * @author Jonathan Nelson <ciasaboark@gmail.com>
 * 
 */
public class Prefs {
	private static final String TAG = "Prefs";
	private Context ctx;
	
	private Boolean isServiceActivated;
	private Boolean silenceFreeTimeEvents;
	private Boolean silenceAllDayEvents;
	private int ringerType;
	private Boolean adjustMedia;
	private Boolean adjustAlarm;
	
	private int curMediaVolume;
	private int curAlarmVolume;
	private int quicksilenceMinutes;
	private int quickSilenceHours;
	private int refreshInterval;
	private int bufferMinutes;
	
	private int lookaheadDays;
	private boolean updatesCheckbox;
	private boolean showDonationThanks;

	
	private SharedPreferences sharedPreferences = ctx.getSharedPreferences(
			"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);


	public Prefs(Context ctx) {
		this.ctx = ctx;
		readPreferences();
	}

	public void readPreferences() {
		isServiceActivated = sharedPreferences.getBoolean(Keys.IS_SERVICE_ACTIVATED,
				DefaultPrefs.IS_ACTIVATED);
		silenceFreeTimeEvents = sharedPreferences.getBoolean(Keys.SILENCE_FREE_TIME,
				DefaultPrefs.SILENCE_FREE_TIME);
		silenceAllDayEvents = sharedPreferences
				.getBoolean(Keys.SILENCE_ALL_DAY, DefaultPrefs.SILENCE_ALL_DAY);
		ringerType = sharedPreferences.getInt(Keys.RINGER_TYPE, DefaultPrefs.RINGER_TYPE);
		adjustMedia = sharedPreferences.getBoolean(Keys.ADJUST_MEDIA, DefaultPrefs.ADJUST_MEDIA);
		adjustAlarm = sharedPreferences.getBoolean(Keys.ADJUST_ALARM, DefaultPrefs.ADJUST_ALARM);
		curMediaVolume = sharedPreferences.getInt(Keys.MEDIA_VOLUME, DefaultPrefs.MEDIA_VOLUME);
		curAlarmVolume = sharedPreferences.getInt(Keys.ALARM_VOLUME, DefaultPrefs.ALARM_VOLUME);
		quicksilenceMinutes = sharedPreferences.getInt(Keys.QUICKSILENCE_MINUTES,
				DefaultPrefs.QUICK_SILENCE_MINUTES);
		quickSilenceHours = sharedPreferences.getInt(Keys.QUICKSILENCE_HOURS,
				DefaultPrefs.QUICK_SILENCE_HOURS);
		bufferMinutes = sharedPreferences.getInt(Keys.BUFFER_MINUTES, DefaultPrefs.BUFFER_MINUTES);
		lookaheadDays = sharedPreferences.getInt(Keys.LOOKAHEAD_DAYS, DefaultPrefs.LOOKAHEAD_DAYS);
		updatesCheckbox = sharedPreferences.getBoolean(Keys.UPDATES_CHECKBOX, DefaultPrefs.UPDATES_CHECKBOX);
		showDonationThanks = sharedPreferences.getBoolean(Keys.SHOW_DONATION_THANKS,
				DefaultPrefs.SHOW_DONATION_THANKS);
	}

	private void storePreferences() {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		
		editor.putBoolean(Keys.IS_SERVICE_ACTIVATED, isServiceActivated);
		editor.putBoolean(Keys.SILENCE_FREE_TIME, silenceFreeTimeEvents);
		editor.putBoolean(Keys.SILENCE_ALL_DAY, silenceAllDayEvents);
		editor.putInt(Keys.RINGER_TYPE, ringerType);
		editor.putBoolean(Keys.ADJUST_MEDIA, adjustMedia);
		editor.putBoolean(Keys.ADJUST_ALARM, adjustAlarm);
		editor.putInt(Keys.MEDIA_VOLUME, curMediaVolume);
		editor.putInt(Keys.ALARM_VOLUME, curAlarmVolume);
		editor.putInt(Keys.QUICKSILENCE_MINUTES, quicksilenceMinutes);
		editor.putInt(Keys.QUICKSILENCE_HOURS, quickSilenceHours);
		editor.putInt(Keys.BUFFER_MINUTES, bufferMinutes);
		editor.putInt(Keys.LOOKAHEAD_DAYS, lookaheadDays);
		editor.putBoolean(Keys.UPDATES_CHECKBOX, updatesCheckbox);
		editor.putBoolean(Keys.SHOW_DONATION_THANKS, showDonationThanks);
		
		editor.commit();
	}
	
	

	public Boolean getIsServiceActivated() {
		return isServiceActivated;
	}

	public void setIsServiceActivated(Boolean isServiceActivated) {
		this.isServiceActivated = isServiceActivated;
		storePreferences();
	}

	public Boolean getSilenceFreeTimeEvents() {
		return silenceFreeTimeEvents;
	}

	public void setSilenceFreeTimeEvents(Boolean silenceFreeTimeEvents) {
		this.silenceFreeTimeEvents = silenceFreeTimeEvents;
		storePreferences();
		
	}

	public Boolean getSilenceAllDayEvents() {
		return silenceAllDayEvents;
	}

	public void setSilenceAllDayEvents(Boolean silenceAllDayEvents) {
		this.silenceAllDayEvents = silenceAllDayEvents;
		storePreferences();
	}

	public int getRingerType() {
		return ringerType;
	}

	public void setRingerType(int ringerType) {
		this.ringerType = ringerType;
		storePreferences();
	}

	public Boolean getAdjustMedia() {
		return adjustMedia;
	}

	public void setAdjustMedia(Boolean adjustMedia) {
		this.adjustMedia = adjustMedia;
		storePreferences();
	}

	public Boolean getAdjustAlarm() {
		return adjustAlarm;
	}

	public void setAdjustAlarm(Boolean adjustAlarm) {
		this.adjustAlarm = adjustAlarm;
		storePreferences();
	}

	public int getCurMediaVolume() {
		return curMediaVolume;
	}

	public void setCurMediaVolume(int curMediaVolume) {
		this.curMediaVolume = curMediaVolume;
		storePreferences();
	}

	public int getCurAlarmVolume() {
		return curAlarmVolume;
	}

	public void setCurAlarmVolume(int curAlarmVolume) {
		this.curAlarmVolume = curAlarmVolume;
		storePreferences();
	}

	public int getQuicksilenceMinutes() {
		return quicksilenceMinutes;
	}

	public void setQuicksilenceMinutes(int quicksilenceMinutes) {
		this.quicksilenceMinutes = quicksilenceMinutes;
		storePreferences();
	}

	public int getQuickSilenceHours() {
		return quickSilenceHours;
	}

	public void setQuickSilenceHours(int quickSilenceHours) {
		this.quickSilenceHours = quickSilenceHours;
		storePreferences();
	}

	public int getRefreshInterval() {
		return refreshInterval;
	}

	public void setRefreshInterval(int refreshInterval) {
		this.refreshInterval = refreshInterval;
		storePreferences();
	}

	public int getBufferMinutes() {
		return bufferMinutes;
	}

	public void setBufferMinutes(int bufferMinutes) {
		this.bufferMinutes = bufferMinutes;
		storePreferences();
	}

	public int getLookaheadDays() {
		return lookaheadDays;
	}

	public void setLookaheadDays(int lookaheadDays) {
		this.lookaheadDays = lookaheadDays;
		storePreferences();
	}

	public boolean isUpdatesCheckboxChecked() {
		return updatesCheckbox;
	}

	public void setUpdatesCheckbox(boolean updatesCheckbox) {
		this.updatesCheckbox = updatesCheckbox;
		storePreferences();
	}

	public boolean isShowDonationThanks() {
		return showDonationThanks;
	}

	public void setShowDonationThanks(boolean showDonationThanks) {
		this.showDonationThanks = showDonationThanks;
		storePreferences();
	}

	public boolean changelogShouldBeShown() {
		return showUpdatesForVersion(Keys.CURRENT_RELEASE_NAME);
	}
	
	/**
	 * Check stored preferences to see if the updates changelog has been shown for the given release
	 * version.
	 * 
	 * @param versionName
	 *            the name of the release version to check. The current release version can be found
	 *            as {@link Keys#CURRENT_RELEASE_NAME}
	 * @return true if the release notes for the given version have not been displayed, or if the
	 *         user has opted to show the updates again, false otherwise
	 */
	public boolean showUpdatesForVersion(String versionName) {
		return sharedPreferences.getBoolean(Keys.CURRENT_RELEASE_NAME, true);
	}
	
	public <V> void storePreferences(String key, V value) {
		SharedPreferences.Editor sharedPrefEdit = sharedPreferences.edit();
		if (value instanceof String) {
			sharedPrefEdit.putString(key.toString(), (String)value);
		} else if (value instanceof Integer) {
			sharedPrefEdit.putInt(key, (Integer)value);
		} else if (value instanceof Long) {
			sharedPrefEdit.putLong(key, (Long)value);
		} else if (value instanceof Boolean) {
			sharedPrefEdit.putBoolean(key, (Boolean)value);
		} else {
			throw new IllegalArgumentException(TAG + " unable to store preference with unsupported type: " + value.getClass().getName());
		}
		
		sharedPrefEdit.commit();
	}
	
	public String readPreferenceString(String key) {
		String value = sharedPreferences.getString(key, "nosuchkey");
		if (value.equals("nosuchkey")) {
			throw new IllegalArgumentException("no such key");
		}
		return value;
	}

	public int getDefaultRinger() {
		return DefaultPrefs.RINGER_TYPE;
	}

	public void restoreDefaultPreferences() {
		// TODO Auto-generated method stub
		
	}

	public int getDefaultMediaVolume() {
		return DefaultPrefs.MEDIA_VOLUME;
	}
	
	public int getDefaultAlarmVolume() {
		return DefaultPrefs.ALARM_VOLUME;
	}

	/**
	 * 
	 * @author Jonathan Nelson <ciasaboark@gmail.com>
	 *
	 */
	private static class Keys {
		//@formatter:off
		public static final String IS_SERVICE_ACTIVATED	= "IS_ACTIVATED";
		public static final String SILENCE_FREE_TIME	= "SILENCE_FREE_TIME";
		public static final String SILENCE_ALL_DAY	 	= "SILENCE_ALL_DAY";
		public static final String RINGER_TYPE 			= "RINGER_TYPE";
		public static final String ADJUST_MEDIA  		= "ADJUST_MEDIA";
		public static final String ADJUST_ALARM 		= "ADJUST_ALARM";
		public static final String MEDIA_VOLUME			= "MEDIA_VOLUME";
		public static final String ALARM_VOLUME 		= "ALARM_VOLUME";
		public static final String QUICKSILENCE_MINUTES = "QUICK_SILENCE_MINUTES";
		public static final String QUICKSILENCE_HOURS 	= "QUICK_SILENCE_HOURS";
//		public static final String REFRESH_INTERVAL 	= "REFRESH_INTERVAL";
		public static final String BUFFER_MINUTES		= "BUFFER_MINUTES";
		public static final String NOTIFICATION_ID		= "NOTIFICATION_ID";
		public static final String LOOKAHEAD_DAYS 		= "LOOKAHEAD_DAYS";
		public static final String UPDATES_CHECKBOX 	= "UPDATES_CHECKBOX";
		public static final String CURRENT_RELEASE_NAME = "2.0.4";
		public static final String SHOW_DONATION_THANKS = "SHOW_DONATION_THANKS";
		public static final String RC_EVENT 			= "RC_EVENT";
		public static final String RC_QUICKSILENT 		= "RC_QUICKSILENT";
		public static final String RC_NOTIFICATION 		= "RC_NOTIFICATION";
		//@formatter:on
	}
}
