/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.event;

import org.ciasaboark.tacere.event.ringer.RingerType;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class EventInstance {
    public static final long MILLISECONDS_IN_SECOND = 1000;
    public static final long MILLISECONDS_IN_MINUTE = MILLISECONDS_IN_SECOND * 60;
    public static final long MILLISECONDS_IN_DAY = MILLISECONDS_IN_MINUTE * 60 * 24;
    @SuppressWarnings("unused")
    private static final String TAG = "CalEvent";
    private long calendarId;
    private int instanceId;     //instance ids are unique in repeating events
    private int eventId;        //event ids are shared among repeating events
    private String title;
    private long begin;
    private long end; // in milliseconds from epoch
    private String description;
    private RingerType ringer;
    private boolean hasCustomRinger;
    private int dispColor;
    private boolean isFreeTime;
    private boolean isAllDay;
    private HashMap<String, String> extraInfo = new HashMap<String, String>();


    public EventInstance(long calendarId, int instanceId, int eventId, String title, long begin, long end,
                         String description, int displayColor, boolean isFreeTime, boolean isAllDay) {
        this.calendarId = calendarId;
        this.instanceId = instanceId;
        this.eventId = eventId;
        this.title = title;
        this.begin = begin;
        this.end = end;
        this.description = description;
        this.dispColor = displayColor;
        this.isFreeTime = isFreeTime;
        this.isAllDay = isAllDay;
        this.ringer = RingerType.UNDEFINED;
    }

    public void putExtraInfo(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("key and value must not be null");
        }
        extraInfo.put(key, value);
    }

    public String getExtraInfo(String key) {
        String value = extraInfo.get(key);
        return value == null ? "" : value;
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

    public String getLocalBeginTime() {
        DateFormat dateFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);
        Date date = new Date(begin);
        return dateFormatter.format(date);
    }

    public String getLocalEndTime() {
        DateFormat dateFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);
        Date date = new Date(end);
        return dateFormatter.format(date);
    }

    public long getCalendarId() {
        return calendarId;
    }

    public int getEventId() {
        return eventId;
    }

    public String getLocalBeginDate() {
        DateFormat dateFormatter = DateFormat.getDateInstance();
        Date date;

        // according to the android calendar all day events start at
        // + 8 PM the day before the event is scheduled. This can
        // + result in a wrong date being returned.
        if (this.isAllDay) {
            // shift ahead by one full day
            date = new Date(begin + MILLISECONDS_IN_DAY);
        } else {
            date = new Date(begin);
        }

        return dateFormatter.format(date);
    }

    public String getLocalEndDate() {
        DateFormat dateFormatter = DateFormat.getDateInstance();
        Date date = new Date(end);
        return dateFormatter.format(date);
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
        if (this.getEnd() > startTime) {
            if (this.getBegin() < endTime) {
                isEventActive = true;
            }
        }
        return isEventActive;
    }

    public Long getEnd() {
        return end;
    }

    public Long getBegin() {
        return begin;
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

        return getId().equals(((EventInstance) o).getId());
    }

    public String toString() {
        DateFormat dateFormatter;
        dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                Locale.getDefault());
        Date date = new Date(end);
        String fdate = dateFormatter.format(date);
        return title + ", ends " + fdate;
    }

    public Integer getId() {
        return instanceId;
    }
}
