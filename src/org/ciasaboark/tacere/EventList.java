package org.ciasaboark.tacere;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract.Instances;
import android.util.Log;

public class EventList {
	private static final String TAG = "EventList";
	private static EventList sEventList;
	private Context mAppContext;
	private ArrayList<CalEvent> mEvents;
	
	private EventList(Context appContext) {
		mAppContext = appContext;
		mEvents = getEventList(7);
	}
	
	public static EventList get(Context c) {
		if (sEventList == null) {
			sEventList = new EventList(c.getApplicationContext());
		}
		return sEventList;
	}
	
	public ArrayList<CalEvent> getEvents() {
		return mEvents;
	}
	
	//Return the element at given position (0 base)
	public CalEvent getEventAtPosition(int position) {
		CalEvent result;
		try {
			result = this.mEvents.get(position);
		} catch (IndexOutOfBoundsException e) {
			Log.e(TAG, "Index " + position + " out of bounds");
			e.printStackTrace();
			result = null;
		}
		
		return result;
	}
	
	/* Returns the first event that matches the given id.
	 * repeating events share the same id, so this can not
	 * be considered to be unique.
	 */
	public CalEvent getEvent(Integer id) {
		CalEvent result = null;
		for (CalEvent e : mEvents) {
			if (e.getId() == id) {
				result = e;
				break;
			}
		}
		return result;
	}
	
	public void updateEventList(int days) {
		if (days > 0) {
			mEvents = getEventList(days);
		} else {
			mEvents = getEventList(1);
		}
	}
	
	public void updateEventList() {
		mEvents = getEventList(7);  //defaults to 7 days
	}
	
	private ArrayList<CalEvent> getEventList(int days) {
		days = Math.abs(days);
		ArrayList<CalEvent> events = new ArrayList<CalEvent>();
		long begin = System.currentTimeMillis();
		long end = begin + 1000 * 60 * 60 * 24 * (long)days; //pull all events n days from now
		
		String[] projection = new String[]{"title", "begin", "end", "description", "displayColor", "allDay", "availability", "event_id" };
		Cursor cursor = Instances.query(mAppContext.getContentResolver(), projection, begin, end);
		
		if (cursor.moveToFirst()) {
			int col_title = cursor.getColumnIndex(projection[0]);
    		int col_begin = cursor.getColumnIndex(projection[1]);
			int col_end = cursor.getColumnIndex(projection[2]);
    		int col_description = cursor.getColumnIndex(projection[3]);
    		int col_displayColor = cursor.getColumnIndex(projection[4]);
    		int col_allDay = cursor.getColumnIndex(projection[5]);
    		int col_availability = cursor.getColumnIndex(projection[6]);
    		int col_id = cursor.getColumnIndex(projection[7]);
    		
    		do {
    			String event_title = cursor.getString(col_title);
    			String event_begin = cursor.getString(col_begin);
    			String event_end = cursor.getString(col_end);
    			String event_description = cursor.getString(col_description);
    			String event_displayColor = cursor.getString(col_displayColor);
    			String event_allDay = cursor.getString(col_allDay);
    			String event_availability = cursor.getString(col_availability);
    			String event_id = cursor.getString(col_id);
    			
    			CalEvent event = new CalEvent(mAppContext);
    			event.setId(Integer.valueOf(event_id));
    			event.setTitle(event_title);
    			event.setBegin(Long.valueOf(event_begin));
    			event.setEnd(Long.valueOf(event_end));
    			event.setDescription(event_description);
    			event.setDisplayColor(Integer.valueOf(event_displayColor));
    			if (Integer.valueOf(event_availability) == 0) {
    				event.setIsFreeTime(false);
    			} else {
    				event.setIsFreeTime(true);
    			}
    			if (Integer.valueOf(event_allDay) == 1) {
    				event.setIsAllDay(true);
    			} else {
    				event.setIsAllDay(false);
    			}
    			event.setId(Integer.valueOf(event_id));
    			
    			if (event.getDescription().contains("ringer=")) {
    				//the event already has a set ringer type
    				if (event.getDescription().contains("ringer=normal")) {
    					event.setRingerType(CalEvent.RINGER_TYPE_NORMAL);
    				} else if (event.getDescription().contains("ringer=vibrate")) {
    					event.setRingerType(CalEvent.RINGER_TYPE_VIBRATE);
    				} else if (event.getDescription().contains("ringer=silent")) {
    					event.setRingerType(CalEvent.RINGER_TYPE_SILENT);
    				} else {
    					Log.e(TAG, "event " + event_title + " id:" + event_id + " has an unknown ringer type set");
    				}
    			}
    			
    			events.add(event);
    		} while (cursor.moveToNext());
			
		}
		
		cursor.close();
		
		return events;
	}
	
	
}