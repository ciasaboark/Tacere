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
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	
	private boolean isActivated;
	private int quickSilenceMinutes;
	private int quickSilenceHours;
	private EventList evList;
	private EventAdapter listAdapter;
	private ArrayList<CalEvent> events;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//create a list of events for the next week
		evList = EventList.get(getApplicationContext());	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onStart() {
		super.onStart();
		setContentView(R.layout.activity_main);
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
			
			if (isActivated) {
				ssImage.setImageDrawable(greenButton);
				ssText.setText(R.string.pref_service_enabled);
			} else {
				ssImage.setImageDrawable(redButton);
				ssText.setText(R.string.pref_service_disabled);
			}
		} catch (IOException e) {
			Log.e(TAG, "Error loading drawable icon");
		}
		
	
		//the event list
		evList.updateEventList();
		events = evList.getEvents();
		
		int num = 0;
		for (CalEvent e : events) {
			Log.d(TAG, "Event " + num + ": " + " id: " + e.getId() + " " + e.toString());
			num++;
		}
		
		ListView lv = (ListView)findViewById(R.id.eventListView);
		lv.setClickable(true);
		listAdapter = new EventAdapter(events);
		lv.setAdapter(listAdapter);
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
	
	private class EventAdapter extends ArrayAdapter<CalEvent> {
		public EventAdapter(ArrayList<CalEvent> events) {
			super(getApplicationContext(), 0, events);
		}
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = MainActivity.this.getLayoutInflater().inflate(R.layout.event_list_item, null);
				
				CalEvent e = getItem(position);
				
				//the base relative layout
				final RelativeLayout eventRL = (RelativeLayout)convertView.findViewById(R.id.eventListItem);
				eventRL.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Toast.makeText(getApplicationContext(), evList.getEventAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
						
					}
				});
				
				//a text view to show the event title and date
				TextView descriptionTV = (TextView)convertView.findViewById(R.id.eventText);
				descriptionTV.setText(e.getTitle() + " - " + e.getLocalBeginDate());
				
				//a text view to show the beginning and ending times for the event
				TextView timeTV = (TextView)convertView.findViewById(R.id.eventTime);
				StringBuilder timeSB = new StringBuilder(e.getLocalBeginTime() + " - ");
				if (!e.getLocalBeginDate().equals(e.getLocalEndDate())) {
					timeSB.append("(" + e.getLocalEndDate() + ") ");
				}
				timeSB.append(e.getLocalEndTime());
				if (e.getIsAllDay()) {
					timeSB = new StringBuilder(getBaseContext().getString(R.string.all_day));
				}
				timeTV.setText(timeSB.toString());
				
				//a color box to match the calendar color
				RelativeLayout calColorBox = (RelativeLayout)convertView.findViewById(R.id.calendarColor);
				calColorBox.setBackgroundColor(e.getDisplayColor());
				
				//an image button to show the ringer state for this event
				ImageView eventIV = (ImageView)convertView.findViewById(R.id.ringerState);
				if ((position % 1) == 0) {
					eventIV.setImageResource(R.drawable.ic_state_silent);
				} else {
					eventIV.setImageResource(R.drawable.ic_state_normal);
				}
				eventIV.setContentDescription(getBaseContext().getString(R.string.icon_alt_text_normal));
			}
			return convertView;	
		}
		
		public CalEvent getItem(int position) {
			return evList.getEventAtPosition(position);
		}
	}
	
	private void readSettings() {
		//read the saved preferences
		try {
			SharedPreferences preferences = this.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
			isActivated = preferences.getBoolean("isActivated", DefPrefs.isActivated);
			quickSilenceMinutes = preferences.getInt("quickSilenceMinutes", DefPrefs.quickSilenceMinutes);
			quickSilenceHours = preferences.getInt("quickSilenceHours", DefPrefs.quickSilenceHours);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}	
	}
}
