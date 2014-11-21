/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.event.ringer;

public enum Intervals {
    WEEK(7, "One week", "week"),
    TWO_WEEKS(14, "Two weeks", "two weeks"),
    MONTH(31, "One Month", "month"),
    THREE_MONTHS(91, "Three Months", "three months");

    public final int value;
    public final String string;
    public final String injectString;

    Intervals(int value, String string, String injectString) {
        this.value = value;
        this.string = string;
        this.injectString = injectString;
    }

    public static Intervals getTypeForInt(int intervalDuration) {
        Intervals interval = null;
        for (Intervals value : Intervals.values()) {
            if (value.value == intervalDuration) {
                interval = value;
            }
        }

        if (interval == null) {
            throw new IllegalArgumentException("Unknown interval for duration: " + intervalDuration);
        }

        return interval;
    }

    public static int getIntForStringValue(String type) {
        if (type == null) {
            throw new IllegalArgumentException("type can not be null");
        }
        type = type.toUpperCase();
        Integer intValue = null;
        //no switching on strings in java 6
        if (type.equals(WEEK.string.toUpperCase())) {
            intValue = WEEK.value;
        } else if (type.equals(TWO_WEEKS.string.toUpperCase())) {
            intValue = TWO_WEEKS.value;
        } else if (type.equals(MONTH.string.toUpperCase())) {
            intValue = MONTH.value;
        } else if (type.equals(THREE_MONTHS.string.toUpperCase())) {
            intValue = THREE_MONTHS.value;
        }

        if (intValue == null) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }

        return intValue;
    }

    public static String[] names() {
        Intervals[] intervals = values();
        String[] names = new String[intervals.length];

        for (int i = 0; i < intervals.length; i++) {
            names[i] = intervals[i].string;
        }

        return names;
    }

    public Intervals getNext() {
        Intervals nextType = this.ordinal() < Intervals.values().length - 1
                ? Intervals.values()[this.ordinal() + 1]
                : Intervals.values()[0];
        //the UNDEFINED type is omitted from the cycle
        return nextType;
    }
}

