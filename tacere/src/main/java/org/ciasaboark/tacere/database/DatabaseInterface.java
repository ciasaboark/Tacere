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
import android.provider.CalendarContract.Events;
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
    private static final String[] projection = new String[]{
            Instances.TITLE,
            Instances.BEGIN,
            Instances.END,
            Instances.DESCRIPTION,
            Instances.DISPLAY_COLOR,
            Instances.ALL_DAY,
            Instances.AVAILABILITY,
            Instances._ID,
            Events.CALENDAR_ID,
            Instances.EVENT_ID,
    };

    private static final long MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;
    private static DatabaseInterface instance;
    private static Context context = null;
    private static Prefs prefs = null;
    private SQLiteDatabase eventsDB;


    private DatabaseInterface(Context context) {
        DatabaseInterface.context = context;
        EventDatabaseOpenHelper dbHelper = new EventDatabaseOpenHelper(context);
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
        return getOrderedEventCursor(Columns._ID);
    }

    private Cursor getOrderedEventCursor(String order) {
        return eventsDB.query(EventDatabaseOpenHelper.TABLE_EVENTS, null, null, null, null, null, order, null);
    }

    public void setRingerForInstance(long instanceId, RingerType ringerType) {
        if (ringerType == null) {
            throw new IllegalArgumentException("ringerType can not be null");
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
        } catch (Exception e) {
            Log.e(TAG, "error setting ringer type: " + e.getMessage());
            e.printStackTrace();
        } finally {
            eventsDB.endTransaction();
        }
    }

    public Deque<EventInstance> syncAndGetAllActiveEvents() {
        // sync the db and prune old events
        syncCalendarDb();

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
            Log.d(TAG, "setRingerForAllInstancesOfEvent() updated " + rowsUpdated +
                    " for event id " + eventId);
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
        update(prefs.getLookaheadDays());
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


    // returns the event that matches the given Instance id, throws NoSuchEventException if no match
    public EventInstance getEvent(long instanceId) throws NoSuchEventInstanceException {
        final String selection = Columns._ID + " = ?";
        final String[] selectionArgs = {String.valueOf(instanceId)};
        final String[] PROJECTION = {
                Columns.RINGER_TYPE
        };
        final int PROJECTION_RINGER_TYPE = 0;

        Cursor cursor = null;
        EventInstance thisEvent = null;

        try {
            cursor = eventsDB.query(EventDatabaseOpenHelper.TABLE_EVENTS, PROJECTION, selection, selectionArgs, null, null, null);
            int ringerInt = RingerType.UNDEFINED.value;
            if (cursor.moveToFirst()) {
                //pull in the stored ringer
                ringerInt = cursor.getInt(PROJECTION_RINGER_TYPE);
            }

            try {
                thisEvent = getEventInstanceFromSystemDatabase(instanceId);
            } catch (NoSuchEventInstanceException e) {
                //if the instanceId does not exist within the system database, then we need to remove
                //it from ours as well.
                deleteEventWithId(instanceId);
            }

            RingerType ringerType = RingerType.getTypeForInt(ringerInt);
            thisEvent.setRingerType(ringerType);
        } catch (Exception e) {
            //TODO handle sql exceptions here
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (thisEvent == null) {
            throw new NoSuchEventInstanceException(TAG + " can not find event with given id " + instanceId);
        }
        return thisEvent;
    }

    private EventInstance getEventInstanceFromSystemDatabase(long instanceId) throws NoSuchEventInstanceException{
        EventInstance thisEvent = null;

        //pull in extra info from the system calendar database
        final String[] S_PROJECTION = new String[] {
                Instances._ID,
                Instances.EVENT_ID,
                Instances.CALENDAR_ID,
                Instances.DISPLAY_COLOR,
                Instances.TITLE,
                Instances.BEGIN,
                Instances.END,
                Instances.DESCRIPTION,
                Instances.AVAILABILITY,
                Instances.ALL_DAY,
                Instances.EVENT_LOCATION
        };
        final int S_PROJECTION_ID = 0;
        final int S_PROJECTION_EVENT_ID = 1;
        final int S_PROJECTION_CAL_ID = 2;
        final int S_PROJECTION_DISPLAY_COLOR = 3;
        final int S_PROJECTION_TITLE = 4;
        final int S_PROJECTION_BEGIN = 5;
        final int S_PROJECTION_END = 6;
        final int S_PROJECTION_DESCRIPTION = 7;
        final int S_PROJECTION_AVAILABILITY = 8;
        final int S_PROJECTION_ALL_DAY = 9;
        final int S_PROJECTION_EVENT_LOCAITON = 10;

        ContentResolver cr = context.getContentResolver();
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, Long.MIN_VALUE);
        ContentUris.appendId(builder, Long.MAX_VALUE);

        //TODO this should be Instances._ID + " = ?", but this generates an SQL exception
        //that '_id' is ambiguous (its also used by Event._ID
        //using hardcoded string for now
        String s_selection = "'" + Instances._ID + "' = ?";
        String[] s_selectionArgs = {String.valueOf(instanceId)};

        Cursor sysCursor = null;
        try {
            sysCursor = cr.query(
                    builder.build(),
                    S_PROJECTION,
                    s_selection,
                    s_selectionArgs,
                    null);

            //The system database might not have this event anymore if it has been deleted
            if (sysCursor.moveToFirst()) {
                String sLocation = sysCursor.getString(S_PROJECTION_EVENT_LOCAITON);
                long sCalId = sysCursor.getInt(S_PROJECTION_CAL_ID);
                int sEventId = sysCursor.getInt(S_PROJECTION_EVENT_ID);
                String sTitle = sysCursor.getString(S_PROJECTION_TITLE);
                long sBegin = sysCursor.getLong(S_PROJECTION_BEGIN);
                long sEnd = sysCursor.getLong(S_PROJECTION_END);
                String sDescription = sysCursor.getString(S_PROJECTION_DESCRIPTION);
                int sDisplayColor = sysCursor.getInt(S_PROJECTION_DISPLAY_COLOR);
                boolean sIsFreetime = sysCursor.getInt(S_PROJECTION_AVAILABILITY) == Instances.AVAILABILITY_FREE;
                boolean sIsAllday = sysCursor.getInt(S_PROJECTION_ALL_DAY) == 1;

                thisEvent = new EventInstance(sCalId, instanceId, sEventId, sTitle, sBegin, sEnd, sDescription,
                        sDisplayColor, sIsFreetime, sIsAllday);


                thisEvent.putExtraInfo("location", sLocation);
            } else {
                //TODO remove event from local database  
            }
        } catch (Exception e) {
            Log.e(TAG, "error reading system calendar info for instance id " + instanceId + ": " +
                    e.getMessage());
        } finally {
            if (sysCursor != null) {
                sysCursor.close();
            }
        }

        if (thisEvent == null) {
            throw new NoSuchEventInstanceException("can not find instance with id " + instanceId + " in the system calendar database");
        }

        return thisEvent;
    }

    private List<Long> getInstancesIdsBetween(long begin, long end) {
        List<Long> instanceIds = new ArrayList<Long>();
        final String[] EVENT_PROJECTION = new String[]{
                Instances._ID
        };
        final int PROJECTION_COL_ID = 0;

        ContentResolver cr = context.getContentResolver();
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, begin);
        ContentUris.appendId(builder, end);

        String selection = null;
        String[] selectionArgs = null;
        //filter out any events belonging to unselected calendars
        if (!prefs.shouldAllCalendarsBeSynced()) {
            Long[] selectedCalendars = prefs.getSelectedCalendarsIds().toArray(new Long[]{});
            selectionArgs = new String[selectedCalendars.length];
            selection = "";  //otherwise we will be appending onto "null"

            //produce a selection string that looks something like:
            //"cal_id = ? OR cal_id = ? OR cal_id = ?"
            for (int i = 0; i < selectedCalendars.length; i++) {
                selection += Instances.CALENDAR_ID + " = ?";
                selectionArgs[i] = String.valueOf(selectedCalendars[i]);
                if (i < selectedCalendars.length -1) {
                    selection += " OR ";
                }
            }
        }

        Cursor calendarCursor = null;
        try {
            calendarCursor = cr.query(
                    builder.build(),
                    EVENT_PROJECTION,
                    selection,
                    selectionArgs,
                    null);
            while (calendarCursor.moveToNext()) {
                instanceIds.add(calendarCursor.getLong(PROJECTION_COL_ID));
            }
        } catch (Exception e) {
            Log.e(TAG, "error getting list of instance ids between " + begin + " and " + end + ", list may be partial");
        } finally {
            if (calendarCursor != null) {
                calendarCursor.close();
            }
        }

        return instanceIds;
    }

    // sync the calendar and the local database for the given number of days
    public void update(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("can not sync for a negative period of days: " + days);
        }

        long begin = System.currentTimeMillis();
        long end = begin + MILLISECONDS_IN_DAY * (long) days; // pull all events n days from now

        List<Long> instanceIds = getInstancesIdsBetween(begin, end);

        for (Long instanceId: instanceIds) {
            // if the event is already in the local database then we need to preserve
            // the ringerType
            EventInstance oldEvent = null;
            try {
                oldEvent = getEvent(instanceId);
                Log.d(TAG, "update() event with id " + instanceId + " had previous ringer "
                        + oldEvent.getRingerType());
            } catch (NoSuchEventInstanceException e) {
                // its perfectly reasonable that this event does not exist within our database
                // yet
            }

            try {
                EventInstance systemEvent = getEventInstanceFromSystemDatabase(instanceId);
                if (oldEvent != null) {
                    systemEvent.setRingerType(oldEvent.getRingerType());
                }

                // inserting an event with the same instance id will clobber all previous data,
                // completing the synchronization of this event
                insertEvent(systemEvent.getId(), systemEvent.getEventId(),
                        systemEvent.getCalendarId(), systemEvent.getRingerType());
            } catch (NoSuchEventInstanceException e) {
                //this should not happen
                Log.e(TAG, "unable to get EventInstance from system calendar db for instance "
                        + instanceId + ", this should not have happened, event not added to " +
                        "local database");
            }
        }

        //if we aren't syncing all calendars then there might be entries in the database that
        //should not be there anymore
        if (!prefs.shouldAllCalendarsBeSynced()) {
            removeEntriesFromUnselectedCalendars();
        }

        removeEventsNotInSystemCalendar();

        //TODO the DB should be pretty small now, do we need to remove old events anymore?
        pruneOldEvents(days);
    }

    private void removeEntriesFromUnselectedCalendars() {
        if (!prefs.shouldAllCalendarsBeSynced()) {
            List<Long> selectedCalendarIds = prefs.getSelectedCalendarsIds();
            String selection = "";
            String[] selectionArgs = new String[selectedCalendarIds.size()];
            for (int i = 0; i < selectedCalendarIds.size(); i++) {
                selection += Columns.CAL_ID + " != ?";
                selectionArgs[i] = selectedCalendarIds.get(i).toString();

                if (i < selectedCalendarIds.size() -1) {
                    selection += " AND ";
                }
            }

            try {
                int removedRows = eventsDB.delete(EventDatabaseOpenHelper.TABLE_EVENTS, selection, selectionArgs);
                Log.d(TAG, "removeEntriesFromUnselectedCalendars() removed " + removedRows + " rows");
            } catch (Exception e) {
                Log.e(TAG, "removeEntriesFromUnselectedCalendars() error deleting events: " + e.getMessage());
            }

        }
    }

    private void removeEventsNotInSystemCalendar() {
        //TODO
    }

    private void removeEventIfExists(EventInstance event) {
        long instanceId = event.getId();
        String whereClause = Columns._ID + "= ?";
        String[] whereArgs = new String[]{String.valueOf(instanceId)};
        int rowsDeleted = eventsDB.delete(EventDatabaseOpenHelper.TABLE_EVENTS, whereClause, whereArgs);
        Log.d(TAG, "deleted " + rowsDeleted + " rows");
    }

    // remove all events from the database with an ending date
    // + before the given time
    public void pruneEventsBefore(long cutoff) {
        //TODO
//        Cursor cursor = getEventCursor(0, cutoff);
//        if (cursor.moveToFirst()) {
//            do {
//                int id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
//                long end = cursor.getLong(cursor.getColumnIndex(Columns.END));
//                if (end <= cutoff) {
//                    deleteEventWithId(id);
//                }
//            } while (cursor.moveToNext());
//        }
//        cursor.close();
    }

    private Cursor getCalendarCursor(long begin, long end) {
        return Instances.query(context.getContentResolver(), projection,
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
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(Calendars.CONTENT_URI, projection, null, null, null);
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

        cursor = cr.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "getCalendarIdList(), unable to get calendar cursor, returning empty list");
        } else {
            try {
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
            } catch (Exception e) {
                Log.e(TAG, "getCalendarIdList(), exception raised reading from cursor " + e.getMessage());
            } finally {
                cursor.close();
            }
        }


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
//            while (cursor.moveToNext()) {
//                String _title = cursor.getString(2);
//                long _eventId = cursor.getLong(0);
//                long _instanceId = cursor.getLong(1);
//                Log.d(TAG, "found repetition of event with id " + eventId + " eventId:" + _title);
//            }

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

    public void insertEvent(long instanceId, long eventId, long calendarId, RingerType ringerType) {
        if (ringerType == null) {
            throw new IllegalArgumentException("ringerType can not be null");
        }

        ContentValues cv = new ContentValues();
        cv.put(Columns._ID, instanceId);
        cv.put(Columns.EVENT_ID, eventId);
        cv.put(Columns.CAL_ID, calendarId);
        cv.put(Columns.RINGER_TYPE, ringerType.value);

        long rowID = eventsDB.insertWithOnConflict(EventDatabaseOpenHelper.TABLE_EVENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d(TAG, "inserted event instance with id " + instanceId + " as row " + rowID);
    }

    // removes events from the local database that can not be found in the calendar
    // + database in the next n days
    private void pruneOldEvents(long cutoff) {
        // we need to make sure not to remove events from our database that might still
        // + be ongoing due to the event buffer
        int bufferMinutes = prefs.getBufferMinutes();
        long begin = System.currentTimeMillis() - (1000 * 60 * (long) bufferMinutes);
        long end = begin + 1000 * 60 * 60 * 24 * cutoff; // pull all events n days from now


        Cursor calendarCursor = getCalendarCursor(begin, end);

        ArrayList<Integer> cal_ids = new ArrayList<Integer>();

        if (calendarCursor.moveToFirst()) {
            int col_id = calendarCursor.getColumnIndex(projection[7]);
            do {
                int event_id = Integer.valueOf(calendarCursor.getString(col_id));
                cal_ids.add(event_id);
            } while (calendarCursor.moveToNext());
        }
        calendarCursor.close();

        Cursor localCursor = getEventCursor();
        if (localCursor.moveToFirst()) {
            do {
                int loc_id = localCursor.getInt(localCursor.getColumnIndex(Columns._ID));
                if (!cal_ids.contains(loc_id)) {
                    deleteEventWithId(loc_id);
                }
            } while (localCursor.moveToNext());
        }
        localCursor.close();
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

        Cursor cursor = getOrderedEventCursor(Columns._ID);
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
