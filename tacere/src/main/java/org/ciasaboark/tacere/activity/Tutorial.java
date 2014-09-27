/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.ciasaboark.tacere.R;

public class Tutorial extends Activity {
    public static final String TAG = "Tutorial";

    private RelativeLayout frame1;
    private RelativeLayout frame2;
    private RelativeLayout frame3;
    private int showingFrame = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        colorizeIcons();
    }

    private void colorizeIcons() {
        ImageView defaultView = (ImageView) findViewById(R.id.tutorial_icon_ringer_default);
        colorizeIcon(defaultView, getResources().getColor(R.color.ringer_default));

        ImageView calendarView = (ImageView) findViewById(R.id.tutorial_icon_ringer_calendar);
        colorizeIcon(calendarView, getResources().getColor(R.color.ringer_calendar));

        ImageView eventSeriesView = (ImageView) findViewById(R.id.tutorial_icon_ringer_event_series);
        colorizeIcon(eventSeriesView, getResources().getColor(R.color.ringer_series));

        ImageView instanceView = (ImageView) findViewById(R.id.tutorial_icon_ringer_instance);
        colorizeIcon(instanceView, getResources().getColor(R.color.ringer_instance));
    }

    private void colorizeIcon(ImageView view, int color) {
        Drawable d = view.getBackground();
        d.mutate().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        view.setBackgroundDrawable(d);
    }

    @Override
    public void onStart() {
        super.onStart();
        frame1 = (RelativeLayout) findViewById(R.id.tutorial_frame_welcome);
        frame1.setVisibility(View.VISIBLE);
        frame2 = (RelativeLayout) findViewById(R.id.tutorial_frame_event_list);
        frame2.setVisibility(View.GONE);
        frame3 = (RelativeLayout) findViewById(R.id.tutorial_frame_ringers);
        frame3.setVisibility(View.GONE);
        final Button skipTutorialButton = (Button) findViewById(R.id.tutorial_button_skip);
        skipTutorialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        final Button nextButton = (Button) findViewById(R.id.tutorial_button_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (showingFrame) {
                    case 1:
                        //hide frame 1 and show frame 2
                        frame1.setVisibility(View.GONE);
                        frame2.setVisibility(View.VISIBLE);
                        frame3.setVisibility(View.GONE);
                        showingFrame = 2;
                        break;
                    case 2:
                        frame1.setVisibility(View.GONE);
                        frame2.setVisibility(View.GONE);
                        frame3.setVisibility(View.VISIBLE);
                        showingFrame = 3;
                        skipTutorialButton.setVisibility(View.GONE);
                        nextButton.setText("Close");
                        break;
                    case 3:
                        skipTutorialButton.performClick();
                        break;
                    default:
                        skipTutorialButton.performClick();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tutorial, menu);
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
