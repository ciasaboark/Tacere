/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.prefs;

import org.ciasaboark.tacere.event.ringer.RingerType;

class DefaultPrefs {
    static final boolean DO_NOT_DISTURB = false;
    // whether or not the service should run
    static final boolean IS_SERVICE_ACTIVATED = true;
    // events marked as free time (or available) will be silenced
    static final boolean SILENCE_FREE_TIME_EVENTS = true;
    // events marked as all day will be silenced
    static final boolean SILENCE_ALL_DAY_EVENTS = false;
    // 1: normal mode, 2: vibrate, 3: silent
    static final RingerType RINGER_TYPE = RingerType.SILENT;
    // change the media volume during an event
    static final boolean ADJUST_MEDIA = false;
    // change the alarm volume during an event
    static final boolean ADJUST_ALARM = false;
    static final int MEDIA_VOLUME = 10;
    static final int ALARM_VOLUME = 6;
    static final int QUICK_SILENCE_MINUTES = 30;
    static final int QUICK_SILENCE_HOURS = 0;
    // how far in advance to begin silencing and continue silencing after event is over
    static final int BUFFER_MINUTES = 5;
    // how many days to merge when a calendar change is detected
    static final int LOOKAHEAD_DAYS = 7;

    //TODO work these in to replace adjusting media and alarm volume with complete silencing
    static final boolean SILENCE_MEDIA = false;
    static final boolean SILENCE_ALARM = false;
    static final boolean SYNC_ALL_CALENDARS = true;
}