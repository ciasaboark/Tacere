/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.DataSetManager;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventException;
import org.ciasaboark.tacere.database.SimpleCalendarEvent;
import org.ciasaboark.tacere.prefs.Prefs;

/**
 * Created by ciasaboark on 8/9/14.
 */
public class EventDetailsFragment extends DialogFragment {
    private static final String TAG = "EventLongClickFragment";
    private DatabaseInterface databaseInterface;
    private Prefs prefs;
    private SimpleCalendarEvent event;
    private int instanceId;
    private Context context;
    private View view;

    public static EventDetailsFragment newInstance(int instanceId) {
        EventDetailsFragment fragment = new EventDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("instanceId", instanceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        int instanceId = getArguments().getInt("instanceId");
        databaseInterface = DatabaseInterface.getInstance(getActivity());
        prefs = new Prefs(getActivity());
        context = getActivity().getApplicationContext();

        try {
            event = databaseInterface.getEvent(instanceId);
        } catch (NoSuchEventException e) {
            Log.e(TAG, "unable to find event with id " + instanceId);
            return null;
        }

        AlertDialog.Builder thisDialog = new AlertDialog.Builder(getActivity());
//        thisDialog.setIcon(getColorizedTitleIcon());
//        thisDialog.setTitle("Edit event");
        view = getActivity().getLayoutInflater().inflate(R.layout.dialog_event_longclick, null);
        setupWidgetsForView();
        thisDialog.setView(view);

        thisDialog.setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetEvent();
            }
        });

        thisDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //nothing to do here
            }
        });

        thisDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveSettings();
            }
        });

        Dialog d = thisDialog.create();
        d.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return d;
    }

    private Drawable getColorizedTitleIcon() {
        Drawable d = getResources().getDrawable(R.drawable.calendar_icon);
        int color = getResources().getColor(R.color.primary);
        d.mutate().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        return d;
    }

    private void resetEvent() {
        CheckBox cb = (CheckBox) view.findViewById(R.id.all_events_checkbox);
        if (cb.isChecked()) {
            resetAllEvents();
        } else {
            databaseInterface.setRingerType(event.getId(), SimpleCalendarEvent.RINGER.UNDEFINED);
        }
        notifyDatasetChanged();
    }

    private void resetAllEvents() {
        databaseInterface.setRingerForAllInstancesOfEvent(event.getEventId(), SimpleCalendarEvent.RINGER.UNDEFINED);
        prefs.unsetRingerTypeForEventSeries(event.getEventId());
    }

    private void saveSettings() {
        CheckBox cb = (CheckBox) view.findViewById(R.id.all_events_checkbox);
        if (cb.isChecked()) {
            saveSettingsForAllEvents();
        } else {
            databaseInterface.setRingerType(event.getId(), event.getRingerType());
        }
        notifyDatasetChanged();
    }

    private void saveSettingsForAllEvents() {
        databaseInterface.setRingerForAllInstancesOfEvent(event.getEventId(), event.getRingerType());
        prefs.setRingerForEventSeries(event.getEventId(), event.getRingerType());
    }

    private void setupWidgetsForView() {
        TextView eventTitle = (TextView) view.findViewById(R.id.event_title);
        eventTitle.setText(event.getTitle());

        TextView calendarTitle = (TextView) view.findViewById(R.id.calendar_title);
        calendarTitle.setText(databaseInterface.getCalendarNameForId(event.getCalendarId()));

        colorizeIcons();
        drawIndicators();

        ImageButton buttonNormal = (ImageButton) view.findViewById(R.id.imageButtonNormal);
        ImageButton buttonVibrate = (ImageButton) view.findViewById(R.id.imageButtonVibrate);
        ImageButton buttonSilent = (ImageButton) view.findViewById(R.id.imageButtonSilent);
        ImageButton buttonIgnore = (ImageButton) view.findViewById(R.id.imageButtonIgnore);
        buttonNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRingerType(SimpleCalendarEvent.RINGER.NORMAL);
            }
        });

        buttonVibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRingerType(SimpleCalendarEvent.RINGER.VIBRATE);
            }
        });

        buttonSilent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRingerType(SimpleCalendarEvent.RINGER.SILENT);
            }
        });

        buttonIgnore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRingerType(SimpleCalendarEvent.RINGER.IGNORE);
            }
        });
    }

    private void setRingerType(int type) {
        event.setRingerType(type);

        drawIndicators();
//        notifyDatasetChanged();
    }

    private void notifyDatasetChanged() {
        DataSetManager dsm = new DataSetManager(this, context);
        dsm.broadcastDataSetChangedMessage();
    }

    private void colorizeIcons() {
        ImageButton[] imageButtons = {
                (ImageButton) view.findViewById(R.id.imageButtonNormal),
                (ImageButton) view.findViewById(R.id.imageButtonVibrate),
                (ImageButton) view.findViewById(R.id.imageButtonSilent),
                (ImageButton) view.findViewById(R.id.imageButtonIgnore)
        };
        for (ImageButton ib : imageButtons) {
            Drawable backgroundDrawable = getColorizedIcon(ib.getBackground());
            ib.setBackgroundDrawable(backgroundDrawable);
        }
    }

    private Drawable getColorizedIcon(Drawable d) {
        Drawable colorizedIcon = d;
        int color = getResources().getColor(R.color.primary);
        colorizedIcon.mutate().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        return colorizedIcon;
    }

    private void drawIndicators() {
        LinearLayout indicatorNormal = (LinearLayout) view.findViewById(R.id.indicator_normal);
        LinearLayout indicatorVibrate = (LinearLayout) view.findViewById(R.id.indicator_vibrate);
        LinearLayout indicatorSilent = (LinearLayout) view.findViewById(R.id.indicator_silent);
        LinearLayout indicatorIgnore = (LinearLayout) view.findViewById(R.id.indicator_ignore);

        int colorActive = getResources().getColor(R.color.accent);
        int colorInactive = getResources().getColor(R.color.primary);
        int ringerMode = event.getRingerType();

        if (ringerMode == SimpleCalendarEvent.RINGER.NORMAL) {
            indicatorNormal.setBackgroundColor(colorActive);
        } else {
            indicatorNormal.setBackgroundColor(colorInactive);
        }

        if (ringerMode == SimpleCalendarEvent.RINGER.VIBRATE) {
            indicatorVibrate.setBackgroundColor(colorActive);
        } else {
            indicatorVibrate.setBackgroundColor(colorInactive);
        }

        if (ringerMode == SimpleCalendarEvent.RINGER.SILENT) {
            indicatorSilent.setBackgroundColor(colorActive);
        } else {
            indicatorSilent.setBackgroundColor(colorInactive);
        }

        if (ringerMode == SimpleCalendarEvent.RINGER.IGNORE) {
            indicatorIgnore.setBackgroundColor(colorActive);
        } else {
            indicatorIgnore.setBackgroundColor(colorInactive);
        }
    }
}
