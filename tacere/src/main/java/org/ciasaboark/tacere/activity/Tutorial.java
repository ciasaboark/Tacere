/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

    }

    @Override
    public void onStart() {
        super.onStart();
        frame1 = (RelativeLayout) findViewById(R.id.tutorial_frame_1);
        frame1.setVisibility(View.VISIBLE);
        frame2 = (RelativeLayout) findViewById(R.id.tutorial_frame_2);
        frame1.setVisibility(View.GONE);
        frame3 = (RelativeLayout) findViewById(R.id.tutorial_frame_3);
        frame1.setVisibility(View.GONE);
        final Button skipTutorialButton = (Button) findViewById(R.id.tutorial_button_skip);
        skipTutorialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button nextButton = (Button) findViewById(R.id.tutorial_button_next);
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
                        break;
                    case 3:
                        skipTutorialButton.performClick();
                        break;
                    default:
                        Log.e(TAG, "showingFrame was not one of [1,2,3], this should not have " +
                                "happened, closing tutorial activity.");
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
