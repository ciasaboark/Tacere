/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.manager;

import android.content.Context;

import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.prefs.Prefs;

public class ServiceStateManager {
    @SuppressWarnings("unused")
    public static final String SERVICE_STATE_KEY = "serviceState";
    private static final String SERVICE_STATE_TIMESTAMP_KEY = "serviceStateTimestampKey";
    private static final String TAG = "StateManager";
    private static ServiceStateManager instance = null;
    private static EventInstance activeEvent = null;
    private final Prefs prefs;

    private ServiceStateManager(Context ctx) {
        this.prefs = new Prefs(ctx);
    }

    public static ServiceStateManager getInstance(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Context can not be null");
        }

        if (instance == null) {
            instance = new ServiceStateManager(ctx);
        }

        return instance;
    }

    public void resetServiceState() {
        activeEvent = null;
        prefs.getBaseSharedPreferences().edit().remove(SERVICE_STATE_TIMESTAMP_KEY).commit();
        setServiceState(ServiceStates.NOT_ACTIVE);
    }

    public void setQuickSilenceActive(long endTimestamp) throws IllegalStateException {
        if (isEventActive()) {
            throw new IllegalArgumentException("Can not transition to quicksilence while an event is still active");
        }
        activeEvent = null;
        setServiceState(ServiceStates.QUICKSILENCE);
        storeEndTimeStamp(endTimestamp);
    }

    public boolean isEventActive() {
        return activeEvent != null &&
                ServiceStates.EVENT_ACTIVE.equals(getServiceState());
    }

    private void storeEndTimeStamp(long timestamp) {
        prefs.storePreference(SERVICE_STATE_TIMESTAMP_KEY, timestamp);
    }

    private String getServiceState() {
        String currentState = tryReadServiceState();
        if (currentState == null) {
            currentState = ServiceStates.NOT_ACTIVE;
        }
        return currentState;
    }

    private void setServiceState(String state) {
        if (!state.equals(ServiceStates.QUICKSILENCE) && !state.equals(ServiceStates.EVENT_ACTIVE)
                & !state.equals(ServiceStates.NOT_ACTIVE)) {
            throw new IllegalArgumentException("unknown state: " + state);
        }

        prefs.storePreference(SERVICE_STATE_KEY, state);
    }

    private String tryReadServiceState() {
        String storedString = null;
        try {
            storedString = prefs.readString(SERVICE_STATE_KEY);
        } catch (IllegalArgumentException e) {
            //the service state might not have been stored yet, this is fine
        }
        return storedString;
    }

    public long getEndTimeStamp() {
        long endTimeStamp = prefs.getBaseSharedPreferences().getLong(SERVICE_STATE_TIMESTAMP_KEY, 0);
        return endTimeStamp;
    }

    public boolean isEventNotActive() {
        return !isEventActive();
    }

    public boolean isServiceNotActive() {
        return !isServiceActive();
    }

    public boolean isServiceActive() {
        return ServiceStates.NOT_ACTIVE.equals(getServiceState());
    }

    public boolean isQuicksilenceNotActive() {
        return !isQuicksilenceActive();
    }

    public boolean isQuicksilenceActive() {
        return ServiceStates.QUICKSILENCE.equals(getServiceState());
    }

    public long getActiveEventId() {
        long id = -1;
        if (activeEvent != null) {
            id = activeEvent.getId();
        }
        return id;
    }

    public EventInstance getActiveEvent() {
        return activeEvent;
    }

    public void setActiveEvent(EventInstance event) throws IllegalStateException {
        if (event == null) {
            throw new IllegalArgumentException("event can not be null");
        }
        if (isQuicksilenceActive()) {
            throw new IllegalStateException("An event can not become active while quicksilence is still active");
        }

        activeEvent = event;
        prefs.storePreference(SERVICE_STATE_TIMESTAMP_KEY, event.getEffectiveEnd());
        setServiceState(ServiceStates.EVENT_ACTIVE);
    }

    public class ServiceStates {
        public static final String QUICKSILENCE = "quickSilence";
        public static final String EVENT_ACTIVE = "active";
        public static final String NOT_ACTIVE = "notActive";
    }
}
