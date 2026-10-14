#!/usr/bin/env node
// Fills a HomeSajja Firebase project with realistic Indian demo data: users and vendors in five cities, about fifty
// listings priced in rupees, material requests, and finished jobs with reviews so vendors show real ratings.
//
//   node seed.mjs                 seed the project in app/google-services.json (the real one)
//   node seed.mjs --emulator      seed the local Auth (9099) and Firestore (8080) emulators instead
//   node seed.mjs --remove        remove what a previous run created (see README for what stays behind)
//
// Everything is written by signing in as the demo accounts, so the app's security rules apply exactly as they do to real users.
import { DEMO_PASSWORD, USERS, VENDORS, ALL_PEOPLE } from './data/people.mjs';
import { POOL, CITY_PICKS, CITY_PRICE_FACTOR, EXCHANGE_SLOTS, MATERIAL_REQUESTS, COMPLETED_JOBS } from './data/listings.mjs';
import { STITCH_PHOTOS, uploadIllustrations } from './lib/cloudinary.mjs';
import { deleteAccount, deleteDoc, docExists, docStatus, ensureAccount, idsWhere, loadConfig, setDoc, updateDoc } from './lib/firebase.mjs';

const args = new Set(process.argv.slice(2));
// --keep=aarav,dadarwood keeps those demo accounts (and their profiles) when removing; only their listings are deleted.
const keep = new Set((process.argv.find((arg) => arg.startsWith('--keep='))?.slice('--keep='.length) ?? '').split(',').filter(Boolean));
const config = loadConfig({ emulator: args.has('--emulator'), dryRun: args.has('--dry-run') });
const log = (message) => console.log(message);
const DAY = 86_400_000;
const HOUR = 3_600_000;
const MIN = 60_000;
const now = Date.now();

// A demo UPI id per person, e.g. aarav@okhdfcbank. The payment record of every finished job carries the payee's.
const upiOf = (person) => `${String(person.key).replace(/[^a-z0-9]/gi, '').slice(0, 14).toLowerCase() || 'demo'}@okhdfcbank`;
// A payment record as the app writes it when an amount is agreed, and the two steps that follow (see firestore.rules).
const openPayment = (payer, payee, payeeUpiId) => ({ payerId: payer.uid, payeeId: payee.uid, payeeUpiId, method: null, status: 'UNPAID', upiRef: null, markedPaidAt: null, confirmedAt: null });
const markedPayment = (payment, method, at) => ({ ...payment, status: 'MARKED_PAID', method, upiRef: method === 'UPI' ? `DEMO${Math.abs(at) % 1000000000}` : null, markedPaidAt: at });
const confirmedPayment = (payment, at) => ({ ...payment, status: 'CONFIRMED', confirmedAt: at });
const REPAIR_PRICES = [1200, 1800, 2500, 900, 3200, 1500];
const REPAIR_DAYS = [2, 3, 5, 2, 4, 3];
const PAYOUT_RECYCLERS = new Set(['delhigreenscrap']);

log(`${config.dryRun ? '[DRY-RUN] ' : ''}${args.has('--remove') ? 'Removing demo data from' : 'Seeding'} ${config.emulator ? 'the local emulators' : `project ${config.projectId}`}\n`);

// ---- sign in every demo person ----
const sessions = {};
for (const person of ALL_PEOPLE) {
  sessions[person.key] = { ...person, ...(await ensureAccount(config, { email: person.email, password: DEMO_PASSWORD, name: person.kind === 'vendor' ? person.businessName : person.name })) };
}
log(`Signed in ${ALL_PEOPLE.length} demo accounts (password: ${DEMO_PASSWORD})`);

if (args.has('--remove')) {
  await removeEverything();
} else {
  await seed();
}

// =================================================================================================

async function seed() {
  const images = await uploadIllustrations(config, log);
  const imageFor = (category, variant = 0) => images[`${category}:${variant % 3}`];

  // profiles
  for (const p of Object.values(sessions)) {
    if (p.kind === 'user') {
      await setDoc(config, p.token, `users/${p.uid}`, { uid: p.uid, name: p.name, email: p.email, phone: p.phone, city: p.city, upiId: upiOf(p), createdAt: now - 40 * DAY });
    } else if (!(await docExists(config, p.token, `vendors/${p.uid}`))) {
      // A vendor document is only written once, so a `verified` flag set in the console is never overwritten.
      await setDoc(config, p.token, `vendors/${p.uid}`, {
        uid: p.uid, name: p.name, email: p.email, phone: p.phone, businessName: p.businessName, businessType: p.businessType,
        city: p.city, shopAddress: `${p.area}, ${p.city}`, upiId: upiOf(p), description: p.description, verified: false,
        brochureImages: STITCH_PHOTOS.workshopBrochure, shopLatitude: p.lat, shopLongitude: p.lng,
        repairServices: p.repairServices ?? [], repairCostMin: p.repairCostMin ?? null, repairCostMax: p.repairCostMax ?? null,
        createdAt: now - 40 * DAY,
      });
    } else {
      await updateDoc(config, p.token, `vendors/${p.uid}`, {
        brochureImages: STITCH_PHOTOS.workshopBrochure,
      });
    }
  }
  log('Profiles written');

  // listings
  let listingCount = 0;
  for (const city of Object.keys(CITY_PICKS)) {
    const sellers = [
      ...VENDORS.filter((v) => v.city === city && (v.businessType === 'SHOP' || v.businessType === 'REFURBISHER')),
      ...USERS.filter((u) => u.city === city),
    ].map((s) => sessions[s.key]);
    for (const [i, poolIndex] of CITY_PICKS[city].entries()) {
      const item = POOL[poolIndex];
      const seller = sellers[i % sellers.length];
      const id = `demo_${city.toLowerCase()}_${i + 1}`;
      const exchange = EXCHANGE_SLOTS.has(i);
      const price = Math.round((item.price * CITY_PRICE_FACTOR[city]) / 100) * 100;
      const created = now - (i * 30 + (poolIndex % 7) * 5) * 3_600_000;
      await setDoc(config, seller.token, `listings/${id}`, listing(id, seller, item, city, exchange ? 0 : price, exchange, created, imageFor));
      listingCount++;
    }
  }
  log(`Listings written: ${listingCount}`);

  // material requests
  let requestCount = 0;
  for (const [i, m] of MATERIAL_REQUESTS.entries()) {
    const vendor = sessions[m.vendor];
    const id = `demo_material_${i + 1}`;
    if (await docExists(config, vendor.token, `materialRequests/${id}`)) continue;
    await setDoc(config, vendor.token, `materialRequests/${id}`, {
      id, vendorId: vendor.uid, vendorName: vendor.businessName, materialType: m.materialType, title: m.title, description: m.description,
      quantity: m.quantity, budgetMin: m.budgetMin, budgetMax: m.budgetMax, city: m.city, status: 'OPEN',
      createdAt: now - (i + 1) * 2 * DAY, updatedAt: now - (i + 1) * 2 * DAY,
    });
    requestCount++;
  }
  log(`Material requests written: ${requestCount} new`);

  // finished jobs with reviews
  let jobCount = 0;
  for (const [i, job] of COMPLETED_JOBS.entries()) {
    const jobId = `demo_job_${i + 1}`;
    const customer = sessions[job.who];
    const vendor = sessions[job.with];
    const day = now - (30 - i) * DAY;
    if (job.type === 'repair') {
      const path = `repairRequests/${jobId}`;
      if (await isDone(customer, path)) continue;
      await setDoc(config, customer.token, path, {
        id: jobId, userId: customer.uid, userName: customer.name, vendorId: vendor.uid, vendorName: vendor.businessName, listingId: null,
        furnitureTitle: job.item, furnitureCategory: job.category, problemType: job.problem, issueDescription: job.text,
        images: [imageFor(job.category, i)], city: customer.city, status: 'REQUESTED', createdAt: day, updatedAt: day,
      });
      // Quote -> the customer accepts (the amount is agreed) -> work -> paid -> the vendor confirms the money arrived.
      const amount = REPAIR_PRICES[i % REPAIR_PRICES.length];
      const quote = { amount, estimatedDays: REPAIR_DAYS[i % REPAIR_DAYS.length], note: 'Parts and labour included.', payeeUpiId: upiOf(vendor), direction: null, revision: 1, sentAt: day + DAY / 4 };
      await updateDoc(config, vendor.token, path, { status: 'QUOTED', quote, updatedAt: day + DAY / 4 });
      const payment = openPayment(customer, vendor, upiOf(vendor));
      await updateDoc(config, customer.token, path, { status: 'AGREED', agreedAmount: amount, agreedAt: day + DAY / 2, payment, updatedAt: day + DAY / 2 });
      for (const status of ['IN_PROGRESS', 'READY', 'COMPLETED']) await updateDoc(config, vendor.token, path, { status, updatedAt: day + DAY });
      const paid = markedPayment(payment, i % 2 === 0 ? 'UPI' : 'CASH', day + DAY);
      await updateDoc(config, customer.token, path, { payment: paid, updatedAt: day + DAY });
      await updateDoc(config, vendor.token, path, { payment: confirmedPayment(paid, day + DAY + 3600_000), updatedAt: day + DAY + 3600_000 });
      await review(customer, vendor, 'REPAIR_REQUEST', jobId, job, day + 2 * DAY);
    } else if (job.type === 'recycle') {
      const path = `recyclingRequests/${jobId}`;
      if (await isDone(customer, path)) continue;
      await setDoc(config, customer.token, path, {
        id: jobId, userId: customer.uid, userName: customer.name, vendorId: vendor.uid, vendorName: vendor.businessName,
        condition: job.condition, material: job.material, method: 'DROP_OFF', images: [imageFor('WARDROBE', i)], city: customer.city,
        status: 'REQUESTED', createdAt: day, updatedAt: day,
      });
      // Free by default. Some recyclers charge a small fee, one pays for the metal: those send a quote the customer accepts.
      const pays = PAYOUT_RECYCLERS.has(job.with) ? 'vendor' : i % 2 === 1 ? 'user' : null;
      if (pays === null) {
        await updateDoc(config, vendor.token, path, { status: 'ACCEPTED', updatedAt: day + DAY / 2 });
      } else {
        const amount = pays === 'user' ? 250 : 400;
        const payer = pays === 'user' ? customer : vendor;
        const payee = pays === 'user' ? vendor : customer;
        const quote = { amount, estimatedDays: null, note: pays === 'user' ? 'Handling fee.' : 'Paid by weight for the metal.', payeeUpiId: pays === 'user' ? upiOf(vendor) : null, direction: pays === 'user' ? 'USER_PAYS_VENDOR' : 'VENDOR_PAYS_USER', revision: 1, sentAt: day + DAY / 4 };
        await updateDoc(config, vendor.token, path, { status: 'QUOTED', quote, updatedAt: day + DAY / 4 });
        const payment = openPayment(payer, payee, upiOf(payee));
        await updateDoc(config, customer.token, path, { status: 'ACCEPTED', agreedAmount: amount, agreedAt: day + DAY / 2, payment, updatedAt: day + DAY / 2 });
        const paid = markedPayment(payment, 'UPI', day + DAY);
        await updateDoc(config, payer.token, path, { payment: paid, updatedAt: day + DAY });
        await updateDoc(config, payee.token, path, { payment: confirmedPayment(paid, day + DAY + 3600_000), updatedAt: day + DAY + 3600_000 });
      }
      for (const status of ['SCHEDULED', 'COMPLETED']) await updateDoc(config, vendor.token, path, { status, updatedAt: day + DAY });
      await review(customer, vendor, 'RECYCLING_REQUEST', jobId, job, day + 2 * DAY);
    } else {
      const path = `purchaseRequests/${jobId}`;
      const item = POOL[(i * 3) % POOL.length];
      const listingId = `demo_sold_${i + 1}`;
      if (await isDone(customer, path)) {
        if (await docExists(config, vendor.token, `listings/${listingId}`)) {
          await updateDoc(config, vendor.token, `listings/${listingId}`, { images: item.images ?? [imageFor(item.category, item.variant), imageFor(item.category, item.variant + 1)] });
        }
        continue;
      }
      await setDoc(config, vendor.token, `listings/${listingId}`, listing(listingId, vendor, item, vendor.city, item.price, false, day - DAY, imageFor));
      await setDoc(config, customer.token, path, {
        id: jobId, listingId, listingTitle: item.title, buyerId: customer.uid, buyerName: customer.name, sellerId: vendor.uid, sellerName: vendor.businessName,
        offeredPrice: item.price, message: '', status: 'REQUESTED', createdAt: day, updatedAt: day,
      });
      // Accepting is the agreement: the price becomes the agreed amount and the payment record opens.
      const payment = openPayment(customer, vendor, upiOf(vendor));
      await updateDoc(config, vendor.token, path, { status: 'ACCEPTED', agreedAmount: item.price, agreedAt: day + DAY, payment, updatedAt: day + DAY });
      await updateDoc(config, vendor.token, `listings/${listingId}`, { status: 'RESERVED', updatedAt: day + DAY });
      const paid = markedPayment(payment, i % 2 === 0 ? 'UPI' : 'CASH', day + DAY);
      await updateDoc(config, customer.token, path, { payment: paid, updatedAt: day + DAY });
      await updateDoc(config, vendor.token, path, { payment: confirmedPayment(paid, day + 2 * DAY), updatedAt: day + 2 * DAY });
      await updateDoc(config, vendor.token, path, { status: 'READY_FOR_PICKUP', updatedAt: day + 2 * DAY });
      await updateDoc(config, vendor.token, path, { status: 'COMPLETED', updatedAt: day + 2 * DAY });
      await updateDoc(config, vendor.token, `listings/${listingId}`, { status: 'SOLD', updatedAt: day + 2 * DAY });
      await review(customer, vendor, 'PURCHASE_REQUEST', jobId, job, day + 3 * DAY);
    }
    jobCount++;
  }
  log(`Finished jobs with reviews written: ${jobCount} new`);

  await seedStitchShowcase(imageFor);

  log(`\nDone. Log in with any demo email (demo.<name>@homesajja.demo), password ${DEMO_PASSWORD}.`);
  log('Example accounts: demo.aarav@homesajja.demo (user, Mumbai), demo.dadarwood@homesajja.demo (repair vendor, Mumbai).');
  log('The "Verified" badge is not set here on purpose (vendors can\'t verify themselves): tick `verified` on a few vendors in the Firebase console.');
}

async function seedStitchShowcase(imageFor) {
  const { aarav, priya, rohan, dadarwood, andherimart, greenloopmum } = sessions;

  const upsertListing = async (id, owner, data, finalStatus = 'ACTIVE') => {
    const isVendor = owner.kind === 'vendor';
    const doc = {
      id, ownerId: owner.uid, ownerName: isVendor ? owner.businessName : owner.name,
      title: data.title, description: data.description,
      images: data.images,
      price: data.price, condition: data.condition, refurbished: data.refurbished ?? false,
      category: data.category, material: data.material, ageYears: data.ageYears ?? 3,
      dimensions: { lengthCm: data.dims[0], widthCm: data.dims[1], heightCm: data.dims[2] },
      sellerType: isVendor ? 'VENDOR' : 'INDIVIDUAL',
      actionType: data.actionType ?? 'SELL', city: owner.city, status: 'ACTIVE',
      createdAt: data.createdAt ?? now - 3 * DAY, updatedAt: data.createdAt ?? now - 3 * DAY,
    };
    await setDoc(config, owner.token, `listings/${id}`, doc);
    if (finalStatus !== 'ACTIVE') {
      await updateDoc(config, owner.token, `listings/${id}`, { status: finalStatus, updatedAt: now - DAY });
    }
  };

  // Curated Stitch listings in Mumbai
  await upsertListing('sc_dad_1', dadarwood, {
    title: 'Restored teak peg stool',
    description: 'Solid Burma teak peg stool, re-glued and finished with two coats of French polish in our workshop.',
    images: STITCH_PHOTOS.pegStool,
    price: 2400, condition: 'LIKE_NEW', refurbished: true, category: 'CHAIR', material: 'WOOD', ageYears: 3, dims: [45, 45, 50],
    createdAt: now - 2 * HOUR,
  });
  await upsertListing('sc_aar_1', aarav, {
    title: 'Cane armchair',
    description: 'Comfortable balcony armchair with fresh hand-woven cane seat. Solid teak armrests.',
    images: STITCH_PHOTOS.caneArmchair,
    price: 3500, condition: 'GOOD', category: 'CHAIR', material: 'WOOD', ageYears: 3, dims: [65, 62, 84],
    createdAt: now - 3 * HOUR,
  });
  await upsertListing('sc_dad_2', dadarwood, {
    title: 'Rosewood bedside table',
    description: 'Small rosewood bedside table with a brass knob. Happy to swap for old teak planks or a wooden chair.',
    images: STITCH_PHOTOS.rosewoodBedside,
    price: 0, actionType: 'EXCHANGE', condition: 'GOOD', refurbished: true, category: 'TABLE', material: 'WOOD', ageYears: 5, dims: [50, 42, 58],
    createdAt: now - 5 * HOUR,
  });
  await upsertListing('sc_aar_2', aarav, {
    title: 'Wooden study stool',
    description: 'Compact teak stool, great height for a writing desk. Looking to swap for a small side table or lamp.',
    images: STITCH_PHOTOS.studyStool,
    price: 0, actionType: 'EXCHANGE', condition: 'GOOD', category: 'CHAIR', material: 'WOOD', ageYears: 2, dims: [40, 40, 48],
    createdAt: now - 6 * HOUR,
  });
  await upsertListing('sc_dad_3', dadarwood, {
    title: 'Oak coffee table',
    description: 'Low coffee table with bevelled edges and a lower magazine shelf.',
    images: STITCH_PHOTOS.coffeeTable,
    price: 4800, condition: 'LIKE_NEW', refurbished: true, category: 'COFFEE_TABLE', material: 'WOOD', ageYears: 4, dims: [110, 60, 45],
    createdAt: now - 12 * DAY,
  }, 'SOLD');
  await upsertListing('sc_aar_3', aarav, {
    title: 'Study desk',
    description: 'Clean engineered-wood desk with cable grommet. Sold to a neighbour.',
    images: STITCH_PHOTOS.studyDesk,
    price: 2800, condition: 'GOOD', category: 'DESK', material: 'WOOD', ageYears: 2, dims: [110, 55, 75],
    createdAt: now - 10 * DAY,
  }, 'SOLD');
  await upsertListing('sc_and_1', andherimart, {
    title: 'Study table with drawers',
    description: 'Compact study table with two lockable drawers.',
    images: STITCH_PHOTOS.studyDesk,
    price: 4800, condition: 'GOOD', category: 'DESK', material: 'WOOD', ageYears: 2, dims: [120, 60, 76],
    createdAt: now - 4 * DAY,
  });
  await upsertListing('sc_pri_1', priya, {
    title: 'Marble-top coffee table',
    description: 'Small round coffee table with white marble top.',
    images: STITCH_PHOTOS.coffeeTable,
    price: 0, actionType: 'EXCHANGE', condition: 'GOOD', category: 'COFFEE_TABLE', material: 'GLASS', ageYears: 3, dims: [80, 80, 45],
    createdAt: now - 5 * DAY,
  });
  // Create sc_aar_4 and sc_dad_4 as ACTIVE first so sc_exc_2 creation passes firestore.rules, then reserve them.
  await upsertListing('sc_aar_4', aarav, {
    title: 'Side table (swap)',
    description: 'Small walnut-stained side table.',
    images: STITCH_PHOTOS.sideTable,
    price: 0, actionType: 'EXCHANGE', condition: 'GOOD', category: 'TABLE', material: 'WOOD', ageYears: 3, dims: [45, 45, 52],
    createdAt: now - 4 * DAY,
  });
  await upsertListing('sc_dad_4', dadarwood, {
    title: 'Carved wall shelf (swap)',
    description: 'Refurbished teak wall shelf with brass brackets.',
    images: STITCH_PHOTOS.carvedShelf,
    price: 0, actionType: 'EXCHANGE', condition: 'GOOD', refurbished: true, category: 'BOOKSHELF', material: 'WOOD', ageYears: 6, dims: [75, 22, 30],
    createdAt: now - 4 * DAY,
  });

  // Favourites for Aarav
  for (const [idx, lid] of ['demo_mumbai_1', 'demo_mumbai_2', 'sc_dad_1'].entries()) {
    const favId = `${aarav.uid}_${lid}`;
    if (!(await docExists(config, aarav.token, `favourites/${favId}`))) {
      await setDoc(config, aarav.token, `favourites/${favId}`, {
        id: favId, userId: aarav.uid, listingId: lid, createdAt: now - (idx + 1) * HOUR,
      });
    }
  }

  // Active repair requests across pipeline states
  const rep1Status = await docStatus(config, priya.token, 'repairRequests/sc_rep_1');
  if (rep1Status === 'REQUESTED') await deleteDoc(config, priya.token, 'repairRequests/sc_rep_1');
  if (rep1Status === null || rep1Status === 'REQUESTED') {
    await setDoc(config, priya.token, 'repairRequests/sc_rep_1', {
      id: 'sc_rep_1', userId: priya.uid, userName: priya.name, vendorId: dadarwood.uid, vendorName: dadarwood.businessName,
      listingId: null, furnitureTitle: 'Cane rocking chair', furnitureCategory: 'CHAIR', problemType: 'FABRIC_DAMAGE',
      issueDescription: 'The cane seat has sagged in the middle and needs re-weaving; armrests also need a light sand and polish.',
      images: [STITCH_PHOTOS.caneRockingChair[0]], city: 'Mumbai', status: 'REQUESTED',
      createdAt: now - 3 * HOUR, updatedAt: now - 3 * HOUR,
    });
  }
  if (!(await docExists(config, aarav.token, 'repairRequests/sc_rep_2'))) {
    const t2 = now - 2 * DAY;
    await setDoc(config, aarav.token, 'repairRequests/sc_rep_2', {
      id: 'sc_rep_2', userId: aarav.uid, userName: aarav.name, vendorId: dadarwood.uid, vendorName: dadarwood.businessName,
      listingId: null, furnitureTitle: 'Sheesham coffee table', furnitureCategory: 'COFFEE_TABLE', problemType: 'POLISHING',
      issueDescription: 'Water rings and dull finish on the tabletop. Looking for a full strip and French polish.',
      images: [STITCH_PHOTOS.coffeeTable[0]], city: 'Mumbai', status: 'REQUESTED',
      createdAt: t2, updatedAt: t2,
    });
    const quote2 = { amount: 1650, estimatedDays: 3, note: 'Includes stripping, two coats of French polish, and pickup/drop in Bandra.', payeeUpiId: upiOf(dadarwood), direction: null, revision: 1, sentAt: t2 + 4 * HOUR };
    await updateDoc(config, dadarwood.token, 'repairRequests/sc_rep_2', { status: 'QUOTED', quote: quote2, updatedAt: t2 + 4 * HOUR });
  }
  if (!(await docExists(config, rohan.token, 'repairRequests/sc_rep_3'))) {
    const t3 = now - 4 * DAY;
    await setDoc(config, rohan.token, 'repairRequests/sc_rep_3', {
      id: 'sc_rep_3', userId: rohan.uid, userName: rohan.name, vendorId: dadarwood.uid, vendorName: dadarwood.businessName,
      listingId: null, furnitureTitle: 'Double-door wardrobe', furnitureCategory: 'WARDROBE', problemType: 'LOOSE_JOINTS',
      issueDescription: 'Left door hinge has pulled out of the wood and the bottom drawer sticks.',
      images: [STITCH_PHOTOS.wardrobe[0]], city: 'Mumbai', status: 'REQUESTED',
      createdAt: t3, updatedAt: t3,
    });
    const quote3 = { amount: 1400, estimatedDays: 2, note: 'Plug hinge holes with teak dowels, fit heavy-duty brass hinges, plane drawer runners.', payeeUpiId: upiOf(dadarwood), direction: null, revision: 1, sentAt: t3 + 3 * HOUR };
    await updateDoc(config, dadarwood.token, 'repairRequests/sc_rep_3', { status: 'QUOTED', quote: quote3, updatedAt: t3 + 3 * HOUR });
    const pay3 = openPayment(rohan, dadarwood, upiOf(dadarwood));
    await updateDoc(config, rohan.token, 'repairRequests/sc_rep_3', { status: 'AGREED', agreedAmount: 1400, agreedAt: t3 + 6 * HOUR, payment: pay3, updatedAt: t3 + 6 * HOUR });
    await updateDoc(config, dadarwood.token, 'repairRequests/sc_rep_3', { status: 'IN_PROGRESS', updatedAt: t3 + DAY });
    await updateDoc(config, rohan.token, 'repairRequests/sc_rep_3', { payment: markedPayment(pay3, 'UPI', now - 40 * MIN), updatedAt: now - 40 * MIN });
  }
  if (!(await docExists(config, aarav.token, 'repairRequests/sc_rep_4'))) {
    const t4 = now - 5 * DAY;
    await setDoc(config, aarav.token, 'repairRequests/sc_rep_4', {
      id: 'sc_rep_4', userId: aarav.uid, userName: aarav.name, vendorId: dadarwood.uid, vendorName: dadarwood.businessName,
      listingId: null, furnitureTitle: 'Oak dining chair', furnitureCategory: 'CHAIR', problemType: 'BROKEN_LEG',
      issueDescription: 'Back left leg cracked near the mortise joint.',
      images: [STITCH_PHOTOS.caneRockingChair[1]], city: 'Mumbai', status: 'REQUESTED',
      createdAt: t4, updatedAt: t4,
    });
    const quote4 = { amount: 950, estimatedDays: 2, note: 'New seasoned teak leg turned to match + glue & clamp.', payeeUpiId: upiOf(dadarwood), direction: null, revision: 1, sentAt: t4 + 2 * HOUR };
    await updateDoc(config, dadarwood.token, 'repairRequests/sc_rep_4', { status: 'QUOTED', quote: quote4, updatedAt: t4 + 2 * HOUR });
    const pay4 = openPayment(aarav, dadarwood, upiOf(dadarwood));
    await updateDoc(config, aarav.token, 'repairRequests/sc_rep_4', { status: 'AGREED', agreedAmount: 950, agreedAt: t4 + 5 * HOUR, payment: pay4, updatedAt: t4 + 5 * HOUR });
    await updateDoc(config, dadarwood.token, 'repairRequests/sc_rep_4', { status: 'IN_PROGRESS', updatedAt: t4 + DAY });
    await updateDoc(config, dadarwood.token, 'repairRequests/sc_rep_4', { status: 'READY', updatedAt: now - 2 * HOUR });
  }

  // Purchase requests
  if (!(await docExists(config, priya.token, 'purchaseRequests/sc_pur_1'))) {
    await setDoc(config, priya.token, 'purchaseRequests/sc_pur_1', {
      id: 'sc_pur_1', listingId: 'sc_aar_1', listingTitle: 'Cane armchair',
      buyerId: priya.uid, buyerName: priya.name, sellerId: aarav.uid, sellerName: aarav.name,
      offeredPrice: 3200, message: 'Hi Aarav! Can pick this up from Bandra this Saturday if ₹3,200 works for you.',
      status: 'REQUESTED', createdAt: now - 4 * HOUR, updatedAt: now - 4 * HOUR,
    });
  }
  if (!(await docExists(config, rohan.token, 'purchaseRequests/sc_pur_2'))) {
    await setDoc(config, rohan.token, 'purchaseRequests/sc_pur_2', {
      id: 'sc_pur_2', listingId: 'sc_dad_1', listingTitle: 'Restored teak peg stool',
      buyerId: rohan.uid, buyerName: rohan.name, sellerId: dadarwood.uid, sellerName: dadarwood.businessName,
      offeredPrice: 2400, message: 'Looks lovely — happy to pay asking price. Can I collect tomorrow evening?',
      status: 'REQUESTED', createdAt: now - 90 * MIN, updatedAt: now - 90 * MIN,
    });
  }
  if (!(await docExists(config, aarav.token, 'purchaseRequests/sc_pur_3'))) {
    const tp = now - 2 * DAY;
    await setDoc(config, aarav.token, 'purchaseRequests/sc_pur_3', {
      id: 'sc_pur_3', listingId: 'sc_and_1', listingTitle: 'Study table with drawers',
      buyerId: aarav.uid, buyerName: aarav.name, sellerId: andherimart.uid, sellerName: andherimart.businessName,
      offeredPrice: 4800, message: 'Would like to buy this for my home office.',
      status: 'REQUESTED', createdAt: tp, updatedAt: tp,
    });
    const payP = openPayment(aarav, andherimart, upiOf(andherimart));
    await updateDoc(config, andherimart.token, 'purchaseRequests/sc_pur_3', { status: 'ACCEPTED', agreedAmount: 4800, agreedAt: tp + 3 * HOUR, payment: payP, updatedAt: tp + 3 * HOUR });
  }
  await updateDoc(config, andherimart.token, 'listings/sc_and_1', { status: 'RESERVED', updatedAt: now - DAY });

  // Recycling requests
  if (!(await docExists(config, aarav.token, 'recyclingRequests/sc_rec_1'))) {
    const tr = now - 2 * DAY;
    await setDoc(config, aarav.token, 'recyclingRequests/sc_rec_1', {
      id: 'sc_rec_1', userId: aarav.uid, userName: aarav.name, vendorId: greenloopmum.uid, vendorName: greenloopmum.businessName,
      condition: 'BEYOND_REPAIR', material: 'METAL', method: 'DROP_OFF',
      images: [STITCH_PHOTOS.wardrobe[1]], city: 'Mumbai', status: 'REQUESTED',
      createdAt: tr, updatedAt: tr,
    });
    const quoteR = { amount: 350, estimatedDays: null, note: 'We pay ₹350 by weight for the steel frame on drop-off.', payeeUpiId: null, direction: 'VENDOR_PAYS_USER', revision: 1, sentAt: tr + 4 * HOUR };
    await updateDoc(config, greenloopmum.token, 'recyclingRequests/sc_rec_1', { status: 'QUOTED', quote: quoteR, updatedAt: tr + 4 * HOUR });
  }
  const rec2Status = await docStatus(config, priya.token, 'recyclingRequests/sc_rec_2');
  if (rec2Status === 'REQUESTED') await deleteDoc(config, priya.token, 'recyclingRequests/sc_rec_2');
  if (rec2Status === null || rec2Status === 'REQUESTED') {
    await setDoc(config, priya.token, 'recyclingRequests/sc_rec_2', {
      id: 'sc_rec_2', userId: priya.uid, userName: priya.name, vendorId: null, vendorName: '',
      condition: 'REPAIRABLE', material: 'WOOD', method: 'PICKUP',
      images: [STITCH_PHOTOS.carvedShelf[0]], city: 'Mumbai', status: 'REQUESTED',
      createdAt: now - 5 * HOUR, updatedAt: now - 5 * HOUR,
    });
  }

  // Exchange requests
  const exc1Status = await docStatus(config, priya.token, 'exchangeRequests/sc_exc_1');
  if (exc1Status === 'PENDING') await deleteDoc(config, priya.token, 'exchangeRequests/sc_exc_1');
  if (exc1Status === null || exc1Status === 'PENDING') {
    await setDoc(config, priya.token, 'exchangeRequests/sc_exc_1', {
      id: 'sc_exc_1',
      senderId: priya.uid, senderName: priya.name,
      receiverId: aarav.uid, receiverName: aarav.name,
      offeredListingId: 'sc_pri_1', offeredTitle: 'Marble-top coffee table', offeredImageUrl: STITCH_PHOTOS.coffeeTable[0],
      requestedListingId: 'sc_aar_2', requestedTitle: 'Wooden study stool', requestedImageUrl: STITCH_PHOTOS.studyStool[0],
      message: 'Happy to swap my coffee table for your study stool!',
      status: 'PENDING', createdAt: now - 3 * HOUR, updatedAt: now - 3 * HOUR,
    });
  }
  if (!(await docExists(config, aarav.token, 'exchangeRequests/sc_exc_2'))) {
    const te = now - 2 * DAY;
    await setDoc(config, aarav.token, 'exchangeRequests/sc_exc_2', {
      id: 'sc_exc_2',
      senderId: aarav.uid, senderName: aarav.name,
      receiverId: dadarwood.uid, receiverName: dadarwood.businessName,
      offeredListingId: 'sc_aar_4', offeredTitle: 'Side table (swap)', offeredImageUrl: STITCH_PHOTOS.sideTable[0],
      requestedListingId: 'sc_dad_4', requestedTitle: 'Carved wall shelf (swap)', requestedImageUrl: STITCH_PHOTOS.carvedShelf[0],
      message: 'Would love the carved shelf for my living room — offering my walnut side table.',
      status: 'PENDING', createdAt: te, updatedAt: te,
    });
    await updateDoc(config, dadarwood.token, 'exchangeRequests/sc_exc_2', { status: 'ACCEPTED', updatedAt: te + 5 * HOUR });
  }
  await updateDoc(config, aarav.token, 'listings/sc_aar_4', { status: 'RESERVED', updatedAt: now - DAY });
  await updateDoc(config, dadarwood.token, 'listings/sc_dad_4', { status: 'RESERVED', updatedAt: now - DAY });

  // Material offer on Dadar Woodcraft's teak planks request
  if (!(await docExists(config, aarav.token, `materialRequests/demo_material_1/offers/${aarav.uid}`))) {
    await setDoc(config, aarav.token, `materialRequests/demo_material_1/offers/${aarav.uid}`, {
      id: aarav.uid, requestId: 'demo_material_1',
      offererId: aarav.uid, offererName: aarav.name,
      listingId: 'sc_aar_1', listingTitle: 'Cane armchair', listingImage: STITCH_PHOTOS.caneArmchair[0],
      status: 'PENDING', createdAt: now - 6 * HOUR, updatedAt: now - 6 * HOUR,
    });
  }

  // Chats
  await seedChat(aarav, dadarwood, 'sc_rep_2', 'REPAIR_REQUEST', 'Sheesham coffee table', STITCH_PHOTOS.coffeeTable[0], [
    { from: aarav, text: 'Namaste Prakash bhai, I shared photos of the coffee table with water rings.', ago: 2 * DAY },
    { from: dadarwood, text: 'Namaste Aarav ji! Saw the photos. The wood grain underneath is in great shape.', ago: 2 * DAY - 20 * MIN },
    { from: dadarwood, text: 'I have sent a quote for ₹1,650 including stripping, two coats of French polish, and pickup/drop.', ago: 2 * DAY - 40 * MIN },
    { from: aarav, text: 'Thank you! Does that include matching the warm walnut tone on the legs?', ago: DAY },
    { from: dadarwood, text: 'Yes, we hand-mix the shellac so the top matches the legs seamlessly. Also your oak chair is ready for pickup!', ago: 35 * MIN },
  ], { [aarav.uid]: now - 2 * HOUR, [dadarwood.uid]: now - 10 * MIN });

  await seedChat(aarav, priya, 'sc_aar_1', 'LISTING', 'Cane armchair', STITCH_PHOTOS.caneArmchair[0], [
    { from: priya, text: 'Hi Aarav! Is the cane armchair still available?', ago: 5 * HOUR },
    { from: aarav, text: 'Hi Priya, yes it is! The cane seat was re-woven recently.', ago: 4 * HOUR + 30 * MIN },
    { from: priya, text: 'Lovely! I sent a buy request for ₹3,200 — can pick up from Bandra this Saturday.', ago: 4 * HOUR },
  ], { [aarav.uid]: now - 5 * HOUR, [priya.uid]: now - 3 * HOUR });

  await seedChat(rohan, dadarwood, 'sc_rep_3', 'REPAIR_REQUEST', 'Double-door wardrobe', STITCH_PHOTOS.wardrobe[0], [
    { from: rohan, text: 'Prakash ji, I have transferred ₹1,400 via UPI for the wardrobe hinge repair.', ago: 40 * MIN },
    { from: dadarwood, text: 'Thank you Rohan ji, checking my bank app now and will confirm shortly.', ago: 20 * MIN },
  ], { [rohan.uid]: now - 15 * MIN, [dadarwood.uid]: now - 50 * MIN });

  // Notifications
  const notifications = [
    { id: 'sc_not_1', from: dadarwood, to: aarav, type: 'REPAIR_UPDATE', relatedType: 'REPAIR_REQUEST', title: 'Quote received: ₹1,650', body: 'Dadar Woodcraft & Repairs sent a quote for your Sheesham coffee table.', ref: 'sc_rep_2', ago: 2 * DAY - 4 * HOUR, seen: false },
    { id: 'sc_not_2', from: dadarwood, to: aarav, type: 'REPAIR_UPDATE', relatedType: 'REPAIR_REQUEST', title: 'Repair ready for pickup', body: 'Your Oak dining chair at Dadar Woodcraft & Repairs is ready.', ref: 'sc_rep_4', ago: 2 * HOUR, seen: false },
    { id: 'sc_not_3', from: priya, to: aarav, type: 'NEW_OFFER', relatedType: 'PURCHASE_REQUEST', title: 'New buy request on Cane armchair', body: 'Priya Deshmukh offered ₹3,200 for your Cane armchair.', ref: 'sc_pur_1', ago: 4 * HOUR, seen: false },
    { id: 'sc_not_4', from: priya, to: aarav, type: 'EXCHANGE_REQUEST', relatedType: 'EXCHANGE_REQUEST', title: 'Swap offer received', body: 'Priya Deshmukh offered Marble-top coffee table for your Wooden study stool.', ref: 'sc_exc_1', ago: 3 * HOUR, seen: true },
    { id: 'sc_not_5', from: andherimart, to: aarav, type: 'PURCHASE_UPDATE', relatedType: 'PURCHASE_REQUEST', title: 'Buy request accepted', body: 'Andheri Furniture Mart accepted your request for Study table with drawers.', ref: 'sc_pur_3', ago: DAY, seen: true },
    { id: 'sc_not_6', from: priya, to: dadarwood, type: 'REPAIR_UPDATE', relatedType: 'REPAIR_REQUEST', title: 'New repair request', body: 'Priya Deshmukh requested a repair for Cane rocking chair.', ref: 'sc_rep_1', ago: 3 * HOUR, seen: false },
    { id: 'sc_not_7', from: rohan, to: dadarwood, type: 'NEW_OFFER', relatedType: 'PURCHASE_REQUEST', title: 'New buy request on Restored teak peg stool', body: 'Rohan Kulkarni wants to buy Restored teak peg stool for ₹2,400.', ref: 'sc_pur_2', ago: 90 * MIN, seen: false },
  ];
  for (const n of notifications) {
    if (await docExists(config, n.to.token, `notifications/${n.id}`)) {
      await deleteDoc(config, n.to.token, `notifications/${n.id}`);
    }
    await setDoc(config, n.from.token, `notifications/${n.id}`, {
      id: n.id, recipientId: n.to.uid, senderId: n.from.uid,
      type: n.type, title: n.title, body: n.body, relatedType: n.relatedType, relatedId: n.ref,
      seen: false, createdAt: now - n.ago,
    });
    if (n.seen) {
      await updateDoc(config, n.to.token, `notifications/${n.id}`, { seen: true });
    }
  }
  log('Stitch showcase listings, requests, chats and notifications written');
}

async function seedChat(a, b, contextId, contextType, contextTitle, contextImage, messages, readAt) {
  const [first, second] = [a.uid, b.uid].sort();
  const chatId = `${first}_${second}_${contextId}`;
  if (await docExists(config, a.token, `chats/${chatId}`)) return;
  const nameOf = (p) => (p.kind === 'vendor' ? p.businessName : p.name);
  const last = messages[messages.length - 1];
  await setDoc(config, a.token, `chats/${chatId}`, {
    id: chatId,
    participantIds: [first, second],
    participantNames: { [a.uid]: nameOf(a), [b.uid]: nameOf(b) },
    lastMessage: last.text,
    lastMessageAt: now - last.ago,
    lastMessageSenderId: last.from.uid,
    readAt,
    contextId,
    contextType,
    contextTitle,
    contextImage,
    createdAt: now - messages[0].ago,
  });
  for (const [i, m] of messages.entries()) {
    const msgId = `m_${i + 1}`;
    await setDoc(config, m.from.token, `chats/${chatId}/messages/${msgId}`, {
      id: msgId, chatId, senderId: m.from.uid, text: m.text, sentAt: now - m.ago,
    });
  }
}

/**
 * True when this job already finished on an earlier run. A job that was interrupted half way (still REQUESTED) is removed
 * so it can be redone; anything else in between is left alone.
 */
async function isDone(customer, path) {
  const status = await docStatus(config, customer.token, path);
  if (status === null) return false;
  if (status === 'COMPLETED') return true;
  if (status === 'REQUESTED') {
    await deleteDoc(config, customer.token, path);
    return false;
  }
  console.log(`  skipping ${path}: it is ${status}`);
  return true;
}

function listing(id, seller, item, city, price, exchange, created, imageFor) {
  const isVendor = seller.kind === 'vendor';
  return {
    id, ownerId: seller.uid, ownerName: isVendor ? seller.businessName : seller.name, title: item.title, description: item.description,
    images: item.images ?? [imageFor(item.category, item.variant), imageFor(item.category, item.variant + 1)], price, condition: item.condition,
    refurbished: item.refurbished ?? (seller.businessType === 'REFURBISHER'), category: item.category, material: item.material, ageYears: item.ageYears,
    dimensions: { lengthCm: item.dims[0], widthCm: item.dims[1], heightCm: item.dims[2] },
    sellerType: isVendor ? 'VENDOR' : 'INDIVIDUAL', actionType: exchange ? 'EXCHANGE' : 'SELL', city, status: 'ACTIVE',
    createdAt: created, updatedAt: created,
  };
}

async function review(customer, vendor, contextType, jobId, job, at) {
  const id = `${customer.uid}_${contextType}_${jobId}`;
  await setDoc(config, customer.token, `reviews/${id}`, {
    id, reviewerId: customer.uid, reviewerName: customer.name, targetUserId: vendor.uid, contextType, contextId: jobId,
    rating: job.stars, comment: job.comment, createdAt: at, updatedAt: at,
  });
}

async function removeEverything() {
  for (const p of Object.values(sessions)) {
    const kept = keep.has(p.key);
    // A kept account only loses its listings; everyone else loses everything they own, and then the account itself.
    const owned = kept
      ? [['listings', 'ownerId']]
      : [['listings', 'ownerId'], ['materialRequests', 'vendorId'], ['reviews', 'reviewerId'], ['favourites', 'userId'], ['blocks', 'blockerId'], ['notifications', 'recipientId'], ['deviceTokens', 'userId']];
    for (const [collection, field] of owned) {
      for (const id of await idsWhere(config, p.token, collection, field, p.uid)) await deleteDoc(config, p.token, `${collection}/${id}`);
    }
    if (kept) continue;
    await deleteDoc(config, p.token, `${p.kind === 'user' ? 'users' : 'vendors'}/${p.uid}`);
    await deleteAccount(config, p.token);
  }
  log(keep.size ? `Removed the demo accounts except ${[...keep].join(', ')}, and everyone's listings, material requests and reviews.` : 'Removed the demo accounts, their listings, material requests and reviews.');
  log('Finished repair, recycling and purchase records stay in Firestore, because the security rules only let their other party delete them. They are harmless; delete the collections in the console for a fully clean database.');
}
