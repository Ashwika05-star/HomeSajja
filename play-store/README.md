# Publishing HomeSajja on Google Play

Everything the store needs is in this folder; the signed bundle is built from the project. **Uploading is a step only you can do**
(it needs your Play Console account, which has a one-time registration fee), so this is the checklist.

## What is here

| File | Use |
|---|---|
| `listing.md` | Title, short and full description, category, reviewer login, content-rating notes, release notes |
| `icon-512.png` | App icon (512 × 512) |
| `feature-graphic-1024x500.png` | Feature graphic |
| `screenshots/` | 10 phone screenshots (1080 × 2160) |
| `privacy-policy.md` / `.html` | Privacy policy (contact: ashwikasinha27@gmail.com) |
| `data-safety.md` | Answers for the Data safety form |

## 1. Build the signed bundle

The upload key was generated on this Mac at `release/homesajja-upload.jks`, with its passwords in `keystore.properties`. Both are
**git-ignored and exist only on this machine: back them up now** (copy both files somewhere safe, such as a password manager).
With Play App Signing (step 3) Google keeps the real app-signing key, so a lost upload key can be reset through Play support, but
you would have to ask.

```bash
./gradlew bundleRelease
```

The bundle is `app/build/outputs/bundle/release/app-release.aab`. Before every new upload, raise `versionCode` in
`app/build.gradle.kts` by one (Play rejects a repeat) and update `versionName`.

Upload key SHA-256 (useful when adding Google sign-in to the release build):
`21:C9:8D:E3:39:0B:1A:AF:B5:A1:8A:02:CD:95:ED:D3:ED:C5:91:0B:A4:A4:01:EB:AB:76:64:7F:16:2B:5F:59`

## 2. Two things to do in Firebase before the release build is fully usable

- **Google sign-in:** Firebase console → Project settings → your Android app → **Add fingerprint** → paste the SHA-256 above (and later
  the *App signing* SHA-256 that Play Console shows once the app is uploaded). Then download `google-services.json` again into `app/`.
- **AI:** Build → **AI Logic** → Get started → Gemini Developer API (the AI screens show a friendly message until this is on).

## 3. Create the app in Play Console

1. **Create app** → name `HomeSajja: Furniture Reuse`, default language English (India), App, Free.
2. **Set up your app** dashboard (all under *App content*): privacy policy URL (see below), App access (use the demo logins in
   `listing.md`), Ads (none), Content rating (see `listing.md`), Target audience (18+), **Data safety** (`data-safety.md`),
   Government / Financial / Health apps (none).
3. **Store listing:** paste the texts from `listing.md`; upload `icon-512.png`, `feature-graphic-1024x500.png` and the screenshots.
4. **Testing → Internal testing → Create new release:** accept **Play App Signing** when asked, upload `app-release.aab`, paste the release
   notes, and save. Add testers under *Testers* (a list of Gmail addresses), then **Start rollout** and share the opt-in link.
   Internal testing has no review wait, so testers can install within minutes.

### Host the privacy policy
Play needs a public URL. The easy way is GitHub Pages: in the GitHub repo choose Settings → Pages → Deploy from a branch → `main` and
the `/play-store` folder (or copy `privacy-policy.html` to any website you control). The URL then looks like
`https://<owner>.github.io/HomeSajja/privacy-policy.html`.

## 4. Before a public (Production) release
- Complete the *Closed testing* requirement Play sets for new personal accounts (a number of testers for 14 days), if it applies to yours.
- Deploy the optional push Cloud Function (`functions/`) only if you move to Firebase's paid plan.
- Set `verified` to `true` in Firestore only on vendors you have actually checked.
