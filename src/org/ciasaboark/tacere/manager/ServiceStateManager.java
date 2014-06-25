package org.ciasaboark.tacere.manager;

import org.ciasaboark.tacere.prefs.Prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ServiceStateManager {
	private static final String TAG = "StateManager";
	private Context context;
	private Prefs prefs;
	
	public ServiceStateManager(Context ctx) {
		this.context = ctx;
		this.prefs = new Prefs(ctx);
	}
	
	public void setServiceState(String state) {
		if (state != ServiceStates.QUICKSILENCE || state != ServiceStates.EVENT_ACTIVE
				|| state != ServiceStates.NOT_ACTIVE) {
			throw new IllegalArgumentException("unknown state: " + state);
		}

		SharedPreferences preferences = context.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		if (state != null) {
			editor.putString("serviceState", state);
		} else {
			Log.e(TAG, "Trying to set state to null value");
		}
		editor.commit();
	}
	
	public String getServiceState() {
		return prefs.readPreferenceString("SERVICE_STATE");
	}
	
	public class ServiceStates {
		public static final String QUICKSILENCE = "quickSilence";
		public static final String EVENT_ACTIVE = "active";
		public static final String NOT_ACTIVE = "notActive";
	}
}


