/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
    private static final String TAG = "DatabaseInterface";
    private static final String[] projection = new String[]{
            Instances.TITLE,
            Instances.BEGIN,
            Instances.END,
            Instances.DESCRIPTION,
            Instances.EVENT_COLOR,
            Instances.ALL_DAY,
            Instances.AVAILABILITY,
            Instances._ID
    };
    private static DatabaseInterface mInstance;
    private static Context mAppContext = null;
    private static Prefs prefs = null;


    //imported
    private static final int EVENTS = 1;
    private static final int EVENT_ID = 2;
    private static final String PROVIDER_NAME = "org.ciasaboark.tacere.Events";
    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/event");
    private static final UriMatcher uriMatcher;
    private Context context;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "event", EVENTS);
        uriMatcher.addURI(PROVIDER_NAME, "event/#", EVENT_ID);
    }

    private static final String DB_NAME = "events.sqlite";
    private static final int VERSION = 1;
    private static final String TABLE_EVENTS = "events";
    //Database values
    private SQLiteDatabase eventsDB;

    //end imported


    private DatabaseInterface(Context context) {
        mAppContext = context;
        EventDatabaseHelper dbHelper = new EventDatabaseHelper(context);
        this.eventsDB = dbHelper.getWritableDatabase();
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

    public void pruneEventsAfter(long cutoff) {
        Cursor c = getEventCursor(0, cutoff);

        if (c.moveToFirst()) {
            do {
                int id = c.getInt(c.getColumnIndex(Columns._ID));
                long begin = c.getLong(c.getColumnIndex(Columns.BEGIN));
                if (begin > cutoff) {
                    deleteEventWithId(id);
                }
            } while (c.moveToNext());
        }
        c.close();
    }

    public boolean isDatabaseEmpty() {
        boolean isEmpty = false;
        Cursor cursor = getEventCursor();
        if (cursor.getCount() == 0) {
            isEmpty = true;
        }
        cursor.close();
        return isEmpty;
    }

    public void setRingerType(int eventId, int ringerType) {
        String mSelectionClause = Columns._ID + " = ?";
        String[] mSelectionArgs = {String.valueOf(eventId)};
        ContentValues values = new ContentValues();
        values.put(Columns.RINGER_TYPE, ringerType);
        eventsDB.update(TABLE_EVENTS, values,
                mSelectionClause, mSelectionArgs); //TODO test
    }


    public Cursor getEventCursor() {
        Cursor cursor = getOrderedEventCursor(Columns.BEGIN);
        return cursor;
    }

    private Cursor getOrderedEventCursor(String order) {
        Cursor cursor = eventsDB.query(TABLE_EVENTS, null, null, null, null, null, order, null);
        return cursor;
    }

    public Deque<CalEvent> allActiveEvents() {
        // sync the db and prune old events
        syncCalendarDb();

        Deque<CalEvent> events = new ArrayDeque<CalEvent>();
        Cursor cursor = getEventCursor();
        //TODO test cursor works
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

    // returns the event that matches the given Instance id, null if no match
    public CalEvent getEvent(int id) throws NoSuchEventException {
        //TODO test
        Cursor cursor = getEventCursor();
        CalEvent thisEvent = null;
        if (cursor.moveToFirst()) {
            thisEvent = new CalEvent();
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

            do {
                // the cursor
                String event_title = calendarCursor.getString(col_title);
                long event_begin = calendarCursor.getLong(col_begin);
                long event_end = calendarCursor.getLong(col_end);
                String event_description = calendarCursor.getString(col_description);
                int event_displayColor = calendarCursor.getInt(col_displayColor);
                int event_allDay = calendarCursor.getInt(col_allDay);
                int event_availability = calendarCursor.getInt(col_availability);
                int id = calendarCursor.getInt(col_id);

                // if the event is already in the local database then we need to preserve
                // the ringerType, all other values should be read from the system calendar
                // database
                CalEvent newEvent = new CalEvent();
                try {
                    CalEvent oldEvent = getEvent(id);
                    newEvent.setRingerType(oldEvent.getRingerType());
                } catch (NoSuchEventException e) {
                    // its perfectly reasonable that this event does not exist within our database
                    // yet
                }

                newEvent.setId(id);
                newEvent.setCal_id(id);
                newEvent.setTitle(event_title);
                newEvent.setBegin(event_begin);
                newEvent.setEnd(event_end);
                newEvent.setDescription(event_description);
                newEvent.setDisplayColor(event_displayColor);
                newEvent.setIsFreeTime(event_availability == 0);
                newEvent.setIsAllDay(event_allDay == 1);

                // inserting an event with the same id will clobber all previous data, completing
                // the synchronization of this event
                insertEvent(newEvent);
            } while (calendarCursor.moveToNext());
        }
        calendarCursor.close();

        pruneRemovedEvents(days);
    }

    // remove all events from the database with an ending date
    // + before the given time
    public void pruneEventsBefore(long cutoff) {
        Cursor c = getEventCursor(0, cutoff);
        if (c.moveToFirst()) {
            do {
                int id = c.getInt(c.getColumnIndex(Columns._ID));
                long end = c.getLong(c.getColumnIndex(Columns.END));
                if (end <= cutoff) {
                    deleteEventWithId(id);
                }
            } while (c.moveToNext());
        }
        c.close();
    }

    private void deleteEventWithId(long id) {
        String selection = Columns._ID + " = ?";
        eventsDB.delete(TABLE_EVENTS, selection, new String[]{String.valueOf(id)});
    }

    private Cursor getEventCursor(long begin, long end) {
        String[] args = {String.valueOf(begin), String.valueOf(end)};
        String selection = Columns.BEGIN + " >= ? AND " + Columns.END + " <= ?";
        Cursor cursor = eventsDB.query(TABLE_EVENTS, null, selection, args, null, null, Columns.BEGIN, null);
        return cursor;
    }

    private Cursor getCalendarCursor(long begin, long end) {
        return Instances.query(mAppContext.getContentResolver(), projection,
                begin, end);
    }

    public void insertEvent(CalEvent e) {
        if (e.isValid()) {    //TODO event validity should be checked here, not within the event
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

            long rowID = eventsDB.insertWithOnConflict(TABLE_EVENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "inserted event " + e.toString() + " as row " + rowID);
        } else {
            throw new IllegalArgumentException(
                    "DatabaseInterface:insertEvent given an event with blank values");
        }
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

        Cursor loc_cursor = getEventCursor();
        if (loc_cursor.moveToFirst()) {
            do {
                long loc_id = loc_cursor.getLong(loc_cursor.getColumnIndex(Columns._ID));
                if (!cal_ids.contains(loc_id)) {
                    deleteEventWithId(loc_id);
                }
            } while (loc_cursor.moveToNext());
        }
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

        Cursor cursor = getOrderedEventCursor(Columns.BEGIN);
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
            try {
                nextEvent = getEvent(id);
            } catch (NoSuchEventException e) {
                Log.e(TAG, "unable to retrieve event with id of " + id
                        + " even though a record with this id exists in the database");
            }
        }
        cursor.close();

        return nextEvent;
    }

    private class EventDatabaseHelper extends SQLiteOpenHelper {


        public EventDatabaseHelper(Context context) {
            super(context, DB_NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_EVENTS + " ( " + Columns._ID + " integer primary key," +
                    Columns.TITLE + " varchar(100)," + Columns.DESCRIPTION + " varchar(100)," +
                    Columns.BEGIN + " integer," + Columns.END + " integer," +
                    Columns.IS_ALLDAY + " integer," + Columns.IS_FREETIME + " integer," +
                    Columns.RINGER_TYPE + " integer," + Columns.DISPLAY_COLOR + " integer," +
                    Columns.CAL_ID + " integer)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
            // TODO Add code to update the database from a previous version
        }
    }
}
