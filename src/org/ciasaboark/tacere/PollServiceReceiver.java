package org.ciasaboark.tacere;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class PollServiceReceiver extends BroadcastReceiver {
	
	private static final String TAG = "PollServiceReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "PollServiceREceiver caught boot finished broadcast");
		Intent i = new Intent(context, PollService.class);
		Toast.makeText(context, "PollServiceReceiver starting", Toast.LENGTH_LONG).show();
		context.startService(i);
	}

}
