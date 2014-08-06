/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.database;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class SimpleCalendarEvent {
    public static final long MILLISECONDS_IN_SECOND = 1000;
    public static final long MILLISECONDS_IN_MINUTE = MILLISECONDS_IN_SECOND * 60;
    public static final long MILLISECONDS_IN_DAY = MILLISECONDS_IN_MINUTE * 60 * 24;
    @SuppressWarnings("unused")
    private static final String TAG = "CalEvent";
    private int calendarId;
    private int instanceId;     //instance ids are unique in repeating events
    private int eventId;        //event ids are shared among repeating events
    private String title;
    private long begin;
    private long end; // in milliseconds from epoch
    private String description;
    private int ringerType;
    private int dispColor;
    private boolean isFreeTime;
    private boolean isAllDay;


    public SimpleCalendarEvent(int calendarId, int instanceId, int eventId, String title, long begin, long end,
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
        this.ringerType = RINGER.UNDEFINED;
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

    public int getCalendarId() {
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

    public String toString() {
        DateFormat dateFormatter;
        dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                Locale.getDefault());
        Date date = new Date(end);
        String fdate = dateFormatter.format(date);
        return title + ", ends " + fdate;
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

    public Integer getId() {
        return instanceId;
    }


    public Boolean isAllDay() {
        return isAllDay;
    }

    public Boolean isFreeTime() {
        return isFreeTime;
    }

    public Integer getRingerType() {
        return ringerType;
    }

    public void setRingerType(Integer ringerType) {
        this.ringerType = ringerType;
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
        if (!(o instanceof SimpleCalendarEvent)) {
            return false;
        }

        return getId().equals(((SimpleCalendarEvent) o).getId());
    }

    public class RINGER {
        public static final int UNDEFINED = 0;
        public static final int NORMAL = 1;
        public static final int VIBRATE = 2;
        public static final int SILENT = 3;
        public static final int IGNORE = 4;
    }
}
