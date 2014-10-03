/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.fragment.AdvancedSettingsFragment;
import org.ciasaboark.tacere.prefs.Prefs;

// import android.util.Log;

public class AdvancedSettingsActivity extends FragmentActivity {
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

        attachFragment();
    }

    private void attachFragment() {
        if (findViewById(R.id.advanced_settings_fragment) != null) {
            AdvancedSettingsFragment advancedSettingsFragment = (AdvancedSettingsFragment) getSupportFragmentManager().findFragmentByTag(AdvancedSettingsFragment.TAG);
            if (advancedSettingsFragment == null) {
                advancedSettingsFragment = new AdvancedSettingsFragment();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.advanced_settings_fragment, advancedSettingsFragment, AdvancedSettingsFragment.TAG).commit();
            } else {
                //fragment has already been attached, ask it to update its view
                advancedSettingsFragment.drawAllWidgets();
            }
        }
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

}
