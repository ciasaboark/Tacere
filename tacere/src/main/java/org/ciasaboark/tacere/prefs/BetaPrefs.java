/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.prefs;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Jonathan Nelson on 11/14/14.
 */
public class BetaPrefs {
    public static final String PREFS_FILE = "beta_prefs";
    private static final String TAG = "BetaPrefs";
    private SharedPreferences betaPrefs;
    private SharedPreferences.Editor editor;
    private Context context;

    public BetaPrefs(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        this.context = ctx;
        this.betaPrefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        this.editor = betaPrefs.edit();
    }

    public void setIsBetaPrefsUnlocked(boolean isBetaPrefsUnlocked) {
        if (isBetaPrefsUnlocked) {
            editor.putBoolean(Keys.BETA_PREFS_UNLOCKED, isBetaPrefsUnlocked).commit();
        } else {
            //if we are disabling beta preferences then go ahead and delete any saved preferences
            this.editor.clear().commit();
        }
    }

    public boolean isBetaPrefsUnlocked() {
        return betaPrefs.getBoolean(Keys.BETA_PREFS_UNLOCKED, false);
    }

    public boolean getUseLargeDisplay() {
        return betaPrefs.getBoolean(Keys.LARGEDISPLAY, false);
    }

    public void setUseLargeDisplay(boolean useLargeDisplay) {
        editor.putBoolean(Keys.LARGEDISPLAY, useLargeDisplay).commit();
    }

    public boolean getDisableNotifications() {
        return betaPrefs.getBoolean(Keys.DISABLE_NOTIFICATONS, false);
    }

    public void setDisableNotifications(boolean disableNotifications) {
        editor.putBoolean(Keys.DISABLE_NOTIFICATONS, disableNotifications).commit();
    }

    public boolean getDisableVibration() {
        return betaPrefs.getBoolean(Keys.DISABLE_VIBRATION, false);
    }

    public void setDisableVibration(boolean disableVibration) {
        editor.putBoolean(Keys.DISABLE_VIBRATION, disableVibration).commit();
    }

    private static class Keys {
        public static final String LARGEDISPLAY = "largedisplay";
        public static final String BETA_PREFS_UNLOCKED = "betaPrefsUnlocked";
        public static final String DISABLE_NOTIFICATONS = "disableNotifications";
        public static final String DISABLE_VIBRATION = "disableVibration";
    }
}
