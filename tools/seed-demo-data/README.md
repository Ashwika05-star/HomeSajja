# Demo data

Fills a HomeSajja Firebase project with realistic Indian demo data so the app feels like a real marketplace for a demo or viva.

| What | How many |
|---|---|
| Users (3 per city: Mumbai, Pune, Bengaluru, Delhi, Hyderabad) | 15 |
| Vendors (shops, repair professionals, carpenters, refurbishers, recyclers) | 17 |
| Listings in rupees (sale and exchange, 9 per city, plus a few sold items) | about 49 |
| Material requests | 7 |
| Finished repair, recycling and purchase jobs, each with a review | 17 |

Listing pictures are simple flat furniture illustrations drawn by the tool (no one's photos), uploaded to your Cloudinary account.

```bash
node seed.mjs                # the project in app/google-services.json
node seed.mjs --emulator     # the local Auth (9099) and Firestore (8080) emulators
node seed.mjs --remove       # remove the demo accounts and what they own
```

Needs Node 18+ and nothing to install. **Firestore rules must already be deployed** (the tool writes as the demo users, so the
security rules apply to it exactly as to real users). It is safe to run again: what already exists is skipped, and a job that was
interrupted half way is redone.

- **Login:** `demo.<name>@homesajja.demo` with password `HomeSajja#Demo1`. Examples: `demo.aarav@homesajja.demo` (user, Mumbai),
  `demo.dadarwood@homesajja.demo` (repair vendor, Mumbai), `demo.andherimart@homesajja.demo` (shop, Mumbai).
- **Verified badge:** vendors cannot verify themselves, so the tool leaves `verified` false. For a demo, open the Firebase console →
  Firestore → `vendors` and set `verified` to `true` on a few vendors.
- **Removing:** `--remove` deletes the accounts, their listings, material requests and reviews. Finished repair, recycling and purchase
  records stay behind (the security rules only let their other party delete them); they are harmless.
