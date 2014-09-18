/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.manager;

import android.content.Context;
import android.media.AudioManager;

import org.ciasaboark.tacere.prefs.Prefs;

public class VolumesManager {
    private static final int MAX_MEDIA_VOLUME = 15;
    private static final int MAX_ALARM_VOLUME = 7;

    private Context context;
    private Prefs prefs;
    private RingerStateManager ringerState;

    public VolumesManager(Context ctx) {
        this.context = ctx;
        this.prefs = new Prefs(ctx);
        this.ringerState = new RingerStateManager(ctx);
    }

    public static int getMaxMediaVolume() {
        return MAX_MEDIA_VOLUME;
    }

    public static int getMaxAlarmVolume() {
        return MAX_ALARM_VOLUME;
    }


    public void restoreVolumes() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        switch (ringerState.getStoredRingerState()) {
            case NORMAL:
                audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;
            case SILENT:
                audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                break;
            case VIBRATE:
                audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                break;
            default:
                // by default the ringer will be set back to normal
                audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }

        ringerState.clearStoredRingerState();
        restoreAlarmVolume();
        restoreMediaVolume();
    }

    private void restoreAlarmVolume() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int storedAlarmVolume = prefs.getStoredAlarmVolume();
        audio.setStreamVolume(AudioManager.STREAM_ALARM, storedAlarmVolume, 0);
    }

    private void restoreMediaVolume() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int storedMediaVolume = prefs.getStoredMediaVolume();
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, storedMediaVolume, 0);

    }

    public void silenceMediaAndAlarmVolumesIfNeeded() {
        // change media volume, and alarm volume
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (prefs.shouldMediaVolumeBeSilenced()) {
            storeMediaVolume();
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        }

        if (prefs.shouldAlarmVolumeBeSilenced()) {
            storeAlarmVolume();
            audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
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
