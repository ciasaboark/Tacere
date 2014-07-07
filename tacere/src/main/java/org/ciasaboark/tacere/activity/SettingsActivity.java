/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere.activity;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.R.id;
import org.ciasaboark.tacere.manager.VolumesManager;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.provider.QuickSilenceProvider;
import org.ciasaboark.tacere.service.EventSilencerService;


public class SettingsActivity extends Activity {
	@SuppressWarnings("unused")
	private static final String TAG = "Settings";
	private final Context context = this;
	private Prefs prefs = new Prefs(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		// Show the Up button in the action bar.
		setupActionBar();
        Switch adjustAlarmSwitch = (Switch) findViewById(id.adjustAlarmCheckBox);
        adjustAlarmSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefs.setAdjustAlarm(!prefs.getAdjustAlarm());
                refreshDisplay();
            }
        });

        Switch adjustMediaSwitch = (Switch) findViewById(id.adjustMediaCheckBox);
        adjustMediaSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefs.setAdjustMedia(!prefs.getAdjustMedia());
                refreshDisplay();
            }
        });

        Switch serviceSwitch = (Switch) findViewById(id.activateServiceSwitch);
        serviceSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefs.setIsServiceActivated(!prefs.getIsServiceActivated());
                //if the service has been reactivated then we should restart it
                if (prefs.getIsServiceActivated()) {
                    Intent i = new Intent(context, EventSilencerService.class);
                    i.putExtra("type", "activityRestart");
                    startService(i);
                }
                refreshDisplay();
            }
        });
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
		Switch serviceActivatedSwitch = (Switch)findViewById(R.id.activateServiceSwitch);
		TextView serviceTV = (TextView)findViewById(R.id.activateServiceDescription);
		if (prefs.getIsServiceActivated()) {
			serviceActivatedSwitch.setChecked(true);
			serviceTV.setText(R.string.pref_service_enabled);
		} else {
			serviceActivatedSwitch.setChecked(false);
			serviceTV.setText(R.string.pref_service_disabled);
		}
		
		//the ringer type description
		TextView ringerTV = (TextView)findViewById(R.id.ringerTypeDescription);
		switch (prefs.getRingerType()) {
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
		Switch mediaCB = (Switch)findViewById(R.id.adjustMediaCheckBox);

        if (prefs.getAdjustMedia()) {
            mediaCB.setChecked(true);
        } else {
            mediaCB.setChecked(false);
        }
        mediaCB.setEnabled(prefs.getIsServiceActivated());

		
		//the media volumes slider
		SeekBar mediaSB = (SeekBar)findViewById(R.id.mediaSeekBar);
		mediaSB.setMax(VolumesManager.getMaxMediaVolume());
		mediaSB.setProgress(prefs.getCurMediaVolume());
		if (!prefs.getAdjustMedia()) {
			mediaSB.setEnabled(false);

            mediaSB.setVisibility(View.GONE);
		} else {
			mediaSB.setEnabled(true);
            mediaSB.setVisibility(View.VISIBLE);
		}
		mediaSB.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				prefs.setCurMediaVolume(progress);
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
		Switch alarmCB = (Switch)findViewById(R.id.adjustAlarmCheckBox);
		if (prefs.getAdjustAlarm()) {
			alarmCB.setChecked(true);
		} else {
			alarmCB.setChecked(false);
		}
        alarmCB.setEnabled(prefs.getIsServiceActivated());
		
		//the alarm volumes slider
		SeekBar alarmSB = (SeekBar)findViewById(R.id.alarmSeekBar);
		alarmSB.setMax(VolumesManager.getMaxAlarmVolume());
		alarmSB.setProgress(prefs.getCurAlarmVolume());
		if (!prefs.getAdjustAlarm()) {
			alarmSB.setEnabled(false);
            this.animateHideView(alarmSB);
//            alarmSB.setVisibility(View.GONE);
		} else {
			alarmSB.setEnabled(true);
//            alarmSB.setVisibility(View.VISIBLE);
            this.animateRevealView(alarmSB);
		}
		alarmSB.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				prefs.setCurAlarmVolume(progress);
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
		if (prefs.getQuickSilenceHours() > 0) {
			hrs = String.valueOf(prefs.getQuickSilenceHours()) + " hours "; 
		}
		quickTV.setText(String.format(quicksilenceText, hrs, prefs.getQuicksilenceMinutes()));
	}

    private void animateRevealView(View v) {
        // previously invisible view
        View myView = v;
        myView.setVisibility(View.VISIBLE);

        // get the center for the clipping circle
        int cx = (myView.getLeft() + myView.getRight()) / 2;
        int cy = (myView.getTop() + myView.getBottom()) / 2;

        // get the final radius for the clipping circle
        int finalRadius = myView.getWidth();

        // create and start the animator for this view
        // (the start radius is zero)
        android.animation.ValueAnimator anim =
                android.view.ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, finalRadius);
        anim.start();
    }

    private void animateHideView(final View v) {
        // previously visible view
        final View myView = v;

        // get the center for the clipping circle
        int cx = (myView.getLeft() + myView.getRight()) / 2;
        int cy = (myView.getTop() + myView.getBottom()) / 2;

        // get the initial radius for the clipping circle
        int initialRadius = myView.getWidth();

        // create the animation (the final radius is zero)
        ValueAnimator anim = ViewAnimationUtils.createCircularReveal(myView, cx, cy, initialRadius, 0);

        // make the view invisible when the animation is done
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                super.onAnimationEnd(animation);
                myView.setVisibility(View.GONE);
            }
        });

        // start the animation
        anim.start();
    }
	
	private void restoreDefaults() {
		prefs.restoreDefaultPreferences();
        refreshDisplay();
	}

	private void saveSettings() {
		//we also need to notify any active widgets of the settings change so
		//+ that they can redraw
		AppWidgetManager wManager = AppWidgetManager.getInstance(this.getApplicationContext());
		ComponentName qsWidget = new ComponentName(getApplicationContext(), QuickSilenceProvider.class);
		RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.quicksilence_widget_layout);
		int [] widgets = wManager.getAppWidgetIds(qsWidget);
		wManager.updateAppWidget(widgets, remoteViews);
	}

	public void onPause() {
		super.onPause();
	}

	public void onClickRingerType(View v) {
		AlertDialog alert = new AlertDialog.Builder(this)
				.setTitle("Ringer Type")
				.setSingleChoiceItems(R.array.ringer_types, prefs.getRingerType() - 1, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						prefs.setRingerType(which + 1);
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
		hourP.setValue(prefs.getQuickSilenceHours() + 1);
		
		minP.setMinValue(1);
		minP.setMaxValue(minutes.length - 1);
		minP.setWrapSelectorWheel(false);
		minP.setDisplayedValues(minutes);
		minP.setValue(prefs.getQuicksilenceMinutes() + 1);
		
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				prefs.setQuickSilenceHours(hourP.getValue() - 1);
				prefs.setQuicksilenceMinutes(minP.getValue() - 1);
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
