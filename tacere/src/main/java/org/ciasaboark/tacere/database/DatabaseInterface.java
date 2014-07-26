/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Instances;
import android.util.Log;

import org.ciasaboark.tacere.prefs.Prefs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static android.provider.CalendarContract.Calendars;

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
            Instances._ID,
            CalendarContract.Events.CALENDAR_ID
    };

    private static final String DB_NAME = "events.sqlite";
    private static final int VERSION = 1;
    private static final String TABLE_EVENTS = "events";
    private static final long MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;
    private static DatabaseInterface instance;
    private static Context context = null;
    private static Prefs prefs = null;
    private SQLiteDatabase eventsDB;


    private DatabaseInterface(Context context) {
        DatabaseInterface.context = context;
        EventDatabaseHelper dbHelper = new EventDatabaseHelper(context);
        this.eventsDB = dbHelper.getWritableDatabase();
    }

    public static DatabaseInterface getInstance(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        context = ctx;
        if (instance == null) {
            instance = new DatabaseInterface(context);
        }
        if (prefs == null) {
            prefs = new Prefs(context);
        }

        return instance;
    }

    private Cursor getEventCursor(long begin, long end) {
        assert begin >= 0;
        assert end >= 0;

        String[] args = {String.valueOf(begin), String.valueOf(end)};
        String selection = Columns.BEGIN + " >= ? AND " + Columns.END + " <= ?";
        Cursor cursor = eventsDB.query(TABLE_EVENTS, null, selection, args, null, null, Columns.BEGIN, null);
        return cursor;
    }

    private void deleteEventWithId(int id) {
        String selection = Columns._ID + " = ?";
        eventsDB.delete(TABLE_EVENTS, selection, new String[]{String.valueOf(id)});
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

    public Cursor getEventCursor() {
        return getOrderedEventCursor(Columns.BEGIN);
    }

    private Cursor getOrderedEventCursor(String order) {
        return eventsDB.query(TABLE_EVENTS, null, null, null, null, null, order, null);
    }

    public void setRingerType(int eventId, int ringerType) {
        if (ringerType != SimpleCalendarEvent.RINGER.IGNORE && ringerType != SimpleCalendarEvent.RINGER.NORMAL
                && ringerType != SimpleCalendarEvent.RINGER.SILENT && ringerType != SimpleCalendarEvent.RINGER.UNDEFINED
                && ringerType != SimpleCalendarEvent.RINGER.VIBRATE) {
            throw new IllegalArgumentException("unknown ringer type: " + ringerType);
        }
        String mSelectionClause = Columns._ID + " = ?";
        String[] mSelectionArgs = {String.valueOf(eventId)};
        ContentValues values = new ContentValues();
        values.put(Columns.RINGER_TYPE, ringerType);
        eventsDB.update(TABLE_EVENTS, values,
                mSelectionClause, mSelectionArgs);
    }

    public Deque<SimpleCalendarEvent> syncAndGetAllActiveEvents() {
        // sync the db and prune old events
        syncCalendarDb();

        Deque<SimpleCalendarEvent> events = new ArrayDeque<SimpleCalendarEvent>();
        Cursor cursor = getEventCursor();
        long beginTime = System.currentTimeMillis()
                - (SimpleCalendarEvent.MILLISECONDS_IN_MINUTE * (long) prefs.getBufferMinutes());
        long endTime = System.currentTimeMillis()
                + (SimpleCalendarEvent.MILLISECONDS_IN_MINUTE * (long) prefs.getBufferMinutes());

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
                try {
                    SimpleCalendarEvent e = getEvent(id);
                    if (e.isActiveBetween(beginTime, endTime)) {
                        events.addLast(e);
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
        pruneEventsBefore(System.currentTimeMillis() - SimpleCalendarEvent.MILLISECONDS_IN_MINUTE
                * (long) prefs.getBufferMinutes());
    }

    // returns the event that matches the given Instance id, null if no match
    public SimpleCalendarEvent getEvent(int id) throws NoSuchEventException {
        Cursor cursor = getEventCursor();
        SimpleCalendarEvent thisEvent = null;
        if (cursor.moveToFirst()) {
            do {
                int eventID = cursor.getInt(cursor.getColumnIndex(Columns._ID));
                if (eventID == id) {
                    int cal_id = cursor.getInt(cursor.getColumnIndex(Columns.CAL_ID));
                    String title = cursor.getString(cursor.getColumnIndex(Columns.TITLE));
                    long begin = cursor.getLong(cursor.getColumnIndex(Columns.BEGIN));
                    long end = cursor.getLong(cursor.getColumnIndex(Columns.END));
                    String description = cursor.getString(cursor.getColumnIndex(Columns.DESCRIPTION));
                    int ringerType = cursor.getInt(cursor.getColumnIndex(Columns.RINGER_TYPE));
                    int displayColor = cursor.getInt(cursor.getColumnIndex(Columns.DISPLAY_COLOR));
                    boolean isFreeTime = cursor.getInt(cursor.getColumnIndex(Columns.IS_FREETIME)) == 1;
                    boolean isAllDay = cursor.getInt(cursor.getColumnIndex(Columns.IS_ALLDAY)) == 1;

                    thisEvent = new SimpleCalendarEvent(cal_id, eventID, title, begin, end, description,
                            displayColor, isFreeTime, isAllDay);
                    thisEvent.setRingerType(ringerType);
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
        if (days < 0) {
            throw new IllegalArgumentException("can not sync for a negative period of days: " + days);
        }

        long begin = System.currentTimeMillis();
        long end = begin + MILLISECONDS_IN_DAY * (long) days; // pull all events n days from now

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
                int id = calendarCursor.getInt(col_id);
                int cal_id = calendarCursor.getInt(col_cal_id);

                // if the event is already in the local database then we need to preserve
                // the ringerType, all other values should be read from the system calendar
                // database


                SimpleCalendarEvent newEvent = new SimpleCalendarEvent(cal_id, id, event_title, event_begin, event_end,
                        event_description, event_displayColor, (event_availability == 0),
                        (event_allDay == 1));

                try {
                    SimpleCalendarEvent oldEvent = getEvent(id);
                    newEvent.setRingerType(oldEvent.getRingerType());
                } catch (NoSuchEventException e) {
                    // its perfectly reasonable that this event does not exist within our database
                    // yet
                }

                // inserting an event with the same id will clobber all previous data, completing
                // the synchronization of this event
                //TODO insert the event only if that calendar is set to synchronize
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

    private Cursor getCalendarCursor(long begin, long end) {
        return Instances.query(context.getContentResolver(), projection,
                begin, end);
    }

    public List<SimpleCalendar> getCalendarIdList() {
        List<SimpleCalendar> simpleCalendarIds = new ArrayList<SimpleCalendar>();
        final String[] projection = {
                Calendars._ID,
                Calendars.ACCOUNT_NAME,
                Calendars.CALENDAR_DISPLAY_NAME,
                Calendars.OWNER_ACCOUNT,
                Calendars.CALENDAR_COLOR
        };
        final int projection_id = 0;
        final int projection_accountName = 2;
        final int projection_displayname = 3;
        final int projection_owner = 4;
        final int projection_color = 5;
        Cursor cursor;
        ContentResolver cr = context.getContentResolver();
        cursor = cr.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(projection_id);
                String accountName = cursor.getString(projection_accountName);
                String displayName = cursor.getString(projection_displayname);
                String owner = cursor.getString(projection_owner);
                int color = cursor.getInt(projection_color);
                try {
                    SimpleCalendar c = new SimpleCalendar(id, accountName, displayName, owner, color);
                    simpleCalendarIds.add(c);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "android database supplied bad values calendar info for id " + id +
                            ", accountName:" + accountName + "displayName:" + displayName +
                            " owner:" + owner);
                    Log.w(TAG, e.getMessage());
                }
            } while (cursor.moveToNext());
        } else {
            Log.d(TAG, "no calendars installed");
        }

        return simpleCalendarIds;
    }

    private boolean isEventValidToInsert(SimpleCalendarEvent e) {
        boolean eventIsValid = false;
        if (e.getTitle() != null && e.getId() != null && e.getBegin() != null && e.getEnd() != null
                && e.isFreeTime() != null && e.isAllDay() != null) {
            eventIsValid = true;
        }
        return eventIsValid;
    }

    public void insertEvent(SimpleCalendarEvent e) {
        if (!isEventValidToInsert(e)) {
            throw new IllegalArgumentException(
                    "DatabaseInterface:insertEvent given an event with blank values");
        }

        ContentValues cv = new ContentValues();
        cv.put(Columns._ID, e.getId());
        cv.put(Columns.TITLE, e.getTitle());
        cv.put(Columns.BEGIN, e.getBegin());
        cv.put(Columns.END, e.getEnd());
        cv.put(Columns.DESCRIPTION, e.getDescription());
        cv.put(Columns.RINGER_TYPE, e.getRingerType());
        cv.put(Columns.DISPLAY_COLOR, e.getDisplayColor());
        cv.put(Columns.CAL_ID, e.getCalendarId());
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

        long rowID = eventsDB.insertWithOnConflict(TABLE_EVENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d(TAG, "inserted event " + e.toString() + " as row " + rowID);
    }

    // removes events from the local database that can not be found in the calendar
    // + database in the next n days
    private void pruneRemovedEvents(long cutoff) {
        // we need to make sure not to remove events from our database that might still
        // + be ongoing due to the event buffer
        int bufferMinutes = prefs.getBufferMinutes();
        long begin = System.currentTimeMillis() - (1000 * 60 * (long) bufferMinutes);
        long end = begin + 1000 * 60 * 60 * 24 * cutoff; // pull all events n days from now


        Cursor cal_cursor = getCalendarCursor(begin, end);

        ArrayList<Integer> cal_ids = new ArrayList<Integer>();

        if (cal_cursor.moveToFirst()) {
            int col_id = cal_cursor.getColumnIndex(projection[7]);
            do {
                int event_id = Integer.valueOf(cal_cursor.getString(col_id));
                cal_ids.add(event_id);
            } while (cal_cursor.moveToNext());
        }
        cal_cursor.close();

        Cursor loc_cursor = getEventCursor();
        if (loc_cursor.moveToFirst()) {
            do {
                int loc_id = loc_cursor.getInt(loc_cursor.getColumnIndex(Columns._ID));
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
    public SimpleCalendarEvent nextEvent() {
        // sync the database and prune old events first to make sure that we don't return an event
        // that has already expired
        syncCalendarDb();
        SimpleCalendarEvent nextEvent = null;

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
