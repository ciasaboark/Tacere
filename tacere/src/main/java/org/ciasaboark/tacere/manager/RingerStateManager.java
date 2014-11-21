/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */


package org.ciasaboark.tacere.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;

import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.prefs.Prefs;

public class RingerStateManager {
    private final Context context;
    private final Prefs prefs;

    public RingerStateManager(Context ctx) {
        this.context = ctx;
        this.prefs = new Prefs(ctx);
    }

    public void storeRingerStateIfNeeded() {
        // only store the current ringer state if we are not transitioning from one event to the
        // next and we are not in a quick silence period
        ServiceStateManager stateManager = ServiceStateManager.getInstance(context);
        if (stateManager.isServiceNotActive()) {
            storeRingerState();
        }
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

    public void restorePhoneRinger() {
        RingerType storedRinger = getStoredRingerState();
        if (storedRinger == RingerType.UNDEFINED) {
            storedRinger = RingerType.NORMAL;
        }
        setPhoneRinger(storedRinger);
        clearStoredRingerState();
    }

    /**
     * Retrieves the current stored phone ringer from preferences
     *
     * @return the stored ringer state. Returned value can be compared to CalEvent.RINGER types
     */
    public RingerType getStoredRingerState() {
        SharedPreferences preferences = context.getSharedPreferences(
                "org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
        int ringerInt = preferences.getInt("curRinger", RingerType.UNDEFINED.value);
        RingerType ringer = RingerType.getTypeForInt(ringerInt);
        return ringer;
    }

    public void setPhoneRinger(RingerType ringerType) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        switch (ringerType) {
            case VIBRATE:
                audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                break;
            case SILENT:
                audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                break;
            case IGNORE:
                //ignore this event
                break;
            default:
                audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;

        }
    }

    /**
     * Remove stored ringer state from preferences
     */
    public void clearStoredRingerState() {
        prefs.remove("curRinger");
    }
}
