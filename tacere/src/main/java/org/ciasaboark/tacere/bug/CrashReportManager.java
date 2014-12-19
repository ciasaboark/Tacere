/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.bug;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Jonathan Nelson on 12/15/14.
 */
public class CrashReportManager {
    private static final String TAG = "CrashReportManager";
    private final String ARCA_DISABLE = "acra.disable";
    private final SharedPreferences prefs;
    private Context context;

    public CrashReportManager(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        this.context = ctx;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public void enableReports() {
        setReportsEnabled(true);
    }

    public void disableReports() {
        setReportsEnabled(false);
    }

    public boolean isReportsEnabled() {
        return !isReportsDisabled();
    }

    public void setReportsEnabled(boolean reportsEnabled) {
        prefs.edit().putBoolean(ARCA_DISABLE, !reportsEnabled).commit();
    }

    public boolean isReportsDisabled() {
        boolean isAcraDisabled = prefs.getBoolean(ARCA_DISABLE, false);
        return isAcraDisabled;
    }

}
