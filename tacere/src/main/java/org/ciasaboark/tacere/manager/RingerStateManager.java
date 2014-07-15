/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */


package org.ciasaboark.tacere.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;

import org.ciasaboark.tacere.activity.CalEvent;
import org.ciasaboark.tacere.prefs.Prefs;

public class RingerStateManager {
    private final Context context;
    private final Prefs prefs;

    public RingerStateManager(Context ctx) {
        this.context = ctx;
        this.prefs = new Prefs(ctx);
    }

    /**
     * Store the current ringer state (vibrate, silent, or normal). Ringer state is stored into
     * shared preferences
     */
    private void storeRingerState() {
        // TODO this may return a state indicating that a call is ongoing, this should stop
        // processing and wait for the call to end before adjusting volumes
        int curRinger = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE))
                .getRingerMode();
        prefs.storePreference("curRinger", curRinger);
    }

    /**
     * Remove stored ringer state from preferences
     */
    public void clearStoredRingerState() {
        prefs.remove("curRinger");
    }

    /**
     * Retrieves the current stored phone ringer from preferences
     *
     * @return the stored ringer state. Returned value can be compared to CalEvent.RINGER types
     */
    public int getStoredRingerState() {
        SharedPreferences preferences = context.getSharedPreferences(
                "org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
        return preferences.getInt("curRinger", CalEvent.RINGER.UNDEFINED);
    }

    public void setPhoneRinger(int ringerType) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        switch (ringerType) {
            case CalEvent.RINGER.VIBRATE:
                audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                break;
            case CalEvent.RINGER.SILENT:
                audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                break;
            case CalEvent.RINGER.IGNORE:
                //ignore this event
                break;
            default:
                audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;

        }
    }

    public void storeRingerStateIfNeeded() {
        // only store the current ringer state if we are not transitioning from one event to the
        // next and we are not in a quick silence period
        ServiceStateManager stateManager = new ServiceStateManager(context);
        if (stateManager.getServiceState().equals(ServiceStateManager.ServiceStates.NOT_ACTIVE)) {
            storeRingerState();
        }
    }

    public void restorePhoneRinger() {
        int storedRinger = getStoredRingerState();
        if (storedRinger == CalEvent.RINGER.UNDEFINED) {
            storedRinger = CalEvent.RINGER.NORMAL;
        }
        setPhoneRinger(storedRinger);
    }
}
