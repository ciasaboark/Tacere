/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.ciasaboark.tacere.manager.AlarmManagerWrapper;
import org.ciasaboark.tacere.service.RequestTypes;

public class ProviderChangedReceiver extends BroadcastReceiver {
    private static final String TAG = "ProviderChangedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "received broadcast intent, waking service");
        AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(context);
        alarmManagerWrapper.scheduleImmediateAlarm(RequestTypes.PROVIDER_CHANGED);
    }
}
