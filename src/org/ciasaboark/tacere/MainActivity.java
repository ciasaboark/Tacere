/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 * 
 * Released under the BSD license. For details see the COPYING file.
 */

package org.ciasaboark.tacere;

import org.ciasaboark.tacere.provider.EventProvider;
import org.ciasaboark.tacere.service.PollService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener,
		AdapterView.OnItemLongClickListener {
	@SuppressWarnings("unused")
	private static final String TAG = "MainActivity";

	private int quickSilenceMinutes;
	private int quickSilenceHours;
	private int lookaheadDays;
	private int bufferMinutes;
	private EventCursorAdapter cursorAdapter;
	private Cursor cursor;
	private DatabaseInterface DBIface;
	private ListView lv = null;
	
	private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
		private static final String TAG = "messageReceiver";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String message = intent.getStringExtra("message");
			cursorAdapter.notifyDataSetChanged();
			Log.d(TAG, message);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		DBIface = DatabaseInterface.get(this);

		SharedPreferences preferences = this.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		
		//register to receive broadcast messages
		LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("custom-event-name"));

		// display the updates dialog if it hasn't been shown yet
		boolean showUpdates = preferences.getBoolean(DefPrefs.UPDATES_VERSION, true);
		if (showUpdates) {
			Intent updatesIntent = new Intent(getApplicationContext(), UpdatesActivity.class);
			startActivity(updatesIntent);
		}
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
		// start the background service
		Intent i = new Intent(this, PollService.class);
		i.putExtra("type", "activityRestart");
		startService(i);

		readSettings();

		// set up quick silence button
		Button quickSettingsButton = (Button) findViewById(R.id.quickSilenceButton);
		StringBuilder sb = new StringBuilder("Quick Silence ");
		if (quickSilenceHours != 0) {
			sb.append(quickSilenceHours + " hours, ");
		}
		sb.append(quickSilenceMinutes + " minutes");
		quickSettingsButton.setText(sb.toString());
		quickSettingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// the length of time for the pollService to sleep in minutes
				int duration = 60 * quickSilenceHours + quickSilenceMinutes;

				// an intent to send to PollService immediately
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

		// the event list title
		TextView eventsTitle = (TextView) findViewById(R.id.eventListTitle);
		String eventsText = getResources().getString(R.string.upcoming_events);
		eventsTitle.setText(String.format(eventsText, lookaheadDays));

		DBIface.update(lookaheadDays);

		// prune the database of old events
		DBIface.pruneEventsBefore(System.currentTimeMillis() - 1000 * 60 * (long) bufferMinutes);

		// since the number of days to display can change we need to
		// + remove events beyond the lookahead period
		DBIface.pruneEventsAfter(System.currentTimeMillis() + 1000 * 60 * 60 * 24
				* (long) lookaheadDays);

		// DBIface.printEvents();

		// the list of upcoming events
		lv = (ListView) findViewById(R.id.eventListView);
		lv.setOnItemClickListener(this);
		lv.setOnItemLongClickListener(this);
		lv.setFadingEdgeLength(0);
		cursor = DBIface.getCursor(EventProvider.BEGIN);
		cursorAdapter = new EventCursorAdapter(this, cursor);
		lv.setAdapter(cursorAdapter);

		// display the "thank you" dialog once if the donation key is installed
		SharedPreferences preferences = this.getSharedPreferences(
				"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		boolean show_donation_thanks = preferences.getBoolean("show_donation_thanks",
				DefPrefs.SHOW_DONATION_THANKS);
		if (show_donation_thanks) {
			PackageManager manager = getPackageManager();
			if (manager.checkSignatures("org.ciasaboark.tacere", "org.ciasaboark.tacere.key") == PackageManager.SIGNATURE_MATCH) {
				Intent donationIntent = new Intent(getApplicationContext(), DonationActivity.class);
				startActivity(donationIntent);
			}
		}

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		try {
			CalEvent thisEvent = DBIface.getEvent((int) id);
			int nextRingerType = thisEvent.getRingerType() + 1;
			if (nextRingerType > CalEvent.RINGER_TYPE_SILENT) {
				nextRingerType = CalEvent.RINGER_TYPE_NORMAL;
			}
			DBIface.setRingerType((int) id, nextRingerType);
			lv.getAdapter().getView(position, view, lv);

			// since the database has changed we need to wake the service
			Intent i = new Intent(this, PollService.class);
			i.putExtra("type", "activityRestart");
			startService(i);
		} catch (NullPointerException e) {
			// if the selected event is no longer in the DB, then we need to remove it from the list
			// view
			removeListViewEvent(view);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		try {
			CalEvent thisEvent = DBIface.getEvent((int) id);
			DBIface.setRingerType((int) id, CalEvent.RINGER_TYPE_UNDEFINED);
			lv.getAdapter().getView(position, view, lv);
			Toast.makeText(parent.getContext(), thisEvent.getTitle() + " reset to default ringer",
					Toast.LENGTH_SHORT).show();

			// since the database has changed we need to wake the service
			Intent i = new Intent(this, PollService.class);
			i.putExtra("type", "activityRestart");
			startService(i);
			return true;
		} catch (NullPointerException e) {
			removeListViewEvent(view);
			return false;
		}
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

	private void removeListViewEvent(View view) {
		Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
		anim.setDuration(500);
		view.startAnimation(anim);

		new Handler().postDelayed(new Runnable() {
			public void run() {
				cursor = DBIface.getCursor(EventProvider.BEGIN);
				cursorAdapter.swapCursor(cursor);
				cursorAdapter.notifyDataSetChanged();
			}
		}, anim.getDuration());
	}

	private void readSettings() {
		// read the saved preferences
		try {
			SharedPreferences preferences = this.getSharedPreferences(
					"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
			quickSilenceMinutes = preferences.getInt("quickSilenceMinutes",
					DefPrefs.QUICK_SILENCE_MINUTES);
			quickSilenceHours = preferences.getInt("quickSilenceHours",
					DefPrefs.QUICK_SILENCE_HOURS);
			lookaheadDays = preferences.getInt("lookaheadDays", DefPrefs.LOOKAHEAD_DAYS);
			bufferMinutes = preferences.getInt("bufferMinutes", DefPrefs.BUFFER_MINUTES);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
	
	private class EventCursorAdapter extends CursorAdapter {
		private LayoutInflater mLayoutInflator;
		private DatabaseInterface DBIface;

		@SuppressWarnings("deprecation")
		public EventCursorAdapter(Context ctx, Cursor c) {
			super(ctx, c);
			mLayoutInflator = LayoutInflater.from(ctx);
			DBIface = DatabaseInterface.get(ctx);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup group) {
			View v = mLayoutInflator.inflate(R.layout.event_list_item, group, false);
			return v;
		}

		@Override
		public void bindView(View view, final Context context, final Cursor cursor) {
			int id = cursor.getInt(cursor.getColumnIndex(EventProvider._ID));
			CalEvent thisEvent = DBIface.getEvent(id);

			if (thisEvent != null) {
				// a text view to show the event title
				TextView descriptionTV = (TextView) view.findViewById(R.id.eventText);
				descriptionTV.setText(thisEvent.getTitle());

				// a text view to show the event date span
				TextView dateTV = (TextView) view.findViewById(R.id.eventDate);
				String begin = thisEvent.getLocalBeginDate();
				String end = thisEvent.getLocalEndDate();
				String date;
				if (begin.equals(end)) {
					date = begin;
				} else {
					date = new String(begin + " - " + end);
				}
				dateTV.setText(date);

				// a text view to show the beginning and ending times for the event
				TextView timeTV = (TextView) view.findViewById(R.id.eventTime);
				StringBuilder timeSB = new StringBuilder(thisEvent.getLocalBeginTime() + " - "
						+ thisEvent.getLocalEndTime());

				if (thisEvent.isAllDay()) {
					timeSB = new StringBuilder(getBaseContext().getString(R.string.all_day));
				}
				timeTV.setText(timeSB.toString());

				// a color box to match the calendar color
				RelativeLayout calColorBox = (RelativeLayout) view.findViewById(R.id.calendarColor);
				calColorBox.setBackgroundColor(thisEvent.getDisplayColor());

				// an image button to show the ringer state for this event
				ImageView eventIV = (ImageView) view.findViewById(R.id.ringerState);
				eventIV.setImageDrawable(this.getEventIcon(thisEvent, context));
				eventIV.setContentDescription(getBaseContext().getString(
						R.string.icon_alt_text_normal));

				Animation iconAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
				iconAnim.setDuration(500);
				eventIV.startAnimation(iconAnim);
				iconAnim = null;

				// TODO find out how to animate the list items only when first displayed, this
				// animation will fire every time the event view is replaced
				/*
				 * Animation viewAnim = AnimationUtils.loadAnimation(context,
				 * android.R.anim.slide_in_left); viewAnim.setDuration(500);
				 * view.startAnimation(viewAnim);
				 */
			}
		}

		private Drawable getRingerIcon(Context ctx, int ringerType, Integer color) {
			Drawable icon = null;

			switch (ringerType) {
				case CalEvent.RINGER_TYPE_NORMAL:
					icon = getResources().getDrawable(R.drawable.ic_state_normal);
					break;
				case CalEvent.RINGER_TYPE_SILENT:
					icon = getResources().getDrawable(R.drawable.ic_state_silent);
					break;
				case CalEvent.RINGER_TYPE_VIBRATE:
					icon = getResources().getDrawable(R.drawable.ic_state_vibrate);
					break;
				case CalEvent.RINGER_TYPE_UNDEFINED:
					SharedPreferences preferences = ctx.getSharedPreferences(
							"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
					int defaultRinger = preferences.getInt("ringerType", DefPrefs.RINGER_TYPE);
					icon = getRingerIcon(ctx, defaultRinger, null);
					break;
				case CalEvent.RINGER_TYPE_IGNORE:
				default:
					// events that should be ignored are given a blank icon
					icon = getResources().getDrawable(R.drawable.blank);
			}

			// colorize the icon if a color has been provided
			if (color != null) {
				icon.mutate().setColorFilter(color, Mode.MULTIPLY);
			}

			return icon;
		}

		private boolean eventShouldSilence(Context ctx, CalEvent event) {
			boolean eventShouldSilence = true;
			SharedPreferences preferences = ctx.getSharedPreferences(
					"org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
			boolean silenceFreeTime = preferences.getBoolean("silenceFreeTime",
					DefPrefs.SILENCE_FREE_TIME);
			boolean silenceAllDay = preferences.getBoolean("silenceAllDay",
					DefPrefs.SILENCE_ALL_DAY);

			// if a custom ringer is set then the event should silence, otherwise it depends on the
			// event type and settings
			if (event.getRingerType() == CalEvent.RINGER_TYPE_UNDEFINED) {
				if ((event.isAllDay() && !silenceAllDay) || (event.isFreeTime() && !silenceFreeTime)) {
					eventShouldSilence = false;
				}
			}

			return eventShouldSilence;
		}

		private Drawable getEventIcon(CalEvent event, Context ctx) {
			Drawable icon = null;
			int defaultColor = 0xFFE8E8E8;

			if (eventShouldSilence(ctx, event)) {
				if (event.getRingerType() != CalEvent.RINGER_TYPE_UNDEFINED) {
					// a custom ringer has been applied
					icon = getRingerIcon(ctx, event.getRingerType(), event.getDisplayColor());
				} else {
					icon = getRingerIcon(ctx, CalEvent.RINGER_TYPE_UNDEFINED, defaultColor);
				}
			} else {
				icon = getRingerIcon(ctx, CalEvent.RINGER_TYPE_IGNORE, null);
			}

			return icon;
		}

	}

}
