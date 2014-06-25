package org.ciasaboark.tacere.manager;

import org.ciasaboark.tacere.prefs.ConstVariables;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class AlarmManagerWrapper {
	// requestCodes for the different pending intents
	private static final int RC_EVENT 		= 1;
	private static final int RC_QUICKSILENT 	= 2;
	private static final int RC_NOTIFICATION = 3;
	
	private Context context;

	public AlarmManagerWrapper(Context ctx) {
		this.context = ctx;
	}
	
	public void scheduleNormalWakeAt(long time) {
		scheduleAlarmAt(time, RequestTypes.NORMAL);
	}
	
	public void scheduleQuickSilenceWakeAt(long time) {
		scheduleAlarmAt(time, RequestTypes.QUICKSILENCE);
	}
	
	public void scheduleCancelQuickSilenceAlarmAt(long time) {
		scheduleAlarmAt(time, RequestTypes.CANCEL_QUICKSILENCE);
	}
	
	public void scheduleActivityRestartWakeAt(long time) {
		scheduleAlarmAt(time, RequestTypes.ACTIVITY_RESTART);
	}

	public void scheduleAlarmAt(long time, String type) {
		if (type == null) {
			throw new IllegalArgumentException("unknown type: " + type);
		}

		if (time < 0) {
			throw new IllegalArgumentException("PollService:scheduleAlarmAt not given valid time");
		}

		Intent i = new Intent(context, EventSilencerService.class);
		i.putExtra("type", type);

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
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarm.set(AlarmManager.RTC_WAKEUP, time, pintent);
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


