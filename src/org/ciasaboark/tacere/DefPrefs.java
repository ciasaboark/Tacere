/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere;

public class DefPrefs {
	public static final Boolean IS_ACTIVATED = true;
	public static final Boolean SILENCE_FREE_TIME = true;
	public static final Boolean SILENCE_ALL_DAY = false;
	public static final int 	RINGER_TYPE = 3;				//1: normal mode, 2: vibrate, 3: silent
	public static final Boolean ADJUST_MEDIA = false;
	public static final Boolean ADJUST_ALARM = false;
	public static final int 	MEDIA_VOLUME_MAX = 15;		//max media volume reported by AlarmManager.getStreamMaxVolume
	public static final int 	ALARM_VOLUME_MAX = 7;		//max alarm volume reported by AlarmManager.getStreamMaxVolume
	public static final int 	MEDIA_VOLUME = 10;
	public static final int 	ALARM_VOLUME = 6;
	public static final int 	QUICK_SILENCE_MINUTES = 30;
	public static final int 	QUICK_SILENCE_HOURS = 0;
	public static final int 	REFRESH_INTERVAL = 5;
	public static final int 	BUFFER_MINUTES = 5;
	//public static final Boolean WAKE_DEVICE = false;
	public static final int 	NOTIFICATION_ID = 1;			//an id to reference all notifications
	public static final int		LOOKAHEAD_DAYS = 7;
}
