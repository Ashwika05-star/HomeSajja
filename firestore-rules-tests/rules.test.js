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

  // Agreement helpers: a payment record as the app writes it, and a quote.
  const P = (payer, payee, upi, extra = {}) => ({ payerId: payer, payeeId: payee, payeeUpiId: upi, method: null, status: 'UNPAID', upiRef: null, markedPaidAt: null, confirmedAt: null, ...extra });
  const Q = (extra = {}) => ({ amount: 5000, estimatedDays: 3, note: 'Fix the leg', payeeUpiId: 'vic@okhdfcbank', direction: null, revision: 1, sentAt: now, ...extra });
  const RQ = (extra = {}) => Q({ estimatedDays: null, direction: 'USER_PAYS_VENDOR', ...extra });
  const agreedPurchase = (extra) => ({ buyerId: 'alice', sellerId: 'bob', listingId: 'sell1', status: 'ACCEPTED', offeredPrice: 90, agreedAmount: 90, agreedAt: now, ...extra });

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
    // purchases that are already agreed, in each payment state
    await setDoc(doc(db, 'purchaseRequests/ppay'), agreedPurchase({ payment: P('alice', 'bob', 'bob@okhdfcbank') }));
    await setDoc(doc(db, 'purchaseRequests/ppay2'), agreedPurchase({ payment: P('alice', 'bob', null) }));
    await setDoc(doc(db, 'purchaseRequests/ppay3'), agreedPurchase({ payment: P('alice', 'bob', 'bob@okhdfcbank', { status: 'MARKED_PAID', method: 'UPI', upiRef: 'T123', markedPaidAt: now }) }));
    await setDoc(doc(db, 'purchaseRequests/ppay4'), agreedPurchase({ payment: P('alice', 'bob', null, { status: 'MARKED_PAID', method: 'CASH', markedPaidAt: now }) }));
    await setDoc(doc(db, 'purchaseRequests/ppay5'), agreedPurchase({ payment: P('alice', 'bob', null, { status: 'CONFIRMED', method: 'CASH', markedPaidAt: now, confirmedAt: now }) }));
    await setDoc(doc(db, 'purchaseRequests/ppay6'), agreedPurchase({ status: 'READY_FOR_PICKUP', payment: P('alice', 'bob', null) }));
    await setDoc(doc(db, 'purchaseRequests/ppay7'), agreedPurchase({ payment: P('alice', 'bob', null) }));
    await setDoc(doc(db, 'purchaseRequests/pnew'), { buyerId: 'alice', sellerId: 'bob', listingId: 'sell1', status: 'REQUESTED', offeredPrice: 90 });
    await setDoc(doc(db, 'purchaseRequests/pnew2'), { buyerId: 'alice', sellerId: 'bob', listingId: 'sell1', status: 'REQUESTED', offeredPrice: 90 });
    await setDoc(doc(db, 'purchaseRequests/pnew3'), { buyerId: 'alice', sellerId: 'bob', listingId: 'sell1', status: 'REQUESTED', offeredPrice: 90 });
    await setDoc(doc(db, 'purchaseRequests/pfree'), { buyerId: 'alice', sellerId: 'bob', listingId: 'sell1', status: 'REQUESTED', offeredPrice: 0 });
    // repairs: a quote waiting for the customer, a declined one, a legacy "accepted" one
    await setDoc(doc(db, 'repairRequests/rq1'), { userId: 'alice', vendorId: 'vic', status: 'QUOTED', quote: Q() });
    await setDoc(doc(db, 'repairRequests/rq2'), { userId: 'alice', vendorId: 'vic', status: 'QUOTED', quote: Q() });
    await setDoc(doc(db, 'repairRequests/rdecl'), { userId: 'alice', vendorId: 'vic', status: 'DECLINED', quote: Q() });
    await setDoc(doc(db, 'repairRequests/rdecl2'), { userId: 'alice', vendorId: 'vic', status: 'DECLINED', quote: Q() });
    await setDoc(doc(db, 'repairRequests/rleg'), { userId: 'alice', vendorId: 'vic', status: 'ACCEPTED' });
    await setDoc(doc(db, 'repairRequests/ragreed'), { userId: 'alice', vendorId: 'vic', status: 'AGREED', quote: Q(), agreedAmount: 5000, agreedAt: now, payment: P('alice', 'vic', 'vic@okhdfcbank') });
    await setDoc(doc(db, 'repairRequests/rpaid'), { userId: 'alice', vendorId: 'vic', status: 'IN_PROGRESS', quote: Q(), agreedAmount: 5000, agreedAt: now, payment: P('alice', 'vic', 'vic@okhdfcbank', { status: 'MARKED_PAID', method: 'UPI', markedPaidAt: now }) });
    await setDoc(doc(db, 'repairRequests/rcancelled'), { userId: 'alice', vendorId: 'vic', status: 'CANCELLED', quote: Q(), agreedAmount: 5000, agreedAt: now, payment: P('alice', 'vic', null) });
    // recycling quotes: waiting for the customer (customer pays / recycler pays), declined, and agreed
    await setDoc(doc(db, 'recyclingRequests/cq1'), { userId: 'alice', vendorId: 'vic', method: 'DROP_OFF', status: 'QUOTED', quote: RQ({ amount: 800 }) });
    await setDoc(doc(db, 'recyclingRequests/cq2'), { userId: 'alice', vendorId: 'vic', method: 'DROP_OFF', status: 'QUOTED', quote: RQ({ amount: 800, direction: 'VENDOR_PAYS_USER', payeeUpiId: null }) });
    await setDoc(doc(db, 'recyclingRequests/cq3'), { userId: 'alice', vendorId: 'vic', method: 'DROP_OFF', status: 'QUOTED', quote: RQ({ amount: 800 }) });
    await setDoc(doc(db, 'recyclingRequests/cdecl'), { userId: 'alice', vendorId: 'vic', method: 'DROP_OFF', status: 'DECLINED', quote: RQ({ amount: 800 }) });
    await setDoc(doc(db, 'recyclingRequests/cdecl2'), { userId: 'alice', vendorId: 'vic', method: 'DROP_OFF', status: 'DECLINED', quote: RQ({ amount: 800 }) });
    await setDoc(doc(db, 'recyclingRequests/cagreed'), { userId: 'alice', vendorId: 'vic', method: 'DROP_OFF', status: 'ACCEPTED', quote: RQ({ amount: 800 }), agreedAmount: 800, agreedAt: now, payment: P('alice', 'vic', 'vic@okhdfcbank') });
    await setDoc(doc(db, 'recyclingRequests/cpayout'), { userId: 'alice', vendorId: 'vic', method: 'DROP_OFF', status: 'ACCEPTED', quote: RQ({ amount: 800, direction: 'VENDOR_PAYS_USER' }), agreedAmount: 800, agreedAt: now, payment: P('vic', 'alice', 'alice@ybl') });
    await setDoc(doc(db, 'recyclingRequests/cpaid'), { userId: 'alice', vendorId: 'vic', method: 'DROP_OFF', status: 'ACCEPTED', quote: RQ({ amount: 800 }), agreedAmount: 800, agreedAt: now, payment: P('alice', 'vic', null, { status: 'MARKED_PAID', method: 'CASH', markedPaidAt: now }) });
    await setDoc(doc(db, 'recyclingRequests/c11'), { userId: 'alice', vendorId: 'vic', method: 'DROP_OFF', status: 'REQUESTED' });
    await setDoc(doc(db, 'recyclingRequests/c12'), { userId: 'alice', vendorId: 'vic', method: 'DROP_OFF', status: 'REQUESTED' });
    await setDoc(doc(db, 'recyclingRequests/c13'), { userId: 'alice', vendorId: null, method: 'PICKUP', status: 'REQUESTED', city: 'Mumbai' });
    await setDoc(doc(db, 'recyclingRequests/c14'), { userId: 'alice', vendorId: null, method: 'PICKUP', status: 'REQUESTED', city: 'Mumbai' });
    await setDoc(doc(db, 'repairRequests/rdone'), { userId: 'bob', vendorId: 'vic', status: 'COMPLETED' });
    await setDoc(doc(db, 'purchaseRequests/pdone'), { buyerId: 'bob', sellerId: 'alice', listingId: 'sell1', status: 'COMPLETED' });
    await setDoc(doc(db, 'exchangeRequests/edone'), { senderId: 'alice', receiverId: 'bob', status: 'COMPLETED' });
    await setDoc(doc(db, 'recyclingRequests/cdone'), { userId: 'bob', vendorId: 'vic', status: 'COMPLETED' });
    await setDoc(doc(db, 'reviews/alice_REPAIR_REQUEST_r1'), { reviewerId: 'alice', targetUserId: 'vic', contextType: 'REPAIR_REQUEST', contextId: 'r1', rating: 4, comment: 'ok' });
    await setDoc(doc(db, 'materialRequests/m1'), { vendorId: 'vic', status: 'OPEN', city: 'Mumbai' });
  });

  // users / vendors
  await allow('user creates own doc', setDoc(doc(as('carol'), 'users/carol'), { uid: 'carol' }));
  await deny('user creates someone else\'s doc', setDoc(doc(as('carol'), 'users/dave'), { uid: 'dave' }));
  await deny('vendor uid cannot also create a users doc', setDoc(doc(as('vic'), 'users/vic'), { uid: 'vic' }));
  await deny('user cannot also create a vendors doc', setDoc(doc(as('alice'), 'vendors/alice'), { uid: 'alice' }));
  await allow('user saves a UPI id', setDoc(doc(as('carol'), 'users/carol'), { uid: 'carol', upiId: 'carol@okhdfcbank' }));
  await deny('user saves a malformed UPI id', setDoc(doc(as('carol'), 'users/carol'), { uid: 'carol', upiId: 'not a upi id' }));
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
  await allow('owner marks own listing sold', updateDoc(doc(as('bob'), 'listings/sell1'), { status: 'SOLD', updatedAt: now }));
  await allow('owner reopens it again', updateDoc(doc(as('bob'), 'listings/sell1'), { status: 'ACTIVE', updatedAt: now }));
  await allow('owner deletes listing', deleteDoc(doc(as('bob'), 'listings/swap1')));
  await env.withSecurityRulesDisabled(async (c) => setDoc(doc(c.firestore(), 'listings/swap1'), { ownerId: 'bob', title: 'Chair', price: 0, status: 'ACTIVE', actionType: 'EXCHANGE', images: [] }));

  // purchase requests
  const pr = { buyerId: 'alice', sellerId: 'bob', listingId: 'sell1', status: 'REQUESTED', offeredPrice: 80 };
  await allow('buyer creates purchase request', setDoc(doc(as('alice'), 'purchaseRequests/p2'), pr));
  await deny('purchase request on your own listing', setDoc(doc(as('alice'), 'purchaseRequests/pself'), { ...pr, sellerId: 'alice', listingId: 'alice1' }));
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
  await deny('seller accepts without fixing the amount', updateDoc(doc(as('bob'), 'purchaseRequests/p1'), { status: 'ACCEPTED', updatedAt: now }));
  await deny('seller skips straight to completed', updateDoc(doc(as('bob'), 'purchaseRequests/p1'), { status: 'COMPLETED', updatedAt: now }));
  await allow('seller rejects', updateDoc(doc(as('bob'), 'purchaseRequests/pnew2'), { status: 'REJECTED', updatedAt: now }));
  await deny('seller edits price', updateDoc(doc(as('bob'), 'purchaseRequests/p1'), { offeredPrice: 1 }));
  await deny('buyer accepts own request', updateDoc(doc(as('alice'), 'purchaseRequests/p1'), { status: 'ACCEPTED', updatedAt: now }));
  await deny('buyer cancels after the seller accepted', updateDoc(doc(as('alice'), 'purchaseRequests/ppay'), { status: 'CANCELLED', updatedAt: now }));
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
  await deny('exchange with your own listing', setDoc(doc(as('alice'), 'exchangeRequests/e7b'), { ...ex, receiverId: 'alice', requestedListingId: 'alice1' }));
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
  await deny('repair request that starts past REQUESTED', setDoc(doc(as('alice'), 'repairRequests/r7'), { userId: 'alice', vendorId: 'vic', status: 'AGREED' }));
  await deny('repair request that arrives already agreed', setDoc(doc(as('alice'), 'repairRequests/r7b'), { userId: 'alice', vendorId: 'vic', status: 'REQUESTED', agreedAmount: 1 }));
  await deny('repair request that arrives with a quote', setDoc(doc(as('alice'), 'repairRequests/r7c'), { userId: 'alice', vendorId: 'vic', status: 'REQUESTED', quote: Q() }));
  await deny('vendor skips a stage', updateDoc(doc(as('vic'), 'repairRequests/r1'), { status: 'READY', updatedAt: now }));
  await deny('vendor cannot accept without a quote', updateDoc(doc(as('vic'), 'repairRequests/r1'), { status: 'ACCEPTED', updatedAt: now }));
  await deny('vendor cannot start work before the user agrees', updateDoc(doc(as('vic'), 'repairRequests/r1'), { status: 'IN_PROGRESS', updatedAt: now }));
  await deny('vendor cannot mark a request agreed', updateDoc(doc(as('vic'), 'repairRequests/r1'), { status: 'AGREED', updatedAt: now }));
  await deny('user accepts own request', updateDoc(doc(as('alice'), 'repairRequests/r1'), { status: 'ACCEPTED', updatedAt: now }));
  await deny('vendor edits the description', updateDoc(doc(as('vic'), 'repairRequests/r1'), { issueDescription: 'x', updatedAt: now }));

  // quote: only the vendor sends it, and it must be complete
  const quoted = (q, extra = {}) => ({ status: 'QUOTED', quote: q, updatedAt: now, ...extra });
  await deny('user sends the quote', updateDoc(doc(as('alice'), 'repairRequests/r1'), quoted(Q())));
  await deny('quote without an estimate', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q({ estimatedDays: null }))));
  await deny('quote with a zero estimate', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q({ estimatedDays: 0 }))));
  await deny('quote of zero rupees', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q({ amount: 0 }))));
  await deny('quote of a negative amount', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q({ amount: -5 }))));
  await deny('quote above the cap', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q({ amount: 1000001 }))));
  await deny('quote with a fractional amount', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q({ amount: 99.5 }))));
  await deny('quote with a very long note', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q({ note: 'x'.repeat(501) }))));
  await deny('quote with a malformed upi id', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q({ payeeUpiId: 'not a upi id' }))));
  await deny('repair quote with a payment direction', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q({ direction: 'USER_PAYS_VENDOR' }))));
  await deny('first quote that is not revision 1', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q({ revision: 2 }))));
  await deny('quote with an unknown field', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted({ ...Q(), discount: 10 })));
  await deny('quote that also agrees the amount', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q(), { agreedAmount: 5000 })));
  await allow('vendor sends a quote', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q())));
  await deny('vendor changes the quote while it waits for an answer', updateDoc(doc(as('vic'), 'repairRequests/r1'), quoted(Q({ amount: 100, revision: 2 }))));
  await deny('vendor starts work on an unanswered quote', updateDoc(doc(as('vic'), 'repairRequests/r1'), { status: 'IN_PROGRESS', updatedAt: now }));

  // the user answers the quote
  const agree = (id, extra = {}) => ({ status: 'AGREED', agreedAmount: 5000, agreedAt: now, payment: P('alice', 'vic', 'vic@okhdfcbank'), updatedAt: now, ...extra });
  await deny('vendor accepts their own quote', updateDoc(doc(as('vic'), 'repairRequests/rq1'), agree()));
  await deny('outsider accepts the quote', updateDoc(doc(as('carol'), 'repairRequests/rq1'), agree()));
  await deny('user agrees to a different amount', updateDoc(doc(as('alice'), 'repairRequests/rq1'), agree('', { agreedAmount: 10 })));
  await deny('user accepts without opening the payment', updateDoc(doc(as('alice'), 'repairRequests/rq1'), { status: 'AGREED', agreedAmount: 5000, agreedAt: now, updatedAt: now }));
  await deny('payment names the wrong payer', updateDoc(doc(as('alice'), 'repairRequests/rq1'), agree('', { payment: P('vic', 'alice', 'vic@okhdfcbank') })));
  await deny('payment sends the money to the customer', updateDoc(doc(as('alice'), 'repairRequests/rq1'), agree('', { payment: P('alice', 'alice', 'vic@okhdfcbank') })));
  await deny('payment swaps in another upi id', updateDoc(doc(as('alice'), 'repairRequests/rq1'), agree('', { payment: P('alice', 'vic', 'alice@okhdfcbank') })));
  await deny('payment that starts out paid', updateDoc(doc(as('alice'), 'repairRequests/rq1'), agree('', { payment: P('alice', 'vic', 'vic@okhdfcbank', { status: 'CONFIRMED' }) })));
  await deny('user accepts and edits the quote', updateDoc(doc(as('alice'), 'repairRequests/rq1'), agree('', { quote: Q({ amount: 10 }) })));
  await allow('user accepts the quote: amount agreed, payment opened', updateDoc(doc(as('alice'), 'repairRequests/rq1'), agree()));
  await deny('user declines a quote they already accepted', updateDoc(doc(as('alice'), 'repairRequests/rq1'), { status: 'DECLINED', updatedAt: now }));
  await deny('vendor re-quotes after the agreement', updateDoc(doc(as('vic'), 'repairRequests/rq1'), quoted(Q({ amount: 9000, revision: 2 }))));
  await deny('vendor changes the agreed amount', updateDoc(doc(as('vic'), 'repairRequests/rq1'), { agreedAmount: 9000, updatedAt: now }));
  await deny('user changes the agreed amount', updateDoc(doc(as('alice'), 'repairRequests/rq1'), { agreedAmount: 1, updatedAt: now }));
  await allow('user declines a quote', updateDoc(doc(as('alice'), 'repairRequests/rq2'), { status: 'DECLINED', updatedAt: now }));
  await deny('vendor declines on behalf of the user', updateDoc(doc(as('vic'), 'repairRequests/rq2'), { status: 'DECLINED', updatedAt: now }));
  await deny('revised quote with the same revision', updateDoc(doc(as('vic'), 'repairRequests/rdecl'), quoted(Q({ revision: 1 }))));
  await deny('user sends the revised quote', updateDoc(doc(as('alice'), 'repairRequests/rdecl'), quoted(Q({ revision: 2 }))));
  await allow('vendor sends a revised quote', updateDoc(doc(as('vic'), 'repairRequests/rdecl'), quoted(Q({ amount: 4000, revision: 2 }))));
  await allow('vendor closes a declined request', updateDoc(doc(as('vic'), 'repairRequests/rdecl2'), { status: 'REJECTED', updatedAt: now }));
  await deny('user closes a declined request as rejected', updateDoc(doc(as('alice'), 'repairRequests/rdecl'), { status: 'REJECTED', updatedAt: now }));
  await allow('vendor starts a legacy accepted job', updateDoc(doc(as('vic'), 'repairRequests/rleg'), { status: 'IN_PROGRESS', updatedAt: now }));

  // after the agreement the vendor works through the job; the customer pays and the vendor confirms
  await deny('vendor moves backwards', updateDoc(doc(as('vic'), 'repairRequests/rq1'), { status: 'REQUESTED', updatedAt: now }));
  await allow('vendor starts work', updateDoc(doc(as('vic'), 'repairRequests/rq1'), { status: 'IN_PROGRESS', updatedAt: now }));
  await deny('user cancels once work started', updateDoc(doc(as('alice'), 'repairRequests/rq1'), { status: 'CANCELLED', updatedAt: now }));
  await deny('user marks repair completed', updateDoc(doc(as('alice'), 'repairRequests/rq1'), { status: 'COMPLETED', updatedAt: now }));
  const rpay = (status, extra = {}) => ({ payment: { ...P('alice', 'vic', 'vic@okhdfcbank'), status, ...extra }, updatedAt: now });
  await deny('vendor marks the customer\'s payment as paid', updateDoc(doc(as('vic'), 'repairRequests/rq1'), rpay('MARKED_PAID', { method: 'CASH', markedPaidAt: now })));
  await deny('vendor confirms before the customer paid', updateDoc(doc(as('vic'), 'repairRequests/rq1'), rpay('CONFIRMED', { confirmedAt: now })));
  await allow('customer marks the repair paid', updateDoc(doc(as('alice'), 'repairRequests/rq1'), rpay('MARKED_PAID', { method: 'UPI', upiRef: 'UTR998877', markedPaidAt: now })));
  await deny('customer confirms their own payment', updateDoc(doc(as('alice'), 'repairRequests/rq1'), rpay('CONFIRMED', { method: 'UPI', upiRef: 'UTR998877', markedPaidAt: now, confirmedAt: now })));
  await allow('vendor confirms the payment arrived', updateDoc(doc(as('vic'), 'repairRequests/rq1'), rpay('CONFIRMED', { method: 'UPI', upiRef: 'UTR998877', markedPaidAt: now, confirmedAt: now })));
  await deny('payment cannot be changed after confirmation', updateDoc(doc(as('alice'), 'repairRequests/rq1'), rpay('MARKED_PAID', { method: 'CASH', markedPaidAt: now })));
  await deny('user cancels an agreed job once a payment is marked', updateDoc(doc(as('alice'), 'repairRequests/rpaid'), { status: 'CANCELLED', updatedAt: now }));
  await deny('paying for a cancelled repair', updateDoc(doc(as('alice'), 'repairRequests/rcancelled'), rpay('MARKED_PAID', { method: 'CASH', markedPaidAt: now })));
  await allow('vendor marks ready', updateDoc(doc(as('vic'), 'repairRequests/rq1'), { status: 'READY', updatedAt: now }));
  await allow('vendor completes', updateDoc(doc(as('vic'), 'repairRequests/rq1'), { status: 'COMPLETED', updatedAt: now }));
  await deny('vendor reopens a completed job', updateDoc(doc(as('vic'), 'repairRequests/rq1'), { status: 'READY', updatedAt: now }));
  await allow('user cancels an agreed job before work starts', updateDoc(doc(as('alice'), 'repairRequests/ragreed'), { status: 'CANCELLED', updatedAt: now }));
  await allow('user cancels a request with a revised quote waiting', updateDoc(doc(as('alice'), 'repairRequests/rdecl'), { status: 'CANCELLED', updatedAt: now }));
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
  // recycling quotes (optional): amount and direction
  const rq = (q, extra = {}) => ({ status: 'QUOTED', quote: q, updatedAt: now, ...extra });
  const rAgree = (payment, extra = {}) => ({ status: 'ACCEPTED', agreedAmount: 800, agreedAt: now, payment, updatedAt: now, ...extra });
  await deny('recycling request that arrives with a quote', setDoc(doc(as('alice'), 'recyclingRequests/c15'), { userId: 'alice', vendorId: null, method: 'PICKUP', status: 'REQUESTED', city: 'Mumbai', quote: RQ() }));
  await deny('customer sends the recycling quote', updateDoc(doc(as('alice'), 'recyclingRequests/c11'), rq(RQ())));
  await deny('recycling quote without a direction', updateDoc(doc(as('vic'), 'recyclingRequests/c11'), rq(RQ({ direction: null }))));
  await deny('recycling quote with an unknown direction', updateDoc(doc(as('vic'), 'recyclingRequests/c11'), rq(RQ({ direction: 'SPLIT' }))));
  await deny('recycling quote of zero', updateDoc(doc(as('vic'), 'recyclingRequests/c11'), rq(RQ({ amount: 0 }))));
  await deny('recycling quote above the cap', updateDoc(doc(as('vic'), 'recyclingRequests/c11'), rq(RQ({ amount: 1000001 }))));
  await deny('recycling quote with an out-of-range estimate', updateDoc(doc(as('vic'), 'recyclingRequests/c11'), rq(RQ({ estimatedDays: 400 }))));
  await allow('recycler sends a quote the customer pays', updateDoc(doc(as('vic'), 'recyclingRequests/c11'), rq(RQ({ amount: 800 }))));
  await allow('recycler quotes a payout to the customer', updateDoc(doc(as('vic'), 'recyclingRequests/c12'), rq(RQ({ amount: 300, direction: 'VENDOR_PAYS_USER', payeeUpiId: null }))));
  await deny('recycler cannot accept their own quote', updateDoc(doc(as('vic'), 'recyclingRequests/cq1'), rAgree(P('alice', 'vic', 'vic@okhdfcbank'))));
  await deny('customer agrees to a different amount', updateDoc(doc(as('alice'), 'recyclingRequests/cq1'), rAgree(P('alice', 'vic', 'vic@okhdfcbank'), { agreedAmount: 1 })));
  await deny('customer accepts without opening the payment', updateDoc(doc(as('alice'), 'recyclingRequests/cq1'), { status: 'ACCEPTED', agreedAmount: 800, agreedAt: now, updatedAt: now }));
  await deny('payment goes the wrong way for a customer-pays quote', updateDoc(doc(as('alice'), 'recyclingRequests/cq1'), rAgree(P('vic', 'alice', 'alice@ybl'))));
  await deny('customer-pays payment with a swapped upi id', updateDoc(doc(as('alice'), 'recyclingRequests/cq1'), rAgree(P('alice', 'vic', 'alice@ybl'))));
  await allow('customer accepts a quote and pays the recycler', updateDoc(doc(as('alice'), 'recyclingRequests/cq1'), rAgree(P('alice', 'vic', 'vic@okhdfcbank'))));
  await deny('payment goes the wrong way for a payout quote', updateDoc(doc(as('alice'), 'recyclingRequests/cq2'), rAgree(P('alice', 'vic', null))));
  await deny('payout payment with a malformed upi id', updateDoc(doc(as('alice'), 'recyclingRequests/cq2'), rAgree(P('vic', 'alice', 'nope'))));
  await allow('customer accepts a payout quote and says where to send it', updateDoc(doc(as('alice'), 'recyclingRequests/cq2'), rAgree(P('vic', 'alice', 'alice@ybl'))));
  await deny('customer re-edits the recycling quote after accepting', updateDoc(doc(as('alice'), 'recyclingRequests/cq1'), { quote: RQ({ amount: 1 }), updatedAt: now }));
  await deny('recycler changes the agreed recycling amount', updateDoc(doc(as('vic'), 'recyclingRequests/cq1'), { agreedAmount: 5, updatedAt: now }));
  await allow('customer declines a recycling quote', updateDoc(doc(as('alice'), 'recyclingRequests/cq3'), { status: 'DECLINED', updatedAt: now }));
  await deny('recycler declines for the customer', updateDoc(doc(as('vic'), 'recyclingRequests/cq3'), { status: 'DECLINED', updatedAt: now }));
  await deny('revised recycling quote with the wrong revision', updateDoc(doc(as('vic'), 'recyclingRequests/cdecl'), rq(RQ({ revision: 1 }))));
  await allow('recycler sends a revised quote', updateDoc(doc(as('vic'), 'recyclingRequests/cdecl'), rq(RQ({ amount: 600, revision: 2 }))));
  await allow('recycler closes a declined request', updateDoc(doc(as('vic'), 'recyclingRequests/cdecl2'), { status: 'REJECTED', updatedAt: now }));
  await deny('recycler accepts a declined request for free', updateDoc(doc(as('vic'), 'recyclingRequests/cdecl2'), { status: 'ACCEPTED', updatedAt: now }));
  const rpayTo = (payer, payee, upi, status, extra = {}) => ({ payment: { ...P(payer, payee, upi), status, ...extra }, updatedAt: now });
  await deny('recycler marks the customer\'s payment as paid', updateDoc(doc(as('vic'), 'recyclingRequests/cagreed'), rpayTo('alice', 'vic', 'vic@okhdfcbank', 'MARKED_PAID', { method: 'CASH', markedPaidAt: now })));
  await allow('customer marks the recycling fee paid', updateDoc(doc(as('alice'), 'recyclingRequests/cagreed'), rpayTo('alice', 'vic', 'vic@okhdfcbank', 'MARKED_PAID', { method: 'UPI', upiRef: 'UTR1', markedPaidAt: now })));
  await deny('customer confirms the recycler\'s receipt', updateDoc(doc(as('alice'), 'recyclingRequests/cagreed'), rpayTo('alice', 'vic', 'vic@okhdfcbank', 'CONFIRMED', { method: 'UPI', upiRef: 'UTR1', markedPaidAt: now, confirmedAt: now })));
  await allow('recycler confirms receipt', updateDoc(doc(as('vic'), 'recyclingRequests/cagreed'), rpayTo('alice', 'vic', 'vic@okhdfcbank', 'CONFIRMED', { method: 'UPI', upiRef: 'UTR1', markedPaidAt: now, confirmedAt: now })));
  await deny('customer marks a payout as paid (they are the payee)', updateDoc(doc(as('alice'), 'recyclingRequests/cpayout'), rpayTo('vic', 'alice', 'alice@ybl', 'MARKED_PAID', { method: 'UPI', markedPaidAt: now })));
  await allow('recycler marks the payout paid (they are the payer)', updateDoc(doc(as('vic'), 'recyclingRequests/cpayout'), rpayTo('vic', 'alice', 'alice@ybl', 'MARKED_PAID', { method: 'UPI', upiRef: 'UTR2', markedPaidAt: now })));
  await deny('recycler confirms their own payout', updateDoc(doc(as('vic'), 'recyclingRequests/cpayout'), rpayTo('vic', 'alice', 'alice@ybl', 'CONFIRMED', { method: 'UPI', upiRef: 'UTR2', markedPaidAt: now, confirmedAt: now })));
  await allow('customer confirms they received the payout', updateDoc(doc(as('alice'), 'recyclingRequests/cpayout'), rpayTo('vic', 'alice', 'alice@ybl', 'CONFIRMED', { method: 'UPI', upiRef: 'UTR2', markedPaidAt: now, confirmedAt: now })));
  await deny('customer cancels once a payment is marked', updateDoc(doc(as('alice'), 'recyclingRequests/cpaid'), { status: 'CANCELLED', updatedAt: now }));
  await allow('customer cancels a quoted request', updateDoc(doc(as('alice'), 'recyclingRequests/cdecl'), { status: 'CANCELLED', updatedAt: now }));
  // claiming an unassigned pickup with a quote
  const claimQuote = (q, extra = {}) => ({ vendorId: 'vic', vendorName: 'Vic', status: 'QUOTED', quote: q, updatedAt: now, ...extra });
  await deny('non-recycler claims a pickup with a quote', updateDoc(doc(as('shopv'), 'recyclingRequests/c13'), claimQuote(RQ(), { vendorId: 'shopv' })));
  await deny('claim with a quote but still REQUESTED', updateDoc(doc(as('vic'), 'recyclingRequests/c13'), claimQuote(RQ(), { status: 'REQUESTED' })));
  await deny('claim with a quote that has no direction', updateDoc(doc(as('vic'), 'recyclingRequests/c13'), claimQuote(RQ({ direction: null }))));
  await deny('claim with a quote and an agreed amount', updateDoc(doc(as('vic'), 'recyclingRequests/c13'), claimQuote(RQ(), { agreedAmount: 800 })));
  await allow('recycler claims a pickup with a quote', updateDoc(doc(as('vic'), 'recyclingRequests/c13'), claimQuote(RQ({ amount: 500 }))));
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
  await allow('vendor saves a UPI id', updateDoc(doc(as('vic'), 'vendors/vic'), { upiId: 'vic@okhdfcbank' }));
  await deny('vendor saves a malformed UPI id', updateDoc(doc(as('vic'), 'vendors/vic'), { upiId: 'vic' }));
  await allow('vendor edits their profile', updateDoc(doc(as('vic'), 'vendors/vic'), { description: 'We recycle', shopLatitude: 19.1, shopLongitude: 72.8, brochureImages: ['u'] }));
  await deny('new vendor created already verified', setDoc(doc(as('newv'), 'vendors/newv'), { uid: 'newv', verified: true }));
  await allow('new vendor created unverified', setDoc(doc(as('newv2'), 'vendors/newv2'), { uid: 'newv2', verified: false }));
  await deny('user edits material request', updateDoc(doc(as('alice'), 'materialRequests/m1'), { status: 'CLOSED' }));
  await allow('owning vendor closes material request', updateDoc(doc(as('vic'), 'materialRequests/m1'), { status: 'CLOSED' }));

  // chats
  await allow('get of a missing chat returns empty', getDoc(doc(as('alice'), 'chats/none')));
  await allow('create chat with two participants', setDoc(doc(as('alice'), 'chats/ch2'), { participantIds: ['alice', 'carol'] }));
  await deny('create chat without self', setDoc(doc(as('alice'), 'chats/ch3'), { participantIds: ['bob', 'carol'] }));
  await deny('chat with yourself', setDoc(doc(as('alice'), 'chats/ch3b'), { participantIds: ['alice', 'alice'] }));
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
  const rv = { reviewerId: 'bob', targetUserId: 'vic', contextType: 'REPAIR_REQUEST', contextId: 'rdone', rating: 5, comment: 'great' };
  await allow('review a completed repair', setDoc(doc(as('bob'), 'reviews/bob_REPAIR_REQUEST_rdone'), rv));
  await deny('review with arbitrary id', setDoc(doc(as('bob'), 'reviews/random'), rv));
  await deny('rating out of range', setDoc(doc(as('bob'), 'reviews/bob_REPAIR_REQUEST_rdone'), { ...rv, rating: 6 }));
  await deny('review yourself', setDoc(doc(as('bob'), 'reviews/bob_REPAIR_REQUEST_rdone'), { ...rv, targetUserId: 'bob' }));
  await deny('review as someone else', setDoc(doc(as('carol'), 'reviews/bob_REPAIR_REQUEST_rdone'), rv));
  await deny('review of a transaction that is not completed', setDoc(doc(as('alice'), 'reviews/alice_REPAIR_REQUEST_r2'), { ...rv, reviewerId: 'alice', contextId: 'r2' }));
  await deny('review that never happened', setDoc(doc(as('bob'), 'reviews/bob_REPAIR_REQUEST_ghost'), { ...rv, contextId: 'ghost' }));
  await deny('review about the wrong person', setDoc(doc(as('bob'), 'reviews/bob_REPAIR_REQUEST_rdone'), { ...rv, targetUserId: 'alice' }));
  await deny('vendor reviews the customer of a repair', setDoc(doc(as('vic'), 'reviews/vic_REPAIR_REQUEST_rdone'), { ...rv, reviewerId: 'vic', targetUserId: 'bob' }));
  await deny('comment over 500 characters', setDoc(doc(as('bob'), 'reviews/bob_REPAIR_REQUEST_rdone'), { ...rv, comment: 'x'.repeat(501) }));
  await allow('buyer reviews the seller of a purchase', setDoc(doc(as('bob'), 'reviews/bob_PURCHASE_REQUEST_pdone'), { reviewerId: 'bob', targetUserId: 'alice', contextType: 'PURCHASE_REQUEST', contextId: 'pdone', rating: 4, comment: '' }));
  await deny('seller reviews the buyer of a purchase', setDoc(doc(as('alice'), 'reviews/alice_PURCHASE_REQUEST_pdone'), { reviewerId: 'alice', targetUserId: 'bob', contextType: 'PURCHASE_REQUEST', contextId: 'pdone', rating: 4, comment: '' }));
  await allow('exchange sender reviews receiver', setDoc(doc(as('alice'), 'reviews/alice_EXCHANGE_REQUEST_edone'), { reviewerId: 'alice', targetUserId: 'bob', contextType: 'EXCHANGE_REQUEST', contextId: 'edone', rating: 5, comment: 'ok' }));
  await allow('exchange receiver reviews sender', setDoc(doc(as('bob'), 'reviews/bob_EXCHANGE_REQUEST_edone'), { reviewerId: 'bob', targetUserId: 'alice', contextType: 'EXCHANGE_REQUEST', contextId: 'edone', rating: 3, comment: '' }));
  await deny('outsider reviews an exchange', setDoc(doc(as('carol'), 'reviews/carol_EXCHANGE_REQUEST_edone'), { reviewerId: 'carol', targetUserId: 'alice', contextType: 'EXCHANGE_REQUEST', contextId: 'edone', rating: 1, comment: '' }));
  await allow('customer reviews the recycler', setDoc(doc(as('bob'), 'reviews/bob_RECYCLING_REQUEST_cdone'), { reviewerId: 'bob', targetUserId: 'vic', contextType: 'RECYCLING_REQUEST', contextId: 'cdone', rating: 5, comment: '' }));
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

  // purchase: accepting is the agreement (the offered price becomes the agreed amount) and opens the payment record
  const acc = (extra = {}) => ({ status: 'ACCEPTED', agreedAmount: 90, agreedAt: now, payment: P('alice', 'bob', 'bob@okhdfcbank'), updatedAt: now, ...extra });
  await deny('buyer accepts with an agreement', updateDoc(doc(as('alice'), 'purchaseRequests/pnew'), acc()));
  await deny('seller agrees to a different amount', updateDoc(doc(as('bob'), 'purchaseRequests/pnew'), acc({ agreedAmount: 50 })));
  await deny('seller accepts with no payment record', updateDoc(doc(as('bob'), 'purchaseRequests/pnew'), { status: 'ACCEPTED', agreedAmount: 90, agreedAt: now, updatedAt: now }));
  await deny('payment record names the wrong payer', updateDoc(doc(as('bob'), 'purchaseRequests/pnew'), acc({ payment: P('carol', 'bob', 'bob@okhdfcbank') })));
  await deny('payment record names the wrong payee', updateDoc(doc(as('bob'), 'purchaseRequests/pnew'), acc({ payment: P('alice', 'carol', 'bob@okhdfcbank') })));
  await deny('payment record that starts out paid', updateDoc(doc(as('bob'), 'purchaseRequests/pnew'), acc({ payment: P('alice', 'bob', 'bob@okhdfcbank', { status: 'CONFIRMED' }) })));
  await deny('seller accepts with a malformed upi id', updateDoc(doc(as('bob'), 'purchaseRequests/pnew'), acc({ payment: P('alice', 'bob', 'not a upi id') })));
  await deny('seller accepts and slips in another field', updateDoc(doc(as('bob'), 'purchaseRequests/pnew'), acc({ offeredPrice: 1 })));
  await allow('seller accepts: amount agreed, payment opened', updateDoc(doc(as('bob'), 'purchaseRequests/pnew'), acc()));
  await deny('seller agrees a second time', updateDoc(doc(as('bob'), 'purchaseRequests/pnew'), acc({ agreedAmount: 91 })));
  await allow('seller accepts a free item without a payment record', updateDoc(doc(as('bob'), 'purchaseRequests/pfree'), { status: 'ACCEPTED', agreedAmount: 0, agreedAt: now, updatedAt: now }));
  await allow('seller accepts cash-only (no upi id)', updateDoc(doc(as('bob'), 'purchaseRequests/pnew3'), acc({ payment: P('alice', 'bob', null) })));

  // payment steps on a purchase: the buyer marks paid, only the seller confirms
  const payTo = (status, extra) => ({ payment: { ...P('alice', 'bob', 'bob@okhdfcbank'), status, ...extra }, updatedAt: now });
  await deny('paid before it is accepted', updateDoc(doc(as('alice'), 'purchaseRequests/p1'), payTo('MARKED_PAID', { method: 'UPI', markedPaidAt: now })));
  await deny('outsider marks paid', updateDoc(doc(as('carol'), 'purchaseRequests/ppay'), payTo('MARKED_PAID', { method: 'UPI', markedPaidAt: now })));
  await deny('seller marks the buyer\'s payment as paid', updateDoc(doc(as('bob'), 'purchaseRequests/ppay'), payTo('MARKED_PAID', { method: 'UPI', markedPaidAt: now })));
  await deny('seller confirms before the buyer paid', updateDoc(doc(as('bob'), 'purchaseRequests/ppay'), payTo('CONFIRMED', { confirmedAt: now })));
  await deny('buyer confirms their own payment', updateDoc(doc(as('alice'), 'purchaseRequests/ppay'), payTo('CONFIRMED', { method: 'UPI', markedPaidAt: now, confirmedAt: now })));
  await deny('marking paid while changing the status', updateDoc(doc(as('alice'), 'purchaseRequests/ppay'), { status: 'COMPLETED', ...payTo('MARKED_PAID', { method: 'UPI', markedPaidAt: now }) }));
  await deny('marking paid without a method', updateDoc(doc(as('alice'), 'purchaseRequests/ppay'), payTo('MARKED_PAID', { markedPaidAt: now })));
  await deny('marking paid with an unknown method', updateDoc(doc(as('alice'), 'purchaseRequests/ppay'), payTo('MARKED_PAID', { method: 'CARD', markedPaidAt: now })));
  await deny('transaction reference that is too long', updateDoc(doc(as('alice'), 'purchaseRequests/ppay'), payTo('MARKED_PAID', { method: 'UPI', upiRef: 'x'.repeat(65), markedPaidAt: now })));
  await deny('transaction reference on a cash payment', updateDoc(doc(as('alice'), 'purchaseRequests/ppay'), payTo('MARKED_PAID', { method: 'CASH', upiRef: 'T1', markedPaidAt: now })));
  await deny('buyer redirects the payment to their own upi id', updateDoc(doc(as('alice'), 'purchaseRequests/ppay'), payTo('MARKED_PAID', { method: 'UPI', markedPaidAt: now, payeeUpiId: 'alice@okhdfcbank' })));
  await deny('buyer changes the agreed amount while paying', updateDoc(doc(as('alice'), 'purchaseRequests/ppay'), { agreedAmount: 1, ...payTo('MARKED_PAID', { method: 'UPI', markedPaidAt: now }) }));
  await allow('buyer marks paid by UPI with a transaction reference', updateDoc(doc(as('alice'), 'purchaseRequests/ppay'), payTo('MARKED_PAID', { method: 'UPI', upiRef: 'T2026100412345', markedPaidAt: now })));
  await allow('buyer marks paid in cash', updateDoc(doc(as('alice'), 'purchaseRequests/ppay2'), { payment: { ...P('alice', 'bob', null), status: 'MARKED_PAID', method: 'CASH', markedPaidAt: now }, updatedAt: now }));
  await deny('buyer marks paid twice', updateDoc(doc(as('alice'), 'purchaseRequests/ppay3'), payTo('MARKED_PAID', { method: 'UPI', markedPaidAt: now + 1 })));
  await deny('un-marking a payment', updateDoc(doc(as('alice'), 'purchaseRequests/ppay3'), payTo('UNPAID', {})));
  await deny('buyer confirms receipt for the seller', updateDoc(doc(as('alice'), 'purchaseRequests/ppay3'), payTo('CONFIRMED', { method: 'UPI', upiRef: 'T123', markedPaidAt: now, confirmedAt: now })));
  await deny('outsider confirms', updateDoc(doc(as('carol'), 'purchaseRequests/ppay3'), payTo('CONFIRMED', { method: 'UPI', upiRef: 'T123', markedPaidAt: now, confirmedAt: now })));
  await deny('seller changes the method while confirming', updateDoc(doc(as('bob'), 'purchaseRequests/ppay3'), payTo('CONFIRMED', { method: 'CASH', upiRef: 'T123', markedPaidAt: now, confirmedAt: now })));
  await deny('seller edits the amount while confirming', updateDoc(doc(as('bob'), 'purchaseRequests/ppay3'), { agreedAmount: 9999, ...payTo('CONFIRMED', { method: 'UPI', upiRef: 'T123', markedPaidAt: now, confirmedAt: now }) }));
  await allow('seller confirms the money arrived', updateDoc(doc(as('bob'), 'purchaseRequests/ppay3'), payTo('CONFIRMED', { method: 'UPI', upiRef: 'T123', markedPaidAt: now, confirmedAt: now })));
  await allow('seller confirms a cash payment', updateDoc(doc(as('bob'), 'purchaseRequests/ppay4'), { payment: { ...P('alice', 'bob', null), status: 'CONFIRMED', method: 'CASH', markedPaidAt: now, confirmedAt: now }, updatedAt: now }));
  await deny('a confirmed payment cannot be reverted', updateDoc(doc(as('bob'), 'purchaseRequests/ppay5'), { payment: { ...P('alice', 'bob', null), status: 'MARKED_PAID', method: 'CASH', markedPaidAt: now }, updatedAt: now }));
  await deny('seller edits the agreed amount later', updateDoc(doc(as('bob'), 'purchaseRequests/ppay5'), { agreedAmount: 1, updatedAt: now }));
  await deny('buyer edits the agreed amount later', updateDoc(doc(as('alice'), 'purchaseRequests/ppay5'), { agreedAmount: 1, updatedAt: now }));
  await deny('seller cancels after the buyer says they paid', updateDoc(doc(as('bob'), 'purchaseRequests/ppay4'), { status: 'CANCELLED', updatedAt: now }));
  await allow('seller cancels while nothing is paid', updateDoc(doc(as('bob'), 'purchaseRequests/ppay7'), { status: 'READY_FOR_PICKUP', updatedAt: now }));
  // a sale that is already paid (marked or confirmed) can still move forward; it just can't be cancelled
  await allow('seller marks a paid sale ready for pickup', updateDoc(doc(as('bob'), 'purchaseRequests/ppay3'), { status: 'READY_FOR_PICKUP', updatedAt: now }));
  await allow('seller completes a sale whose payment is confirmed', updateDoc(doc(as('bob'), 'purchaseRequests/ppay3'), { status: 'COMPLETED', updatedAt: now }));
  await allow('seller marks a sale ready after the buyer marked it paid', updateDoc(doc(as('bob'), 'purchaseRequests/ppay4'), { status: 'READY_FOR_PICKUP', updatedAt: now }));
  await deny('seller cancels a paid sale that is ready', updateDoc(doc(as('bob'), 'purchaseRequests/ppay4'), { status: 'CANCELLED', updatedAt: now }));
  await allow('seller completes the sale', updateDoc(doc(as('bob'), 'purchaseRequests/ppay6'), { status: 'COMPLETED', updatedAt: now }));
  await allow('a completed sale can still be paid for', updateDoc(doc(as('alice'), 'purchaseRequests/ppay6'), { payment: { ...P('alice', 'bob', null), status: 'MARKED_PAID', method: 'CASH', markedPaidAt: now }, updatedAt: now }));

  // reports
  const rep = { reporterId: 'alice', targetType: 'LISTING', targetId: 'sell1', targetName: 'Sofa', reason: 'SPAM', details: '' };
  await allow('report a listing', setDoc(doc(as('alice'), 'reports/alice_LISTING_sell1'), rep));
  await deny('report with a wrong id', setDoc(doc(as('alice'), 'reports/whatever'), rep));
  await deny('report as someone else', setDoc(doc(as('carol'), 'reports/alice_LISTING_sell1'), rep));
  await deny('report yourself', setDoc(doc(as('alice'), 'reports/alice_USER_alice'), { ...rep, targetType: 'USER', targetId: 'alice' }));
  await allow('report a user', setDoc(doc(as('alice'), 'reports/alice_USER_bob'), { ...rep, targetType: 'USER', targetId: 'bob' }));
  await deny('report with very long details', setDoc(doc(as('alice'), 'reports/alice_LISTING_swap1'), { ...rep, targetId: 'swap1', details: 'x'.repeat(501) }));
  await deny('read a report back', getDoc(doc(as('alice'), 'reports/alice_LISTING_sell1')));
  await allow('update your own report', setDoc(doc(as('alice'), 'reports/alice_LISTING_sell1'), { ...rep, details: 'more info' }));

  // blocks
  await allow('block someone', setDoc(doc(as('alice'), 'blocks/alice_carol'), { blockerId: 'alice', blockedId: 'carol', blockedName: 'Carol' }));
  await deny('block with a wrong id', setDoc(doc(as('alice'), 'blocks/x'), { blockerId: 'alice', blockedId: 'dave' }));
  await deny('block as someone else', setDoc(doc(as('dave'), 'blocks/alice_dave'), { blockerId: 'alice', blockedId: 'dave' }));
  await deny('block yourself', setDoc(doc(as('alice'), 'blocks/alice_alice'), { blockerId: 'alice', blockedId: 'alice' }));
  await allow('blocker reads own block', getDoc(doc(as('alice'), 'blocks/alice_carol')));
  await deny('blocked person reads the block', getDoc(doc(as('carol'), 'blocks/alice_carol')));
  await allow('blocker lists blocks', getDocs(query(collection(as('alice'), 'blocks'), where('blockerId', '==', 'alice'))));
  await deny('list someone else\'s blocks', getDocs(query(collection(as('carol'), 'blocks'), where('blockerId', '==', 'alice'))));
  await deny('blocked person starts a chat', setDoc(doc(as('carol'), 'chats/blk1'), { participantIds: ['alice', 'carol'] }));
  await deny('blocker starts a chat', setDoc(doc(as('alice'), 'chats/blk2'), { participantIds: ['alice', 'carol'] }));
  await env.withSecurityRulesDisabled(async (ctx) => { await setDoc(doc(ctx.firestore(), 'chats/blkchat'), { participantIds: ['alice', 'carol'], lastMessage: '', lastMessageAt: 0, lastMessageSenderId: '' }); });
  await deny('blocked person messages the blocker', setDoc(doc(as('carol'), 'chats/blkchat/messages/m1'), { senderId: 'carol', text: 'hi' }));
  await deny('blocker messages the blocked person', setDoc(doc(as('alice'), 'chats/blkchat/messages/m2'), { senderId: 'alice', text: 'hi' }));
  await deny('blocked person sends a purchase request', setDoc(doc(as('carol'), 'purchaseRequests/pblk'), { buyerId: 'carol', sellerId: 'alice', listingId: 'sell1', status: 'REQUESTED', offeredPrice: 1 }));
  await allow('someone else still messages an unblocked chat', setDoc(doc(as('alice'), 'chats/ch1/messages/m77'), { senderId: 'alice', text: 'still fine' }));
  await allow('unblock', deleteDoc(doc(as('alice'), 'blocks/alice_carol')));
  await allow('chat works again after unblock', setDoc(doc(as('carol'), 'chats/blkchat/messages/m3'), { senderId: 'carol', text: 'hi again' }));

  console.log(`\n${passed} passed, ${failed} failed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error(e); process.exit(2); });
