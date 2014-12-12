/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.widget.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.DataSetManager;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventInstanceException;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.ringer.RingerType;

public class WidgetPopupRinger extends Activity {
    public static final String EVENT_ID = "eventId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_pop_up);
        final DatabaseInterface databaseInterface = DatabaseInterface.getInstance(this);
        final DataSetManager dataSetManager = new DataSetManager(this, this);

        final long eventId = getIntent().getExtras().getLong(EVENT_ID, -1);
        if (eventId == -1) {
            finish();
        } else {
            try {
                EventInstance e = databaseInterface.getEvent(eventId);
                String dialogText = e.getTitle();
                TextView txt = (TextView) findViewById(R.id.mytxt);
                txt.setText(dialogText);

                View normalBox = findViewById(R.id.ringer_popup_ringer_normal);
                View vibrateBox = findViewById(R.id.ringer_popup_ringer_vibrate);
                View silenceBox = findViewById(R.id.ringer_popup_ringer_silent);
                View ignoreBox = findViewById(R.id.ringer_popup_ringer_ignore);

                normalBox.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        databaseInterface.setRingerForInstance(eventId, RingerType.NORMAL);
                        dataSetManager.broadcastDataSetChangedForId(eventId);
                        finish();
                    }
                });

                vibrateBox.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        databaseInterface.setRingerForInstance(eventId, RingerType.VIBRATE);
                        dataSetManager.broadcastDataSetChangedForId(eventId);
                        finish();
                    }
                });

                silenceBox.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        databaseInterface.setRingerForInstance(eventId, RingerType.SILENT);
                        dataSetManager.broadcastDataSetChangedForId(eventId);
                        finish();
                    }
                });

                ignoreBox.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        databaseInterface.setRingerForInstance(eventId, RingerType.IGNORE);
                        dataSetManager.broadcastDataSetChangedForId(eventId);
                        finish();
                    }
                });


                Button closeButton = (Button) findViewById(R.id.ringer_popup_button_close);
                closeButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });

                Button resetButton = (Button) findViewById(R.id.ringer_popup_button_reset);
                resetButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        databaseInterface.setRingerForInstance(eventId, RingerType.UNDEFINED);
                        dataSetManager.broadcastDataSetChangedForId(eventId);
                        finish();
                    }
                });
            } catch (NoSuchEventInstanceException e) {
                finish();
            }

        }
    }

}
