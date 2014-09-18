/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.event;

import android.content.Context;

import org.ciasaboark.tacere.event.ringer.RingerType;
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

    public RingerType getBestRinger() {
        RingerType defaultRinger = prefs.getRingerType();
        RingerType eventRinger = prefs.getRingerForEventSeries(event.getEventId());
        RingerType calendarRinger = prefs.getRingerForCalendar(event.getCalendarId());

        RingerType bestRinger = defaultRinger;
        if (calendarRinger != RingerType.UNDEFINED) {
            bestRinger = calendarRinger;
        }

        if (eventRinger != RingerType.UNDEFINED) {
            bestRinger = eventRinger;
        }

        //if the user has selected to ignore all day events then the event should be ignored
        if (!prefs.shouldAllDayEventsSilence() && event.isAllDay()) {
            bestRinger = RingerType.IGNORE;
        }

        //if the user has selected to ignore available events then the event should be ignored
        //TODO all day events are by default marked as available, this could lead to unexpected
        //consequences if all day events have not also been marked to be ignored
        if (!prefs.shouldAvailableEventsSilence() && event.isFreeTime()) {
            bestRinger = RingerType.IGNORE;
        }

        //if the event has a specific ringer set then the above preferences should be ignored
        //for this particular instance
        if (event.getRingerType() != RingerType.UNDEFINED) {
            bestRinger = event.getRingerType();
        }

        return bestRinger;
    }
}
