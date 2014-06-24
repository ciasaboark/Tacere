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
	private Context ctx;
	
	private Boolean isServiceActivated;
	private Boolean silenceFreeTimeEvents;
	private Boolean silenceAllDayEvents;
	private int ringerType;
	private Boolean adjustMedia;
	private Boolean adjustAlarm;
	private int maxMediaVolume;
	private int maxAlarmVolume;
	private int curMediaVolume;
	private int curAlarmVolume;
	private int quicksilenceMinutes;
	private int quickSilenceHours;
	@Deprecated
	private int refreshInterval;
	private int bufferMinutes;
	@Deprecated
	private Boolean wakeDevice;
	private int notificationId;
	private int lookaheadDays;
	private boolean updatesCheckbox;
	private boolean showDonationThanks;
	private int requestCodeEvent;
	private int requestCodeQuicksilence;
	private int requestCodeNotification;

	public Prefs(Context ctx) {
		this.ctx = ctx;
		readPreferences();
	}

	public void readPreferences() {
		SharedPreferences preferences = ctx.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);

		isServiceActivated = preferences.getBoolean(Keys.IS_SERVICE_ACTIVATED,
				DefaultPrefs.IS_ACTIVATED);
		silenceFreeTimeEvents = preferences.getBoolean(Keys.SILENCE_FREE_TIME,
				DefaultPrefs.SILENCE_FREE_TIME);
		silenceAllDayEvents = preferences
				.getBoolean(Keys.SILENCE_ALL_DAY, DefaultPrefs.SILENCE_ALL_DAY);
		ringerType = preferences.getInt(Keys.RINGER_TYPE, DefaultPrefs.RINGER_TYPE);
		adjustMedia = preferences.getBoolean(Keys.ADJUST_MEDIA, DefaultPrefs.ADJUST_MEDIA);
		adjustAlarm = preferences.getBoolean(Keys.ADJUST_ALARM, DefaultPrefs.ADJUST_ALARM);
		maxMediaVolume = preferences.getInt(Keys.MEDIA_VOLUME_MAX, DefaultPrefs.MEDIA_VOLUME_MAX);
		maxAlarmVolume = preferences.getInt(Keys.ALARM_VOLUME_MAX, DefaultPrefs.ALARM_VOLUME_MAX);
		curMediaVolume = preferences.getInt(Keys.MEDIA_VOLUME, DefaultPrefs.MEDIA_VOLUME);
		curAlarmVolume = preferences.getInt(Keys.ALARM_VOLUME, DefaultPrefs.ALARM_VOLUME);
		quicksilenceMinutes = preferences.getInt(Keys.QUICKSILENCE_MINUTES,
				DefaultPrefs.QUICK_SILENCE_MINUTES);
		quickSilenceHours = preferences.getInt(Keys.QUICKSILENCE_HOURS,
				DefaultPrefs.QUICK_SILENCE_HOURS);

		refreshInterval = preferences.getInt(Keys.REFRESH_INTERVAL, DefaultPrefs.REFRESH_INTERVAL);
		bufferMinutes = preferences.getInt(Keys.BUFFER_MINUTES, DefaultPrefs.BUFFER_MINUTES);

		wakeDevice = preferences.getBoolean(Keys.WAKE_DEVICE, DefaultPrefs.WAKE_DEVICE);
		notificationId = preferences.getInt(Keys.NOTIFICATION_ID, DefaultPrefs.NOTIFICATION_ID);
		lookaheadDays = preferences.getInt(Keys.LOOKAHEAD_DAYS, DefaultPrefs.LOOKAHEAD_DAYS);
		updatesCheckbox = preferences.getBoolean(Keys.UPDATES_CHECKBOX, DefaultPrefs.UPDATES_CHECKBOX);
		showDonationThanks = preferences.getBoolean(Keys.SHOW_DONATION_THANKS,
				DefaultPrefs.SHOW_DONATION_THANKS);
		requestCodeEvent = preferences.getInt(Keys.RC_EVENT, DefaultPrefs.RC_EVENT);
		requestCodeQuicksilence = preferences.getInt(Keys.RC_QUICKSILENT, DefaultPrefs.RC_QUICKSILENT);
		requestCodeNotification = preferences
				.getInt(Keys.RC_NOTIFICATION, DefaultPrefs.RC_NOTIFICATION);
	}

	public void storePreferences() {
		// TODO
	}

	public Boolean getIsServiceActivated() {
		return isServiceActivated;
	}

	public void setIsServiceActivated(Boolean isServiceActivated) {
		this.isServiceActivated = isServiceActivated;
	}

	public Boolean getSilenceFreeTimeEvents() {
		return silenceFreeTimeEvents;
	}

	public void setSilenceFreeTimeEvents(Boolean silenceFreeTimeEvents) {
		this.silenceFreeTimeEvents = silenceFreeTimeEvents;
	}

	public Boolean getSilenceAllDayEvents() {
		return silenceAllDayEvents;
	}

	public void setSilenceAllDayEvents(Boolean silenceAllDayEvents) {
		this.silenceAllDayEvents = silenceAllDayEvents;
	}

	public int getRingerType() {
		return ringerType;
	}

	public void setRingerType(int ringerType) {
		this.ringerType = ringerType;
	}

	public Boolean getAdjustMedia() {
		return adjustMedia;
	}

	public void setAdjustMedia(Boolean adjustMedia) {
		this.adjustMedia = adjustMedia;
	}

	public Boolean getAdjustAlarm() {
		return adjustAlarm;
	}

	public void setAdjustAlarm(Boolean adjustAlarm) {
		this.adjustAlarm = adjustAlarm;
	}

	public int getMaxMediaVolume() {
		return maxMediaVolume;
	}

	public void setMaxMediaVolume(int maxMediaVolume) {
		this.maxMediaVolume = maxMediaVolume;
	}

	public int getMaxAlarmVolume() {
		return maxAlarmVolume;
	}

	public void setMaxAlarmVolume(int maxAlarmVolume) {
		this.maxAlarmVolume = maxAlarmVolume;
	}

	public int getCurMediaVolume() {
		return curMediaVolume;
	}

	public void setCurMediaVolume(int curMediaVolume) {
		this.curMediaVolume = curMediaVolume;
	}

	public int getCurAlarmVolume() {
		return curAlarmVolume;
	}

	public void setCurAlarmVolume(int curAlarmVolume) {
		this.curAlarmVolume = curAlarmVolume;
	}

	public int getQuicksilenceMinutes() {
		return quicksilenceMinutes;
	}

	public void setQuicksilenceMinutes(int quicksilenceMinutes) {
		this.quicksilenceMinutes = quicksilenceMinutes;
	}

	public int getQuickSilenceHours() {
		return quickSilenceHours;
	}

	public void setQuickSilenceHours(int quickSilenceHours) {
		this.quickSilenceHours = quickSilenceHours;
	}

	public int getRefreshInterval() {
		return refreshInterval;
	}

	public void setRefreshInterval(int refreshInterval) {
		this.refreshInterval = refreshInterval;
	}

	public int getBufferMinutes() {
		return bufferMinutes;
	}

	public void setBufferMinutes(int bufferMinutes) {
		this.bufferMinutes = bufferMinutes;
	}

	public Boolean getWakeDevice() {
		return wakeDevice;
	}

	public void setWakeDevice(Boolean wakeDevice) {
		this.wakeDevice = wakeDevice;
	}

	public int getNotificationId() {
		return notificationId;
	}

	public void setNotificationId(int notificationId) {
		this.notificationId = notificationId;
	}

	public int getLookaheadDays() {
		return lookaheadDays;
	}

	public void setLookaheadDays(int lookaheadDays) {
		this.lookaheadDays = lookaheadDays;
	}

	public boolean isUpdatesCheckbox() {
		return updatesCheckbox;
	}

	public void setUpdatesCheckbox(boolean updatesCheckbox) {
		this.updatesCheckbox = updatesCheckbox;
	}

	public boolean isShowDonationThanks() {
		return showDonationThanks;
	}

	public void setShowDonationThanks(boolean showDonationThanks) {
		this.showDonationThanks = showDonationThanks;
	}

	public int getRequestCodeEvent() {
		return requestCodeEvent;
	}

	public void setRequestCodeEvent(int requestCodeEvent) {
		this.requestCodeEvent = requestCodeEvent;
	}

	public int getRequestCodeQuicksilence() {
		return requestCodeQuicksilence;
	}

	public void setRequestCodeQuicksilence(int requestCodeQuicksilence) {
		this.requestCodeQuicksilence = requestCodeQuicksilence;
	}

	public int getRequestCodeNotification() {
		return requestCodeNotification;
	}

	public void setRequestCodeNotification(int requestCodeNotification) {
		this.requestCodeNotification = requestCodeNotification;
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
	private boolean showUpdatesForVersion(String versionName) {
		SharedPreferences preferences = ctx.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		return preferences.getBoolean(Keys.CURRENT_RELEASE_NAME, true);
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
		public static final String MEDIA_VOLUME_MAX 	= "MEDIA_VOLUME_MAX";
		public static final String ALARM_VOLUME_MAX		= "ALARM_VOLUME_MAX";
		public static final String MEDIA_VOLUME			= "MEDIA_VOLUME";
		public static final String ALARM_VOLUME 		= "ALARM_VOLUME";
		public static final String QUICKSILENCE_MINUTES = "QUICK_SILENCE_MINUTES";
		public static final String QUICKSILENCE_HOURS 	= "QUICK_SILENCE_HOURS";
		public static final String REFRESH_INTERVAL 	= "REFRESH_INTERVAL";
		public static final String BUFFER_MINUTES		= "BUFFER_MINUTES";
		public static final String WAKE_DEVICE 			= "WAKE_DEVICE";
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
}
