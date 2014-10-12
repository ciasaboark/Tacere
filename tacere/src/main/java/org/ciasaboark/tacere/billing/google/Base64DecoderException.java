/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.billing.google;

/**
 * Exception thrown when encountering an invalid Base64 input character.
 *
 * @author nelson
 */
public class Base64DecoderException extends Exception {
    private static final long serialVersionUID = 1L;

    public Base64DecoderException() {
        super();
    }

    public Base64DecoderException(String s) {
        super(s);
    }
}
