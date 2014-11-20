/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.R.id;
import org.ciasaboark.tacere.billing.Authenticator;
import org.ciasaboark.tacere.prefs.BetaPrefs;
import org.ciasaboark.tacere.prefs.Updates;
import org.ciasaboark.tacere.versioning.Versioning;

public class AboutActivity extends ActionBarActivity {
    private static final String TAG = "AboutActivity";
    View.OnTouchListener mOnTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN: {
                    final int x = (int) event.getX();
                    final int y = (int) event.getY();
                    recordCoordinates(x, y);
                    break;
                }
            }
            return false;
        }
    };
    private int iconTouches = 0;
    private Toast ongoingToast;
    private Integer x;
    private Integer y;

    private void recordCoordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ongoingToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        // Show the Up button in the action bar.
        setupActionBar();

        TextView sourceText = (TextView) findViewById(id.about_source_text);
        sourceText.setMovementMethod(LinkMovementMethod.getInstance());
        TextView bugsText = (TextView) findViewById(id.about_bugs_text);
        bugsText.setMovementMethod(LinkMovementMethod.getInstance());
        TextView commentsText = (TextView) findViewById(id.about_comments_text);
        commentsText.setMovementMethod(LinkMovementMethod.getInstance());

        TextView versionText = (TextView) findViewById(id.about_version_number);
        String formattedVersion = String.format(getString(R.string.about_version), Versioning.getVersionCode());
        versionText.setText(formattedVersion);

        TextView basicOrPro = (TextView) findViewById(id.about_version_pro);
        String type;
        Authenticator authenticator = new Authenticator(this);
        if (authenticator.isAuthenticated()) {
            type = "Pro version";
        } else {
            type = "Basic version";
        }
        basicOrPro.setText(type);

        final View betaSettingsHeaderContent = findViewById(id.about_header_beta_settings);
        betaSettingsHeaderContent.setOnTouchListener(mOnTouch);
        final View normalHeaderContent = findViewById(id.about_header_normal);
        normalHeaderContent.setOnTouchListener(mOnTouch);
        normalHeaderContent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleVisibility(normalHeaderContent);
                return true;
            }
        });
        betaSettingsHeaderContent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleVisibility(normalHeaderContent);
                return true;
            }
        });

        final View betaSettingsClickArea = findViewById(id.about_header_beta_settings_clickarea);
        betaSettingsClickArea.setOnTouchListener(mOnTouch);
        final SwitchCompat betaSettingsSwitch = (SwitchCompat) findViewById(id.about_header_beta_settings_switch);
        final BetaPrefs betaPrefs = new BetaPrefs(this);
        betaSettingsSwitch.setChecked(betaPrefs.isBetaPrefsUnlocked());
        betaSettingsClickArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                betaSettingsSwitch.performClick();
                betaPrefs.setIsBetaPrefsUnlocked(betaSettingsSwitch.isChecked());
            }
        });
        betaSettingsClickArea.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleVisibility(normalHeaderContent);
                return true;
            }
        });
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        } catch (NullPointerException e) {
            Log.e(TAG, "unable to setup action bar");
        }
    }

    private void toggleVisibility(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            toggleVisibilityCircularReveal(view);
        } else {
            toggleVisibilitySimple(view);
        }
    }

    @TargetApi(21)
    private void toggleVisibilityCircularReveal(final View view) {
        if (view.getVisibility() == View.VISIBLE) {
            //the view is visible, so animate it out then set visibility to GONE
            int cx;
            int cy;
            if (this.x == null || this.y == null) {
                // get the center for the clipping circle
                cx = (view.getLeft() + view.getRight()) / 2;
                cy = (view.getTop() + view.getBottom()) / 2;
            } else {
                //a touch event has been recorded, use the last known coordinates
                cx = this.x;
                cy = this.y;
            }

            // get the initial radius for the clipping circle
            int initialRadius = view.getWidth();

            // create the animation (the final radius is zero)
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(View.INVISIBLE);
                }
            });
            anim.setDuration(700);
            // start the animation
            anim.start();
        } else {
            //the view is not visible (either GONE or INVISIBILE), set visibility to VISIBLE, then
            //animate in
            int cx;
            int cy;
            if (this.x == null || this.y == null) {
                // get the center for the clipping circle
                cx = (view.getLeft() + view.getRight()) / 2;
                cy = (view.getTop() + view.getBottom()) / 2;
            } else {
                //a touch event has been recorded, use the last known coordinates
                cx = this.x;
                cy = this.y;
            }

            // get the final radius for the clipping circle
            int finalRadius = view.getWidth();

            // create and start the animator for this view
            // (the start radius is zero)
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);
            anim.setDuration(500);
            view.setVisibility(View.VISIBLE);
            anim.start();
        }
    }

    private void toggleVisibilitySimple(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean itemProcessedHere = false;
        switch (item.getItemId()) {
            case id.action_about_tutorial:
                Intent tutorialIntent = new Intent(this, TutorialActivity.class);
                startActivity(tutorialIntent);
                itemProcessedHere = true;
                break;
            case R.id.action_about_license:
                Intent licenseIntent = new Intent(this, org.ciasaboark.tacere.activity.AboutLicenseActivity.class);
                licenseIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(licenseIntent);
                itemProcessedHere = true;
                break;
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                itemProcessedHere = true;
                break;
            case R.id.action_about_updates:
                Updates updates = new Updates(this, this);
                updates.showUpdatesDialog();
                itemProcessedHere = true;
                break;
        }

        return itemProcessedHere;
    }

}
