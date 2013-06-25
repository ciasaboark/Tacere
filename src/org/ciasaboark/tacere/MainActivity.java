/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
		startService(new Intent(this, PollService.class));
		
		readSettings();
		
		//set up quick silence button
		Button quickSettingsButton = (Button)findViewById(R.id.quickSilenceButton);
		quickSettingsButton.setText("Quick Silence " + quickSilenceMinutes + " minutes");
		quickSettingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO silence phone for required time
				
			}
		});
		
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
			if (PendingIntent.getBroadcast(this, 0, new Intent("org.ciasaboark.tacere.PollService"), PendingIntent.FLAG_NO_CREATE) != null) {
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
			refreshInterval = preferences.getInt("refreshInterval", DefPrefs.refreshInterval);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}	
	}
}
