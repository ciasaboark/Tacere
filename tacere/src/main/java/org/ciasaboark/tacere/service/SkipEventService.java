/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.manager.AlarmManagerWrapper;

public class SkipEventService extends IntentService {
    private static final String TAG = "SkipEventService";

    public SkipEventService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "waking");
        Context ctx = getApplicationContext();

        if (intent.getExtras() != null) {
            Bundle b = intent.getExtras();
            long eventId = b.getLong("org.ciasaboark.tacere.eventId", -1);
            if (eventId != -1) {
                DatabaseInterface dbIface = DatabaseInterface.getInstance(ctx);
                dbIface.setRingerForInstance(eventId, RingerType.IGNORE);

                AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(ctx);
                alarmManagerWrapper.scheduleImmediateAlarm(RequestTypes.NORMAL);
            }
        }

    }
}
