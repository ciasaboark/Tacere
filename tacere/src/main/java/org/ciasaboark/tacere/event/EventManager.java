/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.event;

import android.content.Context;

import org.ciasaboark.tacere.event.ringer.RingerSource;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.prefs.Prefs;

public class EventManager {
    private final EventInstance event;
    private final Context context;
    private final Prefs prefs;
    private RingerType ringerType = null;
    private RingerSource ringerSource = null;
    private Boolean shouldEventByIgnoredByDefault = null;

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
        if (ringerType != null) {
            return ringerType;
        } else {
            RingerType defaultRinger = prefs.getRingerType();
            RingerType eventRinger = prefs.getRingerForEventSeries(event.getEventId());
            RingerType calendarRinger = prefs.getRingerForCalendar(event.getCalendarId());

            RingerType bestRinger = defaultRinger;
            if (calendarRinger != RingerType.UNDEFINED) {
                bestRinger = calendarRinger;
            }

            //if the user has selected to ignore all day events then the event should be ignored,
            //this will be overridden by either a event series ringer, or an instance specific ringer
            if (shouldEventBeIgnoredByDefault()) {
                bestRinger = RingerType.IGNORE;
            }

            if (eventRinger != RingerType.UNDEFINED) {
                bestRinger = eventRinger;
            }

            //if the event has a specific ringer set then the above preferences should be ignored
            //for this particular instance
            if (event.getRingerType() != RingerType.UNDEFINED) {
                bestRinger = event.getRingerType();
            }
            this.ringerType = bestRinger;
            return bestRinger;
        }
    }

    private boolean shouldEventBeIgnoredByDefault() {
        if (shouldEventByIgnoredByDefault != null) {
            return shouldEventByIgnoredByDefault;
        } else {
            boolean ignoreEvent = false;
            if (event.isAllDay()) {
                //if the event is all day then we don't need to consider whether or not it is available
                if (!prefs.shouldAllDayEventsSilence()) {
                    ignoreEvent = true;
                }
            } else if (event.isFreeTime()) {
                if (!prefs.shouldAvailableEventsSilence()) {
                    ignoreEvent = true;
                }
            } else {
                //if the event is neither an all day event nor marked as available then we do not ignore
                //by default
            }
            shouldEventByIgnoredByDefault = ignoreEvent;
            return ignoreEvent;
        }
    }

    public RingerSource getRingerSource() {
        if (ringerSource != null) {
            return ringerSource;
        } else {
            RingerSource source = RingerSource.DEFAULT;
            if (prefs.getRingerForCalendar(event.getCalendarId()) != RingerType.UNDEFINED) {
                //a calendar specific ringer has been set, but this should only be applied to events that
                //should not be ignored by default
                if (!shouldEventBeIgnoredByDefault()) {
                    source = RingerSource.CALENDAR;
                } else {
                    source = RingerSource.DEFAULT;
                }
            }

            if (prefs.getRingerForEventSeries(event.getEventId()) != RingerType.UNDEFINED) {
                source = RingerSource.EVENT_SERIES;
            }

            if (event.getRingerType() != RingerType.UNDEFINED) {
                source = RingerSource.INSTANCE;
            }
            this.ringerSource = source;
            return source;
        }
    }
}
