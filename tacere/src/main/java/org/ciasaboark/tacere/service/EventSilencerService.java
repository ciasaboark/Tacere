/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.ciasaboark.tacere.activity.CalEvent;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.manager.ActiveEventManager;
import org.ciasaboark.tacere.manager.AlarmManagerWrapper;
import org.ciasaboark.tacere.manager.NotificationManagerWrapper;
import org.ciasaboark.tacere.manager.RingerStateManager;
import org.ciasaboark.tacere.manager.ServiceStateManager;
import org.ciasaboark.tacere.manager.ServiceStateManager.ServiceStates;
import org.ciasaboark.tacere.manager.VolumesManager;
import org.ciasaboark.tacere.prefs.Prefs;

import java.util.Deque;

public class EventSilencerService extends IntentService {
    private static final String TAG = "EventSilencerService";
    private static final long TEN_SECONDS = 10000;
    private static Prefs prefs;
    private static AlarmManagerWrapper alarmManager;
    private static NotificationManagerWrapper notificationManager;
    private static RingerStateManager ringerState;
    private static VolumesManager volumesManager;
    private static ServiceStateManager stateManager;
    private static DatabaseInterface databaseInterface;

    public EventSilencerService() {
        super(TAG);
    }

    /**
     * Looks for four different types of intents, one to start a
     * quick silence request, one to stop an ongoing quick silence request,
     * one to start the service (if not already started), and a normal request
     * to check for active events.  The type of intent is determined by mapping
     * the extra info "type" to one of the enumerated types in RequestTypes.
     * Normal request either have no extra info, or equate to RequestTypes.NORMAL
     */
    @Override
    public void onHandleIntent(Intent intent) {
        Log.d(TAG, "waking");


        prefs = new Prefs(this);
        alarmManager = new AlarmManagerWrapper(this);
        notificationManager = new NotificationManagerWrapper(this);
        ringerState = new RingerStateManager(this);
        volumesManager = new VolumesManager(this);
        stateManager = new ServiceStateManager(this);
        databaseInterface = DatabaseInterface.getInstance(this);

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
            // we should never get a quicksilence request with a zero or negative duration
            int duration = intent.getIntExtra("duration", -1);
            if (duration >= 1) {
                quickSilence(duration);
            }
        } else if (requestType.equals(RequestTypes.CANCEL_QUICKSILENCE)) {
            cancelQuickSilence();
        } else if (requestType.equals(RequestTypes.ACTIVITY_RESTART)
                || requestType.equals(RequestTypes.PROVIDER_CHANGED)) {
            scheduleServiceRestart();
        } else if (requestType.equals(RequestTypes.NORMAL)) {
            if (prefs.getIsServiceActivated() && !stateManager.isQuickSilenceActive()) {
                checkForActiveEventsAndSilence();
                notifyCursorAdapterDataChanged();
            } else { // normal wake requests (but service is marked to be inactive)
                shutdownService();
            }
        }


    }

    /**
     * Sync the calendar database, set the service state to be inactive, then schedule an immediate
     * restart to look for any active events
     */
    private void firstWake() {
        databaseInterface.syncCalendarDb();
        // since the state is saved in a SharedPreferences file it can become out of sync
        // if the device restarts. To solve this set the service as not active, then schedule an
        // immediate restart to look for active events
        stateManager.setServiceState(ServiceStates.NOT_ACTIVE);
        alarmManager.scheduleNormalWakeAt(System.currentTimeMillis());
    }

    private void quickSilence(int durationMinutes) {
        // quick silence requests can occur during three states, either no event is active, an event
        // is active, or a previous quick silence request is still ongoing. If this request occurred
        // while no event was active, then save the current ringer state so it can be restored later
        ringerState.storeRingerStateIfNeeded();

        stateManager.setServiceState(ServiceStates.QUICKSILENCE);

        long wakeAt = System.currentTimeMillis() + (CalEvent.MILLISECONDS_IN_MINUTE
                * durationMinutes);
        // TODO check here to make sure scheduling is working
        alarmManager.scheduleCancelQuickSilenceAlarmAt(wakeAt);


        //quick silence requests are always explicitly request to silence the ringer
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

    private void scheduleServiceRestart() {
        // schedule an immediate wakeup only if we aren't in a quick silence period
        if (!stateManager.getServiceState().equals(ServiceStates.QUICKSILENCE)) {
            alarmManager.scheduleNormalWakeAt(System.currentTimeMillis());
        }
    }

    private void checkForActiveEventsAndSilence() {
        Deque<CalEvent> events = databaseInterface.syncAndGetAllActiveEvents();
        boolean foundEvent = false;
        for (CalEvent event : events) {
            if (shouldEventSilence(event)) {
                foundEvent = true;
                ActiveEventManager.setActiveEvent(event);
                silenceEventAndShowNotification(event);
                break;
            }
        }

        if (!foundEvent) {
            // there are no events currently active
            ActiveEventManager.removeActiveEvent();
            if (stateManager.isEventActive()) {
                vibrate();
                stateManager.setServiceState(ServiceStates.NOT_ACTIVE);
                notificationManager.cancelAllNotifications();
                volumesManager.restoreVolumes();
                ringerState.restorePhoneRinger();
            }

            //if no events are active, then we should sleep until the start of the next inactive
            //event, else sleep until the end of the last active event
            long wakeAt;

            if (!events.isEmpty()) {
                CalEvent lastActiveEvent = events.getLast();
                wakeAt = lastActiveEvent.getEnd();
            } else {
                CalEvent nextInactiveEvent = databaseInterface.nextEvent();
                if (nextInactiveEvent != null) {
                    wakeAt = nextInactiveEvent.getBegin() - (CalEvent.MILLISECONDS_IN_MINUTE * prefs.getBufferMinutes());
                } else {
                    wakeAt = System.currentTimeMillis() + CalEvent.MILLISECONDS_IN_DAY;

                }
            }

            alarmManager.scheduleNormalWakeAt(wakeAt);
        }
    }

    private int getBestRingerType(CalEvent e) {
        int eventRingerType = e.getRingerType();
        if (eventRingerType == CalEvent.RINGER.UNDEFINED) {
            eventRingerType = prefs.getRingerType();
        }
        return eventRingerType;
    }

    private void silenceEventAndShowNotification(CalEvent event) {
        ringerState.storeRingerStateIfNeeded();
        ringerState.setPhoneRinger(getBestRingerType(event));

        volumesManager.adjustMediaAndAlarmVolumesIfNeeded();
        //only vibrate if we are transitioning from no event active to an active event
        if (!stateManager.isEventActive()) {
            vibrate();
        }

        stateManager.setServiceState(ServiceStates.EVENT_ACTIVE);
        notificationManager.displayEventNotification(event);
        // the extra ten seconds is to make sure that the event ending is pushed into the
        // correct minute
        long wakeAt = event.getEnd()
                + (CalEvent.MILLISECONDS_IN_MINUTE * prefs.getBufferMinutes()) + TEN_SECONDS;
        alarmManager.scheduleNormalWakeAt(wakeAt);
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

    private void notifyCursorAdapterDataChanged() {
        Log.d(TAG, "Broadcasting message");
        Intent intent = new Intent("custom-event-name");
        // You can also include some extra data.
        intent.putExtra("message", "This is my message!");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 500, 200, 500};
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

        //all of this is negated if the event has been marked to be ignored
        if (event.getRingerType() == CalEvent.RINGER.IGNORE) {
            eventMatches = false;
        }

        return eventMatches;
    }

    private void shutdown() {
        stopSelf();
    }

}