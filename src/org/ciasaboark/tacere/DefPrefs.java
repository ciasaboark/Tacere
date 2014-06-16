/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere;

/**
 * Holds default preferences used to control the service and to populate the preferences activities.
 * @author Jonathan Nelson <ciasaboark@gmail.com>
 *
 */
public class DefPrefs {
	//whether or not the service should run
	public static final Boolean IS_ACTIVATED = true;
	//events marked as free time (or available) will be silenced
	public static final Boolean SILENCE_FREE_TIME = true;		
	//events marked as all day will be silenced
	public static final Boolean SILENCE_ALL_DAY = false;		
	//1: normal mode, 2: vibrate, 3: silent
	public static final int 	RINGER_TYPE = 3;				
	//change the media volume during an event
	public static final Boolean ADJUST_MEDIA = false;			
	//change the alarm volume during an event
	public static final Boolean ADJUST_ALARM = false;			
	//TODO max volumes were grabbed from an AOSP ROM, test with non-standard ROMs
	//max media volume reported by AlarmManager.getStreamMaxVolume
	public static final int 	MEDIA_VOLUME_MAX = 15;			
	//max alarm volume reported by AlarmManager.getStreamMaxVolume
	public static final int 	ALARM_VOLUME_MAX = 7;			
	public static final int 	MEDIA_VOLUME = 10;
	public static final int 	ALARM_VOLUME = 6;
	public static final int 	QUICK_SILENCE_MINUTES = 30;
	public static final int 	QUICK_SILENCE_HOURS = 0;
	@Deprecated
	//how often to refresh the service (deprecated since the change to using intents)
	public static final int 	REFRESH_INTERVAL = 5;			
	//how far in advance to begin silencing and continue silencing after event is over
	public static final int 	BUFFER_MINUTES = 5;				 
	@Deprecated
	public static final Boolean WAKE_DEVICE = false;			
	//an id to reference all notifications
	public static final int 	NOTIFICATION_ID = 1;			
	//how many days to merge when a calendar change is detected
	public static final int		LOOKAHEAD_DAYS = 7;				
	//when the app is updated an activity listing all changes is displayed,
	//by default this dialog will be displayed every time the app is started
	public static final boolean UPDATES_CHECKBOX = true;
	//the release version of this app
	//TODO the release version needs to be updated within assets/about.html and the manifest,
	//this should be automatic 
	public static final String  UPDATES_VERSION = "2.0.4";
	//after the donate thank you activity is displayed this will be changed to false
	public static final boolean SHOW_DONATION_THANKS = true;
	
	//requestCodes for the different pending intents
	public static final int		RC_EVENT		= 1;
	public static final int		RC_QUICKSILENT 	= 2;
	public static final int		RC_NOTIFICATION = 3;
}
