/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.versioning;

import android.content.Context;

import org.ciasaboark.tacere.BuildConfig;
import org.ciasaboark.tacere.R.*;

public class Versioning {
//    private static final String CURRENT_RELEASE_NAME = "magic marmaduke";
//    private static final int VERSION_MAJOR = 2;
//    private static final int VERSION_MINOR = 0;
//    private static final int VERSION_RELEASE = 5;
//
//    private static final int RELEASE_NUMBER = 5;
    private Context ctx;
    public Versioning(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("given context was null");
        }
        this.ctx = context;
    }

    public String getVersionCode() {
        return BuildConfig.APP_VERSION_MAJOR + "."
                + BuildConfig.APP_VERSION_MINOR + "."
                + BuildConfig.APP_VERSION_RELEASE;
    }

    public  String getReleaseName() {
        return BuildConfig.VERSION_NAME;
    }

    public  int getReleaseNumber() {
        return BuildConfig.VERSION_CODE;
    }


}
