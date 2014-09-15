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
import org.ciasaboark.tacere.database.EventInstance;

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
            int eventId = b.getInt("org.ciasaboark.tacere.eventId", -1);
            if (eventId != -1) {
                DatabaseInterface dbIface = DatabaseInterface.getInstance(ctx);
                dbIface.setRingerForInstance(eventId, EventInstance.RINGER.IGNORE);

                Intent i = new Intent(this, EventSilencerService.class);
                i.putExtra("type", "activityRestart");
                startService(i);
            }
        }

    }
}
