/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.manager;

import org.ciasaboark.tacere.database.SimpleCalendarEvent;

public class ActiveEventManager {
    private static SimpleCalendarEvent activeEvent = null;

    private ActiveEventManager() {
        // ♫ can't touch this ♫
    }

    public static void setActiveEvent(SimpleCalendarEvent e) {
        activeEvent = e;
    }

    public static boolean isActiveEvent(SimpleCalendarEvent e) {
        if (activeEvent == null || e == null) {
            return false;
        }

        return e.equals(activeEvent);
    }

    public static void removeActiveEvent() {
        activeEvent = null;
    }
}
