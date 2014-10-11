/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.ciasaboark.tacere.manager.AlarmManagerWrapper;
import org.ciasaboark.tacere.service.RequestTypes;

public class BootCompletedReceiver extends BroadcastReceiver {

    // private static final String TAG = "PollServiceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(context);
        alarmManagerWrapper.scheduleImmediateAlarm(RequestTypes.FIRST_WAKE);
    }

}
