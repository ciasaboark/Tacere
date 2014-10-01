/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.R.id;
import org.ciasaboark.tacere.activity.fragment.MainSettingsFragment;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;


public class SettingsActivity extends FragmentActivity implements MainSettingsFragment.OnFragmentInteractionListener {
    @SuppressWarnings("unused")
    private static final String TAG = "Settings";
    private final Prefs prefs = new Prefs(this);
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
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
    }

    public void onFragmentInteraction(Uri uri) {
        //nothing to do here
    }

    public void onPause() {
        super.onPause();
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

    private void drawAllWidgets() {
        drawServiceWidget();
        drawFragments();
    }

    private void drawFragments() {

        if (findViewById(id.settings_fragment_main) != null) {
            MainSettingsFragment mainSettingsFragment = new MainSettingsFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(id.settings_fragment_main, mainSettingsFragment, MainSettingsFragment.TAG).commit();
        }

        if (findViewById(id.settings_fragment_advanced) != null) {
            //TODO make advanced settings fragment and add it here
        }
    }

    private void drawServiceWidget() {
        //the service state toggle
        Switch serviceActivatedSwitch = (Switch) findViewById(id.activateServiceSwitch);
        if (prefs.isServiceActivated()) {
            serviceActivatedSwitch.setChecked(true);
        } else {
            serviceActivatedSwitch.setChecked(false);
        }
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

    public void onClickToggleService(View v) {
        prefs.setIsServiceActivated(!prefs.isServiceActivated());
        restartEventSilencerService();
        drawServiceWidget();
    }


    private void restartEventSilencerService() {
        Intent i = new Intent(this, EventSilencerService.class);
        i.putExtra("type", RequestTypes.ACTIVITY_RESTART);
        startService(i);
    }

    public void onClickAdvancedSettings(View v) {
        Intent i = new Intent(this, AdvancedSettingsActivity.class);
        startActivity(i);
    }
}
