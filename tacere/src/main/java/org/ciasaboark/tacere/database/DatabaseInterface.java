/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.database;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Instances;
import android.util.Log;

import org.ciasaboark.tacere.event.Calendar;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.prefs.Prefs;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static android.provider.CalendarContract.Calendars;

public class DatabaseInterface {
    private static final String TAG = "DatabaseInterface";
    private static final String[] PROJECTION = new String[]{
            Instances.TITLE,
            Instances.BEGIN,
            Instances.END,
            Instances.DESCRIPTION,
            Instances.DISPLAY_COLOR,    //TODO this column does not exists in API < 16
            Instances.ALL_DAY,
            Instances.AVAILABILITY,
            Instances._ID,
            Instances.CALENDAR_ID,
            Instances.EVENT_ID,
            Instances.EVENT_LOCATION
    };
    private static final long MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;
    private static DatabaseInterface instance;
    private static Context context = null;
    private static Prefs prefs = null;
    private final int PROJECTION_TITLE = 0;
    private final int PROJECTION_BEGIN = 1;
    private final int PROJECTION_END = 2;
    private final int PROJECTION_DESCRIPTION = 3;
    private final int PROJECTION_COLOR = 4;
    private final int PROJECTION_ALL_DAY = 5;
    private final int PROJECTION_AVAILABLE = 6;
    private final int PROJECTION_ID = 7;
    private final int PROJECTION_CAL_ID = 8;
    private final int PROJECTION_EVENT_ID = 9;
    private final int PROJECTION_LOCATION = 10;
    private final SQLiteDatabase eventsDB;
    private final DataSetManager dataSetManager;


    private DatabaseInterface(Context context) {
        DatabaseInterface.context = context;
        EventDatabaseOpenHelper dbHelper = new EventDatabaseOpenHelper(context);
        this.eventsDB = dbHelper.getWritableDatabase();
        this.dataSetManager = new DataSetManager(this, context);
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
        Cursor cursor = eventsDB.query(EventDatabaseOpenHelper.TABLE_EVENTS, null, selection, args, null, null, Columns.BEGIN, null);
        return cursor;
    }

    private void deleteEventWithId(long id) {
        String selection = Columns._ID + " = ?";
        eventsDB.delete(EventDatabaseOpenHelper.TABLE_EVENTS, selection, new String[]{String.valueOf(id)});
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
        return eventsDB.query(EventDatabaseOpenHelper.TABLE_EVENTS, null, null, null, null, null, order, null);
    }

    public void setRingerForInstance(long instanceId, RingerType ringerType) {
        if (ringerType == null) {
            throw new IllegalArgumentException("ringerType must not be null");
        }

        String mSelectionClause = Columns._ID + " = ?";
        String[] mSelectionArgs = {String.valueOf(instanceId)};
        ContentValues values = new ContentValues();
        values.put(Columns.RINGER_TYPE, ringerType.value);
        eventsDB.beginTransaction();
        try {
            int rowsUpdated = eventsDB.update(EventDatabaseOpenHelper.TABLE_EVENTS, values,
                    mSelectionClause, mSelectionArgs);
            if (rowsUpdated != 1) {
                throw new SQLException("setRingerForInstance() should have updated 1 row for instance id " + instanceId + ", updated " + rowsUpdated);
            }
            eventsDB.setTransactionSuccessful();
            dataSetManager.broadcastDataSetChangedForId(instanceId);
        } catch (Exception e) {
            Log.e(TAG, "error setting ringer type: " + e.getMessage());
            e.printStackTrace();
        } finally {
            eventsDB.endTransaction();
        }
    }

    public Deque<EventInstance> getAllActiveEvents() {
        //TODO better SQL select
        Deque<EventInstance> events = new ArrayDeque<EventInstance>();
        Cursor cursor = getEventCursor();
        long beginTime = System.currentTimeMillis()
                - (EventInstance.MILLISECONDS_IN_MINUTE * (long) prefs.getBufferMinutes());
        long endTime = System.currentTimeMillis()
                + (EventInstance.MILLISECONDS_IN_MINUTE * (long) prefs.getBufferMinutes());

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
                try {
                    EventInstance e = getEvent(id);
                    if (e.isActiveBetween(beginTime, endTime)) {
                        events.addLast(e);
                    }
                } catch (NoSuchEventInstanceException e) {
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

    // returns the event that matches the given Instance id, throws NoSuchEventException if no match
    public EventInstance getEvent(long instanceId) throws NoSuchEventInstanceException {
        //TODO use better SQL SELECT
        Cursor cursor = getEventCursor();
        EventInstance thisEvent = null;
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
                if (id == instanceId) {
                    long cal_id = cursor.getInt(cursor.getColumnIndex(Columns.CAL_ID));
                    int event_id = cursor.getInt(cursor.getColumnIndex(Columns.EVENT_ID));
                    String title = cursor.getString(cursor.getColumnIndex(Columns.TITLE));
                    long begin = cursor.getLong(cursor.getColumnIndex(Columns.BEGIN));
                    long end = cursor.getLong(cursor.getColumnIndex(Columns.END));
                    String description = cursor.getString(cursor.getColumnIndex(Columns.DESCRIPTION));
                    int ringerInt = cursor.getInt(cursor.getColumnIndex(Columns.RINGER_TYPE));
                    int displayColor = cursor.getInt(cursor.getColumnIndex(Columns.DISPLAY_COLOR));
                    boolean isFreeTime = cursor.getInt(cursor.getColumnIndex(Columns.IS_FREETIME)) == 1;
                    boolean isAllDay = cursor.getInt(cursor.getColumnIndex(Columns.IS_ALLDAY)) == 1;
                    String location = cursor.getString(cursor.getColumnIndex(Columns.LOCATION));

                    thisEvent = new EventInstance(cal_id, id, event_id, title, begin, end, description,
                            displayColor, isFreeTime, isAllDay);
                    RingerType ringerType = RingerType.getTypeForInt(ringerInt);
                    thisEvent.setRingerType(ringerType);
                    thisEvent.setLocation(location);

                    break;
                }
            } while (cursor.moveToNext());

        }
        cursor.close();
        if (thisEvent == null) {
            throw new NoSuchEventInstanceException(TAG + " can not find event with given id " + instanceId);
        }
        return thisEvent;
    }

    public void setRingerForAllInstancesOfEvent(long eventId, RingerType ringerType) {
        if (ringerType == null) {
            throw new IllegalArgumentException("ringerType can not be null");
        }

        String mSelectionClause = Columns.EVENT_ID + " = ?";
        String[] mSelectionArgs = {String.valueOf(eventId)};
        ContentValues values = new ContentValues();
        values.put(Columns.RINGER_TYPE, ringerType.value);
        eventsDB.beginTransaction();
        try {
            int rowsUpdated = eventsDB.update(EventDatabaseOpenHelper.TABLE_EVENTS, values,
                    mSelectionClause, mSelectionArgs);
            Log.d(TAG, "setRingerForAllInstancesOfEvent updated " + rowsUpdated + " for event id " + eventId);
            eventsDB.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "setRingerForAllInstancesOfEvent() error setting ringer type: " +
                    e.getMessage() + " for event id " + eventId + ", aborting");
            e.printStackTrace();
        } finally {
            eventsDB.endTransaction();
        }
    }

    /**
     * Sync the internal database with the system calendar database. Forward syncing is limited to
     * the period specified in preferences. Old events are pruned.
     */
    public void syncCalendarDb() {
        // update(n) will also remove all events not found in the next n days, so we
        // + need to keep this in sync with the users preferences.
        update(prefs.getLookaheadDays().value);
        pruneEventsBefore(System.currentTimeMillis() - EventInstance.MILLISECONDS_IN_MINUTE
                * (long) prefs.getBufferMinutes());
    }

    public List<Long> getInstanceIdsForEvent(long eventId) {
        List<Long> events = new ArrayList<Long>();
        //TODO make faster with select query to avoid event id check
        Cursor cursor = getEventCursor();
        if (cursor.moveToFirst()) {
            do {
                long _eventId = cursor.getLong(cursor.getColumnIndex(Columns.EVENT_ID));
                if (eventId == _eventId) {
                    long instanceId = cursor.getLong(cursor.getColumnIndex(Columns._ID));
                    events.add(instanceId);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return events;
    }

    // sync the calendar and the local database for the given number of days
    private void update(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("can not sync for a negative period of days: " + days);
        }

        long begin = System.currentTimeMillis();
        long end = begin + MILLISECONDS_IN_DAY * (long) days; // pull all events n days from now

        Cursor calendarCursor = getCalendarCursor(begin, end);

        if (calendarCursor.moveToFirst()) {
            do {
                // the cursor
                String event_title = calendarCursor.getString(PROJECTION_TITLE);
                long event_begin = calendarCursor.getLong(PROJECTION_BEGIN);
                long event_end = calendarCursor.getLong(PROJECTION_END);
                String event_description = calendarCursor.getString(PROJECTION_DESCRIPTION);
                int event_displayColor = calendarCursor.getInt(PROJECTION_COLOR);
                int event_allDay = calendarCursor.getInt(PROJECTION_ALL_DAY);
                int event_availability = calendarCursor.getInt(PROJECTION_AVAILABLE);
                int id = calendarCursor.getInt(PROJECTION_ID);
                long cal_id = calendarCursor.getInt(PROJECTION_CAL_ID);
                int event_id = calendarCursor.getInt(PROJECTION_EVENT_ID);
                String event_location = calendarCursor.getString(PROJECTION_LOCATION);

                // if the event is already in the local database then we need to preserve
                // the ringerType, all other values should be read from the system calendar
                // database
                EventInstance newEvent = new EventInstance(cal_id, id, event_id, event_title, event_begin, event_end,
                        event_description, event_displayColor, (event_availability == 0),
                        (event_allDay == 1));
                newEvent.setLocation(event_location);

                try {
                    EventInstance oldEvent = getEvent(id);
                    RingerType oldRinger = oldEvent.getRingerType();
                    newEvent.setRingerType(oldRinger);
                } catch (NoSuchEventInstanceException e) {
                    // its perfectly reasonable that this event does not exist within our database
                    // yet
                }

                // inserting an event with the same id will clobber all previous data, completing
                // the synchronization of this event
                List calendarsToSync = prefs.getSelectedCalendarsIds();
                long calendarId = newEvent.getCalendarId();
                if (prefs.shouldAllCalendarsBeSynced() || calendarsToSync.contains(calendarId)) {
                    insertEvent(newEvent);
                } else {
                    removeEventIfExists(newEvent);
                }
            } while (calendarCursor.moveToNext());
        }
        calendarCursor.close();
        pruneRemovedEvents(days);
    }

    private void removeEventIfExists(EventInstance event) {
        long instanceId = event.getId();
        String whereClause = Columns._ID + "=?";
        String[] whereArgs = new String[]{String.valueOf(instanceId)};
        int rowsDeleted = eventsDB.delete(EventDatabaseOpenHelper.TABLE_EVENTS, whereClause, whereArgs);
        Log.d(TAG, "deleted " + rowsDeleted + " rows");
    }

    // remove all events from the database with an ending date
    // + before the given time
    private void pruneEventsBefore(long cutoff) {
        long rowsDeleted = 0;
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
        return Instances.query(context.getContentResolver(), PROJECTION,
                begin, end);
    }

    public String getCalendarNameForId(long id) {
        String calendarName = "";
        final String[] projection = {
                Calendars._ID,
                Calendars.NAME
        };
        final int projection_id = 0;
        final int projection_name = 1;
        Cursor cursor;
        ContentResolver cr = context.getContentResolver();
        cursor = cr.query(Calendars.CONTENT_URI, projection, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                long calendarId = cursor.getLong(projection_id);
                if (calendarId == id) {
                    calendarName = cursor.getString(projection_name);
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return calendarName;
    }

    public List<Calendar> getCalendarIdList() {
        List<Calendar> calendarIds = new ArrayList<Calendar>();
        final String[] projection = {
                Calendars._ID,
                Calendars.ACCOUNT_NAME,
                Calendars.CALENDAR_DISPLAY_NAME,
                Calendars.OWNER_ACCOUNT,
                Calendars.CALENDAR_COLOR
        };
        final int projection_id = 0;
        final int projection_accountName = 1;
        final int projection_displayname = 2;
        final int projection_owner = 3;
        final int projection_color = 4;
        Cursor cursor;
        ContentResolver cr = context.getContentResolver();
        //TODO wrap in try/catch/finally close
        cursor = cr.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(projection_id);
                String accountName = cursor.getString(projection_accountName);
                String displayName = cursor.getString(projection_displayname);
                String owner = cursor.getString(projection_owner);
                int color = cursor.getInt(projection_color);
                try {
                    Calendar c = new Calendar(id, accountName, displayName, owner, color);
                    calendarIds.add(c);
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
        cursor.close();

        return calendarIds;
    }

    private boolean isEventValidToInsert(EventInstance e) {
        boolean eventIsValid = false;
        if (e.getTitle() != null && e.getId() >= 0 && e.getBegin() != null && e.getEnd() != null
                && e.isFreeTime() != null && e.isAllDay() != null) {
            eventIsValid = true;
        }
        return eventIsValid;
    }

    public long getRemainingEventRepetitionCount(long eventId) {
        return getEventRepetitionCountFromTime(eventId, System.currentTimeMillis());
    }

    public long getEventRepetitionCountFromTime(long eventId, long begin) {
        long eventRepetitions = 0;
        boolean querySucceeded = false;
        int tryCount = 0;
        while (!querySucceeded && tryCount < 20) {
            try {
                eventRepetitions = tryGetEventRepetitionCountFromTime(eventId, begin);
                querySucceeded = true;
            } catch (Exception e) {
                Log.w(TAG, "Error getting repetition count for event with id " + eventId + " on attempt number " + tryCount);
            }
        }

        return eventRepetitions;
    }

    private long tryGetEventRepetitionCountFromTime(long eventId, long begin) throws Exception {
        //the local database can not be trusted to return an accurate count of the event repetions
        //since its window is limited.  Instead we have to query the system database
        long eventRepetitions = 0;

        final String[] EVENT_PROJECTION = new String[]{
                Instances.EVENT_ID,
                Instances._ID,
                Instances.TITLE
        };

        ContentResolver cr = context.getContentResolver();
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, begin);
        ContentUris.appendId(builder, Long.MAX_VALUE);

        String selection = Instances.EVENT_ID + " = ?";
        String[] selectionArgs = {String.valueOf(eventId)};

        Cursor cursor = null;
        try {
            cursor = cr.query(
                    builder.build(),
                    EVENT_PROJECTION,
                    selection,
                    selectionArgs,
                    null);

            eventRepetitions = cursor.getCount();
        } catch (Exception e) {
            Log.e(TAG, "error getting repetition count for event with id " + eventId);
            throw new Exception("Error performing query");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return eventRepetitions;
    }

    public boolean doesEventRepeat(long eventId) {
        boolean eventRepeats = false;
        if (getEventRepetitionCount(eventId) > 1) {
            eventRepeats = true;
        }
        return eventRepeats;
    }

    public long getEventRepetitionCount(long eventId) {
        return getEventRepetitionCountFromTime(eventId, 0);
    }

    public void insertEvent(EventInstance e) {
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
        cv.put(Columns.RINGER_TYPE, e.getRingerType().value);
        cv.put(Columns.DISPLAY_COLOR, e.getDisplayColor());
        cv.put(Columns.CAL_ID, e.getCalendarId());
        cv.put(Columns.EVENT_ID, e.getEventId());
        cv.put(Columns.IS_ALLDAY,
                e.isAllDay() ? 1 : 0);
        cv.put(Columns.IS_FREETIME,
                e.isFreeTime() ? 0 : 1);    //yes, these are swapped. No, I don't remember why
        cv.put(Columns.LOCATION, e.getLocation());

        long rowID = eventsDB.insertWithOnConflict(EventDatabaseOpenHelper.TABLE_EVENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if (rowID != -1) {
            //TODO notifiy dataset changed?, this may cause too many messages to be received
        }
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

        ArrayList<Integer> cal_ids = new ArrayList<Integer>();

        Cursor cal_cursor = getCalendarCursor(begin, end);
        if (cal_cursor.moveToFirst()) {
            int col_id = cal_cursor.getColumnIndex(PROJECTION[7]);
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
        loc_cursor.close();
    }

    /**
     * Query the internal database for the next event.
     *
     * @return the next event in the database, or null if there are no events
     */
    public EventInstance nextEvent() {
        // sync the database and prune old events first to make sure that we don't return an event
        // that has already expired
        syncCalendarDb();
        EventInstance nextEvent = null;

        Cursor cursor = getOrderedEventCursor(Columns.BEGIN);
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
            try {
                nextEvent = getEvent(id);
            } catch (NoSuchEventInstanceException e) {
                Log.e(TAG, "unable to retrieve event with id of " + id
                        + " even though a record with this id exists in the database");
            }
        }
        cursor.close();

        return nextEvent;
    }

}
