/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.prefs;

class DefaultPrefs {
	// whether or not the service should run
	static final Boolean IS_ACTIVATED = true;
	// events marked as free time (or available) will be silenced
	static final Boolean SILENCE_FREE_TIME = true;
	// events marked as all day will be silenced
	static final Boolean SILENCE_ALL_DAY = false;
	// 1: normal mode, 2: vibrate, 3: silent
	static final int RINGER_TYPE = 3;
	// change the media volume during an event
	static final Boolean ADJUST_MEDIA = false;
	// change the alarm volume during an event
	static final Boolean ADJUST_ALARM = false;
	static final int MEDIA_VOLUME = 10;
	static final int ALARM_VOLUME = 6;
	static final int QUICK_SILENCE_MINUTES = 30;
	static final int QUICK_SILENCE_HOURS = 0;;
	// how far in advance to begin silencing and continue silencing after event is over
	static final int BUFFER_MINUTES = 5;
	
	// how many days to merge when a calendar change is detected
	static final int LOOKAHEAD_DAYS = 7;
	// when the app is updated an activity listing all changes is displayed,
	// by default this dialog will be displayed every time the app is started
	static final boolean UPDATES_CHECKBOX = true;
	// the release version of this app
	// TODO the release version needs to be updated within assets/about.html and the manifest,
	// this should be automatic
	// static final String UPDATES_VERSION = "2.0.4";
	// after the donate thank you activity is displayed this will be changed to false
	static final boolean SHOW_DONATION_THANKS = true;
	
}