/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.versioning;

import org.ciasaboark.tacere.BuildConfig;

public class Versioning {
    public static String getVersionCode() {
        return BuildConfig.APP_VERSION_MAJOR + "."
                + BuildConfig.APP_VERSION_MINOR + "."
                + BuildConfig.APP_VERSION_RELEASE;
    }

    public static String getReleaseName() {
        return BuildConfig.VERSION_NAME;
    }

    public static int getReleaseNumber() {
        return BuildConfig.VERSION_CODE;
    }


}
