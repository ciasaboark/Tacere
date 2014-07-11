/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class EventProvider extends ContentProvider {

    private static final int EVENTS = 1;
    private static final int EVENT_ID = 2;
    private static final String PROVIDER_NAME = "org.ciasaboark.tacere.Events";
    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/event");
    private static final UriMatcher uriMatcher;

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

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case EVENTS:
                count = eventsDB.delete(TABLE_EVENTS, selection, selectionArgs);
                break;
            case EVENT_ID:
                String id = uri.getPathSegments().get(1);
                count = eventsDB.delete(TABLE_EVENTS, Columns._ID + " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown uri " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case EVENTS:
                return "vnd.android.cursor.dir/org.ciasaboark.tacere.Events";
            case EVENT_ID:
                return "vnd.android.cursor.item/org.ciasaboark.tacere.Events";
            default:
                throw new IllegalArgumentException("Unsupported Uri " + uri);
        }
    }

    @Override
    //note that values should represent a complete CalEvent object, inserting with
    //+ CONFLICT_REPLACE will cause the existing row to be deleted before the insert.
    public Uri insert(Uri uri, ContentValues values) {
        //add the event, replace if the row already exists
        long rowID = eventsDB.insertWithOnConflict(TABLE_EVENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        //if the event was added
        if (rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        } else {
            throw new SQLException("Failed to insert/update new row into " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        EventDatabaseHelper dbHelper = new EventDatabaseHelper(context);
        eventsDB = dbHelper.getWritableDatabase();
        return (eventsDB != null);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        sqlBuilder.setTables(TABLE_EVENTS);

        if (uriMatcher.match(uri) == EVENT_ID) {
            sqlBuilder.appendWhere(Columns._ID + " = " + uri.getPathSegments().get(1));
        }

        if (sortOrder == null || sortOrder == "") {
            //if we aren't given a sort order preference, order the results chronologically
            sortOrder = Columns.BEGIN;
        }

        Cursor c = sqlBuilder.query(eventsDB, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case EVENTS:
                count = eventsDB.update(TABLE_EVENTS, values, selection, selectionArgs);
                break;
            case EVENT_ID:
                count = eventsDB.update(TABLE_EVENTS, values, Columns._ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
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
