/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class EventDatabaseOpenHelper extends SQLiteOpenHelper {
    public static final String TABLE_EVENTS = "events";
    private static final String DB_NAME = "events.sqlite";
    private static final int VERSION = 7;

    public EventDatabaseOpenHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
    }

    private void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_EVENTS + " ( " + Columns._ID + " integer primary key," +
                Columns.EVENT_ID + " integer, " +
                Columns.RINGER_TYPE + " integer, " +
                Columns.CAL_ID + " integer" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        createTable(db);
    }
}
