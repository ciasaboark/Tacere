/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.SimpleCalendar;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;

import java.util.ArrayList;
import java.util.List;

public class CalendarsActivity extends Activity {
    @SuppressWarnings("unused")
    private final String TAG = "CalendarsActivity";
    //    private final ArrayList<CheckBox> calendarIds = new ArrayList<CheckBox>();
    private Prefs prefs;
    private DatabaseInterface databaseInterface;
    private List<SimpleCalendar> simpleCalendars;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_calendars);
        prefs = new Prefs(getApplicationContext());
        databaseInterface = DatabaseInterface.getInstance(getApplicationContext());
        simpleCalendars = databaseInterface.getCalendarIdList();

        final Switch syncAllCalendarsSwitch = (Switch) findViewById(R.id.sync_all_calendars_switch);
        syncAllCalendarsSwitch.setChecked(prefs.shouldAllCalendarsBeSynced());
        final RelativeLayout syncAllCalendarsBox = (RelativeLayout) findViewById(R.id.sync_all_calendars);
        syncAllCalendarsBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                syncAllCalendarsSwitch.setChecked(!syncAllCalendarsSwitch.isChecked());
                drawListView();
            }
        });


        Button okButton = (Button) findViewById(R.id.calendars_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (syncAllCalendarsSwitch.isChecked()) {
                    prefs.setSyncAllCalendars(true);
                } else {
                    List<Long> calendarsToSync = new ArrayList<Long>();
                    for (SimpleCalendar simpleCalendar : simpleCalendars) {
                        if (simpleCalendar.isSelected()) {
                            calendarsToSync.add(simpleCalendar.getId());
                        }
                    }
                    prefs.setSelectedCalendars(calendarsToSync);
                }

                //restart the service to make sure that the event list is updated
                restartService();
                finish();
            }
        });

        Button closeButton = (Button) findViewById(R.id.calendars_cancel);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void drawListView() {
        ListView lv = (ListView) findViewById(R.id.calendar_listview);
        SimpleCalendarListAdapter listAdapter = new SimpleCalendarListAdapter(this, R.layout.calendar_list_item, simpleCalendars);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                SimpleCalendar simpleCalendar = simpleCalendars.get(position);
                CheckBox checkBox = (CheckBox) findViewById(R.id.calendar_checkbox);
                checkBox.setChecked(!checkBox.isChecked());
                simpleCalendar.setSelected(!simpleCalendar.isSelected());
            }
        });
        if (prefs.shouldAllCalendarsBeSynced()) {
            lv.setEnabled(false);
        }
    }

    private void restartService() {
        Intent i = new Intent(this, EventSilencerService.class);
        i.putExtra("type", RequestTypes.ACTIVITY_RESTART);
        startService(i);
    }

    @Override
    public void onStart() {

    }

    private class SimpleCalendarListAdapter extends ArrayAdapter<SimpleCalendar> {
        private static final String TAG = "CalendarListAdapter";
        private final Context context;
        private final List<SimpleCalendar> simpleCalendarList;


        public SimpleCalendarListAdapter(Context ctx, int resourceId, List<SimpleCalendar> simpleCalendars) {
            super(ctx, resourceId, simpleCalendars);
            this.context = ctx;
            this.simpleCalendarList = simpleCalendars;
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
                SimpleCalendar simpleCalendar = simpleCalendarList.get(position);
                calendarColor.setBackgroundColor(simpleCalendar.getColor());
                calendarName.setText(simpleCalendar.getDisplayName());
                calendarAccountName.setText(simpleCalendar.getAccountName());
                calendarCheckBox.setSelected(simpleCalendar.isSelected());
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "error getting calendar at position " + position);
            }

            return row;
        }

    }

}
