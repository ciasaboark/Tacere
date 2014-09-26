/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.event.ringer;

public enum RingerType {
    UNDEFINED(0),
    NORMAL(1),
    VIBRATE(2),
    SILENT(3),
    IGNORE(4);

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

    public static int getIntForStringValue(String type) {
        if (type == null) {
            throw new IllegalArgumentException("type can not be null");
        }
        type = type.toUpperCase();
        Integer intValue = null;
        //no switching on strings in java 6
        if (type.equals(UNDEFINED.toString().toUpperCase())) {
            intValue = UNDEFINED.value;
        } else if (type.equals(NORMAL.toString().toUpperCase())) {
            intValue = NORMAL.value;
        } else if (type.equals(VIBRATE.toString().toUpperCase())) {
            intValue = VIBRATE.value;
        } else if (type.equals(SILENT.toString().toUpperCase())) {
            intValue = SILENT.value;
        } else if (type.equals(IGNORE.toString().toUpperCase())) {
            intValue = IGNORE.value;
        }

        if (intValue == null) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }

        return intValue;
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
        RingerType nextType = this.ordinal() < RingerType.values().length - 1
                ? RingerType.values()[this.ordinal() + 1]
                : RingerType.values()[0];
        //the UNDEFINED type is omitted from the cycle
        return nextType == UNDEFINED ? NORMAL : nextType;
    }
}
