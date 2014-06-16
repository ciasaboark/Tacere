/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere;

import org.ciasaboark.tacere.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
//import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.TextView;

public class AdvancedSettingsActivity extends Activity {
	// private static final String TAG = "AdvancedSettingsActivity";

	private boolean silenceFreeTime;
	private boolean silenceAllDay;
	private int refreshInterval;
	private int bufferMinutes;
	private int lookaheadDays;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_advanced_settings);
		// Show the Up button in the action bar.
		setupActionBar();

		// read the saved preferences
		readSettings();

		refreshDisplay();
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setIcon(R.drawable.action_settings);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.advanced_settings, menu);
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

	public void onPause() {
		// save all changes to the preferences
		saveSettings();
		super.onPause();
	}

	private void refreshDisplay() {
		// the silence free time state toggle
		CheckBox freeCB = (CheckBox) findViewById(R.id.silenceFreeTimeCheckBox);
		TextView freeTV = (TextView) findViewById(R.id.silenceFreeTimeDescription);
		if (silenceFreeTime) {
			freeCB.setChecked(true);
			freeTV.setText(R.string.pref_silence_free_enabled);
		} else {
			freeCB.setChecked(false);
			freeTV.setText(R.string.pref_silence_free_disabled);
		}

		// the silence all day state toggle
		CheckBox dayCB = (CheckBox) findViewById(R.id.silenceAllDayCheckBox);
		TextView dayTV = (TextView) findViewById(R.id.silenceAllDayDescription);
		if (silenceAllDay) {
			dayCB.setChecked(true);
			dayTV.setText(R.string.pref_all_day_enabled);
		} else {
			dayCB.setChecked(false);
			dayTV.setText(R.string.pref_all_day_disabled);
		}

		// the event buffer button
		TextView bufferTV = (TextView) findViewById(R.id.bufferMinutesDescription);
		String bufferText = getResources().getString(R.string.pref_buffer_minutes);
		bufferTV.setText(String.format(bufferText, bufferMinutes));

		// the lookahead interval button
		TextView lookaheadTV = (TextView) findViewById(R.id.lookaheadDaysDescription);
		String lookaheadText = getResources().getString(R.string.pref_list_days);
		lookaheadTV.setText(String.format(lookaheadText, lookaheadDays));
	}

	private void readSettings() {
		SharedPreferences preferences = this.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		silenceFreeTime = preferences.getBoolean("silenceFreeTime", DefPrefs.SILENCE_FREE_TIME);
		silenceAllDay = preferences.getBoolean("silenceAllDay", DefPrefs.SILENCE_ALL_DAY);
		refreshInterval = preferences.getInt("refreshInterval", DefPrefs.REFRESH_INTERVAL);
		bufferMinutes = preferences.getInt("bufferMinutes", DefPrefs.BUFFER_MINUTES);
		lookaheadDays = preferences.getInt("lookaheadDays", DefPrefs.LOOKAHEAD_DAYS);
	}

	private void saveSettings() {
		SharedPreferences preferences = this.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("silenceFreeTime", silenceFreeTime);
		editor.putBoolean("silenceAllDay", silenceAllDay);
		editor.putInt("ringerType", 3);
		editor.putInt("refreshInterval", refreshInterval);
		editor.putInt("bufferMinutes", bufferMinutes);
		editor.putInt("lookaheadDays", lookaheadDays);
		editor.commit();
	}

	public void onClickSilenceFreeTime(View v) {
		silenceFreeTime = !silenceFreeTime;
		refreshDisplay();
	}

	public void onClickSilenceAllDay(View v) {
		silenceAllDay = !silenceAllDay;
		refreshDisplay();
	}

	public void onClickRefreshInterval(View v) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Refresh Interval");
		final NumberPicker number = new NumberPicker(this);
		String[] nums = new String[120];

		for (int i = 0; i < nums.length; i++) {
			nums[i] = Integer.toString(i + 1);
		}

		number.setMinValue(1);
		number.setMaxValue(nums.length - 1);
		number.setWrapSelectorWheel(false);
		number.setDisplayedValues(nums);
		number.setValue(refreshInterval);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				refreshInterval = number.getValue();
				saveSettings();
				refreshDisplay();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing
			}
		});

		alert.setView(number);
		alert.show();
	}

	public void onClickBufferMinutes(View v) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Buffer Minutes");
		final NumberPicker number = new NumberPicker(this);
		String[] nums = new String[32];

		for (int i = 0; i < nums.length; i++) {
			nums[i] = Integer.toString(i);
		}

		number.setMinValue(1);
		number.setMaxValue(nums.length - 1);
		number.setWrapSelectorWheel(false);
		number.setDisplayedValues(nums);
		number.setValue(bufferMinutes + 1);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				bufferMinutes = number.getValue() - 1;
				saveSettings();
				refreshDisplay();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing
			}
		});

		alert.setView(number);
		alert.show();
	}

	public void onClickListDays(View v) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Lookahead Interval");
		final NumberPicker number = new NumberPicker(this);
		String[] nums = new String[32];

		for (int i = 0; i < nums.length; i++) {
			nums[i] = Integer.toString(i + 1);
		}

		number.setMinValue(1);
		number.setMaxValue(nums.length - 1);
		number.setWrapSelectorWheel(false);
		number.setDisplayedValues(nums);
		number.setValue(lookaheadDays);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				lookaheadDays = number.getValue();
				saveSettings();
				refreshDisplay();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing
			}
		});

		alert.setView(number);
		alert.show();
	}
}
