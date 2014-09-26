/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.event.ringer;

public enum RingerSource {
    DEFAULT(1),
    CALENDAR(2),
    EVENT_SERIES(3),
    INSTANCE(4),
    UNDEFINED(5);

    public final int value;

    RingerSource(int v) {
        value = v;
    }

    public static RingerType getTypeForInt(int ringerSource) {
        RingerType ringer = null;
        for (RingerType value : RingerType.values()) {
            if (value.value == ringerSource) {
                ringer = value;
            }
        }

        if (ringer == null) {
            throw new IllegalArgumentException("Unknown ringer source int value: " + ringerSource);
        }

        return ringer;
    }

    public static String[] names() {
        RingerSource[] sources = values();
        String[] names = new String[sources.length];

        for (int i = 0; i < sources.length; i++) {
            names[i] = sources[i].name();
        }

        return names;
    }
}
