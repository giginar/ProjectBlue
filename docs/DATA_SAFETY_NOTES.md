# Data Safety notes

Review date: 2026-09-19. This is an implementation inventory, not a submitted Play declaration.
Complete declarations for the exact signed release, SDK configuration, regions and account setup.

| Component | Current behavior | Declaration review |
| --- | --- | --- |
| Profile/progression/achievements/settings | Local files; no cloud upload; Android backup disabled | Local-only facts are not sent by app code |
| AdMob on Android | Enabled only in debug or explicitly configured release | Advertising SDK data collection/sharing and purposes |
| UMP on Android | Consent information/messages and privacy choices when ads are enabled | Consent-service network/storage and configured messages |
| Desktop | No-Op ads/consent/analytics/achievement provider | No advertising network requests |
| Analytics/account/cloud/Play Games | No active provider or sign-in | Do not claim an integration that is not enabled |
| Vibration | Local feedback only | No sensor collection by the game |

Google's [Mobile Ads data disclosure](https://developers.google.com/admob/android/privacy/play-data-disclosure)
describes SDK handling of IP addresses (which can estimate general location), user product
interactions, diagnostic information, and device/account identifiers. Review collection, sharing,
advertising/fraud-prevention purposes, encryption in transit, and choices for the exact SDK and
configuration. Do not declare “no data collected” for an ad-enabled build simply because gameplay
and saves are offline. There is no precise-location permission in the game.

The SDK's merged manifest can contribute `AD_ID`, network and AdServices-related permissions.
Explicit Android 12+ data extraction rules exclude local storage from both cloud backup and
device transfer, in addition to the existing `allowBackup=false`; see
[Android backup behavior](https://developer.android.com/about/versions/12/behavior-changes-12#backup-restore).
Inspect the generated release manifest and Play's Advertising ID declaration, and ensure the
target-audience/Families answers agree with the configured age treatment. Adding an SDK can
change disclosure requirements without adding a new game feature.

The disabled-ad verification release does not call UMP update or MobileAds initialization through
app code; the libraries and manifest declarations remain packaged. Verify actual network behavior
on devices before making a privacy claim about that release. No raw consent string, request ID,
ad ID, device hash, signing secret or ad error payload is logged by this integration. App libGDX
logging is disabled in release; R8 removes verbose/debug/info Android logs. SDK behavior still
requires inspection of the final build on a device.

Owner checklist: public privacy-policy URL and developer contact, ads declaration, audience,
app access, content rating, data collection/sharing purposes, deletion/retention disclosures,
regional consent messages, and store listing consistency. Google's
[app review declarations guide](https://support.google.com/googleplay/android-developer/answer/9859455)
and [Data Safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469)
are the authoritative starting points. Reassess this inventory before adding mediation,
analytics, Play Games sign-in, server-side verification, or cloud saves.
