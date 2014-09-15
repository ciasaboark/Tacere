/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.manager;

import org.ciasaboark.tacere.database.EventInstance;

public class ActiveEventManager {
    private static EventInstance activeEvent = null;

    private ActiveEventManager() {
        // ♫ can't touch this ♫
    }

    public static void setActiveEvent(EventInstance e) {
        activeEvent = e;
    }

    public static boolean isActiveEvent(EventInstance e) {
        if (activeEvent == null || e == null) {
            return false;
        }

        return e.equals(activeEvent);
    }

    public static void removeActiveEvent() {
        activeEvent = null;
    }
}
