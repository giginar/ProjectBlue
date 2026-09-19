# Ads integration

Reviewed against official documentation on 2026-09-19. The implementation uses the Java
Google Mobile Ads SDK (documented as Legacy), pinned to **25.5.0**, and **UMP 4.0.0**.
These versions were verified in Google's [setup guide](https://developers.google.com/admob/android/quick-start),
[release notes](https://developers.google.com/admob/android/rel-notes), and
[UMP guide](https://developers.google.com/admob/android/privacy). No mediation adapters are installed.

## Boundaries and startup

`core` only knows `AdsService` and `ConsentService`; their new methods have default implementations.
`AndroidAdsService` and `AndroidConsentService` live in `android`. Desktop keeps No-Op services.
The game starts loading assets and displaying menus independently of consent/network callbacks.
No progression, level unlock, purchase, or achievement requires an ad.

UMP requests consent information on each Activity creation, then loads/shows the required form.
Only UMP's `canRequestAds()` authorizes SDK initialization and ad requests, including after errors
or when UMP recognizes a prior session. No profile flag, IP inference, or cached consent string
replaces that decision. Initialization is guarded against duplicate calls; SDK initialization
runs off the UI thread. Load/show operations run on the Android UI thread and callbacks return
to the game thread. Privacy forms suspend requests and invalidate previously loaded ads.

Settings shows **Privacy Options** when UMP reports it is required; the entry updates while
Settings is visible. Returning from the form reevaluates permission and discards stale ads.
Load failures have a 30-second retry floor, retried at resume/result boundaries; no network
wait blocks gameplay. Cached ads expire conservatively after 55 minutes.

## Rewards and persistence

Successful results (including the campaign finale) offer double the run's salvage. Failed
results offer one continue: full hull, three seconds of protection, and 60 added seconds.
Continue preserves the frozen world and returns through Pause; the player chooses Resume Dive.
An unavailable button explains that the player can keep playing. Rewards require an explicit tap.

Only an SDK earned-reward event followed by dismissal can settle a reward. Dismissal without
that event, show failure, stale callbacks, and duplicate callbacks grant nothing. This follows
the callback ordering guaranteed for Google-served ads in the official
[rewarded guide](https://developers.google.com/admob/android/rewarded); adding mediation requires
revisiting its different ordering contracts.

Each dive has a random local ID. Claim flags and extra salvage commit in one profile write,
before the in-memory reward changes. Continue is persisted before resuming the world. The
second result adds only salvage/enemy/plastic deltas, avoiding double payment of the first result.
Save errors never grant a reward. An eligible player can retry after fixing storage.
Optional fields preserve existing profile schemas and migration paths.

Active-run authority exists only in the running process. Relaunch cannot replay an old result,
continue, or reward; earned but unsettled rewards may be lost if the process dies before dismissal
or persistence. This is local at-most-once protection, not server verification or protection
against deliberate save-file editing/rollback. No backend or SSV service was introduced.

## Interstitial placement

Only leaving a successful result/finale for Main Menu, Level Select, or Hangar can show an
interstitial. It never appears during gameplay, on failure, at app open, on replay, or after
using a rewarded opportunity on that result. The first session is completely exempt.
Later sessions need all of: three successful runs since the last reserved slot, three minutes
since session start, and three minutes since the previous slot. Counts/time persist; clock
rollback suppresses eligibility. The slot is saved before showing; a failed show conservatively
uses the slot. No-fill or show failure completes navigation normally. These are product choices,
guided by Google's [interstitial guidance](https://developers.google.com/admob/android/interstitial).

## Configuration

Debug always uses the official sample App ID and rewarded/interstitial units from Google's
[test IDs](https://developers.google.com/admob/android/test-ads); environment-provided production
IDs cannot override debug. Production values must be supplied through environment variables:

| Variable | Meaning |
| --- | --- |
| `PB_ADS_ENABLED` | Explicit `true` enables release ads; absent means disabled |
| `PB_ADMOB_APP_ID` | Registered production AdMob App ID |
| `PB_REWARDED_AD_ID` | Production rewarded unit |
| `PB_INTERSTITIAL_AD_ID` | Production interstitial unit |
| `PB_AD_AGE_TREATMENT` | Explicit `UNSPECIFIED`, `CHILD`, or `TEEN` |
| `PB_UMP_UNDER_AGE` | Explicit `true` or `false` for UMP's under-age setting |

Release enabling requires all identifiers and both audience settings; no age is inferred.
Google's [targeting guide](https://developers.google.com/admob/android/targeting) documents
`AgeRestrictedTreatment` as the replacement for deprecated TFCD/TFUA ad request flags. UMP's
separate under-age setting is not inferred from it. The ad-content ceiling is G. Mixed-age
products requiring per-user age decisions need an owner-approved age flow before enabling ads;
the current configuration is app-wide. Unknown debug audience settings leave SDK defaults unset.

With ads disabled, release does not request consent/ads or initialize MobileAds through app code.
It retains the sample App ID for SDK manifest validation; no production IDs are generated.
Do not publish the verification artifact as a configured advertising release.

## Google Play Games extension

`LocalAchievementService` remains authoritative and already exposes `synchronize(AchievementService)`.
An Android-only adapter can map stable local enum names to Play achievement IDs, send absolute
progress with `AchievementsClient.setSteps`, and unlock completed achievements after sign-in.
Retry on reconnection without clearing local facts or blocking notifications. Unmapped IDs and
offline/sign-in failures must be harmless. See the official
[Play Games achievement API](https://developer.android.com/games/pgs/android/achievements).
No Play Games SDK, account sign-in, OAuth configuration, or automatic synchronization is activated.
Required future inputs: Play Games project/app configuration, signing certificate fingerprints,
achievement ID/type mappings, tester accounts, and an approved sign-in experience.

## Validation

`AdsIntegrationTest` covers earned/closed/failed/duplicate/stale callbacks, restart replay,
save failures, one continue, incremental results, caps, consent permission, and desktop No-Op/offline logic.
Run `gradlew.bat :core:test :android:verifyDebugAdConfiguration` and
`powershell -File tools/verify-release-inputs.ps1`.

The 2026-09-19 local device pass used only Google's official sample identifiers. UMP ran on the
API 29 physical phone and API 36 emulator and returned a non-EEA result (`gdprApplies=0`). The
emulator loaded Google's visible `Test Ad`, emitted `Reward granted`, dismissed normally and
returned to the one-time continue Pause flow. Offline starts on both targets logged rewarded and
interstitial load error 0 without blocking menus or Blue Coast gameplay. A device-discovered
stale Pause HUD label after continue was fixed by immediately refreshing cached HUD text and is
covered by `HudTest`.

**MANUAL VERIFICATION REQUIRED:** forced EEA consent/denial/Privacy Options, early rewarded
dismissal through the real SDK, background/process death during the ad/form, and a real
interstitial after the three-win/session cooldown. Production identifiers were not supplied or
shown, and the minified QA build keeps ads disabled.

## Changed files in this integration

Pre-existing uncommitted work was retained. Integration edits are limited to:

- Build/privacy configuration: `.gitignore`, `android/build.gradle`, `android/proguard-rules.pro`,
  `android/src/main/AndroidManifest.xml`, `android/src/main/res/xml/data_extraction_rules.xml`.
- Android Java: `AndroidLauncher`, `AndroidPlatformService`, new `AndroidAdsService` and
  `AndroidConsentService` under `android/src/main/java/com/projectblue/game/android/`.
- Core Java: `ProjectBlueGame`, `logic/GameWorld`; `platform/AdsService`, `ConsentService`,
  new `ConsentGate`, `RewardedCompletion`, `RunRewards`, `InterstitialPolicy`;
  `save/Profile`, `ProfileCodec`, `SaveService`; `screens/ScreenRouter`, `StageMenuScreen`,
  `ResultScreen`, `FinaleScreen`, `SettingsScreen`, and the existing untracked `PrivacyScreen`.
- Tests/tools: `core/src/test/java/com/projectblue/game/platform/AdsIntegrationTest.java`,
  `tools/verify-release-inputs.ps1`, `tools/verify-android-artifacts.ps1`.
- Documentation: `README.md`, `docs/ANDROID_TESTING.md` and the five ads/privacy/Play/signing/
  Data Safety documents requested for this task. No game assets or licenses were changed by
  this integration.
