/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.database;

/**
 * Columns available within the database
 *
 * @author Jonathan Nelson <ciasaboark@gmail.com>
 */
public class Columns {
    public static final String _ID = "_id"; //primary key, tied to the event's instance id
    public static final String EVENT_ID = "event_id";
    public static final String TITLE = "title";
    public static final String CAL_ID = "cal_id";
    public static final String DESCRIPTION = "description";
    public static final String BEGIN = "start";
    public static final String END = "end";
    public static final String RINGER_TYPE = "ringer_type";
    public static final String DISPLAY_COLOR = "display_color";
    public static final String IS_ALLDAY = "is_allday";
    public static final String IS_FREETIME = "is_freetime";
    public static final String LOCATION = "location";
}