/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.DataSetManager;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventInstanceException;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.EventManager;
import org.ciasaboark.tacere.event.ringer.RingerSource;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.prefs.Prefs;

import java.util.HashMap;

public class EventDetailsFragment extends DialogFragment {
    public static final String TAG = "EventLongClickFragment";
    private DatabaseInterface databaseInterface;
    private Prefs prefs;
    private EventInstance event;
    private int instanceId;
    private Context context;
    private View view;
    private Button positiveButton;

    public static EventDetailsFragment newInstance(EventInstance eventInstance) {
        EventDetailsFragment fragment = new EventDetailsFragment();
        Bundle args = new Bundle();
        args.putLong("instanceId", eventInstance.getId());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        long instanceId = getArguments().getLong("instanceId");
        context = getActivity();
        databaseInterface = DatabaseInterface.getInstance(context);
        prefs = new Prefs(context);

        try {
            event = databaseInterface.getEvent(instanceId);
        } catch (NoSuchEventInstanceException e) {
            Log.e(TAG, "unable to find event with id " + instanceId);
            return null;
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        view = getActivity().getLayoutInflater().inflate(R.layout.dialog_event_longclick, null);
        setupWidgetsForView();
        dialogBuilder.setView(view);

        //the clear button should only be visible if the event has a custom ringer or if the events
        //series has a custom ringer set
        EventManager eventManager = new EventManager(context, event);

        if (eventManager.getRingerSource() == RingerSource.EVENT_SERIES ||
                eventManager.getRingerSource() == RingerSource.INSTANCE) {
            dialogBuilder.setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    resetEvent();
                }
            });
        }


        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //nothing to do here
            }
        });

        dialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveSettings();
            }
        });

        //the positive button should only be enabled if the selected ringer is not UNDEFINED
        AlertDialog dialog = dialogBuilder.create();
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        AlertDialog thisDialog = (AlertDialog) getDialog();
        positiveButton = thisDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setEnabled(event.getRingerType() == RingerType.UNDEFINED ? false : true);
    }

    private void setupWidgetsForView() {
        TextView eventTitle = (TextView) view.findViewById(R.id.event_title);
        eventTitle.setText(event.getTitle());

        TextView calendarTitle = (TextView) view.findViewById(R.id.calendar_title);
        calendarTitle.setText(databaseInterface.getCalendarNameForId(event.getCalendarId()));

        LinearLayout eventRepetitonBox = (LinearLayout) view.findViewById(R.id.event_details_repetitions_box);
        if (databaseInterface.doesEventRepeat(event.getEventId())) {
            eventRepetitonBox.setVisibility(View.VISIBLE);
            TextView eventRepetitionText = (TextView) view.findViewById(R.id.event_details_repetition_text);
            long eventRepetitions = databaseInterface.getEventRepetitionCount(event.getEventId());
            eventRepetitionText.setText("Repeats " + eventRepetitions + " times");
        } else {
            eventRepetitonBox.setVisibility(View.GONE);
        }

        colorizeIcons();
        drawIndicators();

        ImageButton buttonNormal = (ImageButton) view.findViewById(R.id.imageButtonNormal);
        ImageButton buttonVibrate = (ImageButton) view.findViewById(R.id.imageButtonVibrate);
        ImageButton buttonSilent = (ImageButton) view.findViewById(R.id.imageButtonSilent);
        ImageButton buttonIgnore = (ImageButton) view.findViewById(R.id.imageButtonIgnore);
        buttonNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRingerType(RingerType.NORMAL);
            }
        });

        buttonVibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRingerType(RingerType.VIBRATE);
            }
        });

        buttonSilent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRingerType(RingerType.SILENT);
            }
        });

        buttonIgnore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRingerType(RingerType.IGNORE);
            }
        });
    }

    private void resetEvent() {
        if (databaseInterface.doesEventRepeat(event.getEventId())) {
            long eventRepetions = databaseInterface.getEventRepetitionCount(event.getEventId());
            String positiveButtonText = getResources().getString(R.string.event_dialog_reset_all_instances_message);
            Drawable icon = getResources().getDrawable(R.drawable.history);
            icon.mutate().setColorFilter(
                    getResources().getColor(R.color.primary), PorterDuff.Mode.MULTIPLY);
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.event_dialog_repeating_event_conformation_title)
                    .setMessage(String.format(positiveButtonText, event.getTitle(), eventRepetions))
                    .setPositiveButton(R.string.event_dialog_save_all_instances, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            resetAllEvents();
                        }
                    })
                    .setNegativeButton(R.string.event_dialog_save_instance, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            resetEventInstance();
                        }
                    })
                    .setIcon(icon)
                    .show();
        } else {
            resetEventInstance();
        }
    }

    private void resetEventInstance() {
        databaseInterface.setRingerForInstance(event.getId(), RingerType.UNDEFINED);
        notifyDatasetChanged();
    }

    private void saveSettings() {
        if (databaseInterface.doesEventRepeat(event.getEventId())) {
            long eventRepetions = databaseInterface.getEventRepetitionCount(event.getEventId());
            String positiveButtonText = getResources().getString(R.string.event_dialog_save_all_instances_message);
            Drawable icon = getResources().getDrawable(R.drawable.history);
            icon.mutate().setColorFilter(
                    getResources().getColor(R.color.primary), PorterDuff.Mode.MULTIPLY);
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.event_dialog_repeating_event_conformation_title)
                    .setMessage(String.format(positiveButtonText, event.getTitle(), eventRepetions))
                    .setPositiveButton(R.string.event_dialog_save_all_instances, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            resetAllEvents();
                            saveSettingsForAllEvents();
                        }
                    })
                    .setNegativeButton(R.string.event_dialog_save_instance, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            saveSettingsForEventInstance();
                        }
                    })
                    .setIcon(icon)
                    .show();
        } else {
            saveSettingsForEventInstance();
        }
    }

    private void saveSettingsForEventInstance() {
        databaseInterface.setRingerForInstance(event.getId(), event.getRingerType());
        notifyDatasetChanged();
    }

    private void colorizeIcons() {
        HashMap<ImageButton, RingerType> buttons = new HashMap<ImageButton, RingerType>();
        buttons.put((ImageButton) view.findViewById(R.id.imageButtonNormal), RingerType.NORMAL);
        buttons.put((ImageButton) view.findViewById(R.id.imageButtonVibrate), RingerType.VIBRATE);
        buttons.put((ImageButton) view.findViewById(R.id.imageButtonSilent), RingerType.SILENT);
        buttons.put((ImageButton) view.findViewById(R.id.imageButtonIgnore), RingerType.IGNORE);

        for (ImageButton thisButton : buttons.keySet()) {
            thisButton.setImageDrawable(getColorizedIcon(buttons.get(thisButton)));
        }


    }

    private void drawIndicators() {
        LinearLayout indicatorNormal = (LinearLayout) view.findViewById(R.id.indicator_normal);
        LinearLayout indicatorVibrate = (LinearLayout) view.findViewById(R.id.indicator_vibrate);
        LinearLayout indicatorSilent = (LinearLayout) view.findViewById(R.id.indicator_silent);
        LinearLayout indicatorIgnore = (LinearLayout) view.findViewById(R.id.indicator_ignore);

        int colorActive = getResources().getColor(R.color.accent);
        int colorInactive = getResources().getColor(R.color.primary);
        RingerType ringerMode = event.getRingerType();

        if (ringerMode == RingerType.NORMAL) {
            indicatorNormal.setBackgroundColor(colorActive);
        } else {
            indicatorNormal.setBackgroundColor(colorInactive);
        }

        if (ringerMode == RingerType.VIBRATE) {
            indicatorVibrate.setBackgroundColor(colorActive);
        } else {
            indicatorVibrate.setBackgroundColor(colorInactive);
        }

        if (ringerMode == RingerType.SILENT) {
            indicatorSilent.setBackgroundColor(colorActive);
        } else {
            indicatorSilent.setBackgroundColor(colorInactive);
        }

        if (ringerMode == RingerType.IGNORE) {
            indicatorIgnore.setBackgroundColor(colorActive);
        } else {
            indicatorIgnore.setBackgroundColor(colorInactive);
        }
    }

    private void setRingerType(RingerType type) {
        positiveButton.setEnabled(true);
        event.setRingerType(type);
        drawIndicators();
        colorizeIcons();
    }

    private void resetAllEvents() {
        prefs.unsetRingerTypeForEventSeries(event.getEventId());
        databaseInterface.setRingerForAllInstancesOfEvent(event.getEventId(),
                RingerType.UNDEFINED);
        notifyDatasetChanged();
    }

    private void resetThisEvent() {
        databaseInterface.setRingerForInstance(event.getId(), event.getRingerType());
        notifyDatasetChanged();
    }

    private void notifyDatasetChanged() {
        DataSetManager dsm = new DataSetManager(this, context);
        dsm.broadcastDataSetChangedMessage();
    }

    private void saveSettingsForAllEvents() {
        prefs.setRingerForEventSeries(event.getEventId(), event.getRingerType());
        notifyDatasetChanged();
    }

    private Drawable getColorizedIcon(RingerType ringerType) {
        Drawable colorizedIcon;
        switch (ringerType) {
            case NORMAL:
                colorizedIcon = getResources().getDrawable(R.drawable.ic_state_normal);
                break;
            case VIBRATE:
                colorizedIcon = getResources().getDrawable(R.drawable.ic_state_vibrate);
                break;
            case SILENT:
                colorizedIcon = getResources().getDrawable(R.drawable.ic_state_silent);
                break;
            default:
                colorizedIcon = getResources().getDrawable(R.drawable.ic_state_ignore);
        }

        int color = getResources().getColor(R.color.primary);
        if (event.getRingerType() == ringerType) {
            color = getResources().getColor(R.color.accent);
        }

        colorizedIcon.mutate().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        return colorizedIcon;
    }
}
