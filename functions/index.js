// Optional server side of push notifications. The Android app already writes a `notifications/{id}`
// document for every event (a message, a request update...). This function watches that collection and
// sends an FCM push to each of the recipient's phones, so they are alerted even when the app is closed.
//
// Deploying Cloud Functions needs the Blaze (pay-as-you-go) plan, so it is not deployed yet:
//   firebase deploy --only functions --project <project id>
// The app works without it: notifications still appear inside the app, and as system alerts while the app runs.

const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { initializeApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');

initializeApp();

exports.pushOnNotification = onDocumentCreated('notifications/{notificationId}', async (event) => {
  const notification = event.data && event.data.data();
  if (!notification || !notification.recipientId) return;

  const db = getFirestore();
  const snapshot = await db.collection('deviceTokens').where('userId', '==', notification.recipientId).get();
  const tokens = snapshot.docs.map((doc) => doc.id);
  if (tokens.length === 0) return;

  const isChat = String(notification.relatedType || '').toUpperCase() === 'CHAT';
  const channelId = isChat ? 'chat_messages' : 'activity';

  // Dual notification + data payload:
  // - When the app is closed/killed, Android displays the notification directly in the system tray with heads-up alert.
  // - When tapped, Android passes the data payload to MainActivity so NotificationRouting opens the exact chat/request screen.
  // - When the app is open/foreground, HomeSajjaMessagingService receives it and shows the alert unless the user is already inside that thread.
  const result = await getMessaging().sendEachForMulticast({
    tokens,
    notification: {
      title: String(notification.title || (isChat ? 'New Message' : 'HomeSajja Notification')),
      body: String(notification.body || ''),
    },
    data: {
      notificationId: String(event.params.notificationId || ''),
      title: String(notification.title || ''),
      body: String(notification.body || ''),
      relatedType: String(notification.relatedType || ''),
      relatedId: String(notification.relatedId || ''),
    },
    android: {
      priority: 'high',
      notification: {
        channelId,
        sound: 'default',
        priority: 'high',
        defaultSound: true,
        defaultVibrateTimings: true,
      },
    },
  });

  // Forget phones whose token is no longer valid (app uninstalled, token rotated...).
  const stale = [];
  result.responses.forEach((response, index) => {
    const code = response.error && response.error.code;
    if (code === 'messaging/registration-token-not-registered' || code === 'messaging/invalid-registration-token') {
      stale.push(tokens[index]);
    }
  });
  await Promise.all(stale.map((token) => db.collection('deviceTokens').doc(token).delete()));
});
