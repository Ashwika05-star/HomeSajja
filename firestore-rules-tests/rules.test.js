const fs = require('fs');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { ref, uploadBytes, deleteObject, getBytes } = require('firebase/storage');
const { setLogLevel, doc, setDoc, getDoc, updateDoc, deleteDoc, collection, query, where, getDocs, orderBy, writeBatch } = require('firebase/firestore');

const RULES = require('path').join(__dirname, '..', 'firestore.rules');
const STORAGE_RULES = require('path').join(__dirname, '..', 'storage.rules');
setLogLevel('silent');
let env, passed = 0, failed = 0;

async function check(name, promise, expectOk) {
  try {
    await (expectOk ? assertSucceeds(promise) : assertFails(promise));
    passed++;
  } catch (e) {
    failed++;
    console.log(`FAIL  ${name} (expected ${expectOk ? 'allow' : 'deny'})`);
  }
}
const allow = (n, p) => check(n, p, true);
const deny = (n, p) => check(n, p, false);

(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-homesajja', firestore: { rules: fs.readFileSync(RULES, 'utf8'), host: '127.0.0.1', port: 8181 },
    storage: { rules: fs.readFileSync(STORAGE_RULES, 'utf8'), host: '127.0.0.1', port: 9299 } });
  const now = Date.now();
  const as = (uid) => env.authenticatedContext(uid).firestore();
  const anon = env.unauthenticatedContext().firestore();

  // seed with rules disabled
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/alice'), { uid: 'alice', name: 'Alice' });
    await setDoc(doc(db, 'users/bob'), { uid: 'bob', name: 'Bob' });
    await setDoc(doc(db, 'vendors/vic'), { uid: 'vic', name: 'Vic', city: 'Mumbai' });
    await setDoc(doc(db, 'listings/sell1'), { ownerId: 'bob', title: 'Sofa', price: 100, status: 'ACTIVE', actionType: 'SELL', images: [] });
    await setDoc(doc(db, 'listings/swap1'), { ownerId: 'bob', title: 'Chair', price: 0, status: 'ACTIVE', actionType: 'EXCHANGE', images: [] });
    await setDoc(doc(db, 'purchaseRequests/p1'), { buyerId: 'alice', sellerId: 'bob', listingId: 'sell1', status: 'REQUESTED', offeredPrice: 90 });
    await setDoc(doc(db, 'repairRequests/r1'), { userId: 'alice', vendorId: 'vic', status: 'REQUESTED', quotedPrice: null });
    await setDoc(doc(db, 'recyclingRequests/c1'), { userId: 'alice', vendorId: 'vic', status: 'REQUESTED', pickupDate: null });
    await setDoc(doc(db, 'chats/ch1'), { participantIds: ['alice', 'bob'], lastMessage: '', lastMessageAt: 0, lastMessageSenderId: '' });
    await setDoc(doc(db, 'notifications/n1'), { recipientId: 'alice', senderId: 'bob', title: 't', seen: false });
    await setDoc(doc(db, 'reviews/alice_REPAIR_REQUEST_r1'), { reviewerId: 'alice', targetUserId: 'vic', contextType: 'REPAIR_REQUEST', contextId: 'r1', rating: 4, comment: 'ok' });
    await setDoc(doc(db, 'materialRequests/m1'), { vendorId: 'vic', status: 'OPEN', city: 'Mumbai' });
  });

  // users / vendors
  await allow('user creates own doc', setDoc(doc(as('carol'), 'users/carol'), { uid: 'carol' }));
  await deny('user creates someone else\'s doc', setDoc(doc(as('carol'), 'users/dave'), { uid: 'dave' }));
  await deny('vendor uid cannot also create a users doc', setDoc(doc(as('vic'), 'users/vic'), { uid: 'vic' }));
  await deny('user cannot also create a vendors doc', setDoc(doc(as('alice'), 'vendors/alice'), { uid: 'alice' }));
  await allow('read own user doc', getDoc(doc(as('alice'), 'users/alice')));
  await deny('read another user doc', getDoc(doc(as('bob'), 'users/alice')));
  await allow('signed-in reads vendor', getDoc(doc(as('alice'), 'vendors/vic')));
  await deny('anonymous reads vendor', getDoc(doc(anon, 'vendors/vic')));
  await allow('vendor discovery query', getDocs(query(collection(as('alice'), 'vendors'), where('city', '==', 'Mumbai'))));

  // listings
  await allow('signed-in reads listing', getDoc(doc(as('alice'), 'listings/sell1')));
  await deny('anonymous reads listing', getDoc(doc(anon, 'listings/sell1')));
  await allow('owner creates listing', setDoc(doc(as('alice'), 'listings/new1'), { ownerId: 'alice', title: 'Desk', price: 50, status: 'ACTIVE', images: ['a.jpg'] }));
  await deny('create listing for another owner', setDoc(doc(as('alice'), 'listings/new2'), { ownerId: 'bob', title: 'Desk', price: 50, status: 'ACTIVE', images: ['a.jpg'] }));
  await deny('listing with no photos', setDoc(doc(as('alice'), 'listings/new4'), { ownerId: 'alice', title: 'Desk', price: 50, status: 'ACTIVE', images: [] }));
  await deny('listing with too many photos', setDoc(doc(as('alice'), 'listings/new5'), { ownerId: 'alice', title: 'Desk', price: 50, status: 'ACTIVE', images: ['1','2','3','4','5','6'] }));
  await deny('negative price', setDoc(doc(as('alice'), 'listings/new3'), { ownerId: 'alice', title: 'Desk', price: -5, status: 'ACTIVE', images: ['a.jpg'] }));
  await deny('non-owner edits listing', updateDoc(doc(as('alice'), 'listings/sell1'), { price: 1 }));
  await deny('owner reassigns listing', updateDoc(doc(as('bob'), 'listings/sell1'), { ownerId: 'alice' }));
  await allow('owner edits listing', updateDoc(doc(as('bob'), 'listings/sell1'), { price: 120 }));
  await deny('non-owner deletes listing', deleteDoc(doc(as('alice'), 'listings/swap1')));
  await allow('owner deletes listing', deleteDoc(doc(as('bob'), 'listings/swap1')));
  await env.withSecurityRulesDisabled(async (c) => setDoc(doc(c.firestore(), 'listings/swap1'), { ownerId: 'bob', title: 'Chair', price: 0, status: 'ACTIVE', actionType: 'EXCHANGE', images: [] }));

  // purchase requests
  const pr = { buyerId: 'alice', sellerId: 'bob', listingId: 'sell1', status: 'REQUESTED', offeredPrice: 80 };
  await allow('buyer creates purchase request', setDoc(doc(as('alice'), 'purchaseRequests/p2'), pr));
  await deny('purchase request naming wrong seller', setDoc(doc(as('alice'), 'purchaseRequests/p3'), { ...pr, sellerId: 'carol' }));
  await deny('purchase request as someone else', setDoc(doc(as('carol'), 'purchaseRequests/p4'), pr));
  await deny('purchase request pre-accepted', setDoc(doc(as('alice'), 'purchaseRequests/p5'), { ...pr, status: 'ACCEPTED' }));
  await allow('buyer reads own request', getDoc(doc(as('alice'), 'purchaseRequests/p1')));
  await allow('seller reads request', getDoc(doc(as('bob'), 'purchaseRequests/p1')));
  await deny('third party reads request', getDoc(doc(as('carol'), 'purchaseRequests/p1')));
  await allow('get of a missing request returns empty', getDoc(doc(as('alice'), 'purchaseRequests/missing')));
  await allow('buyer lists own requests', getDocs(query(collection(as('alice'), 'purchaseRequests'), where('buyerId', '==', 'alice'))));
  await deny('listing another user\'s requests', getDocs(query(collection(as('carol'), 'purchaseRequests'), where('buyerId', '==', 'alice'))));
  await deny('unfiltered request list', getDocs(collection(as('alice'), 'purchaseRequests')));
  await allow('seller accepts', updateDoc(doc(as('bob'), 'purchaseRequests/p1'), { status: 'ACCEPTED', updatedAt: now }));
  await deny('seller edits price', updateDoc(doc(as('bob'), 'purchaseRequests/p1'), { offeredPrice: 1 }));
  await deny('buyer accepts own request', updateDoc(doc(as('alice'), 'purchaseRequests/p1'), { status: 'ACCEPTED', updatedAt: now }));
  await allow('buyer cancels', updateDoc(doc(as('alice'), 'purchaseRequests/p1'), { status: 'CANCELLED', updatedAt: now }));
  await deny('third party updates request', updateDoc(doc(as('carol'), 'purchaseRequests/p1'), { status: 'REJECTED', updatedAt: now }));

  await allow('seller lists received requests', getDocs(query(collection(as('bob'), 'purchaseRequests'), where('sellerId', '==', 'bob'), orderBy('createdAt', 'desc'))));
  await allow('requester lists exchange proposals', getDocs(query(collection(as('alice'), 'exchangeRequests'), where('requesterId', '==', 'alice'))));
  await allow('owner lists exchange proposals', getDocs(query(collection(as('bob'), 'exchangeRequests'), where('ownerId', '==', 'bob'))));
  await allow('user lists repair requests', getDocs(query(collection(as('alice'), 'repairRequests'), where('userId', '==', 'alice'))));
  await allow('vendor lists repair requests', getDocs(query(collection(as('vic'), 'repairRequests'), where('vendorId', '==', 'vic'))));
  await allow('user lists recycling requests', getDocs(query(collection(as('alice'), 'recyclingRequests'), where('userId', '==', 'alice'))));
  await allow('vendor lists recycling requests', getDocs(query(collection(as('vic'), 'recyclingRequests'), where('vendorId', '==', 'vic'))));
  await deny('user lists another user\'s repair requests', getDocs(query(collection(as('carol'), 'repairRequests'), where('userId', '==', 'alice'))));

  // exchange
  const ex = { requesterId: 'alice', ownerId: 'bob', targetListingId: 'swap1', status: 'PENDING' };
  await allow('exchange proposal on EXCHANGE listing', setDoc(doc(as('alice'), 'exchangeRequests/e1'), ex));
  await deny('exchange proposal on SELL listing', setDoc(doc(as('alice'), 'exchangeRequests/e2'), { ...ex, targetListingId: 'sell1' }));

  // repair & recycling
  await allow('user creates repair request', setDoc(doc(as('alice'), 'repairRequests/r2'), { userId: 'alice', vendorId: 'vic', status: 'REQUESTED' }));
  await deny('repair request to a non-vendor', setDoc(doc(as('alice'), 'repairRequests/r3'), { userId: 'alice', vendorId: 'bob', status: 'REQUESTED' }));
  await deny('vendor creates repair request', setDoc(doc(as('vic'), 'repairRequests/r4'), { userId: 'vic', vendorId: 'vic', status: 'REQUESTED' }));
  await allow('vendor quotes', updateDoc(doc(as('vic'), 'repairRequests/r1'), { quotedPrice: 500, status: 'QUOTED', updatedAt: now }));
  await allow('user accepts quote', updateDoc(doc(as('alice'), 'repairRequests/r1'), { status: 'ACCEPTED', updatedAt: now }));
  await deny('user changes the quote', updateDoc(doc(as('alice'), 'repairRequests/r1'), { quotedPrice: 1, updatedAt: now }));
  await deny('user marks repair completed', updateDoc(doc(as('alice'), 'repairRequests/r1'), { status: 'COMPLETED', updatedAt: now }));
  await allow('vendor schedules pickup', updateDoc(doc(as('vic'), 'recyclingRequests/c1'), { pickupDate: now, status: 'PICKUP_SCHEDULED', updatedAt: now }));
  await deny('user schedules own pickup', updateDoc(doc(as('alice'), 'recyclingRequests/c1'), { pickupDate: now, updatedAt: now }));
  await allow('user cancels recycling', updateDoc(doc(as('alice'), 'recyclingRequests/c1'), { status: 'CANCELLED', updatedAt: now }));

  // material requests
  await allow('vendor posts material request', setDoc(doc(as('vic'), 'materialRequests/m2'), { vendorId: 'vic', status: 'OPEN', city: 'Mumbai' }));
  await deny('user posts material request', setDoc(doc(as('alice'), 'materialRequests/m3'), { vendorId: 'alice', status: 'OPEN', city: 'Mumbai' }));
  await allow('user browses open material requests', getDocs(query(collection(as('alice'), 'materialRequests'), where('city', '==', 'Mumbai'))));
  await deny('user edits material request', updateDoc(doc(as('alice'), 'materialRequests/m1'), { status: 'CLOSED' }));
  await allow('owning vendor closes material request', updateDoc(doc(as('vic'), 'materialRequests/m1'), { status: 'CLOSED' }));

  // chats
  await allow('get of a missing chat returns empty', getDoc(doc(as('alice'), 'chats/none')));
  await allow('create chat with two participants', setDoc(doc(as('alice'), 'chats/ch2'), { participantIds: ['alice', 'carol'] }));
  await deny('create chat without self', setDoc(doc(as('alice'), 'chats/ch3'), { participantIds: ['bob', 'carol'] }));
  await deny('non-participant reads chat', getDoc(doc(as('carol'), 'chats/ch1')));
  await allow('inbox query', getDocs(query(collection(as('alice'), 'chats'), where('participantIds', 'array-contains', 'alice'))));
  await deny('inbox query for someone else', getDocs(query(collection(as('carol'), 'chats'), where('participantIds', 'array-contains', 'alice'))));
  await allow('participant sends message', setDoc(doc(as('alice'), 'chats/ch1/messages/m1'), { senderId: 'alice', text: 'hi' }));
  await deny('message with spoofed sender', setDoc(doc(as('alice'), 'chats/ch1/messages/m2'), { senderId: 'bob', text: 'hi' }));
  await deny('non-participant sends message', setDoc(doc(as('carol'), 'chats/ch1/messages/m3'), { senderId: 'carol', text: 'hi' }));
  await allow('participant reads messages', getDocs(collection(as('bob'), 'chats/ch1/messages')));
  await deny('non-participant reads messages', getDocs(collection(as('carol'), 'chats/ch1/messages')));
  await allow('update last-message preview', updateDoc(doc(as('alice'), 'chats/ch1'), { lastMessage: 'hi', lastMessageAt: now, lastMessageSenderId: 'alice' }));
  await deny('change chat participants', updateDoc(doc(as('alice'), 'chats/ch1'), { participantIds: ['alice', 'carol'] }));
  const bobDb = as('bob');
  const batch = writeBatch(bobDb);
  batch.set(doc(bobDb, 'chats/ch1/messages/m9'), { senderId: 'bob', text: 'yo' });
  batch.update(doc(bobDb, 'chats/ch1'), { lastMessage: 'yo', lastMessageAt: now, lastMessageSenderId: 'bob' });
  await allow('sendMessage batch (message + preview)', batch.commit());
  await allow('sender deletes own message', deleteDoc(doc(as('alice'), 'chats/ch1/messages/m1')));
  await deny('delete chat', deleteDoc(doc(as('alice'), 'chats/ch1')));

  // notifications
  await allow('send notification as self', setDoc(doc(as('alice'), 'notifications/n2'), { recipientId: 'bob', senderId: 'alice', title: 't', seen: false }));
  await deny('notification with spoofed sender', setDoc(doc(as('alice'), 'notifications/n3'), { recipientId: 'bob', senderId: 'carol', title: 't' }));
  await allow('recipient reads notifications', getDocs(query(collection(as('alice'), 'notifications'), where('recipientId', '==', 'alice'))));
  await deny('other user reads notification', getDoc(doc(as('bob'), 'notifications/n1')));
  await allow('recipient marks seen', updateDoc(doc(as('alice'), 'notifications/n1'), { seen: true }));
  await deny('recipient edits notification text', updateDoc(doc(as('alice'), 'notifications/n1'), { title: 'x' }));
  await allow('recipient deletes notification', deleteDoc(doc(as('alice'), 'notifications/n1')));

  // reviews
  const rv = { reviewerId: 'bob', targetUserId: 'vic', contextType: 'REPAIR_REQUEST', contextId: 'r9', rating: 5, comment: 'great' };
  await allow('review with matching id', setDoc(doc(as('bob'), 'reviews/bob_REPAIR_REQUEST_r9'), rv));
  await deny('review with arbitrary id', setDoc(doc(as('bob'), 'reviews/random'), rv));
  await deny('rating out of range', setDoc(doc(as('bob'), 'reviews/bob_REPAIR_REQUEST_r10'), { ...rv, contextId: 'r10', rating: 6 }));
  await deny('review yourself', setDoc(doc(as('bob'), 'reviews/bob_REPAIR_REQUEST_r11'), { ...rv, contextId: 'r11', targetUserId: 'bob' }));
  await deny('review as someone else', setDoc(doc(as('carol'), 'reviews/bob_REPAIR_REQUEST_r12'), { ...rv, contextId: 'r12' }));
  await allow('read reviews', getDocs(query(collection(as('carol'), 'reviews'), where('targetUserId', '==', 'vic'))));
  await allow('reviewer edits comment', updateDoc(doc(as('alice'), 'reviews/alice_REPAIR_REQUEST_r1'), { comment: 'better', rating: 5, updatedAt: now }));
  await deny('reviewer retargets review', updateDoc(doc(as('alice'), 'reviews/alice_REPAIR_REQUEST_r1'), { targetUserId: 'bob' }));
  await deny('someone else deletes review', deleteDoc(doc(as('bob'), 'reviews/alice_REPAIR_REQUEST_r1')));

  // favourites
  await allow('add favourite', setDoc(doc(as('alice'), 'favourites/alice_sell1'), { userId: 'alice', listingId: 'sell1' }));
  await deny('favourite with wrong id', setDoc(doc(as('alice'), 'favourites/whatever'), { userId: 'alice', listingId: 'sell1' }));
  await deny('favourite for someone else', setDoc(doc(as('carol'), 'favourites/alice_swap1'), { userId: 'alice', listingId: 'swap1' }));
  await allow('isFavourite on missing doc', getDoc(doc(as('alice'), 'favourites/alice_none')));
  await allow('list own favourites', getDocs(query(collection(as('alice'), 'favourites'), where('userId', '==', 'alice'))));
  await deny('list another user\'s favourites', getDocs(query(collection(as('carol'), 'favourites'), where('userId', '==', 'alice'))));
  await allow('remove favourite', deleteDoc(doc(as('alice'), 'favourites/alice_sell1')));

  // storage (listing photos)
  const st = (uid) => env.authenticatedContext(uid).storage();
  const photo = (s, path) => ref(s, path);
  const jpeg = { contentType: 'image/jpeg' };
  await allow('owner uploads a listing photo', uploadBytes(photo(st('alice'), 'listings/alice/l1/p1'), new Uint8Array([1, 2, 3]), jpeg));
  await deny('upload into someone else\'s folder', uploadBytes(photo(st('bob'), 'listings/alice/l1/p2'), new Uint8Array([1]), jpeg));
  await deny('upload a non-image', uploadBytes(photo(st('alice'), 'listings/alice/l1/p3'), new Uint8Array([1]), { contentType: 'text/plain' }));
  await deny('upload over 10 MB', uploadBytes(photo(st('alice'), 'listings/alice/l1/p4'), new Uint8Array(11 * 1024 * 1024), jpeg));
  await deny('upload outside listings/', uploadBytes(photo(st('alice'), 'other/alice/x'), new Uint8Array([1]), jpeg));
  await allow('signed-in user reads a photo', getBytes(photo(st('bob'), 'listings/alice/l1/p1')));
  await deny('anonymous reads a photo', getBytes(photo(env.unauthenticatedContext().storage(), 'listings/alice/l1/p1')));
  await deny('non-owner deletes a photo', deleteObject(photo(st('bob'), 'listings/alice/l1/p1')));
  await allow('owner deletes a photo', deleteObject(photo(st('alice'), 'listings/alice/l1/p1')));

  console.log(`\n${passed} passed, ${failed} failed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error(e); process.exit(2); });
