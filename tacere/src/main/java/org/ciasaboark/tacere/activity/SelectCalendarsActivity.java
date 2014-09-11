/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.SimpleCalendar;
import org.ciasaboark.tacere.database.SimpleCalendarEvent;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class SelectCalendarsActivity extends Activity {
    @SuppressWarnings("unused")
    private final String TAG = "CalendarsActivity";
    private Prefs prefs;
    private List<SimpleCalendar> simpleCalendars;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_calendars);
        Drawable upIcon = getResources().getDrawable(R.drawable.calendar_icon);
        int c = getResources().getColor(R.color.header_text_color);
        upIcon.mutate().setColorFilter(c, PorterDuff.Mode.MULTIPLY);
        getActionBar().setIcon(upIcon);

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

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        restartService();
    }

    private void restartService() {
        Intent i = new Intent(this, EventSilencerService.class);
        i.putExtra("type", RequestTypes.ACTIVITY_RESTART);
        startService(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.select_calendars, menu);
        return true;
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
        final ListView lv = (ListView) findViewById(R.id.calendar_listview);
        final SimpleCalendarListAdapter listAdapter = new SimpleCalendarListAdapter(this, R.layout.calendar_list_item, simpleCalendars);
        lv.setAdapter(listAdapter);

        //the calendars should only be clickable if we arent syncing all calendars
        if (!prefs.shouldAllCalendarsBeSynced()) {
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    SimpleCalendar simpleCalendar = simpleCalendars.get(position);
                    simpleCalendar.setSelected(!simpleCalendar.isSelected());
                    toggleSyncCalendar(simpleCalendar);
                    listAdapter.notifyDataSetChanged();
                }
            });
            lv.setClickable(true);
        } else {
            lv.setClickable(false);
        }

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

    private Drawable getDesaturatedDrawable(Drawable drawable) {
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(1f);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
        drawable.setColorFilter(filter);
        return drawable;
    }

    private class SimpleCalendarListAdapter extends ArrayAdapter<SimpleCalendar> {
        private static final String TAG = "CalendarListAdapter";
        private final Context context;
        private final List<SimpleCalendar> simpleCalendarList;
        private final SimpleCalendarListAdapter thisAdapter = this;


        public SimpleCalendarListAdapter(Context ctx, int resourceId, List<SimpleCalendar> simpleCalendars) {
            super(ctx, resourceId, simpleCalendars);
            this.context = ctx;
            this.simpleCalendarList = simpleCalendars;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                LayoutInflater inflator = ((Activity) context).getLayoutInflater();
                row = inflator.inflate(R.layout.calendar_list_item, parent, false);
            }
            TextView calendarName = (TextView) row.findViewById(R.id.calendar_name);
            TextView calendarAccountName = (TextView) row.findViewById(R.id.calendar_account);
            ImageView calendarSidebar = (ImageView) row.findViewById(R.id.calendar_sidebar);
            ImageView calendarIcon = (ImageView) row.findViewById(R.id.configure_calendar_icon);
            ImageButton calendarSettingsButton = (ImageButton) row.findViewById(R.id.configure_calendar_button);
            CheckBox calendarCheckBox = (CheckBox) row.findViewById(R.id.calendar_checkbox);

            try {
                final SimpleCalendar simpleCalendar = simpleCalendarList.get(position);
                Drawable sideBarImage = (Drawable) getResources().getDrawable(R.drawable.sidebar).mutate();
                int iconColor;
                if (simpleCalendar.isSelected() || prefs.shouldAllCalendarsBeSynced()) {
                    iconColor = getResources().getColor(R.color.primary);
                } else {
                    iconColor = getResources().getColor(R.color.textColorDisabled);
                }

                sideBarImage.setColorFilter(simpleCalendar.getColor(), PorterDuff.Mode.MULTIPLY);
                calendarSidebar.setBackgroundDrawable(sideBarImage); //TODO deprecated method use

                Drawable calendarIconDrawable = null;
                int calendarRinger = prefs.getRingerForCalendar(simpleCalendar.getId());
                switch (calendarRinger) {
                    case SimpleCalendarEvent.RINGER.NORMAL:
                        calendarIconDrawable = getResources().getDrawable(R.drawable.ic_state_normal);
                        break;
                    case SimpleCalendarEvent.RINGER.IGNORE:
                        calendarIconDrawable = getResources().getDrawable(R.drawable.ic_state_ignore);
                        break;
                    case SimpleCalendarEvent.RINGER.VIBRATE:
                        calendarIconDrawable = getResources().getDrawable(R.drawable.ic_state_vibrate);
                        break;
                    case SimpleCalendarEvent.RINGER.SILENT:
                        calendarIconDrawable = getResources().getDrawable(R.drawable.ic_state_silent);
                        break;
                    default:
                        calendarIconDrawable = getResources().getDrawable(R.drawable.blank);
                }
                calendarIconDrawable.mutate().setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
                calendarIcon.setBackgroundDrawable(calendarIconDrawable);

                Drawable calendarSettingsIcon = (Drawable) getResources().getDrawable(R.drawable.action_settings).mutate();
                calendarSettingsIcon.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
                calendarSettingsButton.setBackgroundDrawable(calendarSettingsIcon); //TODO deprecated method use

                calendarSettingsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        if (simpleCalendar.isSelected() || prefs.shouldAllCalendarsBeSynced()) {
                            //TODO fragile connection to the ringer types
                            final String[] options = {"Normal", "Vibrate", "Silent", "Ignore"};
                            final Deque<Integer> selectedItem = new ArrayDeque<Integer>();
                            int calendarRinger = prefs.getRingerForCalendar(simpleCalendar.getId());
                            if (calendarRinger != SimpleCalendarEvent.RINGER.UNDEFINED) {
                                calendarRinger++;
                            } else {
                                calendarRinger = -1;
                            }

                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("Configure calendar");
                            builder.setSingleChoiceItems(options, calendarRinger, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    selectedItem.clear();
                                    selectedItem.addFirst(i);
                                }
                            });
                            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        if (!selectedItem.isEmpty()) {
                                            prefs.setRingerForCalendar(simpleCalendar.getId(), selectedItem.getFirst() + 1); //selection has to be offset since undefined (ringer type 0) is ommited from the list
                                            Toast.makeText(context, "set ringer for calendar " + simpleCalendar.getDisplayName() + " to ringer type: " + options[i], Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(context, "no ringer selected", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (Exception e) {
                                        //TODO
                                    }
                                    thisAdapter.notifyDataSetChanged();
                                }
                            });
                            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //nothing to do here
                                }
                            });

                            //the clear button should only be visible if a ringer has been set
                            if (prefs.getRingerForCalendar(simpleCalendar.getId()) != SimpleCalendarEvent.RINGER.UNDEFINED) {
                                builder.setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        try {
                                            SimpleCalendar calendar = simpleCalendarList.get(position);
                                            prefs.unsetRingerTypeForCalendar(calendar.getId());
                                            //TODO stringify
                                            Toast.makeText(context, "Unset ringer for calendar " + calendar.getDisplayName(), Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            //TODO
                                        }
                                        thisAdapter.notifyDataSetChanged();
                                    }
                                });
                            }

                            builder.show();
                        }
                    }
                });

                calendarName.setText(simpleCalendar.getDisplayName());
                calendarName.setTextColor(iconColor);
                calendarAccountName.setText(simpleCalendar.getAccountName());
                calendarAccountName.setTextColor(iconColor);
                List<Long> selectedCalendars = prefs.getSelectedCalendars();

                if (selectedCalendars.contains(simpleCalendar.getId())) {
                    simpleCalendar.setSelected(true);
                } else {
                    simpleCalendar.setSelected(false);
                }


                if (simpleCalendar.isSelected() || prefs.shouldAllCalendarsBeSynced()) {
                    calendarCheckBox.setChecked(true);
                } else {
                    calendarCheckBox.setChecked(false);
                }

            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "error getting calendar at position " + position);
                Log.e(TAG, e.getMessage());
                calendarName.setText("Error getting calendar for this position, check logcat");
            }

            if (prefs.shouldAllCalendarsBeSynced()) {
                int disabledTextColor = getResources().getColor(R.color.textColorDisabled);
                calendarAccountName.setTextColor(disabledTextColor);
                calendarCheckBox.setChecked(true);
                calendarCheckBox.setEnabled(false);
            }

            return row;
        }
    }

}
