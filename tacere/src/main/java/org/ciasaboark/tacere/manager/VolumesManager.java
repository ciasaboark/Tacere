/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.manager;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import org.ciasaboark.tacere.prefs.Prefs;

public class VolumesManager {
    private static final int MAX_MEDIA_VOLUME = 15;
    private static final int MAX_ALARM_VOLUME = 7;
    private static final String TAG = "VolumesManager";

    private Context context;
    private Prefs prefs;

    public VolumesManager(Context ctx) {
        this.context = ctx;
        this.prefs = new Prefs(ctx);
    }

    public static int getMaxMediaVolume() {
        return MAX_MEDIA_VOLUME;
    }

    public static int getMaxAlarmVolume() {
        return MAX_ALARM_VOLUME;
    }


    public void restoreVolumes() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        restoreAlarmVolume();
        restoreMediaVolume();
        clearStoredVolumes();
    }

    private void restoreAlarmVolume() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int storedAlarmVolume = prefs.getStoredAlarmVolume();
        if (!hasAlarmVolumeBeenStored()) {
            Log.w(TAG, "asked to restore alarm volume when none has been previously stored");
        } else {
            audio.setStreamVolume(AudioManager.STREAM_ALARM, storedAlarmVolume, 0);
        }
    }

    private void restoreMediaVolume() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int storedMediaVolume = prefs.getStoredMediaVolume();
        if (!hasMediaVolumeBeenStored()) {
            Log.w(TAG, "asked to restore media volume when none has been previously stored");
        } else {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, storedMediaVolume, 0);
        }
    }

    public void clearStoredVolumes() {
        prefs.storeAlarmVolume(-1);
        prefs.storeMediaVolume(-1);
    }

    private boolean hasAlarmVolumeBeenStored() {
        return getStoredAlarmVolume() != -1;
    }

    private boolean hasMediaVolumeBeenStored() {
        return getStoredMediaVolume() != -1;
    }

    private int getStoredAlarmVolume() {
        return prefs.getStoredAlarmVolume();
    }

    private int getStoredMediaVolume() {
        return prefs.getStoredMediaVolume();
    }

    public void silenceMediaAndAlarmVolumesIfNeeded() {
        // change media volume, and alarm volume
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (prefs.shouldMediaVolumeBeSilenced()) {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        } else {
            restoreMediaVolume();
        }

        if (prefs.shouldAlarmVolumeBeSilenced()) {
            audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
        } else {
            restoreAlarmVolume();
        }
    }

    public void storeVolumesIfNeeded() {
        //store the current media and alarm volumes if none are already set
        if (!hasMediaVolumeBeenStored() && prefs.shouldMediaVolumeBeSilenced()) {
            storeMediaVolume();
        }
        if (!hasAlarmVolumeBeenStored() && prefs.shouldAlarmVolumeBeSilenced()) {
            storeAlarmVolume();
        }
    }

    private void storeMediaVolume() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int mediaVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        prefs.storeMediaVolume(mediaVolume);
    }

    private void storeAlarmVolume() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int alarmVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM);
        prefs.storeAlarmVolume(alarmVolume);
    }
}
