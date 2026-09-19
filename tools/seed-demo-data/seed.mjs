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
import { uploadIllustrations } from './lib/cloudinary.mjs';
import { deleteAccount, deleteDoc, docExists, docStatus, ensureAccount, idsWhere, loadConfig, setDoc, updateDoc } from './lib/firebase.mjs';

const args = new Set(process.argv.slice(2));
const config = loadConfig({ emulator: args.has('--emulator') });
const log = (message) => console.log(message);
const DAY = 86_400_000;
const now = Date.now();

log(`${args.has('--remove') ? 'Removing demo data from' : 'Seeding'} ${config.emulator ? 'the local emulators' : `project ${config.projectId}`}\n`);

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
      await setDoc(config, p.token, `users/${p.uid}`, { uid: p.uid, name: p.name, email: p.email, phone: p.phone, city: p.city, createdAt: now - 40 * DAY });
    } else if (!(await docExists(config, p.token, `vendors/${p.uid}`))) {
      // A vendor document is only written once, so a `verified` flag set in the console is never overwritten.
      await setDoc(config, p.token, `vendors/${p.uid}`, {
        uid: p.uid, name: p.name, email: p.email, phone: p.phone, businessName: p.businessName, businessType: p.businessType,
        city: p.city, shopAddress: `${p.area}, ${p.city}`, description: p.description, verified: false,
        brochureImages: [imageFor('SOFA', 0), imageFor('WARDROBE', 1)], shopLatitude: p.lat, shopLongitude: p.lng,
        repairServices: p.repairServices ?? [], repairCostMin: p.repairCostMin ?? null, repairCostMax: p.repairCostMax ?? null,
        createdAt: now - 40 * DAY,
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
      if (await docExists(config, seller.token, `listings/${id}`)) continue;
      const exchange = EXCHANGE_SLOTS.has(i);
      const price = Math.round((item.price * CITY_PRICE_FACTOR[city]) / 100) * 100;
      const created = now - (i * 30 + (poolIndex % 7) * 5) * 3_600_000;
      await setDoc(config, seller.token, `listings/${id}`, listing(id, seller, item, city, exchange ? 0 : price, exchange, created, imageFor));
      listingCount++;
    }
  }
  log(`Listings written: ${listingCount} new`);

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
      for (const status of ['ACCEPTED', 'IN_PROGRESS', 'READY', 'COMPLETED']) await updateDoc(config, vendor.token, path, { status, updatedAt: day + DAY });
      await review(customer, vendor, 'REPAIR_REQUEST', jobId, job, day + 2 * DAY);
    } else if (job.type === 'recycle') {
      const path = `recyclingRequests/${jobId}`;
      if (await isDone(customer, path)) continue;
      await setDoc(config, customer.token, path, {
        id: jobId, userId: customer.uid, userName: customer.name, vendorId: vendor.uid, vendorName: vendor.businessName,
        condition: job.condition, material: job.material, method: 'DROP_OFF', images: [imageFor('WARDROBE', i)], city: customer.city,
        status: 'REQUESTED', createdAt: day, updatedAt: day,
      });
      for (const status of ['ACCEPTED', 'SCHEDULED', 'COMPLETED']) await updateDoc(config, vendor.token, path, { status, updatedAt: day + DAY });
      await review(customer, vendor, 'RECYCLING_REQUEST', jobId, job, day + 2 * DAY);
    } else {
      const path = `purchaseRequests/${jobId}`;
      if (await isDone(customer, path)) continue;
      const item = POOL[(i * 3) % POOL.length];
      const listingId = `demo_sold_${i + 1}`;
      await setDoc(config, vendor.token, `listings/${listingId}`, listing(listingId, vendor, item, vendor.city, item.price, false, day - DAY, imageFor));
      await setDoc(config, customer.token, path, {
        id: jobId, listingId, listingTitle: item.title, buyerId: customer.uid, buyerName: customer.name, sellerId: vendor.uid, sellerName: vendor.businessName,
        offeredPrice: item.price, message: '', status: 'REQUESTED', paid: false, createdAt: day, updatedAt: day,
      });
      await updateDoc(config, vendor.token, path, { status: 'ACCEPTED', upiId: `${vendor.key.slice(0, 12)}@okhdfcbank`, updatedAt: day + DAY });
      await updateDoc(config, vendor.token, `listings/${listingId}`, { status: 'RESERVED', updatedAt: day + DAY });
      await updateDoc(config, customer.token, path, { paid: true, paidAt: day + DAY, updatedAt: day + DAY });
      await updateDoc(config, vendor.token, path, { status: 'READY_FOR_PICKUP', updatedAt: day + 2 * DAY });
      await updateDoc(config, vendor.token, path, { status: 'COMPLETED', updatedAt: day + 2 * DAY });
      await updateDoc(config, vendor.token, `listings/${listingId}`, { status: 'SOLD', updatedAt: day + 2 * DAY });
      await review(customer, vendor, 'PURCHASE_REQUEST', jobId, job, day + 3 * DAY);
    }
    jobCount++;
  }
  log(`Finished jobs with reviews written: ${jobCount} new`);

  log(`\nDone. Log in with any demo email (demo.<name>@homesajja.demo), password ${DEMO_PASSWORD}.`);
  log('Example accounts: demo.aarav@homesajja.demo (user, Mumbai), demo.dadarwood@homesajja.demo (repair vendor, Mumbai).');
  log('The "Verified" badge is not set here on purpose (vendors can\'t verify themselves): tick `verified` on a few vendors in the Firebase console.');
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
    images: [imageFor(item.category, item.variant), imageFor(item.category, item.variant + 1)], price, condition: item.condition,
    refurbished: seller.businessType === 'REFURBISHER', category: item.category, material: item.material, ageYears: item.ageYears,
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
    const owned = [['listings', 'ownerId'], ['materialRequests', 'vendorId'], ['reviews', 'reviewerId'], ['favourites', 'userId'], ['blocks', 'blockerId'], ['notifications', 'recipientId'], ['deviceTokens', 'userId']];
    for (const [collection, field] of owned) {
      for (const id of await idsWhere(config, p.token, collection, field, p.uid)) await deleteDoc(config, p.token, `${collection}/${id}`);
    }
    await deleteDoc(config, p.token, `${p.kind === 'user' ? 'users' : 'vendors'}/${p.uid}`);
    await deleteAccount(config, p.token);
  }
  log('Removed the demo accounts, their listings, material requests and reviews.');
  log('Finished repair, recycling and purchase records stay in Firestore, because the security rules only let their other party delete them. They are harmless; delete the collections in the console for a fully clean database.');
}
