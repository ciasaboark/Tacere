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
import org.ciasaboark.tacere.database.NoSuchEventInstanceException;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.manager.AlarmManagerWrapper;

public class ExtendEventService extends IntentService {
    public static final String INSTANCE_ID = "instanceId";
    public static final String NEW_EXTEND_LENGTH = "extendLength";
    private static final String TAG = "ExtendEventService";

    public ExtendEventService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "waking");
        Context ctx = getApplicationContext();

        if (intent.getExtras() != null) {
            Bundle b = intent.getExtras();
            long instanceId = b.getLong(INSTANCE_ID, -1);
            int extendLength = b.getInt(NEW_EXTEND_LENGTH, -1);
            if (instanceId == -1 || extendLength == -1) {
                Log.e(TAG, "received a malformed request, one of INSTANCE_ID, or " +
                        "NEW_EXTEND_LENGTH was not supplied");
            } else {
                DatabaseInterface databaseInterface = DatabaseInterface.getInstance(this);
                try {
                    EventInstance eventInstance = databaseInterface.getEvent(instanceId);
                    eventInstance.setExtendMinutes(extendLength);
                    databaseInterface.insertEvent(eventInstance);

                    //wake the event silencer service
                    AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(this);
                    alarmManagerWrapper.scheduleImmediateAlarm(RequestTypes.NORMAL);
                } catch (NoSuchEventInstanceException e) {
                    Log.e(TAG, "unable to find event with instance id: " + instanceId +
                            " can not extend minutes");
                }
            }
        }
    }
}
