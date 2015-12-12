/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.billing;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.ContextThemeWrapper;

import org.ciasaboark.tacere.BuildConfig;
import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.ProUpgradeActivity;

public class Authenticator {
    private static final String TAG = "Authenticator";
    private static final String PREFERENCES_NAME = "licensing";
    private static final String KEY_PRO_VERSION = "pro";
    private final Context context;
    private SharedPreferences prefs;

    public Authenticator(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        this.context = ctx;
        this.prefs = ctx.getSharedPreferences(PREFERENCES_NAME,
                Context.MODE_PRIVATE);
    }

    public void deAuthenticate() {
        prefs.edit().putBoolean(KEY_PRO_VERSION, false).commit();
    }

    public void setAuthenticateSuccess(boolean isAuthenticated) {
        prefs.edit().putBoolean(KEY_PRO_VERSION, isAuthenticated).commit();
    }

    public boolean isAuthenticated() {
        boolean isDonationKeyInstalled = isDonationKeyInstalled();
        boolean hasProVersionBeenPurchased = prefs.getBoolean(KEY_PRO_VERSION, false);
        return isDonationKeyInstalled || hasProVersionBeenPurchased;
    }

    private boolean isDonationKeyInstalled() {
        PackageManager manager = context.getPackageManager();
        boolean keyIsInstalled = false;
        if (BuildConfig.DEBUG) {
            //if we are running in debug mode then don't bother checking the signatures, just go by
            //whether the key is installed
            try {
                manager.getPackageInfo("org.ciasaboark.tacere.key", 0);
                keyIsInstalled = true;
            } catch (PackageManager.NameNotFoundException e) {
            }
        } else {
            keyIsInstalled = manager.checkSignatures("org.ciasaboark.tacere",
                    "org.ciasaboark.tacere.key") == PackageManager.SIGNATURE_MATCH;
        }
        return keyIsInstalled;
    }

    public void showUpgradeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(context, R.style.Dialog)
        );
        builder.setTitle("Upgrade to Pro");
        builder.setMessage("This feature is available in the pro version of Tacere");
        builder.setPositiveButton("Tell Me More", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent(context, ProUpgradeActivity.class);
                context.startActivity(i);
            }
        });
        builder.setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //nothing to do here
            }
        });
        builder.show();
    }
}
