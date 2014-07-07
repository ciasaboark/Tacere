package org.ciasaboark.tacere.manager;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.CalEvent;
import org.ciasaboark.tacere.activity.MainActivity;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;
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
	// an id to reference all notifications
	private static final int NOTIFICATION_ID	= 1;
	
	private Context context;
	
	public NotificationManagerWrapper(Context ctx) {
		this.context = ctx;
	}
	
	public int getNotificationId() {
		return NOTIFICATION_ID;
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
		StringBuilder sb = new StringBuilder("Silencing for ");
		if (hrs > 0) {
			sb.append(hrs + " hr ");
		}

		sb.append(min + " min. Touch to cancel");

		// FLAG_CANCEL_CURRENT is required to make sure that the extras are including in the new
		// pending intent
		PendingIntent pendIntent = PendingIntent.getService(context,
				NOTIFICATION_ID, notificationIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		// TODO use strings in xml
		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(
				context).setContentTitle("Tacere: Quick Silence")
				.setContentText(sb.toString()).setTicker("Quick Silence activating")
				.setSmallIcon(R.drawable.small_mono).setAutoCancel(true).setOngoing(true)
				.setContentIntent(pendIntent);

		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(NOTIFICATION_ID);
		nm.notify(NOTIFICATION_ID, notBuilder.build());
	}
	
	/**
	 * A wrapper method to build and display a notification for the given CalEvent. If possible the
	 * newer Notification API will be used to place an action button in the notification, otherwise
	 * the older notification style will be used.
	 * 
	 * @param event
	 *            the CalEvent that is currently active.
	 */
	public void displayEventNotification(CalEvent event) {
		int apiLevelAvailable = android.os.Build.VERSION.SDK_INT;
		if (apiLevelAvailable >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			displayNewEventNotification(event);
		} else {
			displayCompatEventNotification(event);
		}
	}
	
	/**
	 * Builds and displays a notification for the given CalEvent. This method uses the older
	 * Notification API, and does not include an action button
	 * 
	 * @param event
	 *            the CalEvent that is currently active.
	 */
	private void displayCompatEventNotification(CalEvent event) {
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
				context).setContentTitle("Tacere: Event active")
				.setContentText(event.toString()).setSmallIcon(R.drawable.small_mono)
				.setAutoCancel(false).setOnlyAlertOnce(true).setOngoing(true)
				.setContentIntent(pendIntent);

		// the ticker text should only be shown the first time the notification is
		// created, not on each update
		notBuilder.setTicker("Tacere event starting: " + event.toString());

		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(NOTIFICATION_ID, notBuilder.build());
	}

	/**
	 * Builds and displays a notification for the given CalEvent. This method uses the new
	 * Notification API to place an action button in the notification.
	 * 
	 * @param event
	 *            the CalEvent that is currently active
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void displayNewEventNotification(CalEvent event) {
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
		nm.notify(NOTIFICATION_ID, notBuilder.build());
	}
}
