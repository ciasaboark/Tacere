package org.ciasaboark.tacere.manager;

import org.ciasaboark.tacere.prefs.Prefs;

import android.content.Context;
import android.media.AudioManager;

public class VolumesManager {
	public static final int MAX_MEDIA_VOLUME = 15;
	public static final int MAX_ALARM_VOLUME = 7;
	
	private Context context;
	private Prefs prefs;
	private RingerStateManager ringerState;
	
	public VolumesManager(Context ctx) {
		this.context = ctx;
		this.prefs = new Prefs(ctx);
		this.ringerState = new RingerStateManager(ctx);
	}
	
	public int getMaxMediaVolume() {
		return MAX_MEDIA_VOLUME;
	}
	
	public int getMaxAlarmVolume() {
		return MAX_ALARM_VOLUME;
	}
	
	public void restoreVolumes() {
		AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		switch (ringerState.getStoredRingerState()) {
			case AudioManager.RINGER_MODE_NORMAL:
				audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
				break;
			case AudioManager.RINGER_MODE_SILENT:
				audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
				break;
			case AudioManager.RINGER_MODE_VIBRATE:
				audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
				break;
			default:
				// by default the ringer will be set back to normal
				audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		}

		ringerState.clearStoredRingerState();

		if (prefs.getAdjustMedia()) {
			audio.setStreamVolume(AudioManager.STREAM_MUSIC, prefs.getDefaultMediaVolume(), 0);
		}

		if (prefs.getAdjustAlarm()) {
			audio.setStreamVolume(AudioManager.STREAM_ALARM, prefs.getDefaultAlarmVolume(), 0);
		}
	}

	

	public void adjustMediaAndAlarmVolumes() {
		// change media volume, and alarm volume
		AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if (prefs.getAdjustMedia()) {
			audio.setStreamVolume(AudioManager.STREAM_MUSIC, prefs.getCurMediaVolume(), 0);
		}

		if (prefs.getAdjustAlarm()) {
			audio.setStreamVolume(AudioManager.STREAM_ALARM, prefs.getCurAlarmVolume(), 0);
		}
	}
}
