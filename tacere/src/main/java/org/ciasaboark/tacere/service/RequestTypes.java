/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */


package org.ciasaboark.tacere.service;

public enum RequestTypes {
    NORMAL,
    QUICKSILENCE,
    CANCEL_QUICKSILENCE,
    FIRST_WAKE,
    ACTIVITY_RESTART,
    PROVIDER_CHANGED,
    SETTINGS_CHANGED,
}