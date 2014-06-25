package org.ciasaboark.tacere.service;

import org.ciasaboark.tacere.CalEvent;
import org.ciasaboark.tacere.database.DatabaseInterface;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ResetEventService extends IntentService {
	private static final String TAG = "ResetEventService";
	
	public ResetEventService() {
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
				dbIface.setRingerType(eventId, CalEvent.RINGER.UNDEFINED);
				
				Intent i = new Intent(this, EventSilencerService.class);
				i.putExtra("type", "activityRestart");
				startService(i);
			}
		}
		
	}
}