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

  // Data-only message: the app builds the alert itself (see HomeSajjaMessagingService), so it looks the
  // same whether the app is in the background or in the foreground.
  const result = await getMessaging().sendEachForMulticast({
    tokens,
    data: {
      notificationId: event.params.notificationId,
      title: String(notification.title || ''),
      body: String(notification.body || ''),
      relatedType: String(notification.relatedType || ''),
      relatedId: String(notification.relatedId || ''),
    },
    android: { priority: 'high' },
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
