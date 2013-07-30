package org.ciasaboark.tacere;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Instances;
import android.util.Log;

public class DatabaseInterface {
	private static DatabaseInterface mInstance;
	private static Context mAppContext;
	private static final String TAG = "DatabaseInterface";
	
	private DatabaseInterface(Context context) {
		mAppContext = context;
	}
	
	public static DatabaseInterface get(Context context) {
		if (mInstance == null) {
			mInstance = new DatabaseInterface(context);
		}
		return mInstance;
	}
	
	//remove all events from the database with an ending date
	//+ before the given time
	public void pruneEventsBefore(long cutoff) {
		/* this should work, but doesn't
		ContentResolver cr = mAppContext.getContentResolver();
		String[] selectionArgs = new String[] {String.valueOf(cutoff)};
		cr.delete(EventProvider.CONTENT_URI, EventProvider.END + "<?", selectionArgs);
       */
		
		//doing things the long way
		Cursor c;
		Uri allEvents = Uri.parse("content://org.ciasaboark.tacere.Events/events");
		CursorLoader cl = new CursorLoader(mAppContext, allEvents,null, null, null, EventProvider._ID);
		c = cl.loadInBackground();
		if (c.moveToFirst()) {
			do {
				int id = c.getInt(c.getColumnIndex(EventProvider._ID));
				long end = c.getLong(c.getColumnIndex(EventProvider.END));
				if (end <= cutoff) {
					ContentResolver cr = mAppContext.getContentResolver();
					cr.delete(EventProvider.CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), null, null);
				}
			} while (c.moveToNext());
		}
		c.close();
	}
	
	public void pruneEventsAfter(long cutoff) {
		Cursor c;
		Uri allEvents = Uri.parse("content://org.ciasaboark.tacere.Events/events");
		CursorLoader cl = new CursorLoader(mAppContext, allEvents,null, null, null, EventProvider._ID);
		c = cl.loadInBackground();
		if (c.moveToFirst()) {
			do {
				int id = c.getInt(c.getColumnIndex(EventProvider._ID));
				long begin = c.getLong(c.getColumnIndex(EventProvider.BEGIN));
				if (begin > cutoff) {
					ContentResolver cr = mAppContext.getContentResolver();
					cr.delete(EventProvider.CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), null, null);
				}
			} while (c.moveToNext());
		}
		c.close();
	}
	
	//removes events from the local database that can not be found in the calendar
	//+ database in the next n days
	private void pruneRemovedEvents(long cutoff) {
		//we need to make sure not to remove events from our database that might still
		//+ be ongoing due to the event buffer
		SharedPreferences preferences = mAppContext.getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
		int bufferMinutes = preferences.getInt("bufferMinutes", DefPrefs.bufferMinutes);
		long begin = System.currentTimeMillis() - 1000 * 60 * (long)bufferMinutes;
		long end = begin + 1000 * 60 * 60 * 24 * (long)cutoff; //pull all events n days from now
		
		String[] projection = new String[]{"title", "begin", "end", "description", "displayColor", "allDay", "availability", "_id", "event_id"};
		Cursor cal_cursor = Instances.query(mAppContext.getContentResolver(), projection, begin, end);
		ArrayList<Long> cal_ids = new ArrayList<Long>();
		
		if (cal_cursor.moveToFirst()) {
    		int col_id = cal_cursor.getColumnIndex(projection[7]);
    		do {
    			long event_id = Long.valueOf(cal_cursor.getString(col_id));
    			cal_ids.add(event_id);
    		} while (cal_cursor.moveToNext());
		}
		cal_cursor.close();
		
		Cursor loc_cursor = getCursor(EventProvider._ID);
		if (loc_cursor.moveToFirst()) {
			do {
				long loc_id = loc_cursor.getLong(loc_cursor.getColumnIndex(EventProvider._ID));
				if (!cal_ids.contains(loc_id)) {
					String loc_title = loc_cursor.getString(loc_cursor.getColumnIndex(EventProvider.TITLE));
					Log.d(TAG, "Event with id:" + loc_id + " and title: " + loc_title + " not found in calendar DB, removing");
					ContentResolver cr = mAppContext.getContentResolver();
					cr.delete(EventProvider.CONTENT_URI.buildUpon().appendPath(String.valueOf(loc_id)).build(), null, null);
				}
			} while (loc_cursor.moveToNext());
		}
	}
	
	//sync the calendar and the local database for the given number of days
	public void update(int days) {
		days = Math.abs(days);
		long begin = System.currentTimeMillis();
		long end = begin + 1000 * 60 * 60 * 24 * (long)days; //pull all events n days from now
		
		String[] projection = new String[]{"title", "begin", "end", "description", "displayColor", "allDay", "availability", "_id", "event_id"};
		Cursor cal_cursor = Instances.query(mAppContext.getContentResolver(), projection, begin, end);
		
		if (cal_cursor.moveToFirst()) {
			int col_title = cal_cursor.getColumnIndex(projection[0]);
    		int col_begin = cal_cursor.getColumnIndex(projection[1]);
			int col_end = cal_cursor.getColumnIndex(projection[2]);
    		int col_description = cal_cursor.getColumnIndex(projection[3]);
    		int col_displayColor = cal_cursor.getColumnIndex(projection[4]);
    		int col_allDay = cal_cursor.getColumnIndex(projection[5]);
    		int col_availability = cal_cursor.getColumnIndex(projection[6]);
    		int col_id = cal_cursor.getColumnIndex(projection[7]);
    		int col_cal_id = cal_cursor.getColumnIndex(projection[8]);
    		
    		do {
    			String event_title = cal_cursor.getString(col_title);
    			String event_begin = cal_cursor.getString(col_begin);
    			String event_end = cal_cursor.getString(col_end);
    			String event_description = cal_cursor.getString(col_description);
    			String event_displayColor = cal_cursor.getString(col_displayColor);
    			String event_allDay = cal_cursor.getString(col_allDay);
    			String event_availability = cal_cursor.getString(col_availability);
    			String event_id = cal_cursor.getString(col_id);
    			String event_cal_id = cal_cursor.getString(col_cal_id);
    			
    			//if the event is already in the local database then we need to preserve
    			//+ the ringerType
    			CalEvent newEvent = new CalEvent(mAppContext);
    			CalEvent oldEvent = getEvent(Integer.valueOf(event_id));
    			if (oldEvent != null) {
    				newEvent.setRingerType(oldEvent.getRingerType());
    			}
    			
    			newEvent.setId(Integer.valueOf(event_id));
    			newEvent.setCal_id(Integer.valueOf(event_cal_id));
    			newEvent.setTitle(event_title);
    			newEvent.setBegin(Long.valueOf(event_begin));
    			newEvent.setEnd(Long.valueOf(event_end));
    			newEvent.setDescription(event_description);
    			newEvent.setDisplayColor(Integer.valueOf(event_displayColor));
    			if (Integer.valueOf(event_availability) == 0) {
    				newEvent.setIsFreeTime(true);
    			} else {
    				newEvent.setIsFreeTime(false);
    			}
    			if (Integer.valueOf(event_allDay) == 1) {
    				newEvent.setIsAllDay(true);
    			} else {
    				newEvent.setIsAllDay(false);
    			}
    			
    			insertEvent(newEvent);
    		} while (cal_cursor.moveToNext());
		}
		cal_cursor.close();
		
		pruneRemovedEvents(days);
	}
	
	
	//returns the event that matches the given Instance id, null if no match
	public CalEvent getEvent(int id) {
		Uri thisEventUri = Uri.parse("content://org.ciasaboark.tacere.Events/events");
		CursorLoader cl = new CursorLoader(mAppContext, thisEventUri.buildUpon().appendPath(String.valueOf(id)).build(), null, null, null, EventProvider._ID);
		Cursor cursor = cl.loadInBackground();
		//cr.delete(EventProvider.CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), null, null);
		CalEvent thisEvent = null;
		if (cursor.moveToFirst()) {
			thisEvent = new CalEvent(mAppContext);
			do {
				int eventID = cursor.getInt(cursor.getColumnIndex(EventProvider._ID));
				if (eventID == id) {
					//this event matched
					thisEvent.setCal_id(cursor.getInt(cursor.getColumnIndex(EventProvider.CAL_ID)));
					thisEvent.setId(cursor.getInt(cursor.getColumnIndex(EventProvider._ID)));
					thisEvent.setTitle(cursor.getString(cursor.getColumnIndex(EventProvider.TITLE)));
					thisEvent.setBegin(cursor.getLong(cursor.getColumnIndex(EventProvider.BEGIN)));
					thisEvent.setEnd(cursor.getLong(cursor.getColumnIndex(EventProvider.END)));
					thisEvent.setDescription(cursor.getString(cursor.getColumnIndex(EventProvider.DESCRIPTION)));
					thisEvent.setRingerType(cursor.getInt(cursor.getColumnIndex(EventProvider.RINGER_TYPE)));
					thisEvent.setDisplayColor(cursor.getInt(cursor.getColumnIndex(EventProvider.DISPLAY_COLOR)));
					int isFreeTime = cursor.getInt(cursor.getColumnIndex(EventProvider.IS_FREETIME));
					if (isFreeTime == 1) {
						thisEvent.setIsFreeTime(true);
					} else {
						thisEvent.setIsFreeTime(false);
					}
					int isAllDay = cursor.getInt(cursor.getColumnIndex(EventProvider.IS_ALLDAY));
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
		return thisEvent;
	}
	
	public void insertEvent(CalEvent e) {
		if (e.isValid()) {
			ContentValues cv = new ContentValues();
			cv.put(EventProvider._ID, e.getId());
			cv.put(EventProvider.TITLE, e.getTitle());
			cv.put(EventProvider.BEGIN, e.getBegin());
			cv.put(EventProvider.END, e.getEnd());
			cv.put(EventProvider.DESCRIPTION, e.getDescription());
			cv.put(EventProvider.RINGER_TYPE, e.getRingerType());
			cv.put(EventProvider.DISPLAY_COLOR, e.getDisplayColor());
			if (e.getIsAllDay()) {
				cv.put(EventProvider.IS_ALLDAY, 1);
			} else {
				cv.put(EventProvider.IS_ALLDAY, 0);
			}
			if (e.getIsFreeTime()) {
				cv.put(EventProvider.IS_FREETIME, 0);
			} else {
				cv.put(EventProvider.IS_FREETIME, 1);
			}
			cv.put(EventProvider.CAL_ID, e.getCal_id());
			
			mAppContext.getContentResolver().insert(EventProvider.CONTENT_URI, cv);
		} else {
			throw new IllegalArgumentException("DatabaseInterface:insertEvent given an event with blank values");
		}
	}
	
	public void setRingerType(int id, int ringerType) {
		String mSelectionClause = EventProvider._ID +  " = ?";
		String[] mSelectionArgs = {String.valueOf(id)};
		ContentValues values = new ContentValues();
		values.put(EventProvider.RINGER_TYPE, ringerType);
		mAppContext.getContentResolver().update(EventProvider.CONTENT_URI, values, mSelectionClause, mSelectionArgs);
	}
	
	public Cursor getCursor(String sortBy) {
		Uri allEvents = Uri.parse("content://org.ciasaboark.tacere.Events/events");
		if (sortBy == null) {
			throw new IllegalArgumentException("DatabaseInterface:getCursor(String) given a null value");
		} else {
			return new CursorLoader(mAppContext, allEvents, null, null, null, sortBy).loadInBackground();
		}
	}
	
	//the default sort is by the unique event ID
	public Cursor getCursor() {
		return getCursor(EventProvider._ID);
	}
	
	public void printEvents() {
		Cursor c = getCursor();
		if (c.moveToFirst()) {
			do {
				int id = c.getInt(c.getColumnIndex(EventProvider._ID));
				CalEvent e = getEvent(id);
				Log.i(TAG, "Event id:" + e.getCal_id() + " Instance id:" + e.getId() + " Title:" + e.getTitle() + " Begins:" + e.getLocalBeginDate() + " - " + e.getLocalBeginTime());
			} while (c.moveToNext());
		}
		c.close();
	}
}
