import org.ciasaboark.tacere.CalEvent;
import org.ciasaboark.tacere.MainActivity;
import org.ciasaboark.tacere.manager.NotificationManagerWrapper;
import org.junit.Before;
import org.junit.Test;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.test.AndroidTestCase;

public class NotificationManagerWrapperTest extends AndroidTestCase {
	private NotificationManagerWrapper nmWrapper = new NotificationManagerWrapper(this.getContext().getApplicationContext());

	@Before
	public void setup() {
	}

	@Test
	public void testCancelEventNotification() {
		Context context = this.getContext();
		CalEvent event = new CalEvent(this.getContext());
		event.setTitle("test event");
		event.setId(999);

		// display the notification
		nmWrapper.displayEventNotification(event);
		// remove the notification
		nmWrapper.cancelAllNotifications();

		Intent notificationIntent = new Intent(context, MainActivity.class);
		PendingIntent test = PendingIntent.getActivity(context, nmWrapper.getNotificationId(),
				notificationIntent, PendingIntent.FLAG_NO_CREATE);
		assert (test == null);
	}

	@Test
	public void testCancelQuicksilenceNotification() {
		Context context = this.getContext();
		// display the notification
		nmWrapper.displayQuickSilenceNotification(10);
		// remove the notification
		nmWrapper.cancelAllNotifications();

		Intent notificationIntent = new Intent(context, MainActivity.class);
		PendingIntent test = PendingIntent.getActivity(context, nmWrapper.getNotificationId(),
				notificationIntent, PendingIntent.FLAG_NO_CREATE);
		assert (test == null);
	}

	@Test
	public void testCancelAllNotificationsNoNotificationActive() {
		Context context = this.getContext();

		nmWrapper.cancelAllNotifications();

		Intent notificationIntent = new Intent(context, MainActivity.class);
		PendingIntent test = PendingIntent.getActivity(context, nmWrapper.getNotificationId(),
				notificationIntent, PendingIntent.FLAG_NO_CREATE);
		assert (test == null);
	}

	@Test
	public void testDisplayQuicksilenceNotification() {
		Context context = this.getContext();
		CalEvent event = new CalEvent(this.getContext());
		event.setTitle("test event");
		event.setId(999);

		nmWrapper.displayQuickSilenceNotification(10);

		Intent notificationIntent = new Intent(context, MainActivity.class);
		PendingIntent test = PendingIntent.getActivity(context, nmWrapper.getNotificationId(),
				notificationIntent, PendingIntent.FLAG_NO_CREATE);
		assert (test != null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDisplayQuickSilenceNotficationNegativeDuration() {
		nmWrapper.displayQuickSilenceNotification(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDisplayQuickSilenceNotficationZeroDuration() {
		nmWrapper.displayQuickSilenceNotification(0);
	}

	@Test
	public void testDisplayEventNotification() {
		Context context = this.getContext();
		CalEvent event = new CalEvent(this.getContext());
		event.setTitle("test event");
		event.setId(999);

		nmWrapper.displayEventNotification(event);

		Intent notificationIntent = new Intent(context, MainActivity.class);
		PendingIntent test = PendingIntent.getActivity(context, nmWrapper.getNotificationId(),
				notificationIntent, PendingIntent.FLAG_NO_CREATE);
		assert (test != null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDisplayEventNotificationNullEvent() {
		nmWrapper.displayEventNotification(null);
	}

}
