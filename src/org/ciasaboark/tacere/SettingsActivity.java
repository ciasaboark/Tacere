/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.provider.QuickSilenceProvider;
import org.ciasaboark.tacere.service.PollService;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;


public class SettingsActivity extends Activity {
	@SuppressWarnings("unused")
	private static final String TAG = "Settings";
	
	private boolean isActivated;
	private int ringerType;
	private boolean adjustMedia;
	private int mediaVolume;
	private boolean adjustAlarm;
	private int alarmVolume;
	private int quickSilenceMinutes;
	private int quickSilenceHours;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		// Show the Up button in the action bar.
		setupActionBar();
		
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
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings_restore:
            //restore settings to default values then navigate to the main activity
			restoreDefaults();
			//navigate back to the main screen
			Toast.makeText(getApplicationContext(),"Settings have been restored to defaults", Toast.LENGTH_SHORT).show();
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
	
	private void refreshDisplay() {
		//the service state toggle
		CheckBox serviceCB = (CheckBox)findViewById(R.id.activateServiceCheckBox);
		TextView serviceTV = (TextView)findViewById(R.id.activateServiceDescription);
		if (isActivated) {
			serviceCB.setChecked(true);
			serviceTV.setText(R.string.pref_service_enabled);
		} else {
			serviceCB.setChecked(false);
			serviceTV.setText(R.string.pref_service_disabled);
		}
		
		//the ringer type description
		TextView ringerTV = (TextView)findViewById(R.id.ringerTypeDescription);
		switch (ringerType) {
			case 1:
				ringerTV.setText(R.string.pref_ringer_type_normal);
				break;
			case 2:
				ringerTV.setText(R.string.pref_ringer_type_vibrate);
				break;
			case 3:
				ringerTV.setText(R.string.pref_ringer_type_silent);
				break;
			default :
				ringerTV.setText(R.string.pref_ringer_type_silent);
		}
		

		
		//the media volumes toggle
		CheckBox mediaCB = (CheckBox)findViewById(R.id.adjustMediaCheckBox);
		TextView mediaTV = (TextView)findViewById(R.id.adjustMediaDescription);
		if (adjustMedia) {
			mediaCB.setChecked(true);
			mediaTV.setText(R.string.pref_media_enabled);
		} else {
			mediaCB.setChecked(false);
			mediaTV.setText(R.string.pref_media_disabled);
		}
		
		//the media volumes slider
		SeekBar mediaSB = (SeekBar)findViewById(R.id.mediaSeekBar);
		mediaSB.setMax(DefPrefs.MEDIA_VOLUME_MAX);
		mediaSB.setProgress(mediaVolume);
		if (!adjustMedia) {
			mediaSB.setEnabled(false);
		} else {
			mediaSB.setEnabled(true);
		}
		mediaSB.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mediaVolume = progress;
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
                //required stub
			}
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
                //required stub
			}
		});

		
		//the alarm volumes toggle
		CheckBox alarmCB = (CheckBox)findViewById(R.id.adjustAlarmCheckBox);
		TextView alarmTV = (TextView)findViewById(R.id.adjustAlarmDescription);
		if (adjustAlarm) {
			alarmCB.setChecked(true);
			alarmTV.setText(R.string.pref_alarm_enabled);
		} else {
			alarmCB.setChecked(false);
			alarmTV.setText(R.string.pref_alarm_disabled);
		}
		
		//the alarm volumes slider
		SeekBar alarmSB = (SeekBar)findViewById(R.id.alarmSeekBar);
		alarmSB.setMax(DefPrefs.ALARM_VOLUME_MAX);
		alarmSB.setProgress(alarmVolume);
		if (!adjustAlarm) {
			alarmSB.setEnabled(false);
		} else {
			alarmSB.setEnabled(true);
		}
		alarmSB.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				alarmVolume = progress;
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
                //required stub
			}
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
                //required stub
			}
		});
		
		//the quick silence button
		TextView quickTV = (TextView)findViewById(R.id.quickSilenceDescription);
		String quicksilenceText = getResources().getString(R.string.pref_quicksilent_duration);
		String hrs = "";
		if (quickSilenceHours > 0) {
			hrs = String.valueOf(quickSilenceHours) + " hours "; 
		}
		quickTV.setText(String.format(quicksilenceText, hrs, quickSilenceMinutes));
	}
	
	private void readSettings() {
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		isActivated = preferences.getBoolean("isActivated", DefPrefs.IS_ACTIVATED);
		ringerType = preferences.getInt("ringerType", DefPrefs.RINGER_TYPE);
		adjustMedia = preferences.getBoolean("adjustMedia", DefPrefs.ADJUST_MEDIA);
		mediaVolume = preferences.getInt("mediaVolume", DefPrefs.MEDIA_VOLUME);
		adjustAlarm = preferences.getBoolean("adjustAlarm", DefPrefs.ADJUST_ALARM);
		alarmVolume = preferences.getInt("alarmVolume", DefPrefs.ALARM_VOLUME);
		quickSilenceMinutes = preferences.getInt("quickSilenceMinutes", DefPrefs.QUICK_SILENCE_MINUTES);
		quickSilenceHours = preferences.getInt("quickSilenceHours", DefPrefs.QUICK_SILENCE_HOURS);
	}
	
	private void restoreDefaults() {
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("isActivated", DefPrefs.IS_ACTIVATED);
		editor.putBoolean("silenceFreeTime", DefPrefs.SILENCE_FREE_TIME);
		editor.putBoolean("silenceAllDay", DefPrefs.SILENCE_ALL_DAY);
		editor.putInt("ringerType", DefPrefs.RINGER_TYPE);
		editor.putBoolean("adjustMedia", DefPrefs.ADJUST_MEDIA);
		editor.putBoolean("adjustAlarm", DefPrefs.ADJUST_ALARM);
		editor.putInt("mediaVolume", DefPrefs.MEDIA_VOLUME);
		editor.putInt("alarmVolume", DefPrefs.ALARM_VOLUME);
		editor.putInt("quickSilenceMinutes", DefPrefs.QUICK_SILENCE_MINUTES);
		editor.putInt("quickSilenceHours", DefPrefs.QUICK_SILENCE_HOURS);
		editor.putInt("refreshInterval", DefPrefs.REFRESH_INTERVAL);
		editor.putInt("bufferMinutes", DefPrefs.BUFFER_MINUTES);
		editor.putInt("lookaheadDays", DefPrefs.LOOKAHEAD_DAYS);
		editor.commit();
		readSettings();
		refreshDisplay();
	}

	private void saveSettings() {
		SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("isActivated", isActivated);
		editor.putInt("ringerType", ringerType);
		editor.putBoolean("adjustMedia", adjustMedia);
		editor.putBoolean("adjustAlarm", adjustAlarm);
		editor.putInt("mediaVolume", mediaVolume);
		editor.putInt("alarmVolume", alarmVolume);
		editor.putInt("quickSilenceMinutes", quickSilenceMinutes);
		editor.putInt("quickSilenceHours", quickSilenceHours);
		editor.commit();
		
		//we also need to notify any active widgets of the settings change so
		//+ that they can redraw
		AppWidgetManager wManager = AppWidgetManager.getInstance(this.getApplicationContext());
		ComponentName qsWidget = new ComponentName(getApplicationContext(), QuickSilenceProvider.class);
		RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.quicksilence_widget_layout);
		int [] widgets = wManager.getAppWidgetIds(qsWidget);
		wManager.updateAppWidget(widgets, remoteViews);
	}

	public void onPause() {
		//onDestroy might not be called before the next activities onCreate
		//+ so settings should be saved here
		saveSettings();
		super.onPause();
	}

	public void onClickActivateService(View v) {
		isActivated = !isActivated;
		//if the service has been reactivated then we should restart it
		if (isActivated) {
			Intent i = new Intent(this, PollService.class);
			i.putExtra("type", "activityRestart");
			startService(i);
		}
		refreshDisplay();
	}

	public void onClickRingerType(View v) {
		AlertDialog alert = new AlertDialog.Builder(this)
				.setTitle("Ringer Type")
				.setSingleChoiceItems(R.array.ringer_types, ringerType - 1, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ringerType = which + 1;
						saveSettings();
						refreshDisplay();
						
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//do nothing
						
					}
				})
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//do nothing
						
					}
				})
				.create();
				
		alert.show();
	}
	
	public void onClickAdjustMedia(View v) {
		adjustMedia = !adjustMedia;
		refreshDisplay();
	}
	
	public void onClickAdjustAlarm(View v) {
		adjustAlarm = !adjustAlarm;
		refreshDisplay();
	}
	
	public void onClickQuickSilence(View v) {
		LayoutInflater inflator = LayoutInflater.from(this);
		View view = inflator.inflate(R.layout.dialog_quicksilent, null);
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Quick Silence");
		builder.setView(view);
	
		final NumberPicker hourP = (NumberPicker)view.findViewById(R.id.hourPicker);
		final NumberPicker minP = (NumberPicker)view.findViewById(R.id.minutePicker);
		
		String[] hours = new String[25];
		String[] minutes = new String[61];
		
		for(int i = 0; i < hours.length; i++) {
            hours[i] = Integer.toString(i);
		}
		
		for(int i = 0; i < minutes.length; i++) {
			 StringBuilder sb = new StringBuilder(Integer.toString(i));
			    if (i < 10) {
			    	sb.insert(0, "0");
			}
            minutes[i] = sb.toString();
		}
		
		hourP.setMinValue(1);
		hourP.setMaxValue(hours.length - 1);
		hourP.setWrapSelectorWheel(false);
		hourP.setDisplayedValues(hours);
		hourP.setValue(quickSilenceHours + 1);
		
		minP.setMinValue(1);
		minP.setMaxValue(minutes.length - 1);
		minP.setWrapSelectorWheel(false);
		minP.setDisplayedValues(minutes);
		minP.setValue(quickSilenceMinutes + 1);
		
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				quickSilenceHours = hourP.getValue() - 1;
				quickSilenceMinutes = minP.getValue() - 1;
				saveSettings();
				refreshDisplay();
			}
		});
	           
	    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialog, int id) {
	    		//do nothing
	    	}
	    });
	    
	    builder.show();
	}
	
	public void onClickAdvancedSettings(View v) {
		Intent i = new Intent(this, AdvancedSettingsActivity.class);
		startActivity(i);
	}
}
