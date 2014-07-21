/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.Calendar;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.prefs.Prefs;

import java.util.ArrayList;
import java.util.List;

public class CalendarsActivity extends Activity {
    private final String TAG = "CalendarsActivity";
    //    private final ArrayList<CheckBox> calendarIds = new ArrayList<CheckBox>();
    private Prefs prefs;
    private DatabaseInterface databaseInterface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_calendars);
        prefs = new Prefs(getApplicationContext());
        databaseInterface = DatabaseInterface.getInstance(getApplicationContext());

        ListView lv = (ListView) findViewById(R.id.calendar_listview);
        final List<Calendar> calendars = databaseInterface.getCalendarIdList();
        lv.setAdapter(new CalendarListAdapter(this, R.layout.calendar_list_item, calendars));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Calendar calendar = calendars.get(position);
                calendar.setSelected(!calendar.isSelected());
            }
        });


        final Switch syncAllCalendarsSwitch = (Switch) findViewById(R.id.calendars_sync_all);
        Button okButton = (Button) findViewById(R.id.calendars_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (syncAllCalendarsSwitch.isChecked()) {
                    prefs.setSyncAllCalendars();
                } else {
                    List<Long> calendarsToSync = new ArrayList<Long>();
                    for (Calendar calendar : calendars) {
                        if (calendar.isSelected()) {
                            calendarsToSync.add(calendar.getId());
                        }
                    }
                    prefs.setSelectedCalendars(calendarsToSync);
                }

            }
        });
    }

    @Override
    public void onStart() {

    }

    private class CalendarListAdapter extends ArrayAdapter<Calendar> {
        private static final String TAG = "CalendarListAdapter";
        private final Context context;
        private final int layoutResourceId;
        private final List<Calendar> calendarList;


        public CalendarListAdapter(Context ctx, int resourceId, List<Calendar> calendars) {
            super(ctx, resourceId, calendars);
            this.context = ctx;
            this.layoutResourceId = resourceId;
            this.calendarList = calendars;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                LayoutInflater inflator = ((Activity) context).getLayoutInflater();
                row = inflator.inflate(R.layout.calendar_list_item, parent, false);
            }
            RelativeLayout calendarColor = (RelativeLayout) row.findViewById(R.id.calendar_color);
            TextView calendarName = (TextView) row.findViewById(R.id.calendar_name);
            TextView calendarAccountName = (TextView) row.findViewById(R.id.calendar_account);
            CheckBox calendarCheckBox = (CheckBox) row.findViewById(R.id.calendar_checkbox);

            try {
                Calendar calendar = calendarList.get(position);
                calendarColor.setBackgroundColor(calendar.getColor());
                calendarName.setText(calendar.getDisplayName());
                calendarAccountName.setText(calendar.getAccountName());
                calendarCheckBox.setSelected(calendar.isSelected());

            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "error getting calendar at position " + position);
            }

            return row;
        }

    }

}
