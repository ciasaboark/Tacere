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
    private static final String TAG = "StateManager";
    private final Prefs prefs;
    private EventInstance activeEvent = null;

    public ServiceStateManager(Context ctx) {
        this.prefs = new Prefs(ctx);
    }

    public void resetServiceState() {
        activeEvent = null;
        setServiceState(ServiceStates.NOT_ACTIVE);
    }

    public void setQuickSilenceActive() throws IllegalStateException {
        if (isEventActive()) {
            throw new IllegalArgumentException("Can not transition to quicksilence while an event is still active");
        }
        activeEvent = null;
        setServiceState(ServiceStates.QUICKSILENCE);
    }

    public boolean isEventActive() {
        return activeEvent == null &&
                ServiceStates.EVENT_ACTIVE.equals(getServiceState());
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

    public void setEventActive(EventInstance event) throws IllegalStateException {
        if (event == null) {
            throw new IllegalArgumentException("event can not be null");
        }
        if (isQuicksilenceActive()) {
            throw new IllegalStateException("An event can not become active while quicksilence is still active");
        }

        activeEvent = event;
        setServiceState(ServiceStates.EVENT_ACTIVE);
    }

    public boolean isQuicksilenceActive() {
        return ServiceStates.QUICKSILENCE.equals(getServiceState());
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

    public class ServiceStates {
        public static final String QUICKSILENCE = "quickSilence";
        public static final String EVENT_ACTIVE = "active";
        public static final String NOT_ACTIVE = "notActive";
    }
}
