/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.prefs;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.ciasaboark.tacere.activity.ShowUpdatesActivity;
import org.ciasaboark.tacere.versioning.Versioning;

public class Updates {
    public static final String VERSION_PREFIX = "version-";
    public static final String PREFERENCES_FILE = "versions_shown";
    private final SharedPreferences prefs;
    private final Context context;
    private final Object source;

    public Updates(Context ctx, Object source) {
        if (ctx == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("Source object can not be null");
        }

        this.context = ctx;
        this.prefs = ctx.getSharedPreferences(PREFERENCES_FILE,
                Context.MODE_PRIVATE);
        this.source = source;
    }

    public void showUpdatesDialogIfNeeded() {
        boolean showUpdates = shouldChangelogForCurrentAppVersionBeShown();
        if (showUpdates) {
            showUpdatesDialog(true);
        }
    }

    private boolean shouldChangelogForCurrentAppVersionBeShown() {
        //if this is the first run then disable showing the updates dialog for the current version
        Prefs staticPrefs = new Prefs(context);
        if (staticPrefs.isFirstRun()) {
            hideChangelogForCurrentAppVersion();
        }

        boolean shouldChangelogBeShown = false;
        //the updates dialog should be shown if no value has been stored for the current app version
        shouldChangelogBeShown = prefs.getBoolean(VERSION_PREFIX + Versioning.getVersionCode(), true);

        return shouldChangelogBeShown;
    }

    private void showUpdatesDialog(boolean attachSource) {
        Intent updatesIntent = new Intent(context, ShowUpdatesActivity.class);
        if (attachSource) {
            updatesIntent.putExtra("initiator", source.getClass());
        }
        context.startActivity(updatesIntent);
    }

    public void hideChangelogForCurrentAppVersion() {
        prefs.edit().putBoolean(VERSION_PREFIX + Versioning.getVersionCode(), false).commit();
    }

    public void showUpdatesDialog() {
        showUpdatesDialog(false);
    }
}
