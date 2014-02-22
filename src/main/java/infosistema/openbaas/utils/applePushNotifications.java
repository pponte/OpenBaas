package infosistema.openbaas.utils;

import java.util.ArrayList;
import java.util.List;

import javapns.Push;
import javapns.communication.KeystoreManager;
import javapns.communication.exceptions.CommunicationException;
import javapns.communication.exceptions.KeystoreException;
import javapns.notification.AppleNotificationServer;
import javapns.notification.AppleNotificationServerBasicImpl;
import javapns.notification.Payload;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotification;

public class applePushNotifications {

	
	
	public void pushSimpleTestNotification(String keystore, String password, Boolean production, String token) throws CommunicationException, KeystoreException {
		
		verifyKeystore(keystore, password, production);
		List<PushedNotification> notifications = Push.test(keystore, password, production, token);
		printPushedNotifications(notifications);
		/*
		} else if (complex) {

			// Push a more complex payload 
			List<PushedNotification> notifications = Push.payload(createComplexPayload(), keystore, password, production, token);
			printPushedNotifications(notifications);

		} */
	}
	
	private void verifyKeystore(Object keystoreReference, String password, boolean production) {
		try {
			System.out.print("Validating keystore reference: ");
			KeystoreManager.validateKeystoreParameter(keystoreReference);
			System.out.println("VALID  (keystore was found)");
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (password != null) {
			try {
				System.out.print("Verifying keystore content: ");
				AppleNotificationServer server = new AppleNotificationServerBasicImpl(keystoreReference, password, production);
				KeystoreManager.verifyKeystoreContent(server, keystoreReference);
				System.out.println("VERIFIED  (no common mistakes detected)");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Create a complex payload for test purposes.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static Payload createComplexPayload() {
		PushNotificationPayload complexPayload = PushNotificationPayload.complex();
		try {
			// You can use addBody to add simple message, but we'll use
			// a more complex alert message so let's comment it
			complexPayload.addCustomAlertBody("My alert message");
			complexPayload.addCustomAlertActionLocKey("Open App");
			complexPayload.addCustomAlertLocKey("javapns rocks %@ %@%@");
			ArrayList parameters = new ArrayList();
			parameters.add("Test1");
			parameters.add("Test");
			parameters.add(2);
			complexPayload.addCustomAlertLocArgs(parameters);
			complexPayload.addBadge(45);
			complexPayload.addSound("default");
			complexPayload.addCustomDictionary("acme", "foo");
			complexPayload.addCustomDictionary("acme2", 42);
			ArrayList values = new ArrayList();
			values.add("value1");
			values.add(2);
			complexPayload.addCustomDictionary("acme3", values);
		} catch (Exception e) {
			System.out.println("Error creating complex payload:");
			e.printStackTrace();
		}
		return complexPayload;
	}

	/**
	 * Print to the console a comprehensive report of all pushed notifications and results.
	 * @param notifications a raw list of pushed notifications
	 */
	public static void printPushedNotifications(List<PushedNotification> notifications) {
		List<PushedNotification> failedNotifications = PushedNotification.findFailedNotifications(notifications);
		List<PushedNotification> successfulNotifications = PushedNotification.findSuccessfulNotifications(notifications);
		int failed = failedNotifications.size();
		int successful = successfulNotifications.size();

		if (successful > 0 && failed == 0) {
			printPushedNotifications("All notifications pushed successfully (" + successfulNotifications.size() + "):", successfulNotifications);
		} else if (successful == 0 && failed > 0) {
			printPushedNotifications("All notifications failed (" + failedNotifications.size() + "):", failedNotifications);
		} else if (successful == 0 && failed == 0) {
			System.out.println("No notifications could be sent, probably because of a critical error");
		} else {
			printPushedNotifications("Some notifications failed (" + failedNotifications.size() + "):", failedNotifications);
			printPushedNotifications("Others succeeded (" + successfulNotifications.size() + "):", successfulNotifications);
		}
	}
	
	/**
	 * Print to the console a list of pushed notifications.
	 * @param description a title for this list of notifications
	 * @param notifications a list of pushed notifications to print
	 */
	public static void printPushedNotifications(String description, List<PushedNotification> notifications) {
		System.out.println(description);
		for (PushedNotification notification : notifications) {
			try {
				System.out.println("  " + notification.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
