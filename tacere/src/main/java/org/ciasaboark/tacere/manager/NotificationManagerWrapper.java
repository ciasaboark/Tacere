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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.text.TextPaint;
import android.util.Log;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.MainActivity;
import org.ciasaboark.tacere.billing.Authenticator;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.EventManager;
import org.ciasaboark.tacere.prefs.BetaPrefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.ExtendEventService;
import org.ciasaboark.tacere.service.ExtendQuicksilenceService;
import org.ciasaboark.tacere.service.RequestTypes;
import org.ciasaboark.tacere.service.SkipEventService;

import java.text.DateFormat;
import java.util.Date;

public class NotificationManagerWrapper {
    private static final String TAG = "NotificationManagerWrapper";
    // an id to reference all notifications
    private static final int NOTIFICATION_ID = 1;
    private static final int PUBLIC_NOTIFICATION_ID = 2;

    private final Context context;

    public NotificationManagerWrapper(Context ctx) {
        this.context = ctx;
    }

    public void displayQuickSilenceNotification(int quicksilenceDurationMinutes) {
        BetaPrefs betaPrefs = new BetaPrefs(context);
        if (betaPrefs.getDisableNotifications()) {
            Log.d(TAG, "asked to display quicksilence notification, but this has been disabled in " +
                    "beta settings. Stopping any ongoing notifications");
            cancelAllNotifications();
        } else {
            // the intent attached to the notification should only cancel the quick silence request, but
            // not launch the app
            Intent notificationIntent = new Intent(context, EventSilencerService.class);
            notificationIntent.putExtra(EventSilencerService.WAKE_REASON, RequestTypes.CANCEL_QUICKSILENCE);
            long endTime = System.currentTimeMillis() + (long) quicksilenceDurationMinutes
                    * EventInstance.MILLISECONDS_IN_MINUTE;

            DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
            Date date = new Date(endTime);
            String formattedDate = dateFormat.format(date);
            String formattedString = String.format(context.getString(R.string.notification_quicksilence_description), formattedDate);

            // FLAG_CANCEL_CURRENT is required to make sure that the extras are including in the new
            // pending intent
            PendingIntent pendIntent = PendingIntent.getService(context,
                    REQUEST_CODES.QUICKSILENCE_CANCEL, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            //an intent to open the main app
            Intent openMainIntent = new Intent(context, MainActivity.class);
            openMainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            openMainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent openMainPendIntent = PendingIntent.getActivity(context,
                    REQUEST_CODES.QUICKSILENCE_OPEN_MAIN,
                    openMainIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            //An additional button to extend the quicksilence by 15min
            // this intent will be attached to the button on the notification
            Intent addMinutesIntent = new Intent(context, ExtendQuicksilenceService.class);
            addMinutesIntent.putExtra(ExtendQuicksilenceService.ORIGINAL_END_TIMESTAMP, endTime);
            addMinutesIntent.putExtra(ExtendQuicksilenceService.EXTEND_LENGTH, 15);
            PendingIntent addMinPendIntent = PendingIntent.getService(context,
                    REQUEST_CODES.QUICKSILENCE_ADD_TIME, addMinutesIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(
                    context).setContentTitle(context.getString(R.string.notification_quicksilence_title))
                    .setContentText(formattedString).setTicker(context.getString(R.string.notification_quicksilence_ticker))
                    .setSmallIcon(R.drawable.not_silence).setAutoCancel(true).setOngoing(true)
                    .setContentIntent(pendIntent).setColor(context.getResources().getColor(R.color.accent))
                    .setTicker(context.getString(R.string.notification_quicksilence_ticker));

            notBuilder.addAction(R.drawable.app_mono_small, "Open Tacere", openMainPendIntent);
            //the +15 button should only be available in the Pro version
            Authenticator authenticator = new Authenticator(context);
            if (authenticator.isAuthenticated()) {
                notBuilder.addAction(R.drawable.not_clock, "+15", addMinPendIntent);
            }

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(NOTIFICATION_ID);
            nm.notify(NOTIFICATION_ID, notBuilder.build());
        }
    }

    /**
     * Cancel any ongoing notifications, this will remove both event notifications and quicksilence
     * notifications
     */
    public void cancelAllNotifications() {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
        nm.cancel(PUBLIC_NOTIFICATION_ID);
    }

    /**
     * A wrapper method to build and display a notification for the given CalEvent. If possible the
     * newer Notification API will be used to place an action button in the notification, otherwise
     * the older notification style will be used.
     *
     * @param event the CalEvent that is currently active.
     */
    public void displayEventNotification(EventInstance event) {
        BetaPrefs betaPrefs = new BetaPrefs(context);
        if (betaPrefs.getDisableNotifications()) {
            Log.d(TAG, "asked to display event notification, but this has been disabled in " +
                    "beta preferences.  Stoping any ongoing notifications.");
            cancelAllNotifications();
        } else {
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
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // FLAG_CANCEL_CURRENT is required to make sure that the extras are including in
        // the new pending intent
        PendingIntent pendIntent = PendingIntent.getActivity(context,
                REQUEST_CODES.EVENT_OPEN_MAIN, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);


        Notification.Builder notBuilder = new Notification.Builder(context)
                .setContentTitle(context.getString(R.string.notification_event_active_title)).setContentText(event.toString())
                .setAutoCancel(false).setOnlyAlertOnce(true)
                .setOngoing(true).setContentIntent(pendIntent);

        EventManager eventManager = new EventManager(context, event);
        switch (eventManager.getBestRinger()) {
            case SILENT:
                notBuilder.setSmallIcon(R.drawable.not_silence);
                break;
            case VIBRATE:
                notBuilder.setSmallIcon(R.drawable.not_vibrate);
                break;
            case NORMAL:
                notBuilder.setSmallIcon(R.drawable.not_normal);
                break;
        }


        // this intent will be attached to the button on the notification
        Intent skipEventIntent = new Intent(context, SkipEventService.class);
        skipEventIntent.putExtra(SkipEventService.EVENT_ID_TAG, event.getId());
        PendingIntent skipEventPendIntent = PendingIntent.getService(context,
                REQUEST_CODES.EVENT_IGNORE, skipEventIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notBuilder.addAction(R.drawable.not_ignore, context.getString(R.string.notification_event_skip), skipEventPendIntent);

        //an intent to add an additional 15 minutes of silencing for an event
        Intent addSilencingIntent = new Intent(context, ExtendEventService.class);
        addSilencingIntent.putExtra(ExtendEventService.INSTANCE_ID, event.getId());
        addSilencingIntent.putExtra(ExtendEventService.NEW_EXTEND_LENGTH, event.getExtendMinutes() + 15); //TODO use minutes stored in prefs
        PendingIntent addSilenceingPendIntent = PendingIntent.getService(context,
                REQUEST_CODES.EVENT_ADD_TIME, addSilencingIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Authenticator authenticator = new Authenticator(context);
        if (authenticator.isAuthenticated()) {
            notBuilder.addAction(R.drawable.not_clock, "+15", addSilenceingPendIntent);
        }

        // the ticker text should only be shown the first time the notification is
        // created, not on each update
        notBuilder.setTicker(context.getString(R.string.notification_event_active_starting) +
                " " + event.toString());

        if (Build.VERSION.SDK_INT >= 21) {
            notBuilder.setVisibility(Notification.VISIBILITY_PRIVATE);
            Notification.Builder publicNotification = getBaseEventNotificationBuilder();
            publicNotification.setContentTitle(context.getString(R.string.notification_event_active_title_public));
            publicNotification.setContentIntent(pendIntent);
            notBuilder.setPublicVersion(publicNotification.build());
            notBuilder.setColor(context.getResources().getColor(R.color.accent));

            Drawable d = context.getResources().getDrawable(R.drawable.shape_circle);
            int height = (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_width);
            d.setBounds(0, 0, width, height);
            d.mutate().setColorFilter(event.getDisplayColor(), PorterDuff.Mode.MULTIPLY);
            DatabaseInterface databaseInterface = DatabaseInterface.getInstance(context);
            String calendarTitle = databaseInterface.getCalendarNameForId(event.getCalendarId());
            String c = ((Character) calendarTitle.charAt(0)).toString().toUpperCase();


//            Bitmap largeIcon = createMarkerIcon(d, c, width, height);
            Bitmap largeIcon = combineDrawablesToBitmap(d, getRingerIconForEvent(event), width, height);
            notBuilder.setLargeIcon(largeIcon);
        }

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
                REQUEST_CODES.EVENT_OPEN_MAIN, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(
                context).setContentTitle(context.getString(R.string.notification_event_active_title))
                .setContentText(event.toString()).setAutoCancel(false)
                .setOnlyAlertOnce(true).setOngoing(true).setContentIntent(pendIntent)
                .setColor(context.getResources().getColor(R.color.accent));

        EventManager eventManager = new EventManager(context, event);
        switch (eventManager.getBestRinger()) {
            case SILENT:
                notBuilder.setSmallIcon(R.drawable.not_silence);
                break;
            case VIBRATE:
                notBuilder.setSmallIcon(R.drawable.not_vibrate);
                break;
            case NORMAL:
                notBuilder.setSmallIcon(R.drawable.not_normal);
                break;
        }

        // the ticker text should only be shown the first time the notification is
        // created, not on each update
        notBuilder.setTicker(context.getString(R.string.notification_event_active_starting) + " " + event.toString());

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notBuilder.build());
    }

    private Notification.Builder getBaseEventNotificationBuilder() {
        // clicking the notification should take the user to the app
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // FLAG_CANCEL_CURRENT is required to make sure that the extras are including in
        // the new pending intent
        PendingIntent pendIntent = PendingIntent.getActivity(context,
                PUBLIC_NOTIFICATION_ID, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        Notification.Builder notBuilder = new Notification.Builder(context);
        notBuilder.setContentIntent(pendIntent).setOngoing(true).setOnlyAlertOnce(true)
                .setAutoCancel(false).setSmallIcon(R.drawable.small_mono);
        return notBuilder;
    }

    private Bitmap combineDrawablesToBitmap(final Drawable backgroundImage, final BitmapDrawable overlayImage, int width, int height) {

        Bitmap canvasBitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        // Create a canvas, that will draw on to canvasBitmap.
        Canvas imageCanvas = new Canvas(canvasBitmap);

        // Draw the image to our canvas
        backgroundImage.setBounds(0, 0, width, height);
        backgroundImage.draw(imageCanvas);
        overlayImage.setBounds(0, 0, (int) (overlayImage.getIntrinsicWidth() * 0.5),
                (int) (overlayImage.getIntrinsicHeight() * 0.5));
        final int scaleWidth = 24;
        final int scaleHeight = 24;
        final float scaleWidthPercent = ((float) scaleWidth) / overlayImage.getIntrinsicWidth();
        final float scaleHeightPercent = ((float) scaleHeight) / overlayImage.getIntrinsicHeight();
        ScaleDrawable scaleDrawable = new ScaleDrawable(overlayImage, 0, scaleWidthPercent,
                scaleHeightPercent);
        Drawable scaledOverlay = scaleDrawable.getDrawable();
        scaledOverlay.setBounds(0, 0, width, height);
        scaledOverlay.draw(imageCanvas);

        // Combine background and text to a LayerDrawable
        LayerDrawable layerDrawable = new LayerDrawable(
                new Drawable[]{backgroundImage, new BitmapDrawable(canvasBitmap)});
        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        layerDrawable.setBounds(0, 0, width, height);
        layerDrawable.draw(new Canvas(newBitmap));
        return newBitmap;
    }

    private BitmapDrawable getRingerIconForEvent(EventInstance event) {
        Drawable d = context.getResources().getDrawable(R.drawable.blank);
        EventManager eventManager = new EventManager(context, event);
        switch (eventManager.getBestRinger()) {
            case SILENT:
                d = context.getResources().getDrawable(R.drawable.ic_state_silent);
                break;
            case VIBRATE:
                d = context.getResources().getDrawable(R.drawable.ic_state_vibrate);
                break;
            case NORMAL:
                d = context.getResources().getDrawable(R.drawable.not_normal_icon);
                break;
        }
        return (BitmapDrawable) d;
    }

    private Bitmap createMarkerIcon(Drawable backgroundImage, String text, int width, int height) {

        Bitmap canvasBitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        // Create a canvas, that will draw on to canvasBitmap.
        Canvas imageCanvas = new Canvas(canvasBitmap);

        // Draw the image to our canvas
        backgroundImage.draw(imageCanvas);

        // Set up the paint for use with our Canvas
        TextPaint textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | TextPaint.LINEAR_TEXT_FLAG);
        textPaint.setTextAlign(TextPaint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setTextSize(100f);
        textPaint.setColor(context.getResources().getColor(android.R.color.white));

        int xPos = (imageCanvas.getWidth() / 2);
        int yPos = (int) ((imageCanvas.getHeight() / 2) - ((textPaint.descent() +
                textPaint.ascent()) / 2));
        Rect r = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), r);
//        yPos += (Math.abs(r.height()))/2;

        // Draw the text on top of our image
        imageCanvas.drawText(text, xPos, yPos, textPaint);


        // Combine background and text to a LayerDrawable
        LayerDrawable layerDrawable = new LayerDrawable(
                new Drawable[]{backgroundImage, new BitmapDrawable(canvasBitmap)});
        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        layerDrawable.setBounds(0, 0, width, height);
        layerDrawable.draw(new Canvas(newBitmap));
        return newBitmap;
    }

    private class REQUEST_CODES {
        public static final int QUICKSILENCE_OPEN_MAIN = 1;
        public static final int QUICKSILENCE_CANCEL = 2;
        public static final int QUICKSILENCE_ADD_TIME = 3;
        public static final int EVENT_OPEN_MAIN = 4;
        public static final int EVENT_IGNORE = 5;
        public static final int EVENT_ADD_TIME = 6;
    }
}
