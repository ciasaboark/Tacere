/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.event;

import org.ciasaboark.tacere.event.ringer.RingerType;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class EventInstance {
    public static final long MILLISECONDS_IN_SECOND = 1000;
    public static final long MILLISECONDS_IN_MINUTE = MILLISECONDS_IN_SECOND * 60;
    public static final long MILLISECONDS_IN_DAY = MILLISECONDS_IN_MINUTE * 60 * 24;
    @SuppressWarnings("unused")
    private static final String TAG = "CalEvent";
    private long calendarId;
    private long instanceId;     //instance ids are unique in repeating events
    private long eventId;        //event ids are shared among repeating events
    private String title;
    private long begin;
    private long effectiveEnd; // original end + extendMinutes
    private String description;
    private RingerType ringer;
    private boolean hasCustomRinger;
    private int dispColor;
    private boolean isFreeTime;
    private boolean isAllDay;
    private String location;
    private int extendMinutes;
    private long originalEnd;


    public EventInstance(long calendarId, long instanceId, long eventId, String title, long begin, long originalEnd,
                         String description, int displayColor, boolean isFreeTime, boolean isAllDay, int extendMinutes) {
        if (begin < 0) {
            throw new IllegalArgumentException("date can not be negative");
        }
        if (originalEnd < 0) {
            throw new IllegalArgumentException("date can not be negative");
        }
        if (originalEnd < begin) {
            throw new IllegalArgumentException("event can not end before it begins");
        }

        this.calendarId = calendarId;
        this.instanceId = instanceId;
        this.eventId = eventId;
        this.title = title == null ? "" : title;
        this.begin = begin;
        this.originalEnd = originalEnd;
        this.description = description == null ? "" : description;
        this.dispColor = displayColor;
        this.isFreeTime = isFreeTime;
        this.isAllDay = isAllDay;
        this.ringer = RingerType.UNDEFINED;
        this.location = "";
        this.extendMinutes = extendMinutes;

        this.effectiveEnd = originalEnd + (long) extendMinutes * MILLISECONDS_IN_MINUTE;
    }

    public static EventInstance getBlankEvent() {
        return new EventInstance(-2, -2, -2, "", 0, 0, "", 0, false, false, 0);
    }

    public int getExtendMinutes() {
        return extendMinutes;
    }

    public void setExtendMinutes(int extendMinutes) {
        this.extendMinutes = extendMinutes;
        this.effectiveEnd = originalEnd + (long) extendMinutes * MILLISECONDS_IN_MINUTE;
    }

    public RingerType getRingerType() {
        return ringer == null ?
                RingerType.UNDEFINED :
                ringer;
    }

    public void setRingerType(RingerType newRinger) {
        if (newRinger == null) {
            throw new IllegalArgumentException("New ringer can not be null");
        }
        this.ringer = newRinger;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        if (location == null) {
            this.location = "";
        } else {
            this.location = location;
        }
    }

    public String getLocalBeginTime() {
        return getFormattedTime(this.getBegin());
    }

    public String getFormattedTime(long time) {
        DateFormat dateFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);
        Date date = new Date(time);
        return dateFormatter.format(date);
    }

    public Long getBegin() {
        return begin;
    }

    public String getLocalEndTime() {
        return getFormattedTime(this.getOriginalEnd());
    }

    public Long getOriginalEnd() {
        return originalEnd;
    }

    public String getLocalEffectiveEndTime() {
        return getFormattedTime(this.getEffectiveEnd());
    }

    public long getEffectiveEnd() {
        return effectiveEnd;
    }

    public long getCalendarId() {
        return calendarId;
    }

    public long getEventId() {
        return eventId;
    }

    public String getLocalBeginDate() {
        long time;
        // according to the android calendar all day events start at
        // + 8 PM the day before the event is scheduled. This can
        // + result in a wrong date being returned.
        if (this.isAllDay) {
            // shift ahead by one full day
            time = getBegin() + MILLISECONDS_IN_DAY;
        } else {
            time = getBegin();
        }

        return getFormattedDate(time);
    }

    public String getFormattedDate(long time) {
        DateFormat dateFormatter = DateFormat.getDateInstance();
        Date date;
        date = new Date(time);
        return dateFormatter.format(date);
    }

    public String getLocalEndDate() {
        return getFormattedDate(getOriginalEnd());
    }

    public String getLocalEffectiveEndDate() {
        return getFormattedDate(getEffectiveEnd());
    }

    /**
     * Check if this CalEvent is ongoing between the given times
     *
     * @param startTime
     * @param endTime
     * @return true if this event is ongoing between the startTime and endTime, false otherwise
     */
    public boolean isActiveBetween(long startTime, long endTime) {
        boolean isEventActive = false;
        if (this.getEffectiveEnd() > startTime) {
            if (this.getBegin() < endTime) {
                isEventActive = true;
            }
        }
        return isEventActive;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDisplayColor() {
        return dispColor;
    }

    public Boolean isAllDay() {
        return isAllDay;
    }

    public Boolean isFreeTime() {
        return isFreeTime;
    }

    public String getTitle() {
        String result;
        if (title == null || title.equals("")) {
            result = "(No title)";
        } else {
            result = title;
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EventInstance)) {
            return false;
        }

        return getId() == ((EventInstance) o).getId();
    }

    public String toString() {
        DateFormat dateFormatter;
        dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                Locale.getDefault());
        Date date = new Date(effectiveEnd);
        String fdate = dateFormatter.format(date);
        return title + ", ends " + fdate;
    }

    public long getId() {
        return instanceId;
    }
}
