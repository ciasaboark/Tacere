/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere;

public class DefPrefs {
	public static final Boolean isActivated = true;
	public static final Boolean silenceFreeTime = true;
	public static final Boolean silenceAllDay = false;
	public static final int 	ringerType = 3;				//1: normal mode, 2: vibrate, 3: silent
	public static final Boolean adjustMedia = false;
	public static final Boolean adjustAlarm = false;
	public static final int 	mediaVolumeMax = 15;		//max media volume reported by AlarmManager.getStreamMaxVolume
	public static final int 	alarmVolumeMax = 7;		//max alarm volume reported by AlarmManager.getStreamMaxVolume
	public static final int 	mediaVolume = 10;
	public static final int 	alarmVolume = 6;
	public static final int 	quickSilenceMinutes = 30;
	public static final int 	quickSilenceHours = 0;
	public static final int 	refreshInterval = 5;
	public static final int 	bufferMinutes = 5;
	public static final Boolean wakeDevice = false;
	public static final int 	NOTIFICATION_ID = 1;			//an id to reference all notifications
	public static final int		lookaheadDays = 7;
}
