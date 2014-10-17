/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.event.ringer.Intervals;
import org.ciasaboark.tacere.prefs.Prefs;

public class AdvancedSettingsFragment extends android.support.v4.app.Fragment {
    public static final String TAG = "AdvancedSettingsFragment";
    private View rootView;
    private Context context;
    private Prefs prefs;

    public AdvancedSettingsFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_advanced_settings, container, false);
        context = getActivity();
        if (context == null) {
            throw new IllegalStateException("Fragment " + TAG + " can not find its activity");
        }
        prefs = new Prefs(context);
        drawAllWidgets();

        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void drawAllWidgets() {
        drawFreeTimeWidgets();
        drawSilenceAllDayWidgets();
        drawEventBufferWidgets();
        drawLookaheadWidgets();
        drawQuickSilenceWidget();
    }

    private void drawFreeTimeWidgets() {
        RelativeLayout freeTimeBox = (RelativeLayout) rootView.findViewById(R.id.advanced_settings_freetime_box);
        freeTimeBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.setSilenceFreeTimeEvents(!prefs.shouldAvailableEventsSilence());
                drawFreeTimeWidgets();
            }
        });

        // the silence free time state toggle
        CheckBox freeCB = (CheckBox) rootView.findViewById(R.id.silenceFreeTimeCheckBox);
        TextView freeTV = (TextView) rootView.findViewById(R.id.silenceFreeTimeDescription);
        if (prefs.shouldAvailableEventsSilence()) {
            freeCB.setChecked(true);
            freeTV.setText(R.string.advanced_settings_section_events_available_enabled);
        } else {
            freeCB.setChecked(false);
            freeTV.setText(R.string.advanced_settings_section_events_available_disabled);
        }
    }

    private void drawSilenceAllDayWidgets() {
        RelativeLayout allDayBox = (RelativeLayout) rootView.findViewById(R.id.advanced_settings_allday_box);
        allDayBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.setSilenceAllDayEvents(!prefs.shouldAllDayEventsSilence());
                drawSilenceAllDayWidgets();
            }
        });

        // the silence all day state toggle
        CheckBox dayCB = (CheckBox) rootView.findViewById(R.id.silenceAllDayCheckBox);
        TextView dayTV = (TextView) rootView.findViewById(R.id.silenceAllDayDescription);
        if (prefs.shouldAllDayEventsSilence()) {
            dayCB.setChecked(true);
            dayTV.setText(R.string.advanced_settings_section_events_all_day_enabled);
        } else {
            dayCB.setChecked(false);
            dayTV.setText(R.string.advanced_settings_section_events_all_day_disabled);
        }
    }

    private void drawEventBufferWidgets() {
        LinearLayout bufferBox = (LinearLayout) rootView.findViewById(R.id.advanced_settings_buffer_box);
        bufferBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Buffer Minutes");
                final NumberPicker number = new NumberPicker(context);
                String[] nums = new String[32];

                for (int i = 0; i < nums.length; i++) {
                    nums[i] = Integer.toString(i);
                }

                number.setMinValue(1);
                number.setMaxValue(nums.length - 1);
                number.setWrapSelectorWheel(false);
                number.setDisplayedValues(nums);
                number.setValue(prefs.getBufferMinutes() + 1);

                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        prefs.setBufferMinutes(number.getValue() - 1);
                        drawEventBufferWidgets();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing
                    }
                });

                builder.setView(number);

                AlertDialog dialog = builder.show();

                Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    positiveButton.setTextColor(getResources().getColor(R.color.accent));
                }

                Button negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE);
                if (negativeButton != null) {
                    negativeButton.setTextColor(getResources().getColor(R.color.textColorDisabled));
                }
            }
        });

        // the event buffer button
        TextView bufferTV = (TextView) rootView.findViewById(R.id.bufferMinutesDescription);
        String bufferText = getResources().getString(R.string.advanced_settings_section_intervals_buffer_duration);
        bufferTV.setText(String.format(bufferText, prefs.getBufferMinutes()));
    }

    private void drawLookaheadWidgets() {
        LinearLayout lookaheadBox = (LinearLayout) rootView.findViewById(R.id.advanced_settings_lookahead_box);
        lookaheadBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] intervals = Intervals.names();
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.advanced_settings_section_intervals_lookahead);

                builder.setSingleChoiceItems(intervals, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String intervalString = intervals[i];
                        int intervalValue = Intervals.getIntForStringValue(intervalString);
                        Intervals interval = Intervals.getTypeForInt(intervalValue);

                        prefs.setLookaheadDays(interval);
                        drawLookaheadWidgets();
                    }
                });

                builder.setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //do nothing
                    }
                });

                AlertDialog dialog = builder.show();

                Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    positiveButton.setTextColor(getResources().getColor(R.color.accent));
                }

                Button negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE);
                if (negativeButton != null) {
                    negativeButton.setTextColor(getResources().getColor(R.color.textColorDisabled));
                }
            }
        });

        // the lookahead interval button
        TextView lookaheadTV = (TextView) rootView.findViewById(R.id.lookaheadDaysDescription);
        String lookaheadText = getResources().getString(R.string.advanced_settings_section_intervals_lookahead_duration);
        lookaheadTV.setText(String.format(lookaheadText, prefs.getLookaheadDays().string.toLowerCase()));
    }

    private void drawQuickSilenceWidget() {
        LinearLayout quicksilenceBox = (LinearLayout) rootView.findViewById(R.id.advanced_settings_quicksilence_box);
        quicksilenceBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = LayoutInflater.from(v.getContext());
                View dialogView = inflater.inflate(R.layout.dialog_quicksilent, null);
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.notification_quicksilence_title);
                builder.setView(dialogView);

                final NumberPicker hourP = (NumberPicker) dialogView.findViewById(R.id.hourPicker);
                final NumberPicker minP = (NumberPicker) dialogView.findViewById(R.id.minutePicker);

                String[] hours = new String[25];
                String[] minutes = new String[59];

                for (int i = 0; i < hours.length; i++) {
                    hours[i] = Integer.toString(i);
                }

                int i = 0;
                while (i < minutes.length) {
                    minutes[i] = Integer.toString(++i);
                }


                hourP.setMinValue(1);
                hourP.setMaxValue(hours.length - 1);
                hourP.setWrapSelectorWheel(false);
                hourP.setDisplayedValues(hours);
                hourP.setValue(prefs.getQuickSilenceHours() + 1);

                minP.setMinValue(1);
                minP.setMaxValue(minutes.length - 1);
                minP.setWrapSelectorWheel(false);
                minP.setDisplayedValues(minutes);
                minP.setValue(prefs.getQuicksilenceMinutes());

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        prefs.setQuickSilenceHours(hourP.getValue() - 1);
                        prefs.setQuicksilenceMinutes(minP.getValue());
                        drawQuickSilenceWidget();
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do nothing
                    }
                });

                AlertDialog dialog = builder.show();

                Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    positiveButton.setTextColor(getResources().getColor(R.color.accent));
                }

                Button negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE);
                if (negativeButton != null) {
                    negativeButton.setTextColor(getResources().getColor(R.color.textColorDisabled));
                }
            }
        });

        //the quick silence button
        TextView quickTV = (TextView) rootView.findViewById(R.id.quickSilenceDescription);
        String quicksilenceText = getResources().getString(R.string.advanced_settings_section_interval_quicksilence_duration);
        String hrs = "";
        if (prefs.getQuickSilenceHours() > 0) {
            hrs = String.valueOf(prefs.getQuickSilenceHours()) + " " + getString(R.string.hours_lower) + " ";
        }
        quickTV.setText(String.format(quicksilenceText, hrs, prefs.getQuicksilenceMinutes()));
    }
}
