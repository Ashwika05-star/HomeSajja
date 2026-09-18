# HomeSajja

HomeSajja is an Android app for the full lifecycle of furniture ownership — Buy, Sell,
Exchange, Repair, and Recycle — connecting everyday users with local vendors (shopkeepers,
carpenters, repair professionals, refurbishers, and recyclers) who can also source reusable
materials through Material Requests. Every account picks one of two permanent roles at
signup, User or Vendor, each with its own dedicated dashboard and experience.

## Tech stack

- **Language & UI:** Kotlin, Jetpack Compose, Material 3
- **Architecture:** MVVM (ViewModel + StateFlow), Coroutines, Repository pattern
- **Backend:** Firebase (Authentication, Cloud Firestore, Cloud Messaging); listing photos on Cloudinary (free tier)
- **Maps:** Google Maps SDK + Places API (vendor shop locations)
- **AI:** Google Gemini API, called via Cloud Functions (no client-side keys)
- **Dependency injection:** Manual DI (`di/AppContainer`) — the dependency graph is small
  enough that explicit wiring is simpler to trace than introducing Hilt

## Local development with Firebase emulators

There is no need for a real Firebase project to try the app. With the Firebase CLI installed, start
the Auth and Firestore emulators (they load `firestore.rules`):

```bash
firebase emulators:start --only auth,firestore --project homesajja-placeholder
```

then install a debug build that points at them (off by default; debug builds only):

```bash
./gradlew installDebug -PuseEmulator=true
```

The security rules have their own test suite in `firestore-rules-tests/` (`npm install && npm test`;
needs Node and JDK 21+). Firestore composite indexes are in `firestore.indexes.json` and are deployed
to a real project with `firebase deploy --only firestore`.
