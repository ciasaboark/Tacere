/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.versioning;

public class Versioning {
    private static final String CURRENT_RELEASE_NAME = "beta(7-3-14)";
    private static final int VERSION_MAJOR = 2;
    private static final int VERSION_MINOR = 0;
    private static final int VERSION_RELEASE = 5;

    private static final int RELEASE_NUMBER = 5;

    public static String getVersionCode() {
        return new String(VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_RELEASE);
    }

    public static String getReleaseName() {
        return CURRENT_RELEASE_NAME;
    }

    public static int getReleaseNumber() {
        return RELEASE_NUMBER;
    }


}
