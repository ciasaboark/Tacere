package org.ciasaboark.tacere.manager;

import org.ciasaboark.tacere.prefs.Prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ServiceStateManager {
	@SuppressWarnings("unused")
	private static final String TAG = "StateManager";
	private static final String SERVICE_STATE_KEY = "serviceState";
	private Prefs prefs;

	public ServiceStateManager(Context ctx) {
		this.prefs = new Prefs(ctx);
	}

	public void setServiceState(String state) {
		if (!state.equals(ServiceStates.QUICKSILENCE) && !state.equals(ServiceStates.EVENT_ACTIVE)
				& !state.equals(ServiceStates.NOT_ACTIVE)) {
			throw new IllegalArgumentException("unknown state: " + state);
		}

		prefs.storePreference(SERVICE_STATE_KEY, state);
	}

    public boolean isEventActive() {
        return getServiceState().equals(ServiceStates.EVENT_ACTIVE);
    }

    public boolean isQuickSilenceActive() {
        return getServiceState().equals(ServiceStates.QUICKSILENCE);
    }

	public String getServiceState() {
		String currentState = tryReadServiceState();
        if (currentState == null) {
            currentState = ServiceStates.NOT_ACTIVE;
        }
        return currentState;
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

	public class ServiceStates {
		public static final String QUICKSILENCE = "quickSilence";
		public static final String EVENT_ACTIVE = "active";
		public static final String NOT_ACTIVE = "notActive";
	}
}