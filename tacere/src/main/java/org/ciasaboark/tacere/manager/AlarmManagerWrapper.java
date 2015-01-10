/*
 * Copyright (c) 2015 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.manager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import junit.framework.Assert;

import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;

public class AlarmManagerWrapper {
    private static final String TAG = "AlarmManagerWrapper";
    // requestCodes for the different pending intents
    private static final int RC_EVENT = 1;
    private static final int RC_QUICKSILENT = 2;
    private static final int RC_NOTIFICATION = 3;

    private final Context context;

    public AlarmManagerWrapper(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        this.context = ctx.getApplicationContext();
    }

    public void scheduleNormalWakeAt(long time) {
        scheduleAlarmAt(time, RequestTypes.NORMAL);
    }

    private void scheduleAlarmAt(long time, RequestTypes type) {
        scheduleAlarmAt(time, type, null);
    }

    private void scheduleAlarmAt(long time, RequestTypes type, Bundle additionalArgs) {
        Log.d(TAG, "alarm scheduled at " + time + " for " + type);
        Assert.assertNotNull(type);

        Intent i = new Intent(context, EventSilencerService.class);
        Bundle bundle;
        if (additionalArgs != null) {
            bundle = new Bundle(additionalArgs);
        } else {
            bundle = new Bundle();
        }

        bundle.putInt(EventSilencerService.WAKE_REASON, type.value);
        i.putExtras(bundle);

        // note that the android alarm manager allows multiple pending intents to be scheduled per
        // app but only if each intent has a unique request code. Since we want to schedule wakeups
        // for ending quicksilent durations as well as starting events we check the type and assign
        // a different requestCode
        // default to 0
        int requestCode = 0;
        if (type.equals(RequestTypes.CANCEL_QUICKSILENCE)) {
            requestCode = RC_QUICKSILENT;
        } else if (type.equals(RequestTypes.NORMAL)) {
            requestCode = RC_EVENT;
        }

        PendingIntent pintent = PendingIntent.getService(context, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, time, pintent);
    }

    public void scheduleImmediateQuicksilenceForDuration(int duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("duration must be positive int >= 1");
        }
        Bundle args = new Bundle();
        args.putInt(EventSilencerService.QUICKSILENCE_DURATION, duration);
        scheduleAlarmAt(System.currentTimeMillis(), RequestTypes.QUICKSILENCE, args);
    }

    public void scheduleImmediateAlarm(RequestTypes type) {
        if (type == null) {
            throw new IllegalArgumentException("request type can not be null");
        }
        scheduleAlarmAt(System.currentTimeMillis(), type);
    }

    public void scheduleCancelQuickSilenceAlarmAt(long time) {
        scheduleAlarmAt(time, RequestTypes.CANCEL_QUICKSILENCE);
    }

    public void scheduleActivityRestartWakeAt(long time) {
        scheduleAlarmAt(time, RequestTypes.NORMAL);
    }

    public void cancelAllAlarms() {
        // there could be multiple alarms scheduled, we have to cancel all of them
        for (int requestCode = 0; requestCode <= 4; requestCode++) {
            Intent i = new Intent(context, EventSilencerService.class);
            PendingIntent pintent = PendingIntent.getService(context, requestCode, i, 0);
            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarm.cancel(pintent);
        }
    }
}


