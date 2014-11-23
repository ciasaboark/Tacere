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
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventInstanceException;
import org.ciasaboark.tacere.event.EventInstance;

public class WidgetPopupRinger extends Activity {
    public static final String EVENT_ID = "eventId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_pop_up);
        DatabaseInterface databaseInterface = DatabaseInterface.getInstance(this);

        long eventId = getIntent().getExtras().getLong(EVENT_ID, -1);
        if (eventId == -1) {
            finish();
        } else {
            try {
                EventInstance e = databaseInterface.getEvent(eventId);
                String dialogText = e.getTitle();
                TextView txt = (TextView) findViewById(R.id.mytxt);
                txt.setText(dialogText);

                Button dismissbutton = (Button) findViewById(R.id.closBtn);
                dismissbutton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WidgetPopupRinger.this.finish();
                    }
                });
            } catch (NoSuchEventInstanceException e) {
                finish();
            }

        }
    }

}
