/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.database;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by Jonathan Nelson on 8/11/14.
 */
public class TooltipManager {
    public static final String BROADCAST_MESSAGE_KEY = "tooltip dismissed";
    public static final String SOURCE_KEY = "source";
    private static final String TAG = "TooltipManager";

    private Context context;
    private Object source;

    public TooltipManager(Object src, Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        if (src == null) {
            throw new IllegalArgumentException("source object cannot be null");
        }
        context = ctx;
        source = src;
    }

    public void broadcastTooltipDismissedMessage() {
        Log.d(TAG, "Broadcasting data set changed message on behalf of " + source.getClass());
        Intent intent = new Intent(BROADCAST_MESSAGE_KEY);
        // You can also include some extra data.
        String sourceClass = source.getClass().getName();
        intent.putExtra(SOURCE_KEY, sourceClass);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


}

