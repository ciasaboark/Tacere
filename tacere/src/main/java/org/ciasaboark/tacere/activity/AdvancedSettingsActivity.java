/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;

// import android.util.Log;

public class AdvancedSettingsActivity extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "AdvancedSettingsActivity";
    private Prefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_settings);

        prefs = new Prefs(this);
        // Show the Up button in the action bar.
        setupActionBar();

        Drawable upIcon = getResources().getDrawable(R.drawable.action_settings);
        int c = getResources().getColor(R.color.header_text_color);
        upIcon.mutate().setColorFilter(c, PorterDuff.Mode.MULTIPLY);
        getActionBar().setIcon(upIcon);

        drawAllWidgets();
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        try {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setIcon(R.drawable.action_settings);
        } catch (NullPointerException e) {
            Log.e(TAG, "unable to setup action bar");
        }

    }

    private void drawAllWidgets() {
        drawFreeTimeWidgets();
        drawSilenceAllDayWidgets();
        drawEventBufferWidgets();
        drawLookaheadWidgets();
        drawQuickSilenceWidget();
    }

    private void drawFreeTimeWidgets() {
        // the silence free time state toggle
        CheckBox freeCB = (CheckBox) findViewById(R.id.silenceFreeTimeCheckBox);
        TextView freeTV = (TextView) findViewById(R.id.silenceFreeTimeDescription);
        if (prefs.getSilenceFreeTimeEvents()) {
            freeCB.setChecked(true);
            freeTV.setText(R.string.pref_silence_free_enabled);
        } else {
            freeCB.setChecked(false);
            freeTV.setText(R.string.pref_silence_free_disabled);
        }
    }

    private void drawSilenceAllDayWidgets() {
        // the silence all day state toggle
        CheckBox dayCB = (CheckBox) findViewById(R.id.silenceAllDayCheckBox);
        TextView dayTV = (TextView) findViewById(R.id.silenceAllDayDescription);
        if (prefs.getSilenceAllDayEvents()) {
            dayCB.setChecked(true);
            dayTV.setText(R.string.pref_all_day_enabled);
        } else {
            dayCB.setChecked(false);
            dayTV.setText(R.string.pref_all_day_disabled);
        }
    }

    private void drawEventBufferWidgets() {
        // the event buffer button
        TextView bufferTV = (TextView) findViewById(R.id.bufferMinutesDescription);
        String bufferText = getResources().getString(R.string.pref_buffer_minutes);
        bufferTV.setText(String.format(bufferText, prefs.getBufferMinutes()));
    }

    private void drawLookaheadWidgets() {
        // the lookahead interval button
        TextView lookaheadTV = (TextView) findViewById(R.id.lookaheadDaysDescription);
        String lookaheadText = getResources().getString(R.string.pref_list_days);
        lookaheadTV.setText(String.format(lookaheadText, prefs.getLookaheadDays()));
    }

    private void drawQuickSilenceWidget() {
        //the quick silence button
        TextView quickTV = (TextView) findViewById(R.id.quickSilenceDescription);
        String quicksilenceText = getResources().getString(R.string.pref_quicksilent_duration);
        String hrs = "";
        if (prefs.getQuickSilenceHours() > 0) {
            hrs = String.valueOf(prefs.getQuickSilenceHours()) + " " + getString(R.string.hours_lower) + " ";
        }
        quickTV.setText(String.format(quicksilenceText, hrs, prefs.getQuicksilenceMinutes()));
    }

    @Override
    public void onPause() {
        // save all changes to the preferences
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.advanced_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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

//    private void saveSettings() {
//        prefs.setSilenceFreeTimeEvents(silenceFreeTime);
//        prefs.setSilenceAllDayEvents(silenceAllDay);
//        prefs.setBufferMinutes(bufferMinutes);
//        prefs.setLookaheadDays(lookaheadDays);
//    }

    public void onClickSilenceFreeTime(View v) {
        prefs.setSilenceFreeTimeEvents(!prefs.getSilenceFreeTimeEvents());
        drawFreeTimeWidgets();
        restartEventSilencerService();
    }

    private void restartEventSilencerService() {
        Intent i = new Intent(this, EventSilencerService.class);
        i.putExtra("type", RequestTypes.ACTIVITY_RESTART);
        startService(i);
    }

    public void onClickSilenceAllDay(View v) {
        prefs.setSilenceAllDayEvents(!prefs.getSilenceAllDayEvents());
        drawSilenceAllDayWidgets();
        restartEventSilencerService();
    }

    public void onClickQuickSilence(View v) {
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("Java")
        View view = inflater.inflate(R.layout.dialog_quicksilent, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.notification_quicksilence_title);
        builder.setView(view);

        final NumberPicker hourP = (NumberPicker) view.findViewById(R.id.hourPicker);
        final NumberPicker minP = (NumberPicker) view.findViewById(R.id.minutePicker);

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

        builder.show();
    }

    public void onClickBufferMinutes(View v) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Buffer Minutes");
        final NumberPicker number = new NumberPicker(this);
        String[] nums = new String[32];

        for (int i = 0; i < nums.length; i++) {
            nums[i] = Integer.toString(i);
        }

        number.setMinValue(1);
        number.setMaxValue(nums.length - 1);
        number.setWrapSelectorWheel(false);
        number.setDisplayedValues(nums);
        number.setValue(prefs.getBufferMinutes() + 1);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                prefs.setBufferMinutes(number.getValue() - 1);
                drawEventBufferWidgets();
                restartEventSilencerService();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing
            }
        });

        alert.setView(number);
        alert.show();
    }

    public void onClickListDays(View v) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Lookahead Interval");
        final NumberPicker number = new NumberPicker(this);
        String[] nums = new String[365];

        for (int i = 0; i < nums.length; i++) {
            nums[i] = Integer.toString(i + 1);
        }

        number.setMinValue(1);
        number.setMaxValue(nums.length - 1);
        number.setWrapSelectorWheel(false);
        number.setDisplayedValues(nums);
        number.setValue(prefs.getLookaheadDays());

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                prefs.setLookaheadDays(number.getValue());
                drawLookaheadWidgets();
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing
            }
        });

        alert.setView(number);
        alert.show();
    }
}
