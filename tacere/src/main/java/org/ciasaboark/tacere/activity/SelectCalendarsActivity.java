/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.util.List;

public class SelectCalendarsActivity extends Activity {
    @SuppressWarnings("unused")
    private final String TAG = "CalendarsActivity";
    //    private final ArrayList<CheckBox> calendarIds = new ArrayList<CheckBox>();
    private Prefs prefs;
    private List<SimpleCalendar> simpleCalendars;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_calendars);
        prefs = new Prefs(getApplicationContext());
        buildSimpleCalendarList();

        final RelativeLayout syncAllCalendarsBox = (RelativeLayout) findViewById(R.id.sync_all_calendars);
        syncAllCalendarsBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefs.setSyncAllCalendars(!prefs.shouldAllCalendarsBeSynced());
                buildSimpleCalendarList();
                drawSyncBoxSwitch();
                drawDialogBody();
            }
        });

        drawSyncBoxSwitch();
        drawDialogBodyOrError();
    }

    private void buildSimpleCalendarList() {
        DatabaseInterface databaseInterface = DatabaseInterface.getInstance(getApplicationContext());
        simpleCalendars = databaseInterface.getCalendarIdList();
        List<Long> calendarsToSync = prefs.getSelectedCalendars();
        for (SimpleCalendar c : simpleCalendars) {
            if (calendarsToSync.contains(c.getId())) {
                c.setSelected(true);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        restartService();
    }

    private void drawSyncBoxSwitch() {
        final Switch syncAllCalendarsSwitch = (Switch) findViewById(R.id.sync_all_calendars_switch);
        syncAllCalendarsSwitch.setChecked(prefs.shouldAllCalendarsBeSynced());
    }

    private void drawDialogBodyOrError() {
        if (simpleCalendars.isEmpty()) {
            drawError();
        } else {
            drawDialogBody();
        }
    }

    private void drawError() {
        hideDialogBody();
        LinearLayout error = (LinearLayout) findViewById(R.id.error_box);
        error.setVisibility(View.VISIBLE);

    }

    public void onClickAddAccount(View v) {
        Intent i = new Intent(Settings.ACTION_ADD_ACCOUNT);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void hideDialogBody() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.calendars_box);
        layout.setVisibility(View.GONE);
    }

    private void drawDialogBody() {
        hideError();
        drawSyncBoxSwitch();
        drawListView();
    }

    private void drawListView() {
        ListView lv = (ListView) findViewById(R.id.calendar_listview);
        SimpleCalendarListAdapter listAdapter = new SimpleCalendarListAdapter(this, R.layout.calendar_list_item, simpleCalendars);
        lv.setAdapter(listAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                SimpleCalendar simpleCalendar = simpleCalendars.get(position);
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.calendar_checkbox);
                simpleCalendar.setSelected(!simpleCalendar.isSelected());
                checkBox.setChecked(simpleCalendar.isSelected());
                toggleSyncCalendar(simpleCalendar);
            }
        });

        boolean syncAllCalendars = prefs.shouldAllCalendarsBeSynced();
        lv.setClickable(!syncAllCalendars);
    }

    private void toggleSyncCalendar(SimpleCalendar calendar) {
        if (calendar.isSelected()) {
            addCalendarToSyncList(calendar);
        } else {
            removeCalendarFromSyncList(calendar);
        }
    }

    private void removeCalendarFromSyncList(SimpleCalendar calendar) {
        List<Long> calendarsToSync = prefs.getSelectedCalendars();
        long calendarId = calendar.getId();
        calendarsToSync.remove(calendarId);
        prefs.setSelectedCalendars(calendarsToSync);
    }

    private void addCalendarToSyncList(SimpleCalendar calendar) {
        List<Long> calendarsToSync = prefs.getSelectedCalendars();
        long calendarId = calendar.getId();
        if (!calendarsToSync.contains(calendarId)) {
            calendarsToSync.add(calendarId);
        }
        prefs.setSelectedCalendars(calendarsToSync);
    }

    private void hideError() {
        LinearLayout error = (LinearLayout) findViewById(R.id.error_box);
        error.setVisibility(View.GONE);
    }

    private void restartService() {
        Intent i = new Intent(this, EventSilencerService.class);
        i.putExtra("type", RequestTypes.ACTIVITY_RESTART);
        startService(i);
    }

    @Override
    public void onStart() {
        super.onStart();
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
            TextView calendarName = (TextView) row.findViewById(R.id.calendar_name);
            TextView calendarAccountName = (TextView) row.findViewById(R.id.calendar_account);
            CheckBox calendarCheckBox = (CheckBox) row.findViewById(R.id.calendar_checkbox);
            ImageView calendarSidebar = (ImageView) row.findViewById(R.id.calendar_sidebar);

            try {
                SimpleCalendar simpleCalendar = simpleCalendarList.get(position);
                Drawable sideBarImage = (Drawable) getResources().getDrawable(R.drawable.sidebar).mutate();
                sideBarImage.setColorFilter(simpleCalendar.getColor(), PorterDuff.Mode.MULTIPLY);
                calendarSidebar.setBackgroundDrawable(sideBarImage); //TODO deprecated method

                calendarName.setText(simpleCalendar.getDisplayName());
                calendarAccountName.setText(simpleCalendar.getAccountName());
                List<Long> selectedCalendars = prefs.getSelectedCalendars();
                if (selectedCalendars.contains(simpleCalendar.getId())) {
                    simpleCalendar.setSelected(true);
                }

                if (simpleCalendar.isSelected() || prefs.shouldAllCalendarsBeSynced()) {
                    calendarCheckBox.setChecked(true);
                }
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "error getting calendar at position " + position);
                calendarName.setText("Error getting calendar for this position, check logcat");
            }

            if (prefs.shouldAllCalendarsBeSynced()) {
                calendarCheckBox.setEnabled(false);
                int disabledTextColor = getResources().getColor(R.color.textColorDisabled);
                calendarAccountName.setTextColor(disabledTextColor);
                calendarName.setTextColor(disabledTextColor);
            }

            return row;
        }

    }

}
