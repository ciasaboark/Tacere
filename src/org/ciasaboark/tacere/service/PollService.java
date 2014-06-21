/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 * 
 * Released under the BSD license. For details see the COPYING file.
 */

package org.ciasaboark.tacere.service;

import java.util.ArrayDeque;
import java.util.Deque;

import org.ciasaboark.tacere.CalEvent;
import org.ciasaboark.tacere.DatabaseInterface;
import org.ciasaboark.tacere.DefPrefs;
import org.ciasaboark.tacere.MainActivity;
import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.provider.EventProvider;

import android.annotation.TargetApi;
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
import android.os.Build;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class PollService extends IntentService {
	private static final String TAG = "PollService";

	// Values that should be read from the settings
	private boolean isServiceActivated;
	private boolean silenceFreeTime;
	private boolean silenceAllDay;
	private boolean adjustMedia;
	private int ringerType;
	private int mediaVolume;
	private boolean adjustAlarm;
	private int alarmVolume;
	private int bufferMinutes;
	private int lookaheadDays;
	private int quickSilenceMinutes;
	private int quickSilenceHours;

	private static final long TEN_SECONDS = 10000;
	private DatabaseInterface DBIface = DatabaseInterface.get(this);

	public PollService() {
		super(TAG);
	}

	@Override
	/**
	 * PollService looks for four different types of intents, one to start a
	 * quick silence request, one to stop an ongoing quick silence request,
	 * one to start the service (if not already started), and a normal request
	 * to check for active events.  The type of intent is determined by mapping
	 * the extra info "type" to one of the enumerated types in RequestTypes.
	 * Normal request either have no extra info, or equate to RequestTypes.NORMAL
	 */
	public void onHandleIntent(Intent intent) {
		Log.d(TAG, "Pollservice waking");
		readSettings();

		// pull extra info (if any) from the incoming intent
		String requestType = "";
		if (intent.getExtras() != null) {
			// for some reason intents that have no extras are still getting through this check
			String t = intent.getStringExtra("type");
			if (t != null) {
				requestType = t;
			} else {
				Log.w(TAG, "incoming request type is undefined, using default value RequestTypes.NORMAL");
				requestType = RequestTypes.NORMAL;
			}
		}

		if (requestType.equals(RequestTypes.FIRST_WAKE)) {
			firstWake();
		} else if (requestType.equals(RequestTypes.QUICKSILENCE)) {
			// we should never get a quicksilence request without a duration
			int duration = intent.getIntExtra("duration", -1);
			if (duration != -1) {
				quickSilence(duration);
			}
		} else if (requestType.equals(RequestTypes.CANCEL_QUICKSILENCE)) {
			cancelQuickSilence();
		} else if (requestType.equals(RequestTypes.ACTIVITY_RESTART)
				|| requestType.equals(RequestTypes.PROVIDER_CHANGED)) {
			syncDbAndRestart();
		} else if (requestType.equals(RequestTypes.NORMAL)) { 
			if (isServiceActivated) {
				checkForActiveEvents();
			} else {					//normal wake requests (but service is marked to be inactive)
				shutdownService();
			}
		}

		notifyCursorAdapterDataChanged();
	}

	/**
	 * Sync the calendar database, set the service state to be inactive, then schedule an immediate
	 * restart to look for any active events
	 */
	private void firstWake() {
		syncCalendarDb();
		// since the state is saved in a SharedPreferences file it can become out of sync
		// if the device restarts. To solve this set the service as not active, then schedule an
		// immediate restart to look for active events
		setServiceState(ServiceStates.NOT_ACTIVE);
		scheduleAlarmAt(System.currentTimeMillis(), RequestTypes.NORMAL);
	}

	/**
	 * Set the phone ringer to silent for the given duration
	 * 
	 * @param durationMinutes
	 */
	private void quickSilence(int durationMinutes) {
		Context context = getApplicationContext();

		// store the current ringer state to preferences so it can be restored later
		if (getServiceState().equals(ServiceStates.NOT_ACTIVE)) {
			storeRingerState();
		}

		setServiceState(ServiceStates.QUICKSILENCE);

		// when the quick silence duration is over the device should wake regardless
		// + of the user settings
		// TODO use the given duration instead of using quickSilenceMinutes and hours
		long wakeAt = System.currentTimeMillis() + CalEvent.MILLISECONDS_IN_MINUTE
				* (quickSilenceMinutes + (quickSilenceHours * 60));
		// TODO check here to make sure scheduling is working
		scheduleAlarmAt(wakeAt, RequestTypes.CANCEL_QUICKSILENCE);

		// quick silence requests can occur during three states, either no event is active, an event
		// is active, or a previous quick silence request is still ongoing. If this request occurred
		// while no event was active, then save the current ringer state so it can be restored later
		if (getServiceState().equals(ServiceStates.NOT_ACTIVE)) {
			storeRingerState();
		}

		setPhoneRinger(CalEvent.RINGER.SILENT);

		vibrate();

		// the intent attached to the notification should only cancel the quick silence request, but
		// not launch the app
		Intent notificationIntent = new Intent(this, PollService.class);
		notificationIntent.putExtra("type", RequestTypes.CANCEL_QUICKSILENCE);

		int hrs = durationMinutes / 60;
		int min = durationMinutes % 60;
		StringBuilder sb = new StringBuilder("Silencing for ");
		if (hrs > 0) {
			sb.append(hrs + " hr ");
		}

		sb.append(min + " min. Touch to cancel");

		// FLAG_CANCEL_CURRENT is required to make sure that the extras are including in the new
		// pending intent
		PendingIntent pendIntent = PendingIntent.getService(context, DefPrefs.RC_NOTIFICATION,
				notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		// TODO use strings in xml
		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(
				getApplicationContext()).setContentTitle("Tacere: Quick Silence")
				.setContentText(sb.toString()).setTicker("Quick Silence activating")
				.setSmallIcon(R.drawable.small_mono).setAutoCancel(true).setOngoing(true)
				.setContentIntent(pendIntent);

		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(DefPrefs.NOTIFICATION_ID);
		nm.notify(DefPrefs.NOTIFICATION_ID, notBuilder.build());
	}

	/**
	 * Cancels any ongoing quick silence request then schedules an immediate restart to look for
	 * active events
	 */
	private void cancelQuickSilence() {
		setServiceState(ServiceStates.NOT_ACTIVE);
		restoreVolumes();
		cancelNotification();
		vibrate();
		// schedule an immediate normal wakeup
		scheduleAlarmAt(System.currentTimeMillis(), RequestTypes.NORMAL);
	}

	private void checkForActiveEvents() {
		syncCalendarDb();
		Deque<CalEvent> events = allActiveEvents();
		CalEvent event = events.peek();
		if (event != null && shouldEventSilence(event)) {
			// only store the current ringer state if we are not transitioning from one event to the
			// next and we are not in a quick silence period
			if (getServiceState().equals(ServiceStates.NOT_ACTIVE)) {
				storeRingerState();
			}
			setPhoneRinger(event.getRingerType());
			adjustMediaAndAlarmVolumes();
			if (event.getRingerType() == CalEvent.RINGER.SILENT
					|| event.getRingerType() == CalEvent.RINGER.VIBRATE) {
				vibrate();
			}
			setServiceState(ServiceStates.EVENT_ACTIVE);
			displayNotification(event);
			// the extra ten seconds is to make sure that the event ending is pushed into the
			// correct minute
			long wakeAt = event.getEnd() + (CalEvent.MILLISECONDS_IN_MINUTE * bufferMinutes)
					+ TEN_SECONDS;
			scheduleAlarmAt(wakeAt, RequestTypes.NORMAL);
		} else {
			// there are no events currently active
			if (getServiceState().equals(ServiceStates.EVENT_ACTIVE)) {
				vibrate();
				setServiceState(ServiceStates.NOT_ACTIVE);
				cancelNotification();
				restoreVolumes();
				setPhoneRinger(getStoredRingerState());
			}
	
			// Sleep the service until the start of the next
			// event, or for 24 hours if there are no more events
			CalEvent nextEvent = nextEvent();
			long wakeAt;
			if (nextEvent != null) {
				wakeAt = nextEvent.getBegin() - (CalEvent.MILLISECONDS_IN_MINUTE * bufferMinutes);
			} else {
				wakeAt = System.currentTimeMillis() + CalEvent.MILLISECONDS_IN_DAY;
			}
	
			scheduleAlarmAt(wakeAt, RequestTypes.NORMAL);
		}
	}

	/**
	 * Syncs the apps internal database with the system calendar database then schedules an
	 * immediate restart to look for active events (unless a quick silence request is ongoing)
	 */
	private void syncDbAndRestart() {
		syncCalendarDb();

		// schedule an immediate wakeup only if we aren't in a quick silence period
		if (!getServiceState().equals(ServiceStates.QUICKSILENCE)) {
			scheduleAlarmAt(System.currentTimeMillis(), RequestTypes.NORMAL);
		}
	}

	/**
	 * Sync the internal database with the system calendar database. Forward syncing is limited to
	 * the period specified in preferences. Old events are pruned.
	 */
	private void syncCalendarDb() {
		// update(n) will also remove all events not found in the next n days, so we
		// + need to keep this in sync with the users preferences.
		DBIface.update(lookaheadDays);
		DBIface.pruneEventsBefore(System.currentTimeMillis() - CalEvent.MILLISECONDS_IN_MINUTE
				* (long) bufferMinutes);
	}

	private Deque<CalEvent> allActiveEvents() {
		// sync the db and prune old events
		syncCalendarDb();

		Deque<CalEvent> events = new ArrayDeque<CalEvent>();

		Cursor cursor = DBIface.getCursor(EventProvider.BEGIN);

		long beginTime = System.currentTimeMillis()
				- (CalEvent.MILLISECONDS_IN_MINUTE * (long) bufferMinutes);
		long endTime = System.currentTimeMillis()
				+ (CalEvent.MILLISECONDS_IN_MINUTE * (long) bufferMinutes);

		if (cursor.moveToFirst()) {
			do {
				int id = cursor.getInt(cursor.getColumnIndex(EventProvider._ID));
				CalEvent e = DBIface.getEvent(id);
				if (e.isActiveBetween(beginTime, endTime)) {
					events.add(e);
				}

				if (e.getBegin() <= endTime);

			} while (cursor.moveToNext());
		}

		cursor.close();

		return events;
	}

	/**
	 * Shut down the background service, remove all ongoing notifications, restore volumes and
	 * ringer and cancel any set alarms
	 */
	private void shutdownService() {
		if (getServiceState().equals(ServiceStates.EVENT_ACTIVE)) {
			vibrate();
			setServiceState(ServiceStates.NOT_ACTIVE);
			cancelNotification();
			restoreVolumes();
		}
		setPhoneRinger(getStoredRingerState());
		cancelAlarm();
		shutdown();
	}

	/**
	 * Cancel any ongoing notification, this will remove both event notifications and quicksilence
	 * notifications
	 */
	private void cancelNotification() {
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(DefPrefs.NOTIFICATION_ID);
	}

	/**
	 * A wrapper method to build and display a notification for the given CalEvent. If possible the
	 * newer Notification API will be used to place an action button in the notification, otherwise
	 * the older notification style will be used.
	 * 
	 * @param event
	 *            the CalEvent that is currently active.
	 */
	private void displayNotification(CalEvent event) {
		int apiLevelAvailable = android.os.Build.VERSION.SDK_INT;
		if (apiLevelAvailable >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			displayNewNotification(event);
		} else {
			displayCompatNotification(event);
		}
	}

	/**
	 * Builds and displays a notification for the given CalEvent. This method uses the older
	 * Notification API, and does not include an action button
	 * 
	 * @param event
	 *            the CalEvent that is currently active.
	 */
	private void displayCompatNotification(CalEvent event) {
		Context context = getApplicationContext();
		// clicking the notification should take the user to the app
		Intent notificationIntent = new Intent(context, MainActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		// FLAG_CANCEL_CURRENT is required to make sure that the extras are including in
		// the new pending intent
		PendingIntent pendIntent = PendingIntent.getActivity(context, DefPrefs.RC_NOTIFICATION,
				notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(
				getApplicationContext()).setContentTitle("Tacere: Event active")
				.setContentText(event.toString()).setSmallIcon(R.drawable.small_mono)
				.setAutoCancel(false).setOnlyAlertOnce(true).setOngoing(true)
				.setContentIntent(pendIntent);

		// the ticker text should only be shown the first time the notification is
		// created, not on each update
		notBuilder.setTicker("Tacere event starting: " + event.toString());

		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(DefPrefs.NOTIFICATION_ID, notBuilder.build());
	}

	/**
	 * Builds and displays a notification for the given CalEvent. This method uses the new
	 * Notification API to place an action button in the notification.
	 * 
	 * @param event
	 *            the CalEvent that is currently active
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void displayNewNotification(CalEvent event) {
		Context context = getApplicationContext();

		// clicking the notification should take the user to the app
		Intent notificationIntent = new Intent(context, MainActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		// FLAG_CANCEL_CURRENT is required to make sure that the extras are including in
		// the new pending intent
		PendingIntent pendIntent = PendingIntent.getActivity(context, DefPrefs.RC_NOTIFICATION,
				notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		Notification.Builder notBuilder = new Notification.Builder(this)
				.setContentTitle("Tacere: Event active").setContentText(event.toString())
				.setSmallIcon(R.drawable.small_mono).setAutoCancel(false).setOnlyAlertOnce(true)
				.setOngoing(true).setContentIntent(pendIntent);

		if (event.getRingerType() != CalEvent.RINGER.IGNORE) {
			// this intent will be attached to the button on the notification
			Intent skipEventIntent = new Intent(this, SkipEventService.class);
			skipEventIntent.putExtra("org.ciasaboark.tacere.eventId", event.getId());
			PendingIntent skipEventPendIntent = PendingIntent.getService(this, 0, skipEventIntent,
					PendingIntent.FLAG_CANCEL_CURRENT);

			notBuilder
					.addAction(R.drawable.ic_state_normal, "Skip this event", skipEventPendIntent);
		} else {
			// this intent will be attached to the button on the notification
			Intent skipEventIntent = new Intent(this, ResetEventService.class);
			skipEventIntent.putExtra("org.ciasaboark.tacere.eventId", event.getId());
			PendingIntent skipEventPendIntent = PendingIntent.getService(this, 0, skipEventIntent,
					PendingIntent.FLAG_CANCEL_CURRENT);

			notBuilder.addAction(R.drawable.ic_state_normal, "Enable auto silencing",
					skipEventPendIntent);
		}

		// the ticker text should only be shown the first time the notification is
		// created, not on each update
		notBuilder.setTicker("Tacere event starting: " + event.toString());

		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(DefPrefs.NOTIFICATION_ID, notBuilder.build());
	}

	/**
	 * Store the current ringer state (vibrate, silent, or normal). Ringer state is stored into
	 * shared preferences
	 */
	private void storeRingerState() {
		SharedPreferences preferences = this.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		// TODO this may return a state indicating that a call is ongoing, this should stop
		// processing and wait for the call to end before adjusting volumes
		int curRinger = ((AudioManager) this.getSystemService(Context.AUDIO_SERVICE))
				.getRingerMode();
		preferences.edit().putInt("curRinger", curRinger).commit();
	}

	/**
	 * Remove stored ringer state from preferences
	 */
	private void clearStoredRingerState() {
		SharedPreferences preferences = this.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		preferences.edit().remove("curRinger").commit();
	}

	/**
	 * Retrieves the current stored phone ringer from preferences
	 * 
	 * @return the stored ringer state. Returned value can be compared to CalEvent.RINGER types
	 */
	private int getStoredRingerState() {
		SharedPreferences preferences = this.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		// TODO this may return a state indicating that a call is ongoing, this should stop
		// processing and wait for the call to end before adjusting volumes
		return preferences.getInt("curRinger", CalEvent.RINGER.UNDEFINED);
	}

	private void scheduleAlarmAt(long time, String type) {
		if (type == null) {
			throw new IllegalArgumentException("unknown type: " + type);
		}

		if (time < 0) {
			throw new IllegalArgumentException("PollService:scheduleAlarmAt not given valid time");
		}

		Intent i = new Intent(getApplicationContext(), PollService.class);
		i.putExtra("type", type);

		// note that alarm manager allows multiple pending intents to be scheduled per app
		// + but only if each intent has a unique request code. Since we want to schedule
		// + wakeups for ending quicksilent durations as well as starting events we check
		// + the type and assign a different requestCode
		// default to 0
		int requestCode = 0;
		if (type.equals(RequestTypes.CANCEL_QUICKSILENCE)) {
			requestCode = DefPrefs.RC_QUICKSILENT;
		} else if (type.equals(RequestTypes.NORMAL)) {
			requestCode = DefPrefs.RC_EVENT;
		}

		PendingIntent pintent = PendingIntent.getService(this, requestCode, i,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarm.set(AlarmManager.RTC_WAKEUP, time, pintent);
	}

	private void vibrate() {
		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		long[] pattern = { 0, 500, 200, 500 };
		vibrator.vibrate(pattern, -1);
	}

	/**
	 * Query the internal database for the next event.
	 * 
	 * @return the next event in the database, or null if there are no events
	 */
	private CalEvent nextEvent() {
		// sync the database and prune old events first to make sure that we don't return an event
		// that has already expired
		syncCalendarDb();
		CalEvent nextEvent = null;

		Cursor cursor = DBIface.getCursor(EventProvider.BEGIN);
		if (cursor.moveToFirst()) {
			int id = cursor.getInt(cursor.getColumnIndex(EventProvider._ID));
			nextEvent = DBIface.getEvent(id);
		}
		cursor.close();

		return nextEvent;
	}

	// check all events in the database to see if they match the user's criteria
	// + for silencing. Returns the first event that matches. Does not check
	// + whether that event is active
	private boolean shouldEventSilence(CalEvent event) {
		boolean eventMatches = false;

		// if the event is marked as busy (but is not an all day event)
		// + then we need no further tests
		boolean busy_notAllDay = false;
		if (!event.isFreeTime() && !event.isAllDay()) {
			busy_notAllDay = true;
		}

		// all day events
		boolean allDay = false;
		if (silenceAllDay && event.isAllDay()) {
			allDay = true;
		}

		// events marked as 'free' or 'available'
		boolean free_notAllDay = false;
		if (silenceFreeTime && event.isFreeTime() && !event.isAllDay()) {
			free_notAllDay = true;
		}

		// events with a custom ringer set should always use that ringer
		boolean isCustomRingerSet = false;
		if (event.getRingerType() != CalEvent.RINGER.UNDEFINED) {
			isCustomRingerSet = true;
		}

		if (busy_notAllDay || allDay || free_notAllDay || isCustomRingerSet) {
			eventMatches = true;
		}

		return eventMatches;
	}

	private void cancelAlarm() {
		// there could be multiple alarms scheduled, we have to cancel all of them
		for (int requestCode = 0; requestCode <= 4; requestCode++) {
			Intent i = new Intent(this, PollService.class);
			PendingIntent pintent = PendingIntent.getService(this, requestCode, i, 0);
			AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			alarm.cancel(pintent);
		}
	}

	private void readSettings() {
		SharedPreferences preferences = this.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		isServiceActivated = preferences.getBoolean("isActivated", DefPrefs.IS_ACTIVATED);
		silenceFreeTime = preferences.getBoolean("silenceFreeTime", DefPrefs.SILENCE_FREE_TIME);
		ringerType = preferences.getInt("ringerType", DefPrefs.RINGER_TYPE);
		adjustMedia = preferences.getBoolean("adjustMedia", DefPrefs.ADJUST_MEDIA);
		mediaVolume = preferences.getInt("mediaVolume", DefPrefs.MEDIA_VOLUME);
		adjustAlarm = preferences.getBoolean("adjustAlarm", DefPrefs.ADJUST_ALARM);
		alarmVolume = preferences.getInt("alarmVolume", DefPrefs.ALARM_VOLUME);
		silenceAllDay = preferences.getBoolean("silenceAllDay", DefPrefs.SILENCE_ALL_DAY);
		bufferMinutes = preferences.getInt("bufferMinutes", DefPrefs.BUFFER_MINUTES);
		lookaheadDays = preferences.getInt("lookaheadDays", DefPrefs.LOOKAHEAD_DAYS);
		quickSilenceMinutes = preferences.getInt("quickSilenceMinutes",
				DefPrefs.QUICK_SILENCE_MINUTES);
		quickSilenceHours = preferences.getInt("quickSilenceHours", DefPrefs.QUICK_SILENCE_HOURS);
	}

	private void restoreVolumes() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int curRinger = getStoredRingerState();
		switch (curRinger) {
			case AudioManager.RINGER_MODE_NORMAL:
				audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
				break;
			case AudioManager.RINGER_MODE_SILENT:
				audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
				break;
			case AudioManager.RINGER_MODE_VIBRATE:
				audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
				break;
			default:
				// by default the ringer will be set back to normal
				audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		}

		clearStoredRingerState();

		if (adjustMedia) {
			audio.setStreamVolume(AudioManager.STREAM_MUSIC, DefPrefs.MEDIA_VOLUME, 0);
		}

		if (adjustAlarm) {
			audio.setStreamVolume(AudioManager.STREAM_ALARM, DefPrefs.ALARM_VOLUME, 0);
		}
	}

	private void setPhoneRinger(int ringerType) {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

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

	private void adjustMediaAndAlarmVolumes() {
		// change media volume, and alarm volume
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		if (adjustMedia) {
			audio.setStreamVolume(AudioManager.STREAM_MUSIC, mediaVolume, 0);
		}

		if (adjustAlarm) {
			audio.setStreamVolume(AudioManager.STREAM_ALARM, alarmVolume, 0);
		}

	}

	private void setServiceState(String state) {
		if (state != ServiceStates.QUICKSILENCE || state != ServiceStates.EVENT_ACTIVE
				|| state != ServiceStates.NOT_ACTIVE) {
			throw new IllegalArgumentException("unknown state: " + state);
		}

		SharedPreferences preferences = this.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		if (state != null) {
			editor.putString("serviceState", state);
		} else {
			Log.e(TAG, "Trying to set state to null value");
		}
		editor.commit();
	}

	private String getServiceState() {
		SharedPreferences preferences = this.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		return preferences.getString("serviceState", ServiceStates.NOT_ACTIVE);
	}

	private void shutdown() {
		stopSelf();
	}

	private void notifyCursorAdapterDataChanged() {
		Log.d(TAG, "Broadcasting message");
		Intent intent = new Intent("custom-event-name");
		// You can also include some extra data.
		intent.putExtra("message", "This is my message!");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	public class ServiceStates {
		public static final String QUICKSILENCE = "quickSilence";
		public static final String EVENT_ACTIVE = "active";
		public static final String NOT_ACTIVE = "notActive";
	}

	public class RequestTypes {
		public static final String NORMAL = "normal";
		public static final String QUICKSILENCE = "quickSilence";
		public static final String CANCEL_QUICKSILENCE = "cancelQuickSilence";
		public static final String FIRST_WAKE = "firstWake";
		public static final String ACTIVITY_RESTART = "activityRestart";
		public static final String PROVIDER_CHANGED = "providerChanged";
	}

}