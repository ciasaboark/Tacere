/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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


public class EventLongclickActivity extends Activity {
    public static final String INSTANCE_KEY = "instanceId";
    private static final String TAG = "EventLongclickActivity";
    private static Context context;
    private int instanceId;
    private DatabaseInterface databaseInterface;
    private Prefs prefs;
    private SimpleCalendarEvent eventInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_event_longclick);
        context = this;

        Intent intent = getIntent();
        instanceId = intent.getIntExtra(INSTANCE_KEY, -1);
        if (instanceId == -1) {
            Log.e(TAG, "Activity must be started with a valid event instance id attached");
            finish();
        }

        databaseInterface = DatabaseInterface.getInstance(this);
        prefs = new Prefs(this);
        try {
            eventInstance = databaseInterface.getEvent(instanceId);
        } catch (NoSuchEventException e) {
            Log.e(TAG, "unable to find event with id " + instanceId);
            finish();
        }

        TextView eventTitle = (TextView) findViewById(R.id.event_title);
        eventTitle.setText(eventInstance.getTitle());

        TextView calendarTitle = (TextView) findViewById(R.id.calendar_title);
        calendarTitle.setText(databaseInterface.getCalendarNameForId(eventInstance.getCalendarId()));

        colorizeIcons();
        drawIndicators();

        ImageButton buttonNormal = (ImageButton) findViewById(R.id.imageButtonNormal);
        ImageButton buttonVibrate = (ImageButton) findViewById(R.id.imageButtonVibrate);
        ImageButton buttonSilent = (ImageButton) findViewById(R.id.imageButtonSilent);
        ImageButton buttonIgnore = (ImageButton) findViewById(R.id.imageButtonIgnore);
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


        Button closeButton = (Button) findViewById(R.id.button_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button resetEventButton = (Button) findViewById(R.id.button_reset);
        resetEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRingerType(SimpleCalendarEvent.RINGER.UNDEFINED);
            }
        });

        Button saveForAllEventsButton = (Button) findViewById(R.id.button_all_events);
        saveForAllEventsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(context, "Not yet implemented", 10).show();
            }
        });

    }

    private void setRingerType(int type) {
        databaseInterface.setRingerType(eventInstance.getEventId(), type);
        try {
            eventInstance = databaseInterface.getEvent(eventInstance.getId());
        } catch (NoSuchEventException e) {
            Log.e(TAG, "unable to find event after setting a new ringer type, this should not happen");
            finish();
        }

        drawIndicators();
        nofityDatasetChanged();
    }

    private void nofityDatasetChanged() {
        Log.d(TAG, "Broadcasting message");
        Intent intent = new Intent("custom-event-name");
        // You can also include some extra data.
        intent.putExtra("message", "This is my message!");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void colorizeIcons() {
        ImageButton[] imageButtons = {
                (ImageButton) findViewById(R.id.imageButtonNormal),
                (ImageButton) findViewById(R.id.imageButtonVibrate),
                (ImageButton) findViewById(R.id.imageButtonSilent),
                (ImageButton) findViewById(R.id.imageButtonIgnore)
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
        LinearLayout indicatorNormal = (LinearLayout) findViewById(R.id.indicator_normal);
        LinearLayout indicatorVibrate = (LinearLayout) findViewById(R.id.indicator_vibrate);
        LinearLayout indicatorSilent = (LinearLayout) findViewById(R.id.indicator_silent);
        LinearLayout indicatorIgnore = (LinearLayout) findViewById(R.id.indicator_ignore);

        int colorActive = getResources().getColor(R.color.accent);
        int colorInactive = getResources().getColor(R.color.primary);
        int ringerMode = eventInstance.getRingerType();

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.event_longclick, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
