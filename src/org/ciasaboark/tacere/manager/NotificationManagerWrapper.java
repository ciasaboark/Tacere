package org.ciasaboark.tacere.manager;

import org.ciasaboark.tacere.CalEvent;
import org.ciasaboark.tacere.MainActivity;
import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.ResetEventService;
import org.ciasaboark.tacere.service.SkipEventService;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

public class NotificationManagerWrapper {
	private Context context;
	private Prefs prefs;
	
	public NotificationManagerWrapper(Context ctx) {
		this.context = ctx;
		this.prefs = new Prefs(ctx);
	}
	
	/**
	 * Cancel any ongoing notification, this will remove both event notifications and quicksilence
	 * notifications
	 */
	public void cancelAllNotifications() {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(prefs.getNotificationId());
	}

	/**
	 * A wrapper method to build and display a notification for the given CalEvent. If possible the
	 * newer Notification API will be used to place an action button in the notification, otherwise
	 * the older notification style will be used.
	 * 
	 * @param event
	 *            the CalEvent that is currently active.
	 */
	public void displayNotification(CalEvent event) {
		int apiLevelAvailable = android.os.Build.VERSION.SDK_INT;
		if (apiLevelAvailable >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			displayNewNotification(event);
		} else {
			displayCompatNotification(event);
		}
	}

	/**
	 * Builds and displays a notification for the given CalEvent. This method uses the older
	 * Notification API, and does not include an action button
	 * 
	 * @param event
	 *            the CalEvent that is currently active.
	 */
	private void displayCompatNotification(CalEvent event) {
		// clicking the notification should take the user to the app
		Intent notificationIntent = new Intent(context, MainActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		// FLAG_CANCEL_CURRENT is required to make sure that the extras are including in
		// the new pending intent
		PendingIntent pendIntent = PendingIntent.getActivity(context,
				prefs.getRequestCodeNotification(), notificationIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(
				context).setContentTitle("Tacere: Event active")
				.setContentText(event.toString()).setSmallIcon(R.drawable.small_mono)
				.setAutoCancel(false).setOnlyAlertOnce(true).setOngoing(true)
				.setContentIntent(pendIntent);

		// the ticker text should only be shown the first time the notification is
		// created, not on each update
		notBuilder.setTicker("Tacere event starting: " + event.toString());

		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(prefs.getNotificationId(), notBuilder.build());
	}

	/**
	 * Builds and displays a notification for the given CalEvent. This method uses the new
	 * Notification API to place an action button in the notification.
	 * 
	 * @param event
	 *            the CalEvent that is currently active
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void displayNewNotification(CalEvent event) {
		// clicking the notification should take the user to the app
		Intent notificationIntent = new Intent(context, MainActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		// FLAG_CANCEL_CURRENT is required to make sure that the extras are including in
		// the new pending intent
		PendingIntent pendIntent = PendingIntent.getActivity(context,
				prefs.getRequestCodeNotification(), notificationIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		Notification.Builder notBuilder = new Notification.Builder(context)
				.setContentTitle("Tacere: Event active").setContentText(event.toString())
				.setSmallIcon(R.drawable.small_mono).setAutoCancel(false).setOnlyAlertOnce(true)
				.setOngoing(true).setContentIntent(pendIntent);

		if (event.getRingerType() != CalEvent.RINGER.IGNORE) {
			// this intent will be attached to the button on the notification
			Intent skipEventIntent = new Intent(context, SkipEventService.class);
			skipEventIntent.putExtra("org.ciasaboark.tacere.eventId", event.getId());
			PendingIntent skipEventPendIntent = PendingIntent.getService(context, 0, skipEventIntent,
					PendingIntent.FLAG_CANCEL_CURRENT);

			notBuilder
					.addAction(R.drawable.ic_state_normal, "Skip this event", skipEventPendIntent);
		} else {
			// this intent will be attached to the button on the notification
			Intent skipEventIntent = new Intent(context, ResetEventService.class);
			skipEventIntent.putExtra("org.ciasaboark.tacere.eventId", event.getId());
			PendingIntent skipEventPendIntent = PendingIntent.getService(context, 0, skipEventIntent,
					PendingIntent.FLAG_CANCEL_CURRENT);

			notBuilder.addAction(R.drawable.ic_state_normal, "Enable auto silencing",
					skipEventPendIntent);
		}

		// the ticker text should only be shown the first time the notification is
		// created, not on each update
		notBuilder.setTicker("Tacere event starting: " + event.toString());

		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(prefs.getNotificationId(), notBuilder.build());
	}
}
