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

public class PollServiceReceiver extends BroadcastReceiver {
	
	//private static final String TAG = "PollServiceReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent i = new Intent(context, PollService.class);
		context.startService(i);
	}

}
