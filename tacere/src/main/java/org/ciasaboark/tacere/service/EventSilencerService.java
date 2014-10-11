/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.util.Log;

import org.ciasaboark.tacere.database.DataSetManager;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.EventManager;
import org.ciasaboark.tacere.event.ringer.RingerType;
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
        prefs = new Prefs(this);
        alarmManager = new AlarmManagerWrapper(this);
        notificationManager = new NotificationManagerWrapper(this);
        ringerState = new RingerStateManager(this);
        volumesManager = new VolumesManager(this);
        stateManager = new ServiceStateManager(this);
        databaseInterface = DatabaseInterface.getInstance(this);

        // pull extra info (if any) from the incoming intent
        RequestTypes requestType = RequestTypes.NORMAL;
        if (intent.getExtras() != null) {
            requestType = (RequestTypes) intent.getSerializableExtra(AlarmManagerWrapper.WAKE_REASON);
        }
        Log.d(TAG, "waking for request type: " + requestType.toString());

        switch (requestType) {
            case FIRST_WAKE:
                //reset some settings and schedule an immediate restart
                firstWake();
                break;
            case PROVIDER_CHANGED:
                //sync the calendar database and schedule an immediate restart
                databaseInterface.syncCalendarDb();
                notifyCursorAdapterDataChanged();
                scheduleServiceRestart();
                break;
            case SETTINGS_CHANGED:
                //the user might have selected different calendars or changed some other settings
                //update the database and restart
                databaseInterface.syncCalendarDb();
                notifyCursorAdapterDataChanged();
                scheduleServiceRestart();
                break;
            case QUICKSILENCE:
                // we should never get a quicksilence request with a zero or negative duration
                int duration = intent.getIntExtra("duration", -1);
                if (duration <= 0) {
                    Log.e(TAG, "got a quicksilence duration <= 0: " + duration + " this should not " +
                            "have happened");
                } else {
                    quickSilence(duration);
                }
                break;
            case CANCEL_QUICKSILENCE:
                cancelQuickSilence();
                break;
            case ACTIVITY_RESTART:
                scheduleServiceRestart();
                break;
            case NORMAL:
                syncDatabaseIfEmpty();
                if (prefs.isServiceActivated() && !stateManager.isQuickSilenceActive()) {
                    checkForActiveEventsAndSilence();
                } else { // normal wake requests (but service is marked to be inactive)
                    shutdownService();
                }
                break;
            default:
                Log.e(TAG, "got unknown request type, restarting normally");
                scheduleServiceRestart();
        }
    }

    /**
     * Sync the calendar database, set the service state to be inactive, then schedule an immediate
     * restart to look for any active events
     */
    private void firstWake() {
        databaseInterface.syncCalendarDb();
        notifyCursorAdapterDataChanged();
        // since the state is saved in a SharedPreferences file it can become out of sync
        // if the device restarts. To solve this set the service as not active, then schedule an
        // immediate restart to look for active events
        stateManager.setServiceState(ServiceStates.NOT_ACTIVE);
        alarmManager.scheduleImmediateAlarm(RequestTypes.NORMAL);
    }

    private void notifyCursorAdapterDataChanged() {
        DataSetManager dsm = new DataSetManager(this, getApplicationContext());
        dsm.broadcastDataSetChangedMessage();
    }

    private void scheduleServiceRestart() {
        // schedule an immediate wakeup only if we aren't in a quick silence period
        if (!stateManager.getServiceState().equals(ServiceStates.QUICKSILENCE)) {
            alarmManager.scheduleImmediateAlarm(RequestTypes.NORMAL);
        }
    }

    private void quickSilence(int durationMinutes) {
        // quick silence requests can occur during three states, either no event is active, an event
        // is active, or a previous quick silence request is still ongoing. If this request occurred
        // while no event was active, then save the current ringer state so it can be restored later
        ringerState.storeRingerStateIfNeeded();
        stateManager.setServiceState(ServiceStates.QUICKSILENCE);

        long wakeAt = System.currentTimeMillis() + (EventInstance.MILLISECONDS_IN_MINUTE
                * durationMinutes);
        alarmManager.scheduleCancelQuickSilenceAlarmAt(wakeAt);


        //quick silence requests are always explicitly request to silence the ringer
        ringerState.setPhoneRinger(RingerType.SILENT);
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

    private void syncDatabaseIfEmpty() {
        if (databaseInterface.isDatabaseEmpty()) {
            databaseInterface.syncCalendarDb();
            notifyCursorAdapterDataChanged();
        }
    }

    private void checkForActiveEventsAndSilence() {
        Deque<EventInstance> events = databaseInterface.getAllActiveEvents();
        boolean foundEvent = false;
        for (EventInstance event : events) {
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
                notifyCursorAdapterDataChanged();
            }

            //if no events are active, then we should sleep until the start of the next inactive
            //event, else sleep until the end of the last active event
            long wakeAt;

            if (!events.isEmpty()) {
                EventInstance lastActiveEvent = events.getLast();
                wakeAt = lastActiveEvent.getEnd();
            } else {
                EventInstance nextInactiveEvent = databaseInterface.nextEvent();
                if (nextInactiveEvent != null) {
                    wakeAt = nextInactiveEvent.getBegin() - (EventInstance.MILLISECONDS_IN_MINUTE * prefs.getBufferMinutes());
                } else {
                    wakeAt = System.currentTimeMillis() + EventInstance.MILLISECONDS_IN_DAY;

                }
            }

            alarmManager.scheduleNormalWakeAt(wakeAt);
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
        long[] pattern = {0, 500, 200, 500};
        vibrator.vibrate(pattern, -1);
    }

    private boolean shouldEventSilence(EventInstance event) {
        boolean eventShouldSilence = false;
        EventManager eventManger = new EventManager(this, event);
        if (eventManger.getBestRinger() != RingerType.IGNORE) {
            eventShouldSilence = true;
        }

        return eventShouldSilence;
    }

    private void silenceEventAndShowNotification(EventInstance event) {
        ringerState.storeRingerStateIfNeeded();

        EventManager eventManager = new EventManager(this, event);
        RingerType bestRinger = eventManager.getBestRinger();
        ringerState.setPhoneRinger(bestRinger);

        volumesManager.silenceMediaAndAlarmVolumesIfNeeded();
        //only vibrate if we are transitioning from no event active to an active event
        if (!stateManager.isEventActive()) {
            vibrate();
        }

        stateManager.setServiceState(ServiceStates.EVENT_ACTIVE);
        notificationManager.displayEventNotification(event);
        // the extra ten seconds is to make sure that the event ending is pushed into the
        // correct minute
        long wakeAt = event.getEnd()
                + (EventInstance.MILLISECONDS_IN_MINUTE * prefs.getBufferMinutes()) + TEN_SECONDS;
        alarmManager.scheduleNormalWakeAt(wakeAt);
        notifyCursorAdapterDataChanged();
    }

    private void shutdown() {
        stopSelf();
    }
}