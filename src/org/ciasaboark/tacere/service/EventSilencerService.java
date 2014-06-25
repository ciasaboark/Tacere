/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 * 
 * Released under the BSD license. For details see the COPYING file.
 */

package org.ciasaboark.tacere.service;

import java.util.Deque;

import org.ciasaboark.tacere.CalEvent;
import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.manager.AlarmManagerWrapper;
import org.ciasaboark.tacere.manager.NotificationManagerWrapper;
import org.ciasaboark.tacere.manager.RingerStateManager;
import org.ciasaboark.tacere.manager.ServiceStateManager;
import org.ciasaboark.tacere.manager.ServiceStateManager.ServiceStates;
import org.ciasaboark.tacere.manager.VolumesManager;
import org.ciasaboark.tacere.prefs.Prefs;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class EventSilencerService extends IntentService {
	private static final String TAG = "PollService";
	private Prefs prefs = new Prefs(this);
	private AlarmManagerWrapper alarmManager = new AlarmManagerWrapper(this);
	private NotificationManagerWrapper notificationManager = new NotificationManagerWrapper(this);
	private RingerStateManager ringerState = new RingerStateManager(this);
	private VolumesManager volumesManager = new VolumesManager(this);
	private ServiceStateManager stateManager = new ServiceStateManager(this);
	private static final long TEN_SECONDS = 10000;
	private DatabaseInterface DBIface = DatabaseInterface.getInstance(this);

	public EventSilencerService() {
		super(TAG);
	}

	@Override
	/**
	 * Looks for four different types of intents, one to start a
	 * quick silence request, one to stop an ongoing quick silence request,
	 * one to start the service (if not already started), and a normal request
	 * to check for active events.  The type of intent is determined by mapping
	 * the extra info "type" to one of the enumerated types in RequestTypes.
	 * Normal request either have no extra info, or equate to RequestTypes.NORMAL
	 */
	public void onHandleIntent(Intent intent) {
		Log.d(TAG, "Pollservice waking");

		// pull extra info (if any) from the incoming intent
		String requestType = "";
		if (intent.getExtras() != null) {
			// for some reason intents that have no extras are still getting through this check
			String t = intent.getStringExtra("type");
			if (t != null) {
				requestType = t;
			} else {
				Log.w(TAG,
						"incoming request type is undefined, using default value RequestTypes.NORMAL");
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
			if (prefs.getIsServiceActivated()) {
				checkForActiveEvents();
			} else { // normal wake requests (but service is marked to be inactive)
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
		DBIface.syncCalendarDb();
		// since the state is saved in a SharedPreferences file it can become out of sync
		// if the device restarts. To solve this set the service as not active, then schedule an
		// immediate restart to look for active events
		stateManager.setServiceState(ServiceStates.NOT_ACTIVE);
		alarmManager.scheduleNormalWakeAt(System.currentTimeMillis());
	}

	/**
	 * Set the phone ringer to silent for the given duration
	 * 
	 * @param durationMinutes
	 */
	private void quickSilence(int durationMinutes) {
		Context context = getApplicationContext();

		// store the current ringer state to preferences so it can be restored later
		if (stateManager.getServiceState().equals(ServiceStates.NOT_ACTIVE)) {
			ringerState.storeRingerState();
		}

		stateManager.setServiceState(ServiceStates.QUICKSILENCE);

		// when the quick silence duration is over the device should wake regardless
		// + of the user settings
		// TODO use the given duration instead of using quickSilenceMinutes and hours
		long wakeAt = System.currentTimeMillis() + CalEvent.MILLISECONDS_IN_MINUTE
				* (prefs.getQuicksilenceMinutes() + (prefs.getQuickSilenceHours() * 60));
		// TODO check here to make sure scheduling is working
		alarmManager.scheduleCancelQuickSilenceAlarmAt(wakeAt);

		// quick silence requests can occur during three states, either no event is active, an event
		// is active, or a previous quick silence request is still ongoing. If this request occurred
		// while no event was active, then save the current ringer state so it can be restored later
		if (stateManager.getServiceState().equals(ServiceStates.NOT_ACTIVE)) {
			ringerState.storeRingerState();
		}

		ringerState.setPhoneRinger(CalEvent.RINGER.SILENT);

		vibrate();

		notificationManager.displayQuickSilenceNotification(durationMinutes);
	}

	/**
	 * Cancels any ongoing quick silence request then schedules an immediate restart to look for
	 * active events
	 */
	private void cancelQuickSilence() {
		stateManager.setServiceState(ServiceStates.NOT_ACTIVE);
		volumesManager.restoreVolumes();
		notificationManager.cancelAllNotifications();
		vibrate();
		// schedule an immediate normal wakeup
		alarmManager.scheduleNormalWakeAt(System.currentTimeMillis());
	}

	private void checkForActiveEvents() {
		DBIface.syncCalendarDb();
		Deque<CalEvent> events = DBIface.allActiveEvents();
		CalEvent event = events.peek();
		if (event != null && shouldEventSilence(event)) {
			// only store the current ringer state if we are not transitioning from one event to the
			// next and we are not in a quick silence period
			if (stateManager.getServiceState().equals(ServiceStates.NOT_ACTIVE)) {
				ringerState.storeRingerState();
			}
			ringerState.setPhoneRinger(event.getRingerType());
			volumesManager.adjustMediaAndAlarmVolumes();
			if (event.getRingerType() == CalEvent.RINGER.SILENT
					|| event.getRingerType() == CalEvent.RINGER.VIBRATE) {
				vibrate();
			}
			stateManager.setServiceState(ServiceStates.EVENT_ACTIVE);
			notificationManager.displayEventNotification(event);
			// the extra ten seconds is to make sure that the event ending is pushed into the
			// correct minute
			long wakeAt = event.getEnd()
					+ (CalEvent.MILLISECONDS_IN_MINUTE * prefs.getBufferMinutes()) + TEN_SECONDS;
			alarmManager.scheduleNormalWakeAt(wakeAt);
		} else {
			// there are no events currently active
			if (stateManager.getServiceState().equals(ServiceStates.EVENT_ACTIVE)) {
				vibrate();
				stateManager.setServiceState(ServiceStates.NOT_ACTIVE);
				notificationManager.cancelAllNotifications();
				volumesManager.restoreVolumes();
				ringerState.setPhoneRinger(ringerState.getStoredRingerState());
			}

			// Sleep the service until the start of the next
			// event, or for 24 hours if there are no more events
			CalEvent nextEvent = DBIface.nextEvent();
			long wakeAt;
			if (nextEvent != null) {
				wakeAt = nextEvent.getBegin()
						- (CalEvent.MILLISECONDS_IN_MINUTE * prefs.getBufferMinutes());
			} else {
				wakeAt = System.currentTimeMillis() + CalEvent.MILLISECONDS_IN_DAY;
			}
			alarmManager.scheduleNormalWakeAt(wakeAt);
		}
	}

	/**
	 * Syncs the apps internal database with the system calendar database then schedules an
	 * immediate restart to look for active events (unless a quick silence request is ongoing)
	 */
	private void syncDbAndRestart() {
		DBIface.syncCalendarDb();

		// schedule an immediate wakeup only if we aren't in a quick silence period
		if (!stateManager.getServiceState().equals(ServiceStates.QUICKSILENCE)) {
			alarmManager.scheduleNormalWakeAt(System.currentTimeMillis());
		}
	}


	/**
	 * Shut down the background service, remove all ongoing notifications, restore volumes and
	 * ringer and cancel any set alarms
	 */
	private void shutdownService() {
		if (stateManager.getServiceState().equals(ServiceStates.EVENT_ACTIVE)) {
			vibrate();
			stateManager.setServiceState(ServiceStates.NOT_ACTIVE);
			notificationManager.cancelAllNotifications();
			volumesManager.restoreVolumes();
		}
		ringerState.setPhoneRinger(ringerState.getStoredRingerState());
		alarmManager.cancelAllAlarms();
		shutdown();
	}

	private void vibrate() {
		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		long[] pattern = { 0, 500, 200, 500 };
		vibrator.vibrate(pattern, -1);
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
		if (prefs.getSilenceAllDayEvents() && event.isAllDay()) {
			allDay = true;
		}

		// events marked as 'free' or 'available'
		boolean free_notAllDay = false;
		if (prefs.getSilenceFreeTimeEvents() && event.isFreeTime() && !event.isAllDay()) {
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

}