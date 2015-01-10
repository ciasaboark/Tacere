/*
 * Copyright (c) 2015 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */


package org.ciasaboark.tacere.service;

public enum RequestTypes {
    NORMAL(0),
    QUICKSILENCE(1),
    CANCEL_QUICKSILENCE(2),
    FIRST_WAKE(3),
    PROVIDER_CHANGED(4),
    SETTINGS_CHANGED(5);

    public final int value;

    RequestTypes(int val) {
        this.value = val;
    }

    public static RequestTypes getTypeForInt(int requestType) {
        RequestTypes ringer = null;
        for (RequestTypes value : RequestTypes.values()) {
            if (value.value == requestType) {
                ringer = value;
            }
        }

        if (ringer == null) {
            throw new IllegalArgumentException("Unknown request type int value: " + requestType);
        }

        return ringer;
    }
}