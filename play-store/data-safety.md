# Data safety form answers (Play Console → App content → Data safety)

Use these to fill the form. "Collected" = leaves the device; "Shared" = sent to a third party other than a service provider acting for us.
Firebase, Cloudinary and Google's AI service act as **service providers** processing data for HomeSajja, which Play does not count as "sharing".

**Does the app collect or share required user data types?** Yes.
**Is all user data encrypted in transit?** Yes (HTTPS/TLS).
**Can users request that their data be deleted?** Yes: in-app (Profile → Delete my account) and by email.

| Data type | Collected | Shared | Optional? | Purpose |
|---|---|---|---|---|
| Name | Yes | No | Required | Account, shown on listings, requests, reviews |
| Email address | Yes | No | Required | Account, sign-in |
| Phone number | Yes | No | Required at signup | Account |
| User IDs (account id) | Yes | No | Required | App functionality |
| Photos | Yes | No | Optional | Listings, requests, profile, AI suggestions |
| Other user-generated content (listings, reviews, reports) | Yes | No | Optional | App functionality |
| Messages (in-app chat) | Yes | No | Optional | App functionality |
| Approximate location: the city the user picks | Yes | No | Required | Show local listings and vendors |
| Precise location: the shop location a vendor picks on a map (not GPS) | Yes | No | Optional (vendors) | Public shop profile |
| Device or other IDs (push token) | Yes | No | Optional | Notifications |
| Financial info | **No** | No | n/a | Payments happen in Google Pay; only a seller-entered UPI id is stored |

Not collected: contacts, calendar, audio, video, health, web browsing, app activity/analytics, crash logs, advertising id.

**Security practices:** data encrypted in transit; users can request deletion; independent security review: no.
**Ads:** the app contains no ads.
**Target audience:** 18 and over.
**News/Government/Financial features:** none. (Payments are handled by Google Pay; HomeSajja is not a financial app.)
