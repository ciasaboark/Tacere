/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.AdvancedSettingsActivity;
import org.ciasaboark.tacere.billing.Authenticator;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.prefs.Prefs;

import java.util.ArrayList;

public class MainSettingsFragment extends android.support.v4.app.Fragment {
    public static final String SHOW_ADVANCED_SETTINGS_LINK = "showAdvancedSettings";
    public static final String TAG = "MainSettingsFragment";
    private static final int layout = R.layout.fragment_main_settings;
    private Prefs prefs;
    private Context context;
    private View rootView;
    private boolean showAdvancedSettingsLink = true;
    private OnSelectCalendarsListener mListener;

    public MainSettingsFragment() {
        // Required empty public constructor
    }

    public static MainSettingsFragment newInstance(boolean showAdvancedSettingsLink) {
        MainSettingsFragment fragment = new MainSettingsFragment();
        Bundle args = new Bundle();
        args.putBoolean(SHOW_ADVANCED_SETTINGS_LINK, showAdvancedSettingsLink);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSelectCalendarsListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + OnSelectCalendarsListener.class + " to embed this fragment");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        if (context == null) {
            throw new IllegalStateException("Fragment " + TAG + " was not able to find its context");
        }

        if (getArguments() != null) {
            Bundle args = getArguments();
            showAdvancedSettingsLink = args.getBoolean(SHOW_ADVANCED_SETTINGS_LINK, true);
        }

        rootView = inflater.inflate(layout, container, false);
        prefs = new Prefs(getActivity());
        drawAllWidgets();

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void drawAllWidgets() {
        drawCalendarWidgets();
        drawRingerWidgets();
        drawMediaWidgets();
        drawAlarmWidgets();
        drawAdvancedSettingsWidget();
    }

    private void drawAdvancedSettingsWidget() {
        LinearLayout advancedSettingsContainer = (LinearLayout) rootView.findViewById(R.id.settings_advanced_settings_container);
        if (showAdvancedSettingsLink) {
            advancedSettingsContainer.setVisibility(View.VISIBLE);
            LinearLayout advancedSettingsBox = (LinearLayout) rootView.findViewById(R.id.settings_advanced_settings_box);
            advancedSettingsBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(context, AdvancedSettingsActivity.class));
                }
            });
        } else {
            advancedSettingsContainer.setVisibility(View.GONE);
        }
    }

    private void drawCalendarWidgets() {
        ImageView calendarIcon = (ImageView) rootView.findViewById(R.id.calendar_icon);
        TextView calendarTV = (TextView) rootView.findViewById(R.id.calendar_text);
        RelativeLayout calendarBox = (RelativeLayout) rootView.findViewById(R.id.select_calendar_box);

        calendarBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Authenticator authenticator = new Authenticator(context);
                if (authenticator.isAuthenticated()) {
                    mListener.onSelectCalendarsSelectedListener();
                } else {
                    authenticator.showUpgradeDialog();
                }

            }
        });
        calendarBox.setClickable(true);

    }

    private void drawRingerWidgets() {
        RelativeLayout ringerBox = (RelativeLayout) rootView.findViewById(R.id.settings_ringerBox);
        ringerBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] ringerTypes = RingerType.names();
                //we don't want ringer type UNDEFINED to be an option
                ArrayList<String> ringerList = new ArrayList<String>();
                for (String type : ringerTypes) {
                    if (!type.equalsIgnoreCase(RingerType.UNDEFINED.toString())) {
                        type = type.charAt(0) + type.substring(1).toLowerCase();
                        ringerList.add(type);
                    }
                }
                final String[] filteredRingerTypes = ringerList.toArray(new String[]{});
                int previouslySelectedRinger = ringerList.indexOf(prefs.getRingerType().toString());


                final AlertDialog alert = new AlertDialog.Builder(context)
                        .setTitle(R.string.settings_section_general_ringer)
                        .setSingleChoiceItems(filteredRingerTypes, previouslySelectedRinger,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String selectedRingerString = filteredRingerTypes[which];
                                        int selectedRingerValue = RingerType.getIntForStringValue(selectedRingerString);
                                        RingerType selectedRinger = RingerType.getTypeForInt(selectedRingerValue);
                                        prefs.setRingerType(selectedRinger);
                                        drawRingerWidgets();
                                        dialog.dismiss();
                                    }
                                })
                        .create();

                alert.show();
            }
        });

        //the ringer type description
        TextView ringerDescriptionTV = (TextView) rootView.findViewById(R.id.ringerTypeDescription);
        TextView ringerTV = (TextView) rootView.findViewById(R.id.settings_ringerTitle);

        Drawable icon;
        switch (prefs.getRingerType()) {
            case NORMAL:
                ringerDescriptionTV.setText(R.string.settings_section_general_ringer_normal);
                icon = getResources().getDrawable(R.drawable.ic_state_normal);
                break;
            case VIBRATE:
                ringerDescriptionTV.setText(R.string.settings_section_general_ringer_vibrate);
                icon = getResources().getDrawable(R.drawable.ic_state_vibrate);
                break;
            case SILENT:
                ringerDescriptionTV.setText(R.string.settings_section_general_ringer_silent);
                icon = getResources().getDrawable(R.drawable.ic_state_silent);
                break;
            case IGNORE:
                ringerDescriptionTV.setText(R.string.settings_section_general_ringer_ignore);
                icon = getResources().getDrawable(R.drawable.ic_state_ignore);
                break;
            default:
                throw new IllegalStateException("Saved default ringer is of unknown type: "
                        + prefs.getRingerType());
        }


        int iconColor = getResources().getColor(R.color.icon_tint);
        icon.mutate().setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
        ringerDescriptionTV.setTextColor(getResources().getColor(R.color.text_color));
        ringerTV.setTextColor(getResources().getColor(R.color.text_color));

        ImageView ringerIcon = (ImageView) rootView.findViewById(R.id.settings_ringerIcon);
        ringerIcon.setImageDrawable(icon);
    }

    private void drawMediaWidgets() {
        RelativeLayout mediaBox = (RelativeLayout) rootView.findViewById(R.id.settings_mediaBox);
        mediaBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.setMediaVolumeShouldSilence(!prefs.shouldMediaVolumeBeSilenced());
                drawMediaWidgets();
            }
        });

        //the media volumes toggle
        SwitchCompat mediaSwitch = (SwitchCompat) rootView.findViewById(R.id.adjustMediaCheckBox);
        if (prefs.shouldMediaVolumeBeSilenced()) {
            mediaSwitch.setChecked(true);
        } else {
            mediaSwitch.setChecked(false);
        }
    }

    private void drawAlarmWidgets() {
        RelativeLayout alarmBox = (RelativeLayout) rootView.findViewById(R.id.settings_alarmBox);
        alarmBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.setAlarmVolumeShouldSilence(!prefs.shouldAlarmVolumeBeSilenced());
                drawAlarmWidgets();
            }
        });

        //the alarm volumes toggle
        SwitchCompat alarmSwitch = (SwitchCompat) rootView.findViewById(R.id.adjustAlarmCheckBox);
        alarmSwitch.setChecked(prefs.shouldAlarmVolumeBeSilenced());
    }


    public void onClickAdvancedSettings(View v) {
        Intent i = new Intent(context, AdvancedSettingsActivity.class);
        startActivity(i);
    }


    public interface OnSelectCalendarsListener {
        // TODO: Update argument type and name
        public void onSelectCalendarsSelectedListener();
    }
}
