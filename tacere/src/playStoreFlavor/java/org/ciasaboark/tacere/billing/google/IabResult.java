/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.billing.google;

/**
 * Represents the result of an in-app billing operation.
 * A result is composed of a response code (an integer) and possibly a
 * message (String). You can get those by calling
 * {@link #getResponse} and {@link #getMessage()}, respectively. You
 * can also inquire whether a result is a success or a failure by
 * calling {@link #isSuccess()} and {@link #isFailure()}.
 */
public class IabResult {
    int mResponse;
    String mMessage;

    public IabResult(int response, String message) {
        mResponse = response;
        if (message == null || message.trim().length() == 0) {
            mMessage = IabHelper.getResponseDesc(response);
        } else {
            mMessage = message + " (response: " + IabHelper.getResponseDesc(response) + ")";
        }
    }

    public int getResponse() {
        return mResponse;
    }

    public boolean isOwned() {
        return mResponse == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED;
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    public boolean isSuccess() {
        return mResponse == IabHelper.BILLING_RESPONSE_RESULT_OK || mResponse == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED;
    }

    public String toString() {
        return "IabResult: " + getMessage();
    }

    public String getMessage() {
        return mMessage;
    }
}

