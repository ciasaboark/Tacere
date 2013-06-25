/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.Toast;

public class SettingsActivity extends Activity {
	private static final String TAG = "Settings";
	
	private boolean isActivated;
	private boolean silenceFreeTime;
	private int ringerType;
	private boolean adjustMedia;
	private int mediaVolume;
	private boolean adjustAlarm;
	private int alarmVolume;
	private int quickSilenceMinutes;
	private int refreshInterval;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		// Show the Up button in the action bar.
		setupActionBar();
		
		//read the saved preferences
		readSettings();
		
		//Log the results
		Log.d(TAG, "isActivated: " + String.valueOf(isActivated));
		Log.d(TAG, "silenceFreeTime: " + String.valueOf(silenceFreeTime));
		Log.d(TAG, "ringerType: " + String.valueOf(ringerType));
		Log.d(TAG, "adjustMedia: " + String.valueOf(adjustMedia));
		Log.d(TAG, "mediaVolume: " + String.valueOf(mediaVolume));
		Log.d(TAG, "adjustAlarm: " + String.valueOf(adjustAlarm));
		Log.d(TAG, "alarmVolume: " + String.valueOf(alarmVolume));
		Log.d(TAG, "quickSilenceMinutes: " + String.valueOf(quickSilenceMinutes));
		Log.d(TAG, "refreshInterval: " + String.valueOf(refreshInterval));
		
		//the refresh interval picker
		NumberPicker refreshPicker = (NumberPicker)findViewById(R.id.refresh_interval);
		refreshPicker.setMaxValue(30);
		refreshPicker.setMinValue(1);
		refreshPicker.setValue(refreshInterval);
		
		//the service activated toggle
		CheckBox serviceCheckBox = (CheckBox)findViewById(R.id.serviceCheckBox);
		serviceCheckBox.setChecked(isActivated);
		
		
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
		case R.id.action_settings_restore:
            //restore settings to default values then navigate to the main activity
			isActivated = DefPrefs.isActivated;
			silenceFreeTime = DefPrefs.silenceFreeTime;
			ringerType = DefPrefs.ringerType;
			adjustMedia = DefPrefs.adjustMedia;
			mediaVolume = DefPrefs.mediaVolume;
			adjustAlarm = DefPrefs.adjustAlarm;
			alarmVolume = DefPrefs.alarmVolume;
			quickSilenceMinutes = DefPrefs.quickSilenceMinutes;
			refreshInterval = DefPrefs.refreshInterval;
			
			//settings will be saved when onPause() fires
			Toast.makeText(getApplicationContext(),"Settings have been restore to defaults", Toast.LENGTH_SHORT).show();
			NavUtils.navigateUpFromSameTask(this);
            return true;
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			saveSettings();
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Settings:onDestroy() called");
	
	}
	
	private void readSettings() {
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		isActivated = preferences.getBoolean("isActivated", DefPrefs.isActivated);
		silenceFreeTime = preferences.getBoolean("silenceFreeTime",DefPrefs.silenceFreeTime);
		ringerType = preferences.getInt("ringerType", DefPrefs.ringerType);
		adjustMedia = preferences.getBoolean("adjustMedia", DefPrefs.adjustMedia);
		mediaVolume = preferences.getInt("mediaVolume", DefPrefs.mediaVolume);
		adjustAlarm = preferences.getBoolean("adjustAlarm", DefPrefs.adjustAlarm);
		alarmVolume = preferences.getInt("alarmVolume", DefPrefs.alarmVolume);
		quickSilenceMinutes = preferences.getInt("quickSilenceMinutes", DefPrefs.quickSilenceMinutes);
		refreshInterval = preferences.getInt("refreshInterval", DefPrefs.refreshInterval);
		
		Log.d(TAG, "readSettings() called");
	}
	
	private void saveSettings() {
		Log.d(TAG, "saveSettings() called");
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("isActivated", isActivated);
		editor.putBoolean("silenceFreeTime", silenceFreeTime);
		editor.putInt("ringerType", 3);
		editor.putBoolean("adjustMedia", adjustMedia);
		editor.putBoolean("adjustAlarm", adjustAlarm);
		editor.putInt("mediaVolume", mediaVolume);
		editor.putInt("alarmVolume", alarmVolume);
		editor.putInt("quickSilenceMinutes", quickSilenceMinutes);
		editor.putInt("refreshInterval", refreshInterval);
		editor.commit();
	}
	
	public void onCheckBoxClicked(View v) {
		boolean checked = ((CheckBox) v).isChecked();
		
		//which checkbox was clicked?
		switch (v.getId()) {
			case R.id.serviceCheckBox:
				if (checked) {
					isActivated = true;
				} else {
					isActivated = false;
				}
				break;
		}
	}
	
	
	public void onRestart() {
		Log.d(TAG, "onRestart() called");
		super.onRestart();
	}
	
	public void onResume() {
		Log.d(TAG, "onResume() called");
		super.onResume();
	}

	public void onPause() {
		Log.d(TAG, "onPause() called");
		
		//save all changes to the preferences
		saveSettings();
		super.onPause();
	}
	
	public void onStop() {
		Log.d(TAG, "onStop() called");
		super.onStop();
	}

}
