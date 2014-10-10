/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.manager;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.MainActivity;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;
import org.ciasaboark.tacere.service.SkipEventService;

public class NotificationManagerWrapper {
    private static final String TAG = "NotificationManagerWrapper";
    // an id to reference all notifications
    private static final int NOTIFICATION_ID = 1;

    private final Context context;

    public NotificationManagerWrapper(Context ctx) {
        this.context = ctx;
    }

    /**
     * Cancel any ongoing notifications, this will remove both event notifications and quicksilence
     * notifications
     */
    public void cancelAllNotifications() {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    public void displayQuickSilenceNotification(int quicksilenceDurationMinutes) {
        // the intent attached to the notification should only cancel the quick silence request, but
        // not launch the app
        Intent notificationIntent = new Intent(context, EventSilencerService.class);
        notificationIntent.putExtra("type", RequestTypes.CANCEL_QUICKSILENCE);

        int hrs = quicksilenceDurationMinutes / 60;
        int min = quicksilenceDurationMinutes % 60;
        String formattedHours = "";
        if (hrs == 1) {
            formattedHours = String.format(context.getString(R.string.notification_time_and_unit), hrs, context.getString(R.string.hour_lower));
        } else if (hrs > 1) {
            formattedHours = String.format(context.getString(R.string.notification_time_and_unit), hrs, context.getString(R.string.hours_lower));
        }

        String formattedMinutes = "";
        if (min == 1) {
            formattedMinutes = " " + String.format(context.getString(R.string.notification_time_and_unit), min, context.getString(R.string.minute_lower));
        } else if (min > 1) {
            formattedMinutes = " " + String.format(context.getString(R.string.notification_time_and_unit), min, context.getString(R.string.minutes_lower));
        }


        String formattedString = String.format(context.getString(R.string.notification_quicksilence_description), formattedHours, formattedMinutes);

        // FLAG_CANCEL_CURRENT is required to make sure that the extras are including in the new
        // pending intent
        PendingIntent pendIntent = PendingIntent.getService(context,
                NOTIFICATION_ID, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(
                context).setContentTitle(context.getString(R.string.notification_quicksilence_title))
                .setContentText(formattedString).setTicker(context.getString(R.string.notification_quicksilence_ticker))
                .setSmallIcon(R.drawable.small_mono).setAutoCancel(true).setOngoing(true)
                .setContentIntent(pendIntent)
                .setTicker(context.getString(R.string.notification_quicksilence_ticker));

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
        nm.notify(NOTIFICATION_ID, notBuilder.build());
    }

    /**
     * A wrapper method to build and display a notification for the given CalEvent. If possible the
     * newer Notification API will be used to place an action button in the notification, otherwise
     * the older notification style will be used.
     *
     * @param event the CalEvent that is currently active.
     */
    public void displayEventNotification(EventInstance event) {
        if (event == null) {
            throw new IllegalArgumentException(TAG + " displayEventNotification given null event");
        }
        int apiLevelAvailable = android.os.Build.VERSION.SDK_INT;
        if (apiLevelAvailable >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            displayNewEventNotification(event);
        } else {
            displayCompatEventNotification(event);
        }
    }

    /**
     * Builds and displays a notification for the given CalEvent. This method uses the new
     * Notification API to place an action button in the notification.
     *
     * @param event the CalEvent that is currently active
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void displayNewEventNotification(EventInstance event) {
        // clicking the notification should take the user to the app
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // FLAG_CANCEL_CURRENT is required to make sure that the extras are including in
        // the new pending intent
        PendingIntent pendIntent = PendingIntent.getActivity(context,
                NOTIFICATION_ID, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Notification.Builder notBuilder = new Notification.Builder(context)
                .setContentTitle(context.getString(R.string.notification_event_active_title)).setContentText(event.toString())
                .setSmallIcon(R.drawable.small_mono).setAutoCancel(false).setOnlyAlertOnce(true)
                .setOngoing(true).setContentIntent(pendIntent);


        // this intent will be attached to the button on the notification
        Intent skipEventIntent = new Intent(context, SkipEventService.class);
        skipEventIntent.putExtra("org.ciasaboark.tacere.eventId", event.getId());
        PendingIntent skipEventPendIntent = PendingIntent.getService(context, 0, skipEventIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        notBuilder
                .addAction(R.drawable.ic_state_ignore, context.getString(R.string.notification_event_skip), skipEventPendIntent);

        // the ticker text should only be shown the first time the notification is
        // created, not on each update
        notBuilder.setTicker(context.getString(R.string.notification_event_active_starting) + event.toString());

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notBuilder.build());
    }

    /**
     * Builds and displays a notification for the given CalEvent. This method uses the older
     * Notification API, and does not include an action button
     *
     * @param event the CalEvent that is currently active.
     */
    private void displayCompatEventNotification(EventInstance event) {
        // clicking the notification should take the user to the app
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // FLAG_CANCEL_CURRENT is required to make sure that the extras are including in
        // the new pending intent
        PendingIntent pendIntent = PendingIntent.getActivity(context,
                NOTIFICATION_ID, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(
                context).setContentTitle(context.getString(R.string.notification_event_active_title))
                .setContentText(event.toString()).setSmallIcon(R.drawable.small_mono)
                .setAutoCancel(false).setOnlyAlertOnce(true).setOngoing(true)
                .setContentIntent(pendIntent);

        // the ticker text should only be shown the first time the notification is
        // created, not on each update
        notBuilder.setTicker(context.getString(R.string.notification_event_active_starting) + event.toString());

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notBuilder.build());
    }
}
