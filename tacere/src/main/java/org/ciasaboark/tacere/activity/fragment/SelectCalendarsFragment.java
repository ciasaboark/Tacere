/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.event.Calendar;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.prefs.Prefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SelectCalendarsFragment extends Fragment {
    @SuppressWarnings("unused")
    private final String TAG = "SelectCalendarsFragment";
    private int layout = R.layout.fragment_select_calendars;
    private Prefs prefs;
    private List<Calendar> calendars;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                layout, container, false);

        prefs = new Prefs(getActivity());
        buildSimpleCalendarList();
        final RelativeLayout syncAllCalendarsBox = (RelativeLayout) rootView.findViewById(R.id.sync_all_calendars);
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

        return rootView;
    }

    private void buildSimpleCalendarList() {
        DatabaseInterface databaseInterface = DatabaseInterface.getInstance(getActivity());
        calendars = databaseInterface.getCalendarIdList();
        List<Long> calendarsToSync = prefs.getSelectedCalendarsIds();
        for (Calendar c : calendars) {
            if (calendarsToSync.contains(c.getId())) {
                c.setSelected(true);
            }
        }
    }

    private void drawSyncBoxSwitch() {
        final Switch syncAllCalendarsSwitch = (Switch) rootView.findViewById(R.id.sync_all_calendars_switch);
        syncAllCalendarsSwitch.setChecked(prefs.shouldAllCalendarsBeSynced());
    }

    private void drawDialogBodyOrError() {
        if (calendars.isEmpty()) {
            drawError();
        } else {
            drawDialogBody();
        }
    }

    private void drawError() {
        hideDialogBody();
        LinearLayout error = (LinearLayout) rootView.findViewById(R.id.error_box);
        error.setVisibility(View.VISIBLE);

    }

    private void hideDialogBody() {
        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.calendars_box);
        layout.setVisibility(View.GONE);
    }

    private void drawDialogBody() {
        hideError();
        drawSyncBoxSwitch();
        drawListView();
    }

    private void drawListView() {
        final ListView lv = (ListView) rootView.findViewById(R.id.calendar_listview);
        final SimpleCalendarListAdapter listAdapter = new SimpleCalendarListAdapter(getActivity(), R.layout.list_item_calendar, calendars);
        lv.setAdapter(listAdapter);

        //the calendars should only be clickable if we arent syncing all calendars
        if (!prefs.shouldAllCalendarsBeSynced()) {
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    Calendar calendar = calendars.get(position);
                    calendar.setSelected(!calendar.isSelected());
                    toggleSyncCalendar(calendar);
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

    private void toggleSyncCalendar(Calendar calendar) {
        if (calendar.isSelected()) {
            addCalendarToSyncList(calendar);
        } else {
            removeCalendarFromSyncList(calendar);
        }
    }

    private void removeCalendarFromSyncList(Calendar calendar) {
        List<Long> calendarsToSync = prefs.getSelectedCalendarsIds();
        long calendarId = calendar.getId();
        calendarsToSync.remove(calendarId);
        prefs.setSelectedCalendars(calendarsToSync);
    }

    private void addCalendarToSyncList(Calendar calendar) {
        List<Long> calendarsToSync = prefs.getSelectedCalendarsIds();
        long calendarId = calendar.getId();
        if (!calendarsToSync.contains(calendarId)) {
            calendarsToSync.add(calendarId);
        }
        prefs.setSelectedCalendars(calendarsToSync);
    }

    private void hideError() {
        LinearLayout error = (LinearLayout) rootView.findViewById(R.id.error_box);
        error.setVisibility(View.GONE);
    }

    private class SimpleCalendarListAdapter extends ArrayAdapter<Calendar> {
        private static final String TAG = "CalendarListAdapter";
        private final Context context;
        private final List<Calendar> calendarList;
        private final SimpleCalendarListAdapter thisAdapter = this;
        private View row;
        private Calendar calendar;

        public SimpleCalendarListAdapter(Context ctx, int resourceId, List<Calendar> calendars) {
            super(ctx, resourceId, calendars);
            this.context = ctx;
            this.calendarList = calendars;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            row = convertView;
            if (row == null) {
                LayoutInflater inflator = ((Activity) context).getLayoutInflater();
                row = inflator.inflate(R.layout.list_item_calendar, parent, false);
            }

            try {
                calendar = calendarList.get(position);
                List<Long> selectedCalendarIds = prefs.getSelectedCalendarsIds();

                if (selectedCalendarIds.contains(calendar.getId())) {
                    calendar.setSelected(true);
                } else {
                    calendar.setSelected(false);
                }

                drawSidebar();
                colorizeText();
                drawRingerIcon();
                drawSettingsButton(position);
                drawCheckBox();


            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "error getting calendar at position " + position);
                Log.e(TAG, e.getMessage());
                TextView calendarName = (TextView) row.findViewById(R.id.calendar_name);
                calendarName.setText("Error getting calendar for this position, check logcat");
            }

            return row;
        }

        private void drawCheckBox() {
            CheckBox calendarCheckBox = (CheckBox) row.findViewById(R.id.calendar_checkbox);
            if (calendar.isSelected() || prefs.shouldAllCalendarsBeSynced()) {
                calendarCheckBox.setChecked(true);
            } else {
                calendarCheckBox.setChecked(false);
            }

            if (prefs.shouldAllCalendarsBeSynced()) {
                calendarCheckBox.setEnabled(false);
            }
        }

        private void drawSidebar() {
            ImageView calendarSidebar = (ImageView) row.findViewById(R.id.calendar_sidebar);
            Drawable sideBarImage = (Drawable) getResources().getDrawable(R.drawable.sidebar).mutate();

            sideBarImage.setColorFilter(calendar.getColor(), PorterDuff.Mode.MULTIPLY);
            calendarSidebar.setBackgroundDrawable(sideBarImage); //TODO deprecated method use
        }

        private void colorizeText() {
            TextView calendarName = (TextView) row.findViewById(R.id.calendar_name);
            TextView calendarAccountName = (TextView) row.findViewById(R.id.calendar_account);
            calendarName.setText(calendar.getDisplayName());
            calendarAccountName.setText(calendar.getAccountName());

            int textColor;
            if (!calendar.isSelected() || prefs.shouldAllCalendarsBeSynced()) {
                textColor = getResources().getColor(R.color.textColorDisabled);
            } else {
                textColor = getResources().getColor(R.color.textcolor);
            }

            calendarName.setTextColor(textColor);
            calendarAccountName.setTextColor(textColor);
        }

        private void drawRingerIcon() {
            ImageView calendarIcon = (ImageView) row.findViewById(R.id.configure_calendar_icon);
            int iconColor;
            if (calendar.isSelected() || prefs.shouldAllCalendarsBeSynced()) {
                iconColor = getResources().getColor(R.color.icon_enabled);
            } else {
                iconColor = getResources().getColor(R.color.icon_disabled);
            }

            Drawable calendarIconDrawable = null;
            RingerType calendarRinger = prefs.getRingerForCalendar(calendar.getId());
            int visibility = View.VISIBLE;
            switch (calendarRinger) {
                case NORMAL:
                    calendarIconDrawable = getResources().getDrawable(R.drawable.ic_state_normal);
                    break;
                case IGNORE:
                    calendarIconDrawable = getResources().getDrawable(R.drawable.ic_state_ignore);
                    break;
                case VIBRATE:
                    calendarIconDrawable = getResources().getDrawable(R.drawable.ic_state_vibrate);
                    break;
                case SILENT:
                    calendarIconDrawable = getResources().getDrawable(R.drawable.ic_state_silent);
                    break;
                default:
                    calendarIconDrawable = getResources().getDrawable(R.drawable.blank);
                    visibility = View.GONE;
            }
            calendarIconDrawable.mutate().setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
            calendarIcon.setImageDrawable(calendarIconDrawable);
            calendarIcon.setVisibility(visibility);
        }

        private void drawSettingsButton(final int position) {

            ImageView calendarSettingsButton = (ImageView) row.findViewById(R.id.configure_calendar_button);
            Drawable settingsIcon = getResources().getDrawable(R.drawable.action_settings);
            int iconColor;
            if (calendar.isSelected() || prefs.shouldAllCalendarsBeSynced()) {
                iconColor = getResources().getColor(R.color.icon_enabled);
            } else {
                iconColor = getResources().getColor(R.color.icon_disabled);
            }
            settingsIcon.mutate().setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
            calendarSettingsButton.setImageDrawable(settingsIcon);


            calendarSettingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final Calendar selectedCalendar = calendars.get(position);
                    if (selectedCalendar.isSelected() || prefs.shouldAllCalendarsBeSynced()) {
                        final ArrayList<String> ringerTypes = new ArrayList<String>(Arrays.asList(RingerType.names()));
                        String undefinedString = RingerType.UNDEFINED.toString();
                        ringerTypes.remove(undefinedString);
                        for (int i = 0; i < ringerTypes.size(); i++) {
                            String type = ringerTypes.get(i);
                            Character firstChar = type.charAt(0);
                            type = firstChar.toString().toUpperCase() + type.substring(1).toLowerCase();
                            ringerTypes.set(i, type);
                        }

                        final String[] options = ringerTypes.toArray(new String[]{});

                        //set the pre-selected option to the value previously selected
                        int selectedRinger = -1;
                        try {
                            RingerType storedRinger = prefs.getRingerForCalendar(selectedCalendar.getId());
                            if (storedRinger != RingerType.UNDEFINED) {
                                selectedRinger = ringerTypes.indexOf(storedRinger.toString());
                            }
                        } catch (IllegalArgumentException e) {
                            //nothing to do here
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Configure calendar");
                        builder.setSingleChoiceItems(options, selectedRinger, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String selectedRinger = options[i];
                                int selectedRingerInt = RingerType.getIntForStringValue(selectedRinger);
                                prefs.setRingerForCalendar(selectedCalendar.getId(), selectedRingerInt);
                                thisAdapter.notifyDataSetChanged();
                            }
                        });
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //nothing to do here
                            }
                        });
                        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //nothing to do here
                            }
                        });

                        //the clear button should only be visible if a ringer has been set
                        builder.setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                prefs.unsetRingerTypeForCalendar(selectedCalendar.getId());
                                //TODO stringify
                                Toast.makeText(context, "Unset ringer for calendar " + selectedCalendar.getDisplayName(), Toast.LENGTH_SHORT).show();
                                thisAdapter.notifyDataSetChanged();
                            }
                        });

                        builder.show();
                    }
                }
            });
        }

    }

}
