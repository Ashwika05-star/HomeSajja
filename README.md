# HomeSajja

HomeSajja is an Android app for the full lifecycle of furniture ownership — Buy, Sell,
Exchange, Repair, and Recycle — connecting everyday users with local vendors (shopkeepers,
carpenters, repair professionals, refurbishers, and recyclers) who can also source reusable
materials through Material Requests. Every account picks one of two permanent roles at
signup, User or Vendor, each with its own dedicated dashboard and experience.

## Tech stack

- **Language & UI:** Kotlin, Jetpack Compose, Material 3
- **Architecture:** MVVM (ViewModel + StateFlow), Coroutines, Repository pattern
- **Backend:** Firebase (Authentication, Cloud Firestore, Storage, Cloud Functions, Cloud Messaging)
- **Maps:** Google Maps SDK + Places API (vendor shop locations)
- **AI:** Google Gemini API, called via Cloud Functions (no client-side keys)
- **Dependency injection:** Manual DI (`di/AppContainer`) — the dependency graph is small
  enough that explicit wiring is simpler to trace than introducing Hilt
