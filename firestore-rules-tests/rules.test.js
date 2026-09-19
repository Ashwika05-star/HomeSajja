const fs = require('fs');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { setLogLevel, doc, setDoc, getDoc, updateDoc, deleteDoc, collection, query, where, getDocs, orderBy, writeBatch } = require('firebase/firestore');

const RULES = require('path').join(__dirname, '..', 'firestore.rules');
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
  env = await initializeTestEnvironment({ projectId: 'demo-homesajja', firestore: { rules: fs.readFileSync(RULES, 'utf8'), host: '127.0.0.1', port: 8181 } });
  const now = Date.now();
  const as = (uid) => env.authenticatedContext(uid).firestore();
  const anon = env.unauthenticatedContext().firestore();

  // seed with rules disabled
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/alice'), { uid: 'alice', name: 'Alice' });
    await setDoc(doc(db, 'users/bob'), { uid: 'bob', name: 'Bob' });
    await setDoc(doc(db, 'vendors/vic'), { uid: 'vic', name: 'Vic', city: 'Mumbai', businessType: 'RECYCLER' });
    await setDoc(doc(db, 'listings/sell1'), { ownerId: 'bob', title: 'Sofa', price: 100, status: 'ACTIVE', actionType: 'SELL', images: [] });
    await setDoc(doc(db, 'listings/swap1'), { ownerId: 'bob', title: 'Chair', price: 0, status: 'ACTIVE', actionType: 'EXCHANGE', images: [] });
    await setDoc(doc(db, 'purchaseRequests/p1'), { buyerId: 'alice', sellerId: 'bob', listingId: 'sell1', status: 'REQUESTED', offeredPrice: 90 });
    await setDoc(doc(db, 'repairRequests/r1'), { userId: 'alice', vendorId: 'vic', status: 'REQUESTED' });
    await setDoc(doc(db, 'vendors/shopv'), { uid: 'shopv', name: 'Shop', city: 'Mumbai', businessType: 'SHOP' });
    await setDoc(doc(db, 'recyclingRequests/c9'), { userId: 'alice', vendorId: null, method: 'PICKUP', status: 'REQUESTED', city: 'Mumbai' });
    await setDoc(doc(db, 'recyclingRequests/c10'), { userId: 'alice', vendorId: null, method: 'PICKUP', status: 'REQUESTED', city: 'Pune' });
    await setDoc(doc(db, 'listings/alice1'), { ownerId: 'alice', title: 'Chair', price: 1, status: 'ACTIVE', actionType: 'SELL', images: [] });
    await setDoc(doc(db, 'materialRequests/m4'), { vendorId: 'vic', status: 'CLOSED', city: 'Mumbai' });
    await setDoc(doc(db, 'recyclingRequests/c1'), { userId: 'alice', vendorId: 'vic', method: 'DROP_OFF', status: 'REQUESTED' });
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
  await allow('user lists repair requests', getDocs(query(collection(as('alice'), 'repairRequests'), where('userId', '==', 'alice'))));
  await allow('vendor lists repair requests', getDocs(query(collection(as('vic'), 'repairRequests'), where('vendorId', '==', 'vic'))));
  await allow('user lists recycling requests', getDocs(query(collection(as('alice'), 'recyclingRequests'), where('userId', '==', 'alice'))));
  await allow('vendor lists recycling requests', getDocs(query(collection(as('vic'), 'recyclingRequests'), where('vendorId', '==', 'vic'))));
  await deny('user lists another user\'s repair requests', getDocs(query(collection(as('carol'), 'repairRequests'), where('userId', '==', 'alice'))));

  // exchange: creating and reading requests
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore(); const L = (owner, status, actionType) => ({ ownerId: owner, title: 'Item', price: 0, status, actionType, images: ['a.jpg'] });
    await setDoc(doc(db, 'listings/alice1'), L('alice', 'ACTIVE', 'SELL'));
    await setDoc(doc(db, 'listings/aliceReserved'), L('alice', 'RESERVED', 'SELL'));
    await setDoc(doc(db, 'listings/bobReserved'), L('bob', 'RESERVED', 'EXCHANGE'));
    const R = (status, offered, requested) => ({ senderId: 'alice', receiverId: 'bob', offeredListingId: offered, requestedListingId: requested, status, message: 'swap?' });
    await setDoc(doc(db, 'exchangeRequests/exRead'), R('PENDING', 'alice1', 'swap1'));
    await setDoc(doc(db, 'exchangeRequests/exAcc'), R('ACCEPTED', 'alice1', 'swap1'));
    await setDoc(doc(db, 'exchangeRequests/exPend1'), R('PENDING', 'alice1', 'swap1'));
    await setDoc(doc(db, 'exchangeRequests/exPend2'), R('PENDING', 'alice1', 'swap1'));
    await setDoc(doc(db, 'exchangeRequests/exPend3'), R('PENDING', 'alice1', 'swap1'));
    await setDoc(doc(db, 'exchangeRequests/exPend4'), R('PENDING', 'alice1', 'swap1'));
    await setDoc(doc(db, 'exchangeRequests/exAcc2'), R('ACCEPTED', 'alice1', 'swap1'));
  });
  const ex = { senderId: 'alice', receiverId: 'bob', offeredListingId: 'alice1', requestedListingId: 'swap1', status: 'PENDING', message: 'swap?', offeredTitle: 'Item', requestedTitle: 'Chair' };
  await allow('propose an exchange', setDoc(doc(as('alice'), 'exchangeRequests/e1'), ex));
  await deny('offer a listing that is not yours', setDoc(doc(as('alice'), 'exchangeRequests/e2'), { ...ex, offeredListingId: 'sell1' }));
  await deny('request a for-sale listing', setDoc(doc(as('alice'), 'exchangeRequests/e3'), { ...ex, requestedListingId: 'sell1' }));
  await deny('offer a listing that is not active', setDoc(doc(as('alice'), 'exchangeRequests/e4'), { ...ex, offeredListingId: 'aliceReserved' }));
  await deny('request a listing that is not active', setDoc(doc(as('alice'), 'exchangeRequests/e5'), { ...ex, requestedListingId: 'bobReserved' }));
  await deny('receiver does not own the requested listing', setDoc(doc(as('alice'), 'exchangeRequests/e6'), { ...ex, receiverId: 'carol' }));
  await deny('propose as someone else', setDoc(doc(as('carol'), 'exchangeRequests/e7'), ex));
  await deny('propose with status already accepted', setDoc(doc(as('alice'), 'exchangeRequests/e8'), { ...ex, status: 'ACCEPTED' }));
  await allow('sender reads exchange request', getDoc(doc(as('alice'), 'exchangeRequests/exRead')));
  await allow('receiver reads exchange request', getDoc(doc(as('bob'), 'exchangeRequests/exRead')));
  await deny('third party reads exchange request', getDoc(doc(as('carol'), 'exchangeRequests/exRead')));
  await allow('get of a missing exchange request returns empty', getDoc(doc(as('alice'), 'exchangeRequests/missing')));
  await allow('sender lists outgoing exchanges', getDocs(query(collection(as('alice'), 'exchangeRequests'), where('senderId', '==', 'alice'), orderBy('createdAt', 'desc'))));
  await allow('receiver lists incoming exchanges', getDocs(query(collection(as('bob'), 'exchangeRequests'), where('receiverId', '==', 'bob'))));
  await deny('listing another user\'s exchanges', getDocs(query(collection(as('carol'), 'exchangeRequests'), where('senderId', '==', 'alice'))));

  // exchange: status pipeline
  const U = (status) => ({ status, updatedAt: now });
  await allow('receiver accepts a pending request', updateDoc(doc(as('bob'), 'exchangeRequests/exPend1'), U('ACCEPTED')));
  await deny('sender accepts their own request', updateDoc(doc(as('alice'), 'exchangeRequests/exPend2'), U('ACCEPTED')));
  await allow('receiver declines a pending request', updateDoc(doc(as('bob'), 'exchangeRequests/exPend2'), U('DECLINED')));
  await allow('sender cancels a pending request', updateDoc(doc(as('alice'), 'exchangeRequests/exPend3'), U('CANCELLED')));
  await deny('receiver cancels', updateDoc(doc(as('bob'), 'exchangeRequests/exPend4'), U('CANCELLED')));
  await deny('sender cancels after acceptance', updateDoc(doc(as('alice'), 'exchangeRequests/exAcc'), U('CANCELLED')));
  await deny('receiver declines after acceptance', updateDoc(doc(as('bob'), 'exchangeRequests/exAcc'), U('DECLINED')));
  await deny('complete a request that is still pending', updateDoc(doc(as('bob'), 'exchangeRequests/exPend4'), U('COMPLETED')));
  await deny('third party completes', updateDoc(doc(as('carol'), 'exchangeRequests/exAcc'), U('COMPLETED')));
  await deny('edit the message', updateDoc(doc(as('bob'), 'exchangeRequests/exPend4'), { message: 'changed', updatedAt: now }));
  await allow('receiver completes an accepted request', updateDoc(doc(as('bob'), 'exchangeRequests/exAcc'), U('COMPLETED')));
  await allow('sender completes an accepted request', updateDoc(doc(as('alice'), 'exchangeRequests/exAcc2'), U('COMPLETED')));
  await deny('receiver deletes a request', deleteDoc(doc(as('bob'), 'exchangeRequests/exRead')));
  await allow('sender deletes a pending request', deleteDoc(doc(as('alice'), 'exchangeRequests/exRead')));
  await deny('sender deletes an accepted request', deleteDoc(doc(as('alice'), 'exchangeRequests/exAcc2')));

  // exchange: moving both listings together with the request
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore(); const L = (owner, status, extra = {}) => ({ ownerId: owner, title: 'Item', price: 100, status, actionType: 'EXCHANGE', images: ['a.jpg'], ...extra });
    for (const k of ['A', 'B', 'C', 'D']) {
      await setDoc(doc(db, `listings/mine${k}`), L('alice', 'ACTIVE'));
      await setDoc(doc(db, `listings/theirs${k}`), L('bob', 'ACTIVE'));
      await setDoc(doc(db, `exchangeRequests/pend${k}`), { senderId: 'alice', receiverId: 'bob', offeredListingId: `mine${k}`, requestedListingId: `theirs${k}`, status: 'PENDING' });
    }
    await setDoc(doc(db, 'listings/aliceOther'), L('alice', 'ACTIVE'));
    for (const k of ['E', 'F']) {
      await setDoc(doc(db, `listings/mine${k}`), L('alice', 'RESERVED', { exchangeRequestId: `acc${k}` }));
      await setDoc(doc(db, `listings/theirs${k}`), L('bob', 'RESERVED', { exchangeRequestId: `acc${k}` }));
      await setDoc(doc(db, `exchangeRequests/acc${k}`), { senderId: 'alice', receiverId: 'bob', offeredListingId: `mine${k}`, requestedListingId: `theirs${k}`, status: 'ACCEPTED' });
    }
  });
  const swapBatch = (uid, k, reqStatus, itemStatus, extraDoc) => {
    const db = as(uid); const b = writeBatch(db);
    b.update(doc(db, `exchangeRequests/${reqStatus === 'ACCEPTED' ? 'pend' : 'acc'}${k}`), { status: reqStatus, updatedAt: now });
    b.update(doc(db, `listings/mine${k}`), { status: itemStatus, exchangeRequestId: `${reqStatus === 'ACCEPTED' ? 'pend' : 'acc'}${k}`, updatedAt: now });
    b.update(doc(db, `listings/theirs${k}`), { status: itemStatus, exchangeRequestId: `${reqStatus === 'ACCEPTED' ? 'pend' : 'acc'}${k}`, updatedAt: now });
    if (extraDoc) b.update(doc(db, extraDoc), { status: itemStatus, exchangeRequestId: `pend${k}`, updatedAt: now });
    return b.commit();
  };
  await allow('accepting reserves both listings in one batch', swapBatch('bob', 'A', 'ACCEPTED', 'RESERVED'));
  await deny('a third party cannot accept and reserve', swapBatch('carol', 'B', 'ACCEPTED', 'RESERVED'));
  await deny('reserving one of the sender\'s listings that is not part of the request', swapBatch('bob', 'B', 'ACCEPTED', 'RESERVED', 'listings/aliceOther'));
  await deny('reserving the other party\'s listing while the request is still pending',
    updateDoc(doc(as('bob'), 'listings/mineC'), { status: 'RESERVED', exchangeRequestId: 'pendC', updatedAt: now }));
  await deny('changing the price of the other party\'s listing',
    updateDoc(doc(as('bob'), 'listings/mineD'), { price: 1, status: 'RESERVED', exchangeRequestId: 'pendD', updatedAt: now }));
  await deny('reserving without naming a request', updateDoc(doc(as('bob'), 'listings/mineD'), { status: 'RESERVED', updatedAt: now }));
  await allow('completing marks both listings exchanged in one batch', swapBatch('alice', 'E', 'COMPLETED', 'EXCHANGED'));
  await deny('marking the other party\'s listing exchanged while the request is only accepted',
    updateDoc(doc(as('alice'), 'listings/theirsF'), { status: 'EXCHANGED', exchangeRequestId: 'accF', updatedAt: now }));

  // repair & recycling
  await allow('user creates repair request', setDoc(doc(as('alice'), 'repairRequests/r2'), { userId: 'alice', vendorId: 'vic', status: 'REQUESTED' }));
  await deny('repair request to a non-vendor', setDoc(doc(as('alice'), 'repairRequests/r3'), { userId: 'alice', vendorId: 'bob', status: 'REQUESTED' }));
  await deny('vendor creates repair request', setDoc(doc(as('vic'), 'repairRequests/r4'), { userId: 'vic', vendorId: 'vic', status: 'REQUESTED' }));
  await allow('user creates repair request for own listing', setDoc(doc(as('bob'), 'repairRequests/r5'), { userId: 'bob', vendorId: 'vic', listingId: 'sell1', status: 'REQUESTED' }));
  await allow('vendor rejects a request', updateDoc(doc(as('vic'), 'repairRequests/r5'), { status: 'REJECTED', updatedAt: now }));
  await deny('repair request for someone else\'s listing', setDoc(doc(as('alice'), 'repairRequests/r6'), { userId: 'alice', vendorId: 'vic', listingId: 'sell1', status: 'REQUESTED' }));
  await deny('repair request that starts past REQUESTED', setDoc(doc(as('alice'), 'repairRequests/r7'), { userId: 'alice', vendorId: 'vic', status: 'ACCEPTED' }));
  await deny('vendor skips a stage', updateDoc(doc(as('vic'), 'repairRequests/r1'), { status: 'READY', updatedAt: now }));
  await deny('user accepts own request', updateDoc(doc(as('alice'), 'repairRequests/r1'), { status: 'ACCEPTED', updatedAt: now }));
  await deny('vendor edits the description', updateDoc(doc(as('vic'), 'repairRequests/r1'), { issueDescription: 'x', updatedAt: now }));
  await allow('vendor accepts', updateDoc(doc(as('vic'), 'repairRequests/r1'), { status: 'ACCEPTED', updatedAt: now }));
  await deny('vendor moves backwards', updateDoc(doc(as('vic'), 'repairRequests/r1'), { status: 'REQUESTED', updatedAt: now }));
  await allow('vendor starts work', updateDoc(doc(as('vic'), 'repairRequests/r1'), { status: 'IN_PROGRESS', updatedAt: now }));
  await deny('user cancels once work started', updateDoc(doc(as('alice'), 'repairRequests/r1'), { status: 'CANCELLED', updatedAt: now }));
  await deny('user marks repair completed', updateDoc(doc(as('alice'), 'repairRequests/r1'), { status: 'COMPLETED', updatedAt: now }));
  await allow('vendor marks ready', updateDoc(doc(as('vic'), 'repairRequests/r1'), { status: 'READY', updatedAt: now }));
  await allow('vendor completes', updateDoc(doc(as('vic'), 'repairRequests/r1'), { status: 'COMPLETED', updatedAt: now }));
  await deny('vendor reopens a completed job', updateDoc(doc(as('vic'), 'repairRequests/r1'), { status: 'READY', updatedAt: now }));
  await allow('user cancels a fresh request', updateDoc(doc(as('alice'), 'repairRequests/r2'), { status: 'CANCELLED', updatedAt: now }));
  const pickup = { userId: 'alice', vendorId: null, method: 'PICKUP', status: 'REQUESTED' };
  await allow('user requests a pickup with no vendor', setDoc(doc(as('alice'), 'recyclingRequests/c2'), pickup));
  await deny('pickup that names a vendor', setDoc(doc(as('alice'), 'recyclingRequests/c3'), { ...pickup, vendorId: 'vic' }));
  await allow('user requests a drop-off at a recycler', setDoc(doc(as('alice'), 'recyclingRequests/c4'), { ...pickup, method: 'DROP_OFF', vendorId: 'vic' }));
  await deny('drop-off with no vendor', setDoc(doc(as('alice'), 'recyclingRequests/c5'), { ...pickup, method: 'DROP_OFF' }));
  await deny('drop-off at a non-recycler vendor', setDoc(doc(as('alice'), 'recyclingRequests/c6'), { ...pickup, method: 'DROP_OFF', vendorId: 'shopv' }));
  await deny('vendor creates recycling request', setDoc(doc(as('vic'), 'recyclingRequests/c7'), { ...pickup, userId: 'vic' }));
  await deny('recycling request that starts past REQUESTED', setDoc(doc(as('alice'), 'recyclingRequests/c8'), { ...pickup, status: 'ACCEPTED' }));
  await deny('vendor skips a stage', updateDoc(doc(as('vic'), 'recyclingRequests/c1'), { status: 'SCHEDULED', updatedAt: now }));
  await deny('user accepts own request', updateDoc(doc(as('alice'), 'recyclingRequests/c1'), { status: 'ACCEPTED', updatedAt: now }));
  await allow('vendor accepts', updateDoc(doc(as('vic'), 'recyclingRequests/c1'), { status: 'ACCEPTED', updatedAt: now }));
  await allow('vendor schedules', updateDoc(doc(as('vic'), 'recyclingRequests/c1'), { status: 'SCHEDULED', updatedAt: now }));
  await deny('user cancels once scheduled', updateDoc(doc(as('alice'), 'recyclingRequests/c1'), { status: 'CANCELLED', updatedAt: now }));
  await allow('vendor completes', updateDoc(doc(as('vic'), 'recyclingRequests/c1'), { status: 'COMPLETED', updatedAt: now }));
  await deny('vendor reopens a completed job', updateDoc(doc(as('vic'), 'recyclingRequests/c1'), { status: 'SCHEDULED', updatedAt: now }));
  await allow('user cancels an unassigned pickup', updateDoc(doc(as('alice'), 'recyclingRequests/c2'), { status: 'CANCELLED', updatedAt: now }));
  await deny('another vendor grabs an unassigned pickup', updateDoc(doc(as('vic'), 'recyclingRequests/c9'), { status: 'ACCEPTED', updatedAt: now }));
  const open = () => getDocs(query(collection(as('vic'), 'recyclingRequests'), where('city', '==', 'Mumbai'), where('vendorId', '==', null), where('status', '==', 'REQUESTED')));
  await allow('recycler lists unclaimed pickups in their city', open());
  await deny('non-recycler vendor lists unclaimed pickups', getDocs(query(collection(as('shopv'), 'recyclingRequests'), where('city', '==', 'Mumbai'), where('vendorId', '==', null), where('status', '==', 'REQUESTED'))));
  await deny('user lists everyone\'s unclaimed pickups', getDocs(query(collection(as('bob'), 'recyclingRequests'), where('city', '==', 'Mumbai'), where('vendorId', '==', null), where('status', '==', 'REQUESTED'))));
  await deny('recycler claims a pickup in another city', updateDoc(doc(as('vic'), 'recyclingRequests/c10'), { vendorId: 'vic', vendorName: 'Vic', status: 'ACCEPTED', updatedAt: now }));
  await deny('non-recycler vendor claims a pickup', updateDoc(doc(as('shopv'), 'recyclingRequests/c9'), { vendorId: 'shopv', vendorName: 'Shop', status: 'ACCEPTED', updatedAt: now }));
  await deny('claim without accepting', updateDoc(doc(as('vic'), 'recyclingRequests/c9'), { vendorId: 'vic', vendorName: 'Vic', status: 'REQUESTED', updatedAt: now }));
  await deny('claim on behalf of another vendor', updateDoc(doc(as('vic'), 'recyclingRequests/c9'), { vendorId: 'shopv', vendorName: 'Shop', status: 'ACCEPTED', updatedAt: now }));
  await allow('recycler claims a pickup in their city', updateDoc(doc(as('vic'), 'recyclingRequests/c9'), { vendorId: 'vic', vendorName: 'Vic', status: 'ACCEPTED', updatedAt: now }));
  await deny('claim an already claimed pickup', updateDoc(doc(as('vic'), 'recyclingRequests/c9'), { vendorId: 'vic', vendorName: 'Vic', status: 'ACCEPTED', updatedAt: now }));

  // material requests
  await allow('vendor posts material request', setDoc(doc(as('vic'), 'materialRequests/m2'), { vendorId: 'vic', status: 'OPEN', city: 'Mumbai' }));
  await deny('user posts material request', setDoc(doc(as('alice'), 'materialRequests/m3'), { vendorId: 'alice', status: 'OPEN', city: 'Mumbai' }));
  await allow('user browses open material requests', getDocs(query(collection(as('alice'), 'materialRequests'), where('city', '==', 'Mumbai'))));
  const offer = (uid, listingId) => ({ id: uid, requestId: 'm2', offererId: uid, offererName: uid, listingId, listingTitle: 't', status: 'PENDING' });
  const offerDoc = (ctx, uid, req = 'm2') => doc(ctx, `materialRequests/${req}/offers/${uid}`);
  await allow('user offers their own listing', setDoc(offerDoc(as('bob'), 'bob'), offer('bob', 'sell1')));
  await deny('offer with someone else\'s listing', setDoc(offerDoc(as('alice'), 'alice'), offer('alice', 'sell1')));
  await deny('offer filed under another uid', setDoc(offerDoc(as('alice'), 'bob2'), { ...offer('bob2', 'alice1'), offererId: 'bob2' }));
  await deny('vendor makes an offer', setDoc(offerDoc(as('vic'), 'vic'), offer('vic', 'sell1')));
  await deny('offer on a closed request', setDoc(offerDoc(as('bob'), 'bob', 'm4'), offer('bob', 'sell1')));
  await deny('offer that starts accepted', setDoc(offerDoc(as('alice'), 'alice'), { ...offer('alice', 'alice1'), status: 'ACCEPTED' }));
  await allow('vendor lists offers on their request', getDocs(collection(as('vic'), 'materialRequests/m2/offers')));
  await deny('user lists offers on a request', getDocs(collection(as('alice'), 'materialRequests/m2/offers')));
  await allow('offerer reads their own offer', getDoc(offerDoc(as('bob'), 'bob')));
  await deny('user reads someone else\'s offer', getDoc(offerDoc(as('alice'), 'bob')));
  await allow('user checks for an offer they never made', getDoc(offerDoc(as('alice'), 'alice')));
  await deny('offerer accepts their own offer', updateDoc(offerDoc(as('bob'), 'bob'), { status: 'ACCEPTED', updatedAt: now }));
  await deny('vendor edits the offered listing', updateDoc(offerDoc(as('vic'), 'bob'), { listingId: 'swap1', updatedAt: now }));
  await allow('vendor accepts an offer', updateDoc(offerDoc(as('vic'), 'bob'), { status: 'ACCEPTED', updatedAt: now }));
  await deny('vendor changes a decided offer', updateDoc(offerDoc(as('vic'), 'bob'), { status: 'DECLINED', updatedAt: now }));
  await deny('offerer withdraws a decided offer', deleteDoc(offerDoc(as('bob'), 'bob')));
  await allow('a second user offers', setDoc(offerDoc(as('alice'), 'alice'), offer('alice', 'alice1')));
  await allow('offerer withdraws a pending offer', deleteDoc(offerDoc(as('alice'), 'alice')));

  // vendor profile: the verified badge is not self-service
  await deny('vendor verifies themselves', updateDoc(doc(as('vic'), 'vendors/vic'), { verified: true }));
  await allow('vendor edits their profile', updateDoc(doc(as('vic'), 'vendors/vic'), { description: 'We recycle', shopLatitude: 19.1, shopLongitude: 72.8, brochureImages: ['u'] }));
  await deny('new vendor created already verified', setDoc(doc(as('newv'), 'vendors/newv'), { uid: 'newv', verified: true }));
  await allow('new vendor created unverified', setDoc(doc(as('newv2'), 'vendors/newv2'), { uid: 'newv2', verified: false }));
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
  await allow('participant stamps their own read time', updateDoc(doc(as('alice'), 'chats/ch1'), { 'readAt.alice': now }));
  await allow('other participant stamps theirs', updateDoc(doc(as('bob'), 'chats/ch1'), { 'readAt.bob': now }));
  await deny('participant stamps someone else\'s read time', updateDoc(doc(as('alice'), 'chats/ch1'), { 'readAt.bob': now + 5000 }));
  await deny('non-participant stamps read time', updateDoc(doc(as('carol'), 'chats/ch1'), { 'readAt.carol': now }));
  await deny('message over 2000 characters', setDoc(doc(as('alice'), 'chats/ch1/messages/m8'), { senderId: 'alice', text: 'x'.repeat(2001) }));
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
  const note = { recipientId: 'bob', senderId: 'alice', type: 'NEW_MESSAGE', title: 't', seen: false };
  await deny('notification created already seen', setDoc(doc(as('alice'), 'notifications/n4'), { ...note, seen: true }));
  await deny('notification to yourself', setDoc(doc(as('alice'), 'notifications/n5'), { ...note, recipientId: 'alice' }));
  await allow('listing-published notification to yourself', setDoc(doc(as('alice'), 'notifications/n6'), { ...note, recipientId: 'alice', type: 'LISTING_PUBLISHED' }));

  // device tokens
  await allow('register own device token', setDoc(doc(as('alice'), 'deviceTokens/tok1'), { token: 'tok1', userId: 'alice' }));
  await deny('register a token for someone else', setDoc(doc(as('alice'), 'deviceTokens/tok2'), { token: 'tok2', userId: 'bob' }));
  await deny('token document id must match the token', setDoc(doc(as('alice'), 'deviceTokens/tok3'), { token: 'other', userId: 'alice' }));
  await allow('owner reads their token', getDoc(doc(as('alice'), 'deviceTokens/tok1')));
  await deny('someone else reads the token', getDoc(doc(as('bob'), 'deviceTokens/tok1')));
  await allow('owner removes their token', deleteDoc(doc(as('alice'), 'deviceTokens/tok1')));

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

  console.log(`\n${passed} passed, ${failed} failed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error(e); process.exit(2); });
