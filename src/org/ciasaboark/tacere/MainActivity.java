/*
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
 * Created by Jonathan Nelson
*/

package org.ciasaboark.tacere;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	
	private boolean isActivated = true;
	private boolean silenceFreeTime;
	private int ringerType;
	private boolean adjustMedia;
	private int mediaVolume;
	private boolean adjustAlarm;
	private int alarmVolume;
	private int quickSilenceMinutes;
	private int quickSilenceHours;
	private int refreshInterval;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "MainActivity onCreate() called");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		
		//the text and image for the state of pollservice can only be set up after the service has
		//+ started in onStart()
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onClickQuickSilence(View v) {
		Log.d(TAG, "onClickQuickSilence() called");
		Intent i = new Intent(this, PollService.class);
		i.putExtra("quickSilence", 5);
		startService(i);
	}
	
	public void onStart() {
		Log.d(TAG, "MainActivity onStart() called");
		super.onStart();
	
		//start the background service
		Intent i = new Intent(this, PollService.class);
		i.putExtra("type", "activityRestart");
		startService(i);
		
		readSettings();
		
		//set up quick silence button
		Button quickSettingsButton = (Button)findViewById(R.id.quickSilenceButton);
		StringBuilder sb = new StringBuilder("Quick Silence ");
		if (quickSilenceHours != 0) {
			sb.append(quickSilenceHours + " hours, ");
		}
		sb.append(quickSilenceMinutes + " minutes");
		quickSettingsButton.setText(sb.toString());
		quickSettingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//the length of time for the pollService to sleep in minutes
				int duration = 60 *quickSilenceHours + quickSilenceMinutes;
				
				//an intent to send to PollService immediately
				Intent i = new Intent(getApplicationContext(), PollService.class);
				i.putExtra("type", "quickSilent");
				i.putExtra("duration", duration);
				startService(i);
				
			}
		});
		
		if (quickSilenceHours == 0 && quickSilenceMinutes == 0) {
			quickSettingsButton.setEnabled(false);
		} else {
			quickSettingsButton.setEnabled(true);
		}
		
		//set up the text and imageview to display the service state
		try {
			ImageView ssImage = (ImageView) findViewById(R.id.serviceStateImageView);
			InputStream inStreamGreen = getAssets().open("button_green.png");
			Drawable greenButton = Drawable.createFromStream(inStreamGreen, null);
			InputStream inStreamRed = getAssets().open("button_red.png");
			Drawable redButton = Drawable.createFromStream(inStreamRed, null);
			TextView ssText = (TextView) findViewById(R.id.serviceStateTextView);
			
			//since the pollservice only runs intermittently we have to check whether there
			//+ is a pending intent scheduled to determine whether or not the service is
			//+ considered active
			Intent intent = new Intent(this, PollService.class);
			PendingIntent pintent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_NO_CREATE);
			if (pintent != null) {
				ssImage.setImageDrawable(greenButton);
				ssText.setText("Service Active");
			} else {
				ssImage.setImageDrawable(redButton);
				ssText.setText("Service Not Active");
			}
		} catch (IOException e) {
			Log.e(TAG, "Error loading drawable icon");
		}
	}
	
	public void onRestart() {
		Log.d(TAG, "MainActivity onRestart() called");
		super.onRestart();
	}
	
	public void onResume() {
		Log.d(TAG, "MainActivity onResume() called");
		super.onResume();
	}

	public void onPause() {
		Log.d(TAG, "MainActivity onPause() called");
		super.onPause();
	}
	
	public void onStop() {
		Log.d(TAG, "MainActivity onStop() called");
		super.onStop();
	}
	
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "MainActivity onDestroy() called");
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	        case R.id.action_settings:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, SettingsActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
	        case R.id.action_about:
	        	Intent i = new Intent(this, AboutActivity.class);
	        	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	startActivity(i);
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
		}
	}
	
	private void readSettings() {
		//read the saved preferences
		Log.d(TAG, "readSettings() called");
		try {
			SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
			isActivated = preferences.getBoolean("isActivated", DefPrefs.isActivated);
			silenceFreeTime = preferences.getBoolean("silenceFreeTime",DefPrefs.silenceFreeTime);
			ringerType = preferences.getInt("ringerType", DefPrefs.ringerType);
			adjustMedia = preferences.getBoolean("adjustMedia", DefPrefs.adjustMedia);
			mediaVolume = preferences.getInt("mediaVolume", DefPrefs.mediaVolume);
			adjustAlarm = preferences.getBoolean("adjustAlarm", DefPrefs.adjustAlarm);
			alarmVolume = preferences.getInt("alarmVolume", DefPrefs.alarmVolume);
			quickSilenceMinutes = preferences.getInt("quickSilenceMinutes", DefPrefs.quickSilenceMinutes);
			quickSilenceHours = preferences.getInt("quickSilenceHours", DefPrefs.quickSilenceHours);
			refreshInterval = preferences.getInt("refreshInterval", DefPrefs.refreshInterval);
			
			//Log the results
			Log.d(TAG, "isActivated: " + String.valueOf(isActivated));
			Log.d(TAG, "silenceFreeTime: " + String.valueOf(silenceFreeTime));
			Log.d(TAG, "ringerType: " + String.valueOf(ringerType));
			Log.d(TAG, "adjustMedia: " + String.valueOf(adjustMedia));
			Log.d(TAG, "mediaVolume: " + String.valueOf(mediaVolume));
			Log.d(TAG, "adjustAlarm: " + String.valueOf(adjustAlarm));
			Log.d(TAG, "alarmVolume: " + String.valueOf(alarmVolume));
			Log.d(TAG, "quickSilenceMinutes: " + String.valueOf(quickSilenceMinutes));
			Log.d(TAG, "quickSilenceHours: " + String.valueOf(quickSilenceHours));
			Log.d(TAG, "refreshInterval: " + String.valueOf(refreshInterval));
			
		} catch (RuntimeException e) {
			e.printStackTrace();
		}	
	}
}
