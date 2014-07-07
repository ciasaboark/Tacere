/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 * 
 * Released under the BSD license. For details see the COPYING file.
 */

package org.ciasaboark.tacere.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Instances;
import android.util.Log;

import org.ciasaboark.tacere.activity.CalEvent;
import org.ciasaboark.tacere.prefs.Prefs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

public class DatabaseInterface {
	private static DatabaseInterface mInstance;
	private static Context mAppContext = null;
    private static Prefs prefs = null;
	private static final String TAG = "DatabaseInterface";
    private static String[] projection = new String[] {
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_COLOR,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.AVAILABILITY,
            CalendarContract.Events.ORIGINAL_ID,
            CalendarContract.Events.ORIGINAL_SYNC_ID
    };


	private DatabaseInterface(Context context) {
		mAppContext = context;
	}

	public static DatabaseInterface getInstance(Context context) {
		if (context == null) {
            throw new IllegalArgumentException("Context can not be null");
        }

        if (mInstance == null) {
			mInstance = new DatabaseInterface(context);
		}
        if (prefs == null) {
            prefs = new Prefs(context);
        }
        if (mAppContext == null) {
            mAppContext = context;
        }
		return mInstance;
	}

	// remove all events from the database with an ending date
	// + before the given time
	public void pruneEventsBefore(long cutoff) {
		/*-
		 * this should work, but doesn't
		 * ContentResolver cr = mAppContext.getContentResolver();
		 * String[] selectionArgs = new String[] {String.valueOf(cutoff)};
		 * cr.delete(EventProvider.CONTENT_URI, EventProvider.END + "<?", selectionArgs);
		 */

		// doing things the long way
		Cursor c;
		Uri allEvents = Uri.parse("content://org.ciasaboark.tacere.Events/events");
		CursorLoader cl = new CursorLoader(mAppContext, allEvents, null, null, null,
				Columns._ID);
		c = cl.loadInBackground();
		if (c.moveToFirst()) {
			do {
				int id = c.getInt(c.getColumnIndex(Columns._ID));
				long end = c.getLong(c.getColumnIndex(Columns.END));
				if (end <= cutoff) {
					ContentResolver cr = mAppContext.getContentResolver();
					cr.delete(EventProvider.CONTENT_URI.buildUpon().appendPath(String.valueOf(id))
							.build(), null, null);
				}
			} while (c.moveToNext());
		}
		c.close();
	}

	public void pruneEventsAfter(long cutoff) {
		Cursor c;
		Uri allEvents = Uri.parse("content://org.ciasaboark.tacere.Events/events");
		CursorLoader cl = new CursorLoader(mAppContext, allEvents, null, null, null,
				Columns._ID);
		c = cl.loadInBackground();
		if (c.moveToFirst()) {
			do {
				int id = c.getInt(c.getColumnIndex(Columns._ID));
				long begin = c.getLong(c.getColumnIndex(Columns.BEGIN));
				if (begin > cutoff) {
					ContentResolver cr = mAppContext.getContentResolver();
					cr.delete(EventProvider.CONTENT_URI.buildUpon().appendPath(String.valueOf(id))
							.build(), null, null);
				}
			} while (c.moveToNext());
		}
		c.close();
	}

    private Cursor getCalendarCursor(long begin, long end) {
        Cursor calendarCursor = Instances.query(mAppContext.getContentResolver(), projection,
                begin, end);
        return calendarCursor;
    }

	// removes events from the local database that can not be found in the calendar
	// + database in the next n days
	private void pruneRemovedEvents(long cutoff) {
		// we need to make sure not to remove events from our database that might still
		// + be ongoing due to the event buffer
		int bufferMinutes = prefs.getBufferMinutes();
		long begin = System.currentTimeMillis() - (1000 * 60 * (long) bufferMinutes);
		long end = begin + 1000 * 60 * 60 * 24 * (long) cutoff; // pull all events n days from now


        Cursor cal_cursor = getCalendarCursor(begin, end);

		ArrayList<Long> cal_ids = new ArrayList<Long>();

		if (cal_cursor.moveToFirst()) {
			int col_id = cal_cursor.getColumnIndex(projection[7]);
			do {
				long event_id = Long.valueOf(cal_cursor.getString(col_id));
				cal_ids.add(event_id);
			} while (cal_cursor.moveToNext());
		}
		cal_cursor.close();

		Cursor loc_cursor = getCursor(Columns._ID);
		if (loc_cursor.moveToFirst()) {
			do {
				long loc_id = loc_cursor.getLong(loc_cursor.getColumnIndex(Columns._ID));
				if (!cal_ids.contains(loc_id)) {
					// String loc_title =
					// loc_cursor.getString(loc_cursor.getColumnIndex(EventProvider.TITLE));
					// Log.d(TAG, "Event with id:" + loc_id + " and title: " + loc_title +
					// " not found in calendar DB, removing");
					ContentResolver cr = mAppContext.getContentResolver();
					cr.delete(
							EventProvider.CONTENT_URI.buildUpon()
									.appendPath(String.valueOf(loc_id)).build(), null, null);
				}
			} while (loc_cursor.moveToNext());
		}
	}

	// sync the calendar and the local database for the given number of days
	public void update(int days) {
		days = Math.abs(days);
		long begin = System.currentTimeMillis();
		long end = begin + 1000 * 60 * 60 * 24 * (long) days; // pull all events n days from now

        Cursor calendarCursor = getCalendarCursor(begin, end);

		if (calendarCursor.moveToFirst()) {
			int col_title = calendarCursor.getColumnIndex(projection[0]);
			int col_begin = calendarCursor.getColumnIndex(projection[1]);
			int col_end = calendarCursor.getColumnIndex(projection[2]);
			int col_description = calendarCursor.getColumnIndex(projection[3]);
			int col_displayColor = calendarCursor.getColumnIndex(projection[4]);
			int col_allDay = calendarCursor.getColumnIndex(projection[5]);
			int col_availability = calendarCursor.getColumnIndex(projection[6]);
			int col_id = calendarCursor.getColumnIndex(projection[7]);
			int col_cal_id = calendarCursor.getColumnIndex(projection[8]);

			do {
				// the cursor
				String event_title = calendarCursor.getString(col_title);
				long event_begin = calendarCursor.getLong(col_begin);
				long event_end = calendarCursor.getLong(col_end);
				String event_description = calendarCursor.getString(col_description);
				int event_displayColor = calendarCursor.getInt(col_displayColor);
				int event_allDay = calendarCursor.getInt(col_allDay);
				int event_availability = calendarCursor.getInt(col_availability);
				int event_id = calendarCursor.getInt(col_id);
				int event_cal_id = calendarCursor.getInt(col_cal_id);

				// if the event is already in the local database then we need to preserve
				// the ringerType, all other values should be read from the system calendar
				// database
				CalEvent newEvent = new CalEvent(mAppContext);
				try {
					CalEvent oldEvent = getEvent(Integer.valueOf(event_id));
					newEvent.setRingerType(oldEvent.getRingerType());
				} catch (NoSuchEventException e) {
					// its perfectly reasonable that this event does not exist within our database
					// yet
				}

				newEvent.setId(event_id);
				newEvent.setCal_id(event_cal_id);
				newEvent.setTitle(event_title);
				newEvent.setBegin(event_begin);
				newEvent.setEnd(event_end);
				newEvent.setDescription(event_description);
				newEvent.setDisplayColor(event_displayColor);
				newEvent.setIsFreeTime(event_availability == 0 ? true : false);
				newEvent.setIsAllDay(event_allDay == 1 ? true : false);

				// inserting an event with the same id will clobber all previous data, completing
				// the synchronization of this event
				insertEvent(newEvent);
			} while (calendarCursor.moveToNext());
		}
		calendarCursor.close();

		pruneRemovedEvents(days);
	}

	// returns the event that matches the given Instance id, null if no match
	public CalEvent getEvent(int id) throws NoSuchEventException {
		Uri thisEventUri = Uri.parse("content://org.ciasaboark.tacere.Events/events");
		CursorLoader cl = new CursorLoader(mAppContext, thisEventUri.buildUpon()
				.appendPath(String.valueOf(id)).build(), null, null, null, Columns._ID);
		Cursor cursor = cl.loadInBackground();
		CalEvent thisEvent = null;
		if (cursor.moveToFirst()) {
			thisEvent = new CalEvent(mAppContext);
			do {
				int eventID = cursor.getInt(cursor.getColumnIndex(Columns._ID));
				if (eventID == id) {
					// this event matched
					thisEvent.setCal_id(cursor.getInt(cursor.getColumnIndex(Columns.CAL_ID)));
					thisEvent.setId(cursor.getInt(cursor.getColumnIndex(Columns._ID)));
					thisEvent
							.setTitle(cursor.getString(cursor.getColumnIndex(Columns.TITLE)));
					thisEvent.setBegin(cursor.getLong(cursor.getColumnIndex(Columns.BEGIN)));
					thisEvent.setEnd(cursor.getLong(cursor.getColumnIndex(Columns.END)));
					thisEvent.setDescription(cursor.getString(cursor
							.getColumnIndex(Columns.DESCRIPTION)));
					thisEvent.setRingerType(cursor.getInt(cursor
							.getColumnIndex(Columns.RINGER_TYPE)));
					thisEvent.setDisplayColor(cursor.getInt(cursor
							.getColumnIndex(Columns.DISPLAY_COLOR)));
					int isFreeTime = cursor
							.getInt(cursor.getColumnIndex(Columns.IS_FREETIME));
					if (isFreeTime == 1) {
						thisEvent.setIsFreeTime(true);
					} else {
						thisEvent.setIsFreeTime(false);
					}
					int isAllDay = cursor.getInt(cursor.getColumnIndex(Columns.IS_ALLDAY));
					if (isAllDay == 1) {
						thisEvent.setIsAllDay(true);
					} else {
						thisEvent.setIsAllDay(false);
					}
					break;
				}
			} while (cursor.moveToNext());

		}
		cursor.close();
		if (thisEvent == null) {
			throw new NoSuchEventException(TAG + " can not find event with given id " + id);
		}
		return thisEvent;
	}

	public void insertEvent(CalEvent e) {
		if (e.isValid()) {	//TODO event validity should be checked here, not within the event
			ContentValues cv = new ContentValues();
			cv.put(Columns._ID, e.getId());
			cv.put(Columns.TITLE, e.getTitle());
			cv.put(Columns.BEGIN, e.getBegin());
			cv.put(Columns.END, e.getEnd());
			cv.put(Columns.DESCRIPTION, e.getDescription());
			cv.put(Columns.RINGER_TYPE, e.getRingerType());
			cv.put(Columns.DISPLAY_COLOR, e.getDisplayColor());
			if (e.isAllDay()) {
				cv.put(Columns.IS_ALLDAY, 1);
			} else {
				cv.put(Columns.IS_ALLDAY, 0);
			}
			if (e.isFreeTime()) {
				cv.put(Columns.IS_FREETIME, 0);
			} else {
				cv.put(Columns.IS_FREETIME, 1);
			}
			cv.put(Columns.CAL_ID, e.getCal_id());

			mAppContext.getContentResolver().insert(EventProvider.CONTENT_URI, cv);
		} else {
			throw new IllegalArgumentException(
					"DatabaseInterface:insertEvent given an event with blank values");
		}
	}

	public void setRingerType(int eventId, int ringerType) {
		String mSelectionClause = Columns._ID + " = ?";
		String[] mSelectionArgs = { String.valueOf(eventId) };
		ContentValues values = new ContentValues();
		values.put(Columns.RINGER_TYPE, ringerType);
		mAppContext.getContentResolver().update(EventProvider.CONTENT_URI, values,
				mSelectionClause, mSelectionArgs);
	}
	
	public Deque<CalEvent> allActiveEvents() {
		// sync the db and prune old events
		syncCalendarDb();

		Deque<CalEvent> events = new ArrayDeque<CalEvent>();

		Cursor cursor = getCursor(Columns.BEGIN);

		long beginTime = System.currentTimeMillis()
				- (CalEvent.MILLISECONDS_IN_MINUTE * (long) prefs.getBufferMinutes());
		long endTime = System.currentTimeMillis()
				+ (CalEvent.MILLISECONDS_IN_MINUTE * (long) prefs.getBufferMinutes());

		if (cursor.moveToFirst()) {
			do {
				int id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
				try {
					CalEvent e = getEvent(id);
					if (e.isActiveBetween(beginTime, endTime)) {
						events.add(e);
					}
				} catch (NoSuchEventException e) {
					// this should only be reachable if the database has become corrupted
					Log.e(TAG, "unable to get event with id " + id
							+ "while getting a list of all events. This should not have "
							+ "happened unless the database has become corrupted");
				}

			} while (cursor.moveToNext());
		}

		cursor.close();

		return events;
	}
	
	/**
	 * Query the internal database for the next event.
	 * 
	 * @return the next event in the database, or null if there are no events
	 */
	public CalEvent nextEvent() {
		// sync the database and prune old events first to make sure that we don't return an event
		// that has already expired
		syncCalendarDb();
		CalEvent nextEvent = null;

		Cursor cursor = getCursor(Columns.BEGIN);
		if (cursor.moveToFirst()) {
			int id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
			try {
				nextEvent = getEvent(id);
			} catch (NoSuchEventException e) {
				Log.e(TAG, "unable to retreive event with id of " + id
						+ " even though a record with this id exists in the database");
			}
		}
		cursor.close();

		return nextEvent;
	}
	
	/**
	 * Sync the internal database with the system calendar database. Forward syncing is limited to
	 * the period specified in preferences. Old events are pruned.
	 */
	public void syncCalendarDb() {
		// update(n) will also remove all events not found in the next n days, so we
		// + need to keep this in sync with the users preferences.
		update(prefs.getLookaheadDays());
		pruneEventsBefore(System.currentTimeMillis() - CalEvent.MILLISECONDS_IN_MINUTE
				* (long) prefs.getBufferMinutes());
	}

	// Return a cursor, sorting by the column given.
	// + See EventProvider for valid column names
	public Cursor getCursor(String sortBy) {
		Uri allEvents = Uri.parse("content://org.ciasaboark.tacere.Events/events");
		if (sortBy == null) {
			throw new IllegalArgumentException(
					"DatabaseInterface:getCursor(String) given a null value");
		} else {
			return new CursorLoader(mAppContext, allEvents, null, null, null, sortBy)
					.loadInBackground();
		}
	}

	// the default sort is by the unique event ID
	public Cursor getCursor() {
		return getCursor(Columns._ID);
	}

	private void printEvents() {
		Cursor c = getCursor();
		if (c.moveToFirst()) {
			do {
				int id = c.getInt(c.getColumnIndex(Columns._ID));
				try {
					CalEvent e = getEvent(id);
					Log.i(TAG, "Event id:" + e.getCal_id() + " Instance id:" + e.getId()
							+ " Title:" + e.getTitle() + " Begins:" + e.getLocalBeginDate() + " - "
							+ e.getLocalBeginTime());
				} catch (NoSuchEventException e) {
					Log.e(TAG,
							"printEvents() error getting event with id " + id + ": "
									+ e.getMessage());
				}
			} while (c.moveToNext());
		}
		c.close();
	}
}
