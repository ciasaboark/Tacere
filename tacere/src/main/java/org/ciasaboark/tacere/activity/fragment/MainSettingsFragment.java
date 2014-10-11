/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.AdvancedSettingsActivity;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.prefs.Prefs;

import java.util.ArrayList;

public class MainSettingsFragment extends android.support.v4.app.Fragment {
    public static final String TAG = "MainSettingsFragment";
    private static final int layout = R.layout.fragment_main_settings;
    private Prefs prefs;
    private Context context;
    private View rootView;
    private boolean showAdvancedSettingsLink = true;
    private OnSelectCalendarsListener mListener;

    public MainSettingsFragment(boolean showAdvancedSettingsLink) {
        this.showAdvancedSettingsLink = showAdvancedSettingsLink;
    }

    public MainSettingsFragment() {
        // Required empty public constructor
    }

    public static MainSettingsFragment newInstance() {
        MainSettingsFragment fragment = new MainSettingsFragment();
        Bundle args = new Bundle();
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
        drawDoNotDisturbWidgets();
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

        Drawable d = getResources().getDrawable(R.drawable.calendar_calendar);
        d.setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.MULTIPLY);

        calendarBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onSelectCalendarsSelectedListener();

            }
        });

        if (prefs.isServiceActivated()) {
            int iconColor = getResources().getColor(R.color.primary);
            d.mutate().setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
            calendarTV.setTextColor(getResources().getColor(R.color.textcolor));
            calendarBox.setClickable(true);
        } else {
            int iconColor = getResources().getColor(android.R.color.darker_gray);
            d.mutate().setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
            calendarTV.setTextColor(getResources().getColor(R.color.textColorDisabled));
            calendarBox.setClickable(false);
        }
        calendarIcon.setBackgroundDrawable(d);

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


        if (prefs.isServiceActivated()) {
            int iconColor = getResources().getColor(R.color.primary);
            icon.mutate().setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
            ringerDescriptionTV.setTextColor(getResources().getColor(R.color.textcolor));
            ringerTV.setTextColor(getResources().getColor(R.color.textcolor));
        } else {
            int iconColor = getResources().getColor(android.R.color.darker_gray);
            icon.mutate().setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
            ringerDescriptionTV.setTextColor(getResources().getColor(R.color.textColorDisabled));
            ringerTV.setTextColor(getResources().getColor(R.color.textColorDisabled));
        }

        ImageButton ringerIcon = (ImageButton) rootView.findViewById(R.id.settings_ringerIcon);
        setImageButtonIcon(ringerIcon, icon);
        rootView.findViewById(R.id.settings_ringerBox).setEnabled(prefs.isServiceActivated());
    }

    private void drawDoNotDisturbWidgets() {
        RelativeLayout doNotDisturbBox = (RelativeLayout) rootView.findViewById(R.id.do_not_disturb_box);
        doNotDisturbBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean doNotDisturbEnabled = prefs.getDoNotDisturb();
                prefs.setDoNotDisturb(!doNotDisturbEnabled);
                drawDoNotDisturbWidgets();
            }
        });

        Switch doNotDisturbSwitch = (Switch) rootView.findViewById(R.id.doNotDisturbSwitch);
        boolean isDoNotDisturbEnabled = prefs.getDoNotDisturb();
        doNotDisturbSwitch.setChecked(isDoNotDisturbEnabled);
        doNotDisturbSwitch.setEnabled(prefs.isServiceActivated());
        rootView.findViewById(R.id.do_not_disturb_box).setEnabled(prefs.isServiceActivated());

        TextView doNotDisturbHeader = (TextView) rootView.findViewById(R.id.do_not_disturb_header);
        if (prefs.isServiceActivated()) {
            doNotDisturbHeader.setTextColor(getResources().getColor(R.color.textcolor));
        } else {
            doNotDisturbHeader.setTextColor(getResources().getColor(R.color.textColorDisabled));
        }

        int apiLevelAvailable = Build.VERSION.SDK_INT;
        RelativeLayout layout = (RelativeLayout) rootView.findViewById(R.id.do_not_disturb_box);
        if (apiLevelAvailable >= 20) { //TODO this should be 21
            layout.setVisibility(View.VISIBLE);
        } else {
            layout.setVisibility(View.GONE);
        }


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

        TextView mediaTV = (TextView) rootView.findViewById(R.id.settings_mediaText);
        if (prefs.isServiceActivated()) {
            mediaTV.setTextColor(getResources().getColor(R.color.textcolor));
        } else {
            mediaTV.setTextColor(getResources().getColor(R.color.textColorDisabled));
        }

        //the media volumes toggle
        Switch mediaSwitch = (Switch) rootView.findViewById(R.id.adjustMediaCheckBox);
        if (prefs.shouldMediaVolumeBeSilenced()) {
            mediaSwitch.setChecked(true);
        } else {
            mediaSwitch.setChecked(false);
        }
        mediaSwitch.setEnabled(prefs.isServiceActivated());
        rootView.findViewById(R.id.settings_mediaBox).setEnabled(prefs.isServiceActivated());
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

        TextView alarmTV = (TextView) rootView.findViewById(R.id.settings_alarmText);
        if (prefs.isServiceActivated()) {
            alarmTV.setTextColor(getResources().getColor(R.color.textcolor));
        } else {
            alarmTV.setTextColor(getResources().getColor(R.color.textColorDisabled));
        }

        //the alarm volumes toggle
        Switch alarmSwitch = (Switch) rootView.findViewById(R.id.adjustAlarmCheckBox);
        alarmSwitch.setChecked(prefs.shouldAlarmVolumeBeSilenced());
        alarmSwitch.setEnabled(prefs.isServiceActivated());
        rootView.findViewById(R.id.settings_alarmBox).setEnabled(prefs.isServiceActivated());
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("deprecation")
    private void setImageButtonIcon(ImageButton button, Drawable icon) {
        if (Build.VERSION.SDK_INT >= 16) {
            button.setBackground(icon);
        } else {
            button.setBackgroundDrawable(icon);
        }
    }

    public void onClickAdvancedSettings(View v) {
        Intent i = new Intent(context, AdvancedSettingsActivity.class);
        startActivity(i);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnSelectCalendarsListener {
        // TODO: Update argument type and name
        public void onSelectCalendarsSelectedListener();
    }
}
