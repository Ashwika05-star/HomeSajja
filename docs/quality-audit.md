# Quality audit (Phase 11)

## 1. State handling: every screen has a real state for each situation

Each list or detail screen handles **Loading** (a spinner announced as "Loading"), **Error** ("We hit a snag" with the reason and *Try again*),
**Empty** (a warm, specific message and a next step) and, where the screen can be searched or filtered, **No results** (with *Show all*).
Forms show progress on their button and errors inline.

| Area | Screens | Loading | Error | Empty | No results |
|---|---|---|---|---|---|
| Marketplace | Explore, Exchange browse, Listing detail, My listings, Requests (sent/received), Sell flow, Saved | yes | yes | yes | Explore, Exchange (search and filters) |
| Exchange | Proposal flow, Exchange requests, Exchange detail | yes | yes | yes | n/a (no filter) |
| Repair | Request flow, providers, My repairs, Repair detail | yes | yes | yes | My repairs (Active/Closed) |
| Recycle | Request flow, recyclers, My recycling, Recycle detail | yes | yes | yes | My recycling (Active/Closed) |
| Vendor | Dashboard, Listings, Incoming requests (4 sections + open pickups), Materials, Profile, Profile edit | yes | yes | yes | Materials board (filter) |
| Materials | Board, Form, Detail with offers | yes | yes | yes | Board (by material) |
| Social | Chat list, Chat thread, Notifications, Blocked people, User profile | yes | yes | yes | Chat list and Notifications (Unread) |
| AI | Ask HomeSajja (input / thinking / suggestion / failed), Sajja AI (welcome / thinking / error with retry) | yes | yes (never blocks the person) | yes | n/a |
| Entry | Splash, Welcome, Login, Signup | n/a / button progress | inline error | n/a | n/a |

Wording was made consistent: one error title (*We hit a snag*), one retry label (*Try again*).

## 2. Accessibility

- **Contrast:** measured every text and border colour pair the app uses (WCAG). All text is at least 4.6:1 (most 6 to 12:1). Two pairs were
  too low and were darkened: field borders (2.9:1 → 3.4:1) and the rose accent (2.8:1 → 4.0:1). `ContrastTest` keeps this true.
- **Touch targets:** three controls were smaller than 48dp (the heart on cards, the remove-photo button, the tappable rating stars). Each now
  has a 48dp touch area with the smaller visual drawn inside it. Buttons, chips and list rows already met the minimum.
- **Screen readers:** decorative icons next to a text label have no description; every icon that stands alone has one (Chats and
  Notifications include their unread counts; photos say "Photo 2 of 4" or "No photo"). Screen titles and empty/error titles are
  marked as headings. Status steppers read "Accepted, done" or "Ready, not reached yet". Loading is announced. New error banners are
  announced as they appear. Star ratings read "4 out of 5 stars" and each tappable star has its own label.
- **Order:** every screen follows the natural top-to-bottom, left-to-right order, so focus order is logical without overrides.

## 3. Performance

Measured on the emulator with StrictMode (debug builds now log slow work on the main thread):

| Finding | Fix | Result |
|---|---|---|
| Firebase and the map's tile cache did disk work on the main thread at start-up (about 1,150 violations, one of 470 ms) | Warmed on a background thread at launch (`AppContainer.warmUp`) | 2 left, both the first map view (140 to 180 ms) |
| The AI feature decoded the photo on the main thread | Moved to the IO dispatcher | none |
| Full-size Cloudinary photos loaded into small cards | Ask Cloudinary for a resized, compressed version (`w_560,q_auto,f_auto`; thumbnails 320, full width 1080) | far less data per list |
| Lists | All long lists are lazy (LazyColumn / LazyVerticalGrid) with stable keys; pages are fetched by cursor | no change needed |
| Release build | R8 shrinking and resource shrinking, with rules that keep the Firestore model classes | 4.5 MB APK, 9.3 MB bundle |

## 4. Final QA

Flows exercised end to end on the emulators (with the demo data), as a User and as a Vendor, and again on a signed release build against the
real project: signup and login, browse and search, save (heart) and the Saved grid including "No longer available", sell, buy with a UPI id
and the GPay fallback when Google Pay is missing, "I've paid", exchange, repair (request, provider list, every status, review), recycle
(drop-off and unclaimed pickup claim), material requests and offers, chat (live), notifications (in app and system alert), AI screens (including
the failure path), reviews, report, block and unblock, vendor profile with map, and account deletion.

Bugs found and fixed in this phase:
- Android 7 (API 24 to 25) had no launcher icon files → added a full legacy icon set and a new brand icon.
- Slow start-up work on the main thread → warm-up on a background thread (see above).
- The AI photo decode ran on the main thread → moved off it.
- Tiny touch targets and low-contrast borders → fixed (see accessibility).
- The demo-data tool skipped a job that had been interrupted half way → it now redoes it.
- Payment fields were rejected by security rules that had not been deployed to the real project yet → deployed, and the order is in the README.

Known limits: a push to a fully closed app needs the paid-plan Cloud Function; the Verified badge must be set in the console; Google sign-in
and the AI answers need the two Firebase console steps listed in `play-store/README.md`; Google Pay can only be tried on a phone that has it.
