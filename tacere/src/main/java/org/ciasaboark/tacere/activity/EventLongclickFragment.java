/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventException;
import org.ciasaboark.tacere.database.SimpleCalendarEvent;
import org.ciasaboark.tacere.prefs.Prefs;

/**
 * Created by ciasaboark on 8/9/14.
 */
public class EventLongclickFragment extends DialogFragment {
    private static final String TAG = "EventLongClickFragment";
    private DatabaseInterface databaseInterface;
    private Prefs prefs;
    private SimpleCalendarEvent event;
    private int instanceId;
    private View view;
    private Context context = getActivity();

    public static EventLongclickFragment newInstance(int instanceId) {
        EventLongclickFragment fragment = new EventLongclickFragment();
        Bundle args = new Bundle();
        args.putInt("instanceId", instanceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int instanceId = getArguments().getInt("instanceId");
        databaseInterface = DatabaseInterface.getInstance(getActivity());
        prefs = new Prefs(getActivity());

        try {
            event = databaseInterface.getEvent(instanceId);
        } catch (NoSuchEventException e) {
            Log.e(TAG, "unable to find event with id " + instanceId);
            return null;
        }

        AlertDialog.Builder thisDialog = new AlertDialog.Builder(getActivity());
        thisDialog.setIcon(getResources().getDrawable(R.drawable.calendar_icon));
        thisDialog.setTitle("Edit event");
        thisDialog.setNegativeButton("clear", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetEvent();
            }
        });

        thisDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveSettings();
            }
        });

        return thisDialog.create();
    }

    private void resetEvent() {
        Toast.makeText(getActivity(), "not yet implemented", Toast.LENGTH_SHORT).show();
    }

    private void saveSettings() {
        Toast.makeText(getActivity(), "not yet implemented", Toast.LENGTH_SHORT).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
        view = inflator.inflate(R.layout.dialog_event_longclick, null);
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


        Button resetEventButton = (Button) view.findViewById(R.id.button_reset);
        resetEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRingerType(SimpleCalendarEvent.RINGER.UNDEFINED);
            }
        });

        Button saveForAllEventsButton = (Button) view.findViewById(R.id.button_all_events);
        saveForAllEventsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT).show();
            }
        });


        return view;
    }

    private void setRingerType(int type) {
        databaseInterface.setRingerType(event.getEventId(), type);
        try {
            event = databaseInterface.getEvent(event.getId());
        } catch (NoSuchEventException e) {
            Log.e(TAG, "unable to find event after setting a new ringer type, this should not happen");
        }

        drawIndicators();
        nofityDatasetChanged();
    }

    private void nofityDatasetChanged() {
        Log.d(TAG, "Broadcasting message");
        Intent intent = new Intent("custom-event-name");
        // You can also include some extra data.
        intent.putExtra("message", "This is my message!");
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
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
