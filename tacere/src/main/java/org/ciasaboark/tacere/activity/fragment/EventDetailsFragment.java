/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.billing.Authenticator;
import org.ciasaboark.tacere.database.DataSetManager;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventInstanceException;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.EventManager;
import org.ciasaboark.tacere.event.ringer.RingerSource;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.ExtendEventService;

import java.util.HashMap;

public class EventDetailsFragment extends DialogFragment {
    public static final String TAG = "EventLongClickFragment";
    DataSetManager dataSetManager;
    Authenticator authenticator;
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
        dataSetManager = new DataSetManager(this, context);
        databaseInterface = DatabaseInterface.getInstance(context);
        prefs = new Prefs(context);
        authenticator = new Authenticator(context);

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
            dialogBuilder.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    resetEvent();
                }
            });

        }

        dialogBuilder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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

        drawExtendMinutesWidgets();

        colorizeIcons();

        ImageView buttonNormal = (ImageView) view.findViewById(R.id.imageButtonNormal);
        ImageView buttonVibrate = (ImageView) view.findViewById(R.id.imageButtonVibrate);
        ImageView buttonSilent = (ImageView) view.findViewById(R.id.imageButtonSilent);
        ImageView buttonIgnore = (ImageView) view.findViewById(R.id.imageButtonIgnore);
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
        boolean eventRepeats = databaseInterface.doesEventRepeat(event.getEventId());
        boolean eventSeriesRingerSet = prefs.getRingerForEventSeries(event.getEventId()) != RingerType.UNDEFINED;
        if (eventRepeats && eventSeriesRingerSet) {
            if (event.getRingerType() == RingerType.UNDEFINED) {
                //this event does not have an instance ringer set, the reset button should prompt that
                //resetting will reset the entire event series
                String message = getResources().getString(R.string.event_dialog_reset_event_series);
                //if this events calendar has a custom ringer then notify the user that this is what
                //we will drop back to, otherwize use the default ringer
                if (prefs.getRingerForCalendar(event.getCalendarId()) != RingerType.UNDEFINED) {
                    message = String.format(message, new String[]{"calendar"});
                } else {
                    message = String.format(message, new String[]{"default"});
                }

                Drawable icon = getResources().getDrawable(R.drawable.history);
                icon.mutate().setColorFilter(
                        getResources().getColor(R.color.icon_tint), PorterDuff.Mode.MULTIPLY);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.event_dialog_repeating_event_conformation_title)
                        .setMessage(message)
                        .setPositiveButton(R.string.event_dialog_reset_event_series_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                resetAllEvents();
                            }
                        })
                        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //nothing to do here
                            }
                        })
                        .setIcon(icon);
                AlertDialog dialog = builder.show();

            } else {
                //this event has instance and event series ringers set, prompt for which to reset
                long eventRepetions = databaseInterface.getEventRepetitionCount(event.getEventId());
                String message = getResources().getString(R.string.event_dialog_reset_all_instances_message);
                Drawable icon = getResources().getDrawable(R.drawable.history);
                icon.mutate().setColorFilter(
                        getResources().getColor(R.color.icon_tint), PorterDuff.Mode.MULTIPLY);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.event_dialog_repeating_event_conformation_title)
                        .setMessage(String.format(message, event.getTitle(), eventRepetions))
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
                        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //nothing to do here
                            }
                        })
                        .setIcon(icon);
                AlertDialog dialog = builder.show();
            }

        } else {
            resetEventInstance();
        }
    }

    private void saveSettings() {
        if (databaseInterface.doesEventRepeat(event.getEventId())) {
            long eventRepetions = databaseInterface.getEventRepetitionCount(event.getEventId());
            String positiveButtonText = getResources().getString(R.string.event_dialog_save_all_instances_message);
            Drawable icon = getResources().getDrawable(R.drawable.history);
            icon.mutate().setColorFilter(
                    getResources().getColor(R.color.icon_tint), PorterDuff.Mode.MULTIPLY);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.event_dialog_repeating_event_conformation_title)
                    .setMessage(String.format(positiveButtonText, event.getTitle(), eventRepetions))
                    .setPositiveButton(R.string.event_dialog_save_all_instances, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            saveSettingsForAllEvents();
                        }
                    })
                    .setNegativeButton(R.string.event_dialog_save_instance, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            saveSettingsForEventInstance();
                        }
                    })
                    .setIcon(icon);
            AlertDialog dialog = builder.show();
        } else {
            saveSettingsForEventInstance();
        }
    }

    private void drawExtendMinutesWidgets() {
        final LinearLayout extendMinutesBox = (LinearLayout) view.findViewById(R.id.event_extend_box);
        if (event.getExtendMinutes() == 0) {
            extendMinutesBox.setVisibility(View.GONE);
        } else {
            extendMinutesBox.setVisibility(View.VISIBLE);
            TextView extendMinutesText = (TextView) view.findViewById(R.id.event_extend_text);
            extendMinutesText.setText("+" + event.getExtendMinutes() + " minutes");
            Button extendMinutesResetButton = (Button) view.findViewById(R.id.event_extend_button);
            extendMinutesResetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(context, ExtendEventService.class);
                    i.putExtra(ExtendEventService.INSTANCE_ID, event.getId());
                    i.putExtra(ExtendEventService.NEW_EXTEND_LENGTH, 0);
                    context.startService(i);
                    extendMinutesBox.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    private void colorizeIcons() {
        HashMap<ImageView, RingerType> buttons = new HashMap<ImageView, RingerType>();
        buttons.put((ImageView) view.findViewById(R.id.imageButtonNormal), RingerType.NORMAL);
        buttons.put((ImageView) view.findViewById(R.id.imageButtonVibrate), RingerType.VIBRATE);
        buttons.put((ImageView) view.findViewById(R.id.imageButtonSilent), RingerType.SILENT);
        buttons.put((ImageView) view.findViewById(R.id.imageButtonIgnore), RingerType.IGNORE);

        for (ImageView thisButton : buttons.keySet()) {
            thisButton.setImageDrawable(getColorizedIcon(buttons.get(thisButton)));
            thisButton.invalidate();
        }


    }

    private void setRingerType(RingerType type) {
        positiveButton.setEnabled(true);
        event.setRingerType(type);
        colorizeIcons();
    }

    private void resetAllEvents() {
        prefs.unsetRingerTypeForEventSeries(event.getEventId());
        databaseInterface.setRingerForAllInstancesOfEvent(event.getEventId(),
                RingerType.UNDEFINED);
        dataSetManager.broadcastDataSetChangedMessage();
    }

    private void resetEventInstance() {
        databaseInterface.setRingerForInstance(event.getId(), RingerType.UNDEFINED);
        dataSetManager.broadcastDataSetChangedForId(event.getId());
    }

    private void saveSettingsForAllEvents() {
        if (authenticator.isAuthenticated()) {
            prefs.unsetRingerTypeForEventSeries(event.getEventId());
            databaseInterface.setRingerForAllInstancesOfEvent(event.getEventId(),
                    RingerType.UNDEFINED);

            prefs.setRingerForEventSeries(event.getEventId(), event.getRingerType());
            dataSetManager.broadcastDataSetChangedMessage();
        } else {
            authenticator.showUpgradeDialog();
        }
    }

    private void saveSettingsForEventInstance() {
        databaseInterface.setRingerForInstance(event.getId(), event.getRingerType());
        dataSetManager.broadcastDataSetChangedForId(event.getId());
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

        int color = getResources().getColor(R.color.icon_tint);
        if (event.getRingerType() == ringerType) {
            color = getResources().getColor(R.color.accent);
        }

        colorizedIcon.mutate().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        return colorizedIcon;
    }

    @Override
    public void onResume() {
        super.onResume();
        AlertDialog thisDialog = (AlertDialog) getDialog();
        positiveButton = thisDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setEnabled(event.getRingerType() == RingerType.UNDEFINED ? false : true);
        }
    }
}
