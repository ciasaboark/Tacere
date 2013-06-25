/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class SettingsActivity extends Activity {
	private static final String TAG = "Shutdown";
	
	private boolean isActivated = true;
	private boolean silenceFreeTime;
	private int ringerType;
	private boolean adjustMedia;
	private float mediaVolume;
	private boolean adjustAlarm;
	private float alarmVolume;
	private int quickSilenceMinutes;
	private long refreshInterval;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		// Show the Up button in the action bar.
		setupActionBar();
		
		//read the saved preferences
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", this.MODE_PRIVATE);
		isActivated = preferences.getBoolean("isActivated", false);
		silenceFreeTime = preferences.getBoolean("silenceFreeTime", true);
		ringerType = preferences.getInt("ringerType", 3);
		adjustMedia = preferences.getBoolean("adjustMedia", false);
		mediaVolume = preferences.getFloat("mediaVolume", (float) 1.0);
		adjustAlarm = preferences.getBoolean("adjustAlarm", false);
		alarmVolume = preferences.getFloat("alarmVolume", (float) 1.0);
		quickSilenceMinutes = preferences.getInt("quickSilenceMinutes", 5);
		refreshInterval = preferences.getLong("refreshInterval", 5);
		
		
		
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Settings:onDestroy() called");
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", this.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("isActivated", isActivated);
		editor.putBoolean("silenceFreeTime", silenceFreeTime);
		editor.putInt("ringerType", 3);
		editor.putBoolean("adjustMedia", adjustMedia);
		editor.putBoolean("adjustAlarm", adjustAlarm);
		editor.putFloat("mediaVolume", mediaVolume);
		editor.putFloat("alarmVolume", alarmVolume);
		editor.putInt("quickSilenceMinutes", quickSilenceMinutes);
		editor.putLong("refreshInterval", refreshInterval);
		editor.commit();
	}

}
