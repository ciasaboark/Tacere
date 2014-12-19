/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;
import org.ciasaboark.tacere.prefs.BetaPrefs;
import org.ciasaboark.tacere.prefs.Prefs;

@ReportsCrashes(
        formUri = "https://ciasaboark.cloudant.com/acra-tacere/_design/acra-storage/_update/report",
        reportType = HttpSender.Type.JSON,
        httpMethod = HttpSender.Method.POST,
        formUriBasicAuthLogin = "anditateldntnedstraystin",
        formUriBasicAuthPassword = "CeapA4X5JYvxeKGgn3Yndx5I",
        formKey = "", // This is required for backward compatibility but not used
        customReportContent = {
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PACKAGE_NAME,
                ReportField.REPORT_ID,
                ReportField.BUILD,
                ReportField.STACK_TRACE,
                ReportField.APPLICATION_LOG,
                ReportField.BRAND,
                ReportField.DEVICE_FEATURES,
                ReportField.DISPLAY,
                ReportField.USER_CRASH_DATE,
                ReportField.LOGCAT,
                ReportField.EVENTSLOG,
                ReportField.SHARED_PREFERENCES,
                ReportField.CUSTOM_DATA,
        },
        additionalSharedPreferences = {
                Prefs.PREFERENCES_NAME,
                BetaPrefs.PREFS_FILE,
        },
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.toast_crash
)

public class Tacere extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        final String ARCA_DISABLE = "acra.disable";

        boolean isAcraDisabled = prefs.getBoolean(ARCA_DISABLE, false);
        prefs.edit().putBoolean(ARCA_DISABLE, isAcraDisabled).apply();

        if (!BuildConfig.DEBUG) {
            ACRA.init(this);
        }
    }
}
