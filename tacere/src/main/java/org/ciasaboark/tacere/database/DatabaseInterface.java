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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import static android.provider.CalendarContract.Calendars;

public class DatabaseInterface {
    private static final String TAG = "DatabaseInterface";
    private static final String[] SYSTEM_DB_PROJECTION = new String[]{
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
    private static final String[] LOCAL_DB_PROJECTION = new String[]{
            Columns._ID,
            Columns.BEGIN,
            Columns.EFFECTIVE_END,
            Columns.EVENT_ID,
            Columns.TITLE,
            Columns.CAL_ID,
            Columns.DESCRIPTION,
            Columns.DISPLAY_COLOR,
            Columns.IS_ALLDAY,
            Columns.IS_FREETIME,
            Columns.LOCATION,
            Columns.RINGER_TYPE,
            Columns.EXTEND_BUFFER,
            Columns.ORGINAL_END
    };
    private static final int LOCAL_DB_PROJECTION_ID = 0;
    private static final int LOCAL_DB_PROJECTION_BEGIN = 1;
    private static final int LOCAL_DB_PROJECTION_EFFECTIVE_END = 2;
    private static final int LOCAL_DB_PROJECTION_EVENT_ID = 3;
    private static final int LOCAL_DB_PROJECTION_TITLE = 4;
    private static final int LOCAL_DB_PROJECTION_CAL_ID = 5;
    private static final int LOCAL_DB_PROJECTION_DESCRIPTION = 6;
    private static final int LOCAL_DB_PROJECTION_DISPLAY_COLOR = 7;
    private static final int LOCAL_DB_PROJECTION_ALLDAY = 8;
    private static final int LOCAL_DB_PROJECTION_AVAILABLE = 9;
    private static final int LOCAL_DB_PROJECTION_LOCATION = 10;
    private static final int LOCAL_DB_PROJECTION_RINGER = 11;
    private static final int LOCAL_DB_PROJECTION_EXTEND_MINUTES = 12;
    private static final int LOCAL_DB_PROJECTION_ORIGINAL_END = 13;

    private static final long MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;
    private static DatabaseInterface instance;
    private static Context context = null;
    private static Prefs prefs = null;
    private final int SYSTEM_DB_PROJECTION_TITLE = 0;
    private final int SYSTEM_DB_PROJECTION_BEGIN = 1;
    private final int SYSTEM_DB_PROJECTION_END = 2;
    private final int SYSTEM_DB_PROJECTION_DESCRIPTION = 3;
    private final int SYSTEM_DB_PROJECTION_COLOR = 4;
    private final int SYSTEM_DB_PROJECTION_ALL_DAY = 5;
    private final int SYSTEM_DB_PROJECTION_AVAILABLE = 6;
    private final int SYSTEM_DB_PROJECTION_ID = 7;
    private final int SYSTEM_DB_PROJECTION_CAL_ID = 8;
    private final int SYSTEM_DB_PROJECTION_EVENT_ID = 9;
    private final int SYSTEM_DB_PROJECTION_LOCATION = 10;
    private final SQLiteDatabase eventsDB;


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

    private Cursor getEventCursor(long begin, long end) {
        assert begin >= 0;
        assert end >= 0;

        String[] args = {String.valueOf(begin), String.valueOf(end)};
        String selection = Columns.BEGIN + " >= ? AND " + Columns.EFFECTIVE_END + " <= ?";
        Cursor cursor = eventsDB.query(EventDatabaseOpenHelper.TABLE_EVENTS, null, selection, args, null, null, Columns.BEGIN, null);
        return cursor;
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

    public void setExtendMinutesForInstance(long instanceId, int extendMinutes) {
        if (extendMinutes < 0) {
            throw new IllegalArgumentException("extendMinutes must be >= 0");
        }
        try {
            EventInstance eventInstance = getEvent(instanceId);
            eventInstance.setExtendMinutes(extendMinutes);
            insertEvent(eventInstance);
        } catch (NoSuchEventInstanceException e) {
            Log.e(TAG, "not able to find event with instance id : " + instanceId +
                    ", can not extend minutes");
        }
    }

    // returns the event that matches the given Instance id, throws NoSuchEventException if no match
    public EventInstance getEvent(long instanceId) throws NoSuchEventInstanceException {
        //TODO remove loop
        String whereClause = Columns._ID + " = ?";
        String[] whereArgs = new String[]{String.valueOf(instanceId)};
        Cursor cursor = eventsDB.query(EventDatabaseOpenHelper.TABLE_EVENTS, LOCAL_DB_PROJECTION, whereClause, whereArgs, null, null, null);
        EventInstance thisEvent = null;
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
                if (id == instanceId) {
                    long cal_id = cursor.getInt(cursor.getColumnIndex(Columns.CAL_ID));
                    int event_id = cursor.getInt(cursor.getColumnIndex(Columns.EVENT_ID));
                    String title = cursor.getString(cursor.getColumnIndex(Columns.TITLE));
                    long begin = cursor.getLong(cursor.getColumnIndex(Columns.BEGIN));
                    long originalEnd = cursor.getLong(cursor.getColumnIndex(Columns.ORGINAL_END));
                    String description = cursor.getString(cursor.getColumnIndex(Columns.DESCRIPTION));
                    int ringerInt = cursor.getInt(cursor.getColumnIndex(Columns.RINGER_TYPE));
                    int displayColor = cursor.getInt(cursor.getColumnIndex(Columns.DISPLAY_COLOR));
                    boolean isFreeTime = cursor.getInt(cursor.getColumnIndex(Columns.IS_FREETIME)) == 1;
                    boolean isAllDay = cursor.getInt(cursor.getColumnIndex(Columns.IS_ALLDAY)) == 1;
                    String location = cursor.getString(cursor.getColumnIndex(Columns.LOCATION));
                    int extendBuffer = cursor.getInt(cursor.getColumnIndex(Columns.EXTEND_BUFFER));

                    thisEvent = new EventInstance(cal_id, id, event_id, title, begin, originalEnd, description,
                            displayColor, isFreeTime, isAllDay, extendBuffer);
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

    public void insertEvent(EventInstance e) {
        if (!isEventValidToInsert(e)) {
            throw new IllegalArgumentException(
                    "DatabaseInterface:insertEvent given an event with blank values");
        }

        ContentValues cv = new ContentValues();
        cv.put(Columns._ID, e.getId());
        cv.put(Columns.TITLE, e.getTitle());
        cv.put(Columns.BEGIN, e.getBegin());
        cv.put(Columns.ORGINAL_END, e.getOriginalEnd());
        cv.put(Columns.DESCRIPTION, e.getDescription());
        cv.put(Columns.IS_ALLDAY, e.isAllDay());
        cv.put(Columns.IS_FREETIME, e.isFreeTime());
        cv.put(Columns.RINGER_TYPE, e.getRingerType().value);
        cv.put(Columns.DISPLAY_COLOR, e.getDisplayColor());
        cv.put(Columns.CAL_ID, e.getCalendarId());
        cv.put(Columns.EVENT_ID, e.getEventId());
        cv.put(Columns.LOCATION, e.getLocation());
        cv.put(Columns.EXTEND_BUFFER, e.getExtendMinutes());
        cv.put(Columns.EFFECTIVE_END, e.getEffectiveEnd());

        long rowID = eventsDB.insertWithOnConflict(EventDatabaseOpenHelper.TABLE_EVENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d(TAG, "inserted event " + e.toString() + " as row " + rowID);
    }

    private boolean isEventValidToInsert(EventInstance e) {
        boolean eventIsValid = false;
        if (e != null && e.getTitle() != null && e.getId() >= 0 && e.getBegin() != null && e.getOriginalEnd() != null
                && e.isFreeTime() != null && e.isAllDay() != null) {
            eventIsValid = true;
        }
        return eventIsValid;
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
                throw new Exception("setRingerForInstance() should have updated 1 row for instance id " + instanceId + ", updated " + rowsUpdated);
            } else {
                Log.d(TAG, "set new ringer '" + ringerType + "' for instance id " + instanceId);
            }
            eventsDB.setTransactionSuccessful();
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
        String whereClause = Columns.EFFECTIVE_END + " > ? AND " +
                Columns.BEGIN + " < ?";
        long beginTime = System.currentTimeMillis()
                - (EventInstance.MILLISECONDS_IN_MINUTE * (long) prefs.getBufferMinutes());
        long endTime = System.currentTimeMillis()
                + (EventInstance.MILLISECONDS_IN_MINUTE * (long) prefs.getBufferMinutes());
        String[] whereArgs = new String[]{String.valueOf(beginTime), String.valueOf(endTime)};
        Cursor cursor = null;

        try {
            cursor = eventsDB.query(EventDatabaseOpenHelper.TABLE_EVENTS, LOCAL_DB_PROJECTION,
                    whereClause, whereArgs, null, null, null);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(LOCAL_DB_PROJECTION_ID);
                long calId = cursor.getLong(LOCAL_DB_PROJECTION_CAL_ID);
                long eventId = cursor.getLong(LOCAL_DB_PROJECTION_EVENT_ID);
                long begin = cursor.getLong(LOCAL_DB_PROJECTION_BEGIN);
                long end = cursor.getLong(LOCAL_DB_PROJECTION_ORIGINAL_END);
                String title = cursor.getString(LOCAL_DB_PROJECTION_TITLE);
                String description = cursor.getString(LOCAL_DB_PROJECTION_DESCRIPTION);
                String location = cursor.getString(LOCAL_DB_PROJECTION_LOCATION);
                int displayColor = cursor.getInt(LOCAL_DB_PROJECTION_DISPLAY_COLOR);
                boolean isAllDay = cursor.getInt(LOCAL_DB_PROJECTION_ALLDAY) == 1;
                boolean isAvailable = cursor.getInt(LOCAL_DB_PROJECTION_AVAILABLE) == 1;
                int ringerInt = cursor.getInt(LOCAL_DB_PROJECTION_RINGER);
                RingerType ringerType = RingerType.getTypeForInt(ringerInt);
                int extendMinutes = cursor.getInt(LOCAL_DB_PROJECTION_EXTEND_MINUTES);

                EventInstance e = new EventInstance(calId, id, eventId, title, begin, end,
                        description, displayColor, isAvailable, isAllDay, extendMinutes);
                e.setLocation(location);
                e.setRingerType(ringerType);
                if (e.isActiveBetween(beginTime, endTime)) {
                    events.add(e);
                }

            }
        } catch (Exception e) {
            Log.d(TAG, "error getting cursor for active events");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

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
        int lookaheadDays = prefs.getLookaheadDays().value;
        update(lookaheadDays);
        pruneEventsEndBefore(System.currentTimeMillis() - EventInstance.MILLISECONDS_IN_MINUTE
                * (long) prefs.getBufferMinutes());
        pruneEventsBeginAfter(System.currentTimeMillis() + (lookaheadDays * EventInstance.MILLISECONDS_IN_DAY));
        pruneEventsFromRemovedCalendars();
        pruneEventsRemovedFromSystemCalendar();
    }

    // sync the calendar and the local database for the given number of days
    private void update(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("can not sync for a negative period of days: " + days);
        }

        long begin = System.currentTimeMillis();
        long end = begin + MILLISECONDS_IN_DAY * (long) days; // pull all events n days from now

        Cursor calendarCursor = getCalendarCursor(begin, end);
        List calendarsToSync = prefs.getSelectedCalendarsIds();

        if (calendarCursor.moveToFirst()) {
            do {
                // the cursor
                String event_title = calendarCursor.getString(SYSTEM_DB_PROJECTION_TITLE);
                long event_begin = calendarCursor.getLong(SYSTEM_DB_PROJECTION_BEGIN);
                long event_end = calendarCursor.getLong(SYSTEM_DB_PROJECTION_END);
                String event_description = calendarCursor.getString(SYSTEM_DB_PROJECTION_DESCRIPTION);
                int event_displayColor = calendarCursor.getInt(SYSTEM_DB_PROJECTION_COLOR);
                int event_allDay = calendarCursor.getInt(SYSTEM_DB_PROJECTION_ALL_DAY);
                int event_availability = calendarCursor.getInt(SYSTEM_DB_PROJECTION_AVAILABLE);
                long id = calendarCursor.getLong(SYSTEM_DB_PROJECTION_ID);
                long cal_id = calendarCursor.getLong(SYSTEM_DB_PROJECTION_CAL_ID);
                long event_id = calendarCursor.getLong(SYSTEM_DB_PROJECTION_EVENT_ID);
                String event_location = calendarCursor.getString(SYSTEM_DB_PROJECTION_LOCATION);
                boolean isEventAllDay = event_allDay == 1;
                boolean isEventAvailable = event_availability == Instances.AVAILABILITY_FREE;


                EventInstance newEvent = new EventInstance(cal_id, id, event_id, event_title, event_begin, event_end,
                        event_description, event_displayColor, isEventAvailable, isEventAllDay, 0);
                newEvent.setLocation(event_location);

                // if the event is already in the local database then we need to preserve
                // the ringerType, all other values should be read from the system calendar
                // database
                try {
                    EventInstance oldEvent = getEvent(id);
                    RingerType oldRinger = oldEvent.getRingerType();
                    int oldExtendMinutes = oldEvent.getExtendMinutes();
                    newEvent.setRingerType(oldRinger);
                    newEvent.setExtendMinutes(oldExtendMinutes);
                } catch (NoSuchEventInstanceException e) {
                    // its perfectly reasonable that this event does not exist within our database
                    // yet
                }

                // inserting an event with the same id will clobber all previous data, completing
                // the synchronization of this event

                long calendarId = newEvent.getCalendarId();
                if (prefs.shouldAllCalendarsBeSynced() || calendarsToSync.contains(calendarId)) {
                    insertEvent(newEvent);
                } else {
                    removeEventIfExists(newEvent);
                }
            } while (calendarCursor.moveToNext());
        }
        calendarCursor.close();
    }

    private void pruneEventsEndBefore(long time) {
        String whereClause = Columns.EFFECTIVE_END + " < ?";
        String[] args = new String[]{String.valueOf(time)};
        int rowsDeleted = eventsDB.delete(EventDatabaseOpenHelper.TABLE_EVENTS, whereClause, args);
        Log.d(TAG, "pruned " + rowsDeleted + " events that end after " + time);
    }

    private void pruneEventsBeginAfter(long time) {
        String whereClause = Columns.BEGIN + " > ?";
        String[] args = new String[]{String.valueOf(time)};
        int rowsDeleted = eventsDB.delete(EventDatabaseOpenHelper.TABLE_EVENTS, whereClause, args);
        Log.d(TAG, "pruned " + rowsDeleted + " events that end after " + time);
    }

    private void pruneEventsFromRemovedCalendars() {
        if (!prefs.shouldAllCalendarsBeSynced()) {
            List<Long> calendarIds = prefs.getSelectedCalendarsIds();


            Iterator i = calendarIds.iterator();
            int index = 0;
            String idList = "";
            while (i.hasNext()) {
                String instanceId = String.valueOf(i.next());
                idList += " '" + instanceId + "'";
                index++;
                if (i.hasNext()) {
                    idList += ", ";
                }
            }
            String whereClause = Columns.CAL_ID + " NOT IN (" + idList + ")";

            try {
                int deletedRowCount = eventsDB.delete(EventDatabaseOpenHelper.TABLE_EVENTS,
                        whereClause, null);
                Log.d(TAG, "deleted " + deletedRowCount + " events that belonged to calendars " +
                        "that should not be synced");
            } catch (Exception e) {
                Log.e(TAG, "error deleting events not in system calendar: " + e.getMessage());
            }

        }
    }

    private void pruneEventsRemovedFromSystemCalendar() {
        //remove local events that no longer exist within the system calendar
        List<Long> systemCalendarInstanceIds = getInstanceIdsFromSystemDatabase();
        List<Long> localInstanceIds = getInstanceIdsFromLocalDatabase();
        for (Long id : localInstanceIds) {
            if (!systemCalendarInstanceIds.contains(id)) {
                deleteEventWithId(id);
            }
        }
    }

//    private void pruneEventsNotInList(List<Long> calendarInstanceIds) {
//        //TODO test
//        if (calendarInstanceIds == null || calendarInstanceIds.size() == 0) {
//            return;
//        } else {
//
//            String[] whereArgs = new String[calendarInstanceIds.size()];
//            Iterator i = calendarInstanceIds.iterator();
//            int index = 0;
//            String idList = "";
//            while (i.hasNext()) {
//                String instanceId = String.valueOf(i.next());
//                whereArgs[index] = instanceId;
//                idList += " ?";
//                index++;
//                if (i.hasNext()) {
//                    idList += ", ";
//                }
//            }
//            String whereClause = Columns._ID + " NOT IN (" + idList + ")";
//
//            try {
//                int deletedRowCount = eventsDB.delete(EventDatabaseOpenHelper.TABLE_EVENTS,
//                        whereClause, whereArgs);
//                Log.d(TAG, "deleted " + deletedRowCount + " events from local database that were no " +
//                        "longer in system database");
//            } catch (Exception e) {
//                Log.e(TAG, "error deleting events not in system calendar: " + e.getMessage());
//            }
//        }
//    }

    private Cursor getCalendarCursor(long begin, long end) {
        return Instances.query(context.getContentResolver(), SYSTEM_DB_PROJECTION,
                begin, end);
    }

    private void removeEventIfExists(EventInstance event) {
        long instanceId = event.getId();
        String whereClause = Columns._ID + "=?";
        String[] whereArgs = new String[]{String.valueOf(instanceId)};
        int rowsDeleted = eventsDB.delete(EventDatabaseOpenHelper.TABLE_EVENTS, whereClause, whereArgs);
        Log.d(TAG, "deleted " + rowsDeleted + " rows");
    }

    private List<Long> getInstanceIdsFromSystemDatabase() {
        List<Long> systemInstanceIds = new ArrayList<Long>();
        int lookaheadDays = prefs.getLookaheadDays().value;
        long begin = System.currentTimeMillis();
        long end = System.currentTimeMillis() + (lookaheadDays * EventInstance.MILLISECONDS_IN_DAY);
        Cursor calCursor = null;
        try {
            calCursor = getCalendarCursor(begin, end);
            while (calCursor.moveToNext()) {
                long instanceId = calCursor.getLong(SYSTEM_DB_PROJECTION_ID);
                systemInstanceIds.add(instanceId);
            }
        } catch (Exception e) {
            Log.e(TAG, "error getting list of instance ids from system calendar");
        } finally {
            if (calCursor != null) {
                calCursor.close();
            }
        }
        return systemInstanceIds;
    }

    private List<Long> getInstanceIdsFromLocalDatabase() {
        List<Long> localInstanceIds = new ArrayList<Long>();
        Cursor localCursor = getEventCursor();
        while (localCursor.moveToNext()) {
            long id = localCursor.getLong(LOCAL_DB_PROJECTION_ID);
            localInstanceIds.add(id);
        }
        //TODO
        return localInstanceIds;
    }

    private void deleteEventWithId(long id) {
        String selection = Columns._ID + " = ?";
        eventsDB.delete(EventDatabaseOpenHelper.TABLE_EVENTS, selection, new String[]{String.valueOf(id)});
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

    /**
     * Query the internal database for the next event.
     *
     * @return the next event in the database
     * @throws NoSuchEventInstanceException if there is no next event
     */
    public EventInstance nextEvent() {
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
