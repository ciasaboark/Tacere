/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.converter;

import java.util.Date;

/**
 * Created by Jonathan Nelson on 11/28/14.
 */
public class TimeConverter {
    private static final String TAG = "TimeConverter";
    private final long TIME_STAMP;

    public TimeConverter(long epochTime) {
        TIME_STAMP = epochTime;
    }

    public String getEndTime() {
        Date date = new Date(TIME_STAMP);
        return null;
    }
}
