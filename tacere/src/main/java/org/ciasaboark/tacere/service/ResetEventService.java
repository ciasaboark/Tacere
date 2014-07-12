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

import org.ciasaboark.tacere.activity.CalEvent;
import org.ciasaboark.tacere.database.DatabaseInterface;

public class ResetEventService extends IntentService {
    private static final String TAG = "ResetEventService";
    private static final int BOGUS_EVENT_ID = -1;

    public ResetEventService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "waking");
        Context ctx = getApplicationContext();

        if (intent.getExtras() != null) {
            Bundle b = intent.getExtras();
            int eventId = b.getInt("org.ciasaboark.tacere.eventId", BOGUS_EVENT_ID);
            if (eventId != BOGUS_EVENT_ID) {
                DatabaseInterface dbIface = DatabaseInterface.getInstance(ctx);
                dbIface.setRingerType(eventId, CalEvent.RINGER.UNDEFINED);

                Intent i = new Intent(this, EventSilencerService.class);
                i.putExtra("type", "activityRestart");
                startService(i);
            }
        }

    }
}