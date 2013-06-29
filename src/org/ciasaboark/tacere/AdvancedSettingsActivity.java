package org.ciasaboark.tacere;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.TextView;

public class AdvancedSettingsActivity extends Activity {
	private static final String TAG = "AdvancedSettingsActivity";
	
//	private boolean isActivated;
	private boolean silenceFreeTime;
	private boolean silenceAllDay;
	private int ringerType;
	private int refreshInterval;
	private int bufferMinutes;
	private boolean wakeDevice;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_advanced_settings);
		// Show the Up button in the action bar.
		setupActionBar();
		
		//read the saved preferences
		readSettings();
		
		//Log the results
		Log.d(TAG, "silenceFreeTime: " + String.valueOf(silenceFreeTime));
		Log.d(TAG, "silenceAllDay: " + String.valueOf(silenceAllDay));
		Log.d(TAG, "ringerType: " + String.valueOf(ringerType));
		Log.d(TAG, "refreshInterval: " + String.valueOf(refreshInterval));
		Log.d(TAG, "bufferMinutes: " + String.valueOf(bufferMinutes));
		Log.d(TAG, "wakeDevice: " + String.valueOf(wakeDevice));
		
		refreshDisplay();
				
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
		Log.d(TAG, "onPause() called");
		
		//save all changes to the preferences
		saveSettings();
		super.onPause();
	}
	
	private void refreshDisplay() {
		//the silence free time state toggle
		CheckBox freeCB = (CheckBox)findViewById(R.id.silenceFreeTimeCheckBox);
		TextView freeTV = (TextView)findViewById(R.id.silenceFreeTimeDescription);
		if (silenceFreeTime) {
			freeCB.setChecked(true);
			freeTV.setText("Will silence during events marked as free");
		} else {
			freeCB.setChecked(false);
			freeTV.setText("Will not silence during events marked as free");
		}
		
		//the silence all day state toggle
		CheckBox dayCB = (CheckBox)findViewById(R.id.silenceAllDayCheckBox);
		TextView dayTV = (TextView)findViewById(R.id.silenceAllDayDescription);
		if (silenceAllDay) {
			dayCB.setChecked(true);
			dayTV.setText("Will silence during all day events");
		} else {
			dayCB.setChecked(false);
			dayTV.setText("Will not silence during all day events");
		}
		
		//the refresh interval button
		TextView refreshTV = (TextView)findViewById(R.id.refreshIntervalDescription);
		refreshTV.setText("Service will refresh every " + refreshInterval + " minutes");
		
		//the event buffer button
		TextView bufferTV = (TextView)findViewById(R.id.bufferMinutesDescription);
		bufferTV.setText("Events will trigger " + bufferMinutes + " minutes before starting and last " + bufferMinutes + " minutes after the event ends");
		
		//the wake device toggle
		CheckBox wakeCB = (CheckBox)findViewById(R.id.wakeDeviceCheckBox);
		TextView wakeTV = (TextView)findViewById(R.id.wakeDeviceDescription);
		if (wakeDevice) {
			wakeCB.setChecked(true);
			wakeTV.setText("Tacere will force a wakeup every " + refreshInterval + " minutes. This may cause battery drain");
		} else {
			wakeCB.setChecked(false);
			wakeTV.setText("Tacere will wait for the device to wake normally before performing checks.");
			
		}
	}

	private void readSettings() {
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		silenceFreeTime = preferences.getBoolean("silenceFreeTime",DefPrefs.silenceFreeTime);
		silenceAllDay = preferences.getBoolean("silenceAllDay", DefPrefs.silenceAllDay);
		ringerType = preferences.getInt("ringerType", DefPrefs.ringerType);
		refreshInterval = preferences.getInt("refreshInterval", DefPrefs.refreshInterval);
		bufferMinutes = preferences.getInt("bufferMinutes", DefPrefs.bufferMinutes);
		wakeDevice = preferences.getBoolean("wakeDevice", DefPrefs.wakeDevice);
		
		Log.d(TAG, "readSettings() called");
	}
	
	private void saveSettings() {
		Log.d(TAG, "saveSettings() called");
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("silenceFreeTime", silenceFreeTime);
		editor.putBoolean("silenceAllDay", silenceAllDay);
		editor.putInt("ringerType", 3);
		editor.putInt("refreshInterval", refreshInterval);
		editor.putInt("bufferMinutes", bufferMinutes);
		editor.putBoolean("wakeDevice", wakeDevice);
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
		Log.d(TAG, "onClickRefreshInterval() called");
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Refresh Interval");
		final NumberPicker number = new NumberPicker(this);
		String[] nums = new String[120];
		Log.d(TAG, "nums length " + nums.length);
		for(int i = 0; i < nums.length; i++) {
            nums[i] = Integer.toString(i + 1);
		}

	     number.setMinValue(1);
	     number.setMaxValue(nums.length-1);
	     number.setWrapSelectorWheel(false);
	     number.setDisplayedValues(nums);
	     number.setValue(refreshInterval);
		
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	          	Log.d(TAG, "New refresh interval is " + number.getValue() + " minutes");
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
		Log.d(TAG, "onClickBufferMinues() called");
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Buffer Minutes");
		final NumberPicker number = new NumberPicker(this);
		String[] nums = new String[32];
		Log.d(TAG, "nums length " + nums.length);
		for(int i = 0; i < nums.length; i++) {
            nums[i] = Integer.toString(i);
		}

	     number.setMinValue(1);
	     number.setMaxValue(nums.length-1);
	     number.setWrapSelectorWheel(false);
	     number.setDisplayedValues(nums);
	     number.setValue(bufferMinutes + 1);
		
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	          	Log.d(TAG, "New buffer minutes is " + number.getValue() + " minutes");
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
	
	public void onClickWakeDevice(View v) {
		wakeDevice = !wakeDevice;
		refreshDisplay();
	}
}
