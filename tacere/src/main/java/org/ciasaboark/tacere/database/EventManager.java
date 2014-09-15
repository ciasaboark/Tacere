/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.database;

import android.content.Context;

import org.ciasaboark.tacere.prefs.Prefs;

public class EventManager {
    private EventInstance event;
    private Prefs prefs;
    private Context context;

    public EventManager(Context ctx, EventInstance event) {
        if (ctx == null) {
            throw new IllegalArgumentException("Given context can not be null");
        }
        if (event == null) {
            throw new IllegalArgumentException("Given event can not be null");
        }

        this.context = ctx;
        this.event = event;
        prefs = new Prefs(ctx);
    }

    public int getBestRinger() {
        int defaultRinger = prefs.getRingerType();
        int eventRinger = prefs.getRingerForEventSeries(event.getEventId());
        int calendarRinger = prefs.getRingerForCalendar(event.getCalendarId());

        int bestRinger = defaultRinger;
        if (calendarRinger != EventInstance.RINGER.UNDEFINED) {
            bestRinger = calendarRinger;
        }

        if (eventRinger != EventInstance.RINGER.UNDEFINED) {
            bestRinger = eventRinger;
        }

        //if the user has selected to ignore all day events or available events, then the event should
        //be ignored
        if (!prefs.shouldAllDayEventsSilence() && event.isAllDay()) {
            bestRinger = EventInstance.RINGER.IGNORE;
        }

        if (!prefs.shouldAvailableEventsSilence() && event.isFreeTime()) {
            bestRinger = EventInstance.RINGER.IGNORE;
        }

        //if the event has a specific ringer set then the above preferences should be ignored
        if (event.getInstanceRinger() != EventInstance.RINGER.UNDEFINED) {
            bestRinger = event.getInstanceRinger();
        }

        return bestRinger;
    }
}
