/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

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
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.R.id;
import org.ciasaboark.tacere.database.SimpleCalendarEvent;
import org.ciasaboark.tacere.manager.VolumesManager;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;


public class SettingsActivity extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "Settings";
    private final Prefs prefs = new Prefs(this);
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        context = this;
        // Show the Up button in the action bar.
        setupActionBar();
        RelativeLayout serviceToggleBox = (RelativeLayout) findViewById(id.settings_serviceBox);
        serviceToggleBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickToggleService(view);
            }
        });
        drawAllWidgets();
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        try {
            getActionBar().setIcon(R.drawable.action_settings);
        } catch (NullPointerException e) {
            Log.e(TAG, "unable to setup action bar");
        }
    }

    private void drawAllWidgets() {
        drawServiceWidget();
        drawCalendarWidgets();
        drawRingerWidgets();
        drawDoNotDisturbWidgets();
        drawMediaWidgets();
        drawAlarmWidgets();

    }

    private void drawServiceWidget() {
        //the service state toggle
        Switch serviceActivatedSwitch = (Switch) findViewById(id.activateServiceSwitch);
        if (prefs.getIsServiceActivated()) {
            serviceActivatedSwitch.setChecked(true);
        } else {
            serviceActivatedSwitch.setChecked(false);
        }
    }

    private void drawCalendarWidgets() {
        ImageView calendarIcon = (ImageView) findViewById(id.calendar_icon);
        TextView calendarTV = (TextView) findViewById(id.calendar_text);
        RelativeLayout calendarBox = (RelativeLayout) findViewById(id.select_calendar_box);

        Drawable d = getResources().getDrawable(R.drawable.calendar_icon);
        d.setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.MULTIPLY);

        calendarBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(context, SelectCalendarsActivity.class);
                startActivity(i);
            }
        });

        if (prefs.getIsServiceActivated()) {
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
        //the ringer type description
        TextView ringerDescriptionTV = (TextView) findViewById(id.ringerTypeDescription);
        TextView ringerTV = (TextView) findViewById(id.settings_ringerTitle);

        Drawable icon;
        switch (prefs.getRingerType()) {
            case SimpleCalendarEvent.RINGER.NORMAL:
                ringerDescriptionTV.setText(R.string.pref_ringer_type_normal);
                icon = getResources().getDrawable(R.drawable.ic_state_normal);
                break;
            case SimpleCalendarEvent.RINGER.VIBRATE:
                ringerDescriptionTV.setText(R.string.pref_ringer_type_vibrate);
                icon = getResources().getDrawable(R.drawable.ic_state_vibrate);
                break;
            case SimpleCalendarEvent.RINGER.SILENT:
                ringerDescriptionTV.setText(R.string.pref_ringer_type_silent);
                icon = getResources().getDrawable(R.drawable.ic_state_silent);
                break;
            default:
                throw new IllegalStateException("Saved default ringer is of unknown type: "
                        + prefs.getRingerType());
        }


        if (prefs.getIsServiceActivated()) {
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

        ImageButton ringerIcon = (ImageButton) findViewById(id.settings_ringerIcon);
        setImageButtonIcon(ringerIcon, icon);
        findViewById(id.settings_ringerBox).setEnabled(prefs.getIsServiceActivated());
    }

    private void drawDoNotDisturbWidgets() {
        Switch doNotDisturbSwitch = (Switch) findViewById(id.doNotDisturbSwitch);
        boolean isDoNotDisturbEnabled = prefs.getDoNotDisturb();
        doNotDisturbSwitch.setChecked(isDoNotDisturbEnabled);
        doNotDisturbSwitch.setEnabled(prefs.getIsServiceActivated());
        findViewById(id.do_not_disturb_box).setEnabled(prefs.getIsServiceActivated());

        TextView doNotDisturbHeader = (TextView) findViewById(id.do_not_disturb_header);
        if (prefs.getIsServiceActivated()) {
            doNotDisturbHeader.setTextColor(getResources().getColor(R.color.textcolor));
        } else {
            doNotDisturbHeader.setTextColor(getResources().getColor(R.color.textColorDisabled));
        }

        int apiLevelAvailable = Build.VERSION.SDK_INT;
        RelativeLayout layout = (RelativeLayout) findViewById(id.do_not_disturb_box);
        if (apiLevelAvailable >= 20) { //TODO this should be 21
            layout.setVisibility(View.VISIBLE);
        } else {
            layout.setVisibility(View.GONE);
        }


    }

    public void onClickDoNotDisturb(View v) {
        boolean doNotDisturbEnabled = prefs.getDoNotDisturb();
        prefs.setDoNotDisturb(!doNotDisturbEnabled);
        drawDoNotDisturbWidgets();
    }

    private void drawMediaWidgets() {
        TextView mediaTV = (TextView) findViewById(id.settings_mediaText);
        if (prefs.getIsServiceActivated()) {
            mediaTV.setTextColor(getResources().getColor(R.color.textcolor));
        } else {
            mediaTV.setTextColor(getResources().getColor(R.color.textColorDisabled));
        }

        //the media volumes toggle
        Switch mediaSwitch = (Switch) findViewById(id.adjustMediaCheckBox);
        SeekBar mediaSB = (SeekBar) findViewById(id.mediaSeekBar);
        if (prefs.getAdjustMedia()) {
            mediaSwitch.setChecked(true);
        } else {
            mediaSwitch.setChecked(false);
        }
        mediaSwitch.setEnabled(prefs.getIsServiceActivated());
        findViewById(id.settings_mediaBox).setEnabled(prefs.getIsServiceActivated());


        //the media volumes slider
        mediaSB.setMax(VolumesManager.getMaxMediaVolume());
        mediaSB.setProgress(prefs.getCurMediaVolume());
        if (prefs.getAdjustMedia() && prefs.getIsServiceActivated()) {
            this.animateRevealView(mediaSB);
            mediaSB.setEnabled(true);
        } else {
            mediaSB.setEnabled(false);
            this.animateHideView(mediaSB);
        }
        mediaSB.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.setCurMediaVolume(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //required stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //required stub
            }
        });
    }

    private void drawAlarmWidgets() {
        TextView alarmTV = (TextView) findViewById(id.settings_alarmText);
        if (prefs.getIsServiceActivated()) {
            alarmTV.setTextColor(getResources().getColor(R.color.textcolor));
        } else {
            alarmTV.setTextColor(getResources().getColor(R.color.textColorDisabled));
        }

        //the alarm volumes toggle
        Switch alarmSwitch = (Switch) findViewById(id.adjustAlarmCheckBox);
        alarmSwitch.setChecked(prefs.getAdjustAlarm());
        alarmSwitch.setEnabled(prefs.getIsServiceActivated());
        findViewById(id.settings_alarmBox).setEnabled(prefs.getIsServiceActivated());

        //the alarm volumes slider
        SeekBar alarmSB = (SeekBar) findViewById(id.alarmSeekBar);
        alarmSB.setMax(VolumesManager.getMaxAlarmVolume());
        alarmSB.setProgress(prefs.getCurAlarmVolume());
        if (prefs.getAdjustAlarm() && prefs.getIsServiceActivated()) {
            this.animateRevealView(alarmSB);
            alarmSB.setEnabled(true);
        } else {
            alarmSB.setEnabled(false);
            this.animateHideView(alarmSB);
        }
        alarmSB.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.setCurAlarmVolume(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //required stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //required stub
            }
        });
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

    @TargetApi(21)
    private void animateRevealView(View v) {
        v.setVisibility(View.VISIBLE);
    }

    @TargetApi(21)
    private void animateHideView(final View v) {
        v.setVisibility(View.GONE);
    }

    public void onClickAdjustAlarm(View v) {
        prefs.setAdjustAlarm(!prefs.getAdjustAlarm());
        drawAlarmWidgets();
    }

    public void onClickToggleService(View v) {
        prefs.setIsServiceActivated(!prefs.getIsServiceActivated());
        restartEventSilencerService();
        drawServiceWidget();
        drawCalendarWidgets();
        drawMediaWidgets();
        drawAlarmWidgets();
        drawRingerWidgets();
        drawDoNotDisturbWidgets();
    }

    public void onClickAdjustMedia(View v) {
        prefs.setAdjustMedia(!prefs.getAdjustMedia());
        drawMediaWidgets();
    }

    private void restartEventSilencerService() {
        Intent i = new Intent(this, EventSilencerService.class);
        i.putExtra("type", RequestTypes.ACTIVITY_RESTART);
        startService(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings_restore:
                //restore settings to default values then navigate to the main activity
                restoreDefaults();
                //navigate back to the main screen
                Toast.makeText(getApplicationContext(), R.string.settings_restored, Toast.LENGTH_SHORT).show();
                return true;
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void restoreDefaults() {
        prefs.restoreDefaultPreferences();
        drawAllWidgets();
    }

    public void onPause() {
        super.onPause();
    }

    public void onClickSelectCalendars(View v) {

    }

    public void onClickRingerType(View v) {
        //TODO this is a fragile connection to the ringer types, should be replaced with an enum
        String[] ringerTypes = {
                "Normal",
                "Vibrate",
                "Silent",
        };

        final AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle(R.string.pref_ringer_type_title)
                .setSingleChoiceItems(ringerTypes, prefs.getRingerType() - 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.setRingerType(which + 1);
                        drawRingerWidgets();
                        restartEventSilencerService();
                        dialog.dismiss();
                    }
                })
                .create();

        alert.show();
    }

    public void onClickAdvancedSettings(View v) {
        Intent i = new Intent(this, AdvancedSettingsActivity.class);
        startActivity(i);
    }
}
