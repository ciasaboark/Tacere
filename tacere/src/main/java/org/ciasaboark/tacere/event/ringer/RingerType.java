/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.event.ringer;

public enum RingerType {
    NORMAL(1),
    VIBRATE(2),
    SILENT(3),
    IGNORE(4),
    UNDEFINED(5);
    public final int value;

    RingerType(int value) {
        this.value = value;
    }

    public static RingerType getTypeForInt(int ringerType) {
        RingerType ringer = null;
        for (RingerType value : RingerType.values()) {
            if (value.value == ringerType) {
                ringer = value;
            }
        }

        if (ringer == null) {
            throw new IllegalArgumentException("Unknown ringer type int value: " + ringerType);
        }

        return ringer;
    }

    public static String[] names() {
        RingerType[] states = values();
        String[] names = new String[states.length];

        for (int i = 0; i < states.length; i++) {
            names[i] = states[i].name();
        }

        return names;
    }

    public RingerType getNext() {
        return this.ordinal() < RingerType.values().length - 1
                ? RingerType.values()[this.ordinal() + 1]
                : RingerType.values()[0];
    }
}
