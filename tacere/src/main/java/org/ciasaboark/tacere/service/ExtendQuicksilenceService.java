/*
 * Copyright (c) 2015 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.ciasaboark.tacere.event.EventInstance;

public class ExtendQuicksilenceService extends IntentService {
    public static final String ORIGINAL_END_TIMESTAMP = "originalIssue";
    public static final String EXTEND_LENGTH = "extendLength";
    private static final String TAG = "ExtendQuicksilenceService";

    public ExtendQuicksilenceService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "waking");
        Context ctx = getApplicationContext();

        if (intent.getExtras() != null) {
            Bundle b = intent.getExtras();
            long originalTimestamp = b.getLong(ORIGINAL_END_TIMESTAMP, -1);
            int extendLength = b.getInt(EXTEND_LENGTH, -1);
            if (originalTimestamp == -1 || extendLength == -1) {
                Log.e(TAG, "received a malformed request, one of ORIGINAL_ISSUE_TIME, or " +
                        "NEW_EXTEND_LENGTH was not supplied");
            } else {
                long newEndTime = originalTimestamp +
                        ((long) extendLength * EventInstance.MILLISECONDS_IN_MINUTE);
                long newDurationMs = newEndTime - System.currentTimeMillis();
                double newDurationMinutesUnrounded = (double) newDurationMs / (double) EventInstance.MILLISECONDS_IN_MINUTE;
                int newDurationMinutes = (int) Math.round(newDurationMinutesUnrounded);
                Intent i = new Intent(this, EventSilencerService.class);
                i.putExtra(EventSilencerService.WAKE_REASON, RequestTypes.QUICKSILENCE.value);
                i.putExtra(EventSilencerService.QUICKSILENCE_DURATION, newDurationMinutes);
                this.startService(i);
            }
        }
    }
}
