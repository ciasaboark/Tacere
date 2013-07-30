/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ProviderChangedReceiver extends BroadcastReceiver {
	private static final String TAG = "ProviderChangedReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "received broadcast intent, waking service");
		Intent i = new Intent(context, PollService.class);
		i.putExtra("type", "providerChanged");
		context.startService(i);
	}
}
