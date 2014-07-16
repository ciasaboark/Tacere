/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.R.id;
import org.ciasaboark.tacere.manager.VolumesManager;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;


public class SettingsActivity extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "Settings";
    private final Context context = this;
    private final Prefs prefs = new Prefs(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
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
        drawRingerWidgets();
        drawDoNotDisturbWidgets();
        drawMediaWidgets();
        drawAlarmWidgets();
        drawQuickSilenceWidget();
    }

    private void drawServiceWidget() {
        //the service state toggle
        Switch serviceActivatedSwitch = (Switch) findViewById(id.activateServiceSwitch);
        TextView serviceTV = (TextView) findViewById(id.activateServiceDescription);
        if (prefs.getIsServiceActivated()) {
            serviceActivatedSwitch.setChecked(true);
            serviceTV.setText(R.string.pref_service_enabled);
        } else {
            serviceActivatedSwitch.setChecked(false);
            serviceTV.setText(R.string.pref_service_disabled);
        }
    }

    private void drawRingerWidgets() {
        //the ringer type description
        TextView ringerDescriptionTV = (TextView) findViewById(id.ringerTypeDescription);
        TextView ringerTV = (TextView) findViewById(id.settings_ringerTitle);

        Drawable icon;
        switch (prefs.getRingerType()) {
            case CalEvent.RINGER.NORMAL:
                ringerDescriptionTV.setText(R.string.pref_ringer_type_normal);
                icon = getResources().getDrawable(R.drawable.ic_state_normal);
                break;
            case CalEvent.RINGER.VIBRATE:
                ringerDescriptionTV.setText(R.string.pref_ringer_type_vibrate);
                icon = getResources().getDrawable(R.drawable.ic_state_vibrate);
                break;
            case CalEvent.RINGER.SILENT:
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

    private void drawQuickSilenceWidget() {
        //the quick silence button
        TextView quickTV = (TextView) findViewById(id.quickSilenceDescription);
        String quicksilenceText = getResources().getString(R.string.pref_quicksilent_duration);
        String hrs = "";
        if (prefs.getQuickSilenceHours() > 0) {
            hrs = String.valueOf(prefs.getQuickSilenceHours()) + " " + getString(R.string.hours_lower) + " ";
        }
        quickTV.setText(String.format(quicksilenceText, hrs, prefs.getQuicksilenceMinutes()));
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
        int apiLevelAvailable = android.os.Build.VERSION.SDK_INT;
        if (apiLevelAvailable >= 20) {  //TODO this should really be API 21
            // previously invisible view
            v.setVisibility(View.VISIBLE);

            // get the center for the clipping circle
            int cx = (v.getLeft() + v.getRight()) / 2;
            int cy = (v.getTop() + v.getBottom()) / 2;

            // get the final radius for the clipping circle
            int finalRadius = v.getWidth();

            // create and start the animator for this view
            // (the start radius is zero)
            android.animation.ValueAnimator anim =
                    android.view.ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, finalRadius);
            anim.start();
        }
    }

    @TargetApi(21)
    private void animateHideView(final View v) {
        int apiLevelAvailable = android.os.Build.VERSION.SDK_INT;
        if (apiLevelAvailable >= 20) {  //TODO this should really be API 21
            // get the center for the clipping circle
            int cx = (v.getLeft() + v.getRight()) / 2;
            int cy = (v.getTop() + v.getBottom()) / 2;

            // get the initial radius for the clipping circle
            int initialRadius = v.getWidth();

            // create the animation (the final radius is zero)
            ValueAnimator anim = ViewAnimationUtils.createCircularReveal(v, cx, cy, initialRadius, 0);

            // make the view invisible when the animation is done
            anim.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    super.onAnimationEnd(animation);
                    v.setVisibility(View.GONE);
                }
            });

            // start the animation
            anim.start();
        }
    }

    public void onClickAdjustAlarm(View v) {
        prefs.setAdjustAlarm(!prefs.getAdjustAlarm());
        drawAlarmWidgets();
    }

    public void onClickToggleService(View v) {
        prefs.setIsServiceActivated(!prefs.getIsServiceActivated());
        restartEventSilencerService();
        drawServiceWidget();
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

    public void onClickRingerType(View v) {
        //TODO this is a fragile connection to the ringer types, should be replaced with an enum
        String[] ringerTypes = {
                "Normal",
                "Vibrate",
                "Silent",
        };

        AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle(R.string.pref_ringer_type)
                .setSingleChoiceItems(ringerTypes, prefs.getRingerType() - 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.setRingerType(which + 1);
                        drawRingerWidgets();
                        restartEventSilencerService();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                })
                .create();

        alert.show();
    }


    public void onClickQuickSilence(View v) {
        LayoutInflater inflator = LayoutInflater.from(this);
        @SuppressLint("Java")
        View view = inflator.inflate(R.layout.dialog_quicksilent, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.notification_quicksilence_title);
        builder.setView(view);

        final NumberPicker hourP = (NumberPicker) view.findViewById(R.id.hourPicker);
        final NumberPicker minP = (NumberPicker) view.findViewById(R.id.minutePicker);

        String[] hours = new String[25];
        String[] minutes = new String[60];

        for (int i = 0; i < hours.length; i++) {
            hours[i] = Integer.toString(i);
        }

        for (int i = 1; i < minutes.length - 1; i++) {
            StringBuilder sb = new StringBuilder(Integer.toString(i));
//            if (i < 10) {
//                sb.insert(0, "0");
//            }
            minutes[i] = sb.toString();
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
        minP.setValue(prefs.getQuicksilenceMinutes() + 1);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                prefs.setQuickSilenceHours(hourP.getValue() - 1);
                prefs.setQuicksilenceMinutes(minP.getValue() - 1);
                drawQuickSilenceWidget();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //do nothing
            }
        });

        builder.show();
    }

    public void onClickAdvancedSettings(View v) {
        Intent i = new Intent(this, AdvancedSettingsActivity.class);
        startActivity(i);
    }
}
