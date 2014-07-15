/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class CalEvent {
    public static final long MILLISECONDS_IN_SECOND = 1000;
    public static final long MILLISECONDS_IN_MINUTE = MILLISECONDS_IN_SECOND * 60;
    public static final long MILLISECONDS_IN_DAY = MILLISECONDS_IN_MINUTE * 60 * 24;
    @SuppressWarnings("unused")
    private static final String TAG = "CalEvent";
    private Integer instanceId;
    private Integer eventId;
    private String title;
    private Long begin;
    private Long end; // in milliseconds from epoch
    private String descr;
    private int ringerType;
    private Integer dispColor;
    private Boolean isFreeTime;
    private Boolean isAllDay;

    public CalEvent() {
        super();
        this.instanceId = null;
        this.eventId = null;
        this.title = null;
        this.begin = null;
        this.end = null;
        this.descr = null;
        this.ringerType = CalEvent.RINGER.UNDEFINED;
        this.dispColor = null;
        this.isFreeTime = null;
        this.isAllDay = null;
    }

    public void setIsFreeTime(Boolean isFreeTime) {
        this.isFreeTime = isFreeTime;
    }

    public void setIsAllDay(Boolean isAllDay) {
        this.isAllDay = isAllDay;
    }

    public Integer getCal_id() {
        return eventId;
    }

    public void setCal_id(Integer cal_id) {
        this.eventId = cal_id;
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

    public String getLocalBeginDate() {
        DateFormat dateFormatter = DateFormat.getDateInstance();
        Date date;

        // according to the android calendar all day events start at
        // + 8 PM the day before the event is scheduled. This can
        // + result in a wrong date being returned.
        if (isAllDay) {
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

    public boolean isValid() {
        boolean result = false;
        if (title != null && instanceId != null && begin != null && end != null
                && isFreeTime != null && isAllDay != null) {
            result = true;
        }
        return result;
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

    public void setEnd(Long end) {
        this.end = end;
    }

    public Long getBegin() {
        return begin;
    }

    public void setBegin(Long begin) {
        this.begin = begin;
    }

    public void setValuesFrom(CalEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("given calendar event must not be null");
        }

        this.begin = event.getBegin();
        this.descr = event.getDescription();
        this.dispColor = event.getDisplayColor();
        this.end = event.getEnd();
        this.eventId = event.getId();
        this.instanceId = event.getInstanceId();
        this.isAllDay = event.isAllDay();
        this.isFreeTime = event.isFreeTime();
        this.ringerType = event.getRingerType();
        this.title = event.getTitle();
    }

    public String getDescription() {
        return descr;
    }

    public void setDescription(String description) {
        this.descr = description;
    }

    public Integer getDisplayColor() {
        return dispColor;
    }

    public void setDisplayColor(Integer displayColor) {
        this.dispColor = displayColor;
    }

    public Integer getId() {
        return instanceId;
    }

    public void setId(Integer id) {
        this.instanceId = id;
    }

    public Integer getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Integer instanceId) {
        this.instanceId = instanceId;
    }

    public Boolean isAllDay() {
        return isAllDay;
    }

    public Boolean isFreeTime() {
        return isFreeTime;
    }

    public Integer getRingerType() {
        Integer result = ringerType;
        if (result == null) {
            result = 0;
        }
        return result;
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

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CalEvent)) {
            return false;
        }

        return getId().equals(((CalEvent) o).getId())
                && getInstanceId().equals(((CalEvent) o).getInstanceId());
    }

    public class RINGER {
        public static final int UNDEFINED = 0;
        public static final int NORMAL = 1;
        public static final int VIBRATE = 2;
        public static final int SILENT = 3;
        public static final int IGNORE = 4;
    }
}
