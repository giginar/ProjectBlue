# Google Play release checklist

## Final release identity (2026-09-21)

- Game: **Project Blue: Ocean Guard**
- Public publisher/studio: **Blueborn Games**
- Support: **ykucukcinar@gmail.com**
- Application ID: `com.game.diver.oceanguard`
- Expected privacy URL: `https://giginar.github.io/ProjectBlue/privacy.html`
- Expected support URL: `https://giginar.github.io/ProjectBlue/support.html`

**GITHUB PAGES ACTIVATION/URL VERIFICATION REQUIRED.** The legal developer/controller identity
remains **LEGAL NAME INPUT REQUIRED**. Target audience, publishing countries, store language,
rewarded benefit, Data Safety and Content Rating decisions remain open. Production AdMob and
signing configuration are deferred.
**GITHUB PAGES MANUAL ACTIVATION REQUIRED:** select GitHub Actions as the Pages source in the
repository settings if it is not already enabled.

The in-game UI now supports English and Turkish. The first launch requires an explicit choice,
and Settings can switch language without an application restart. This does not decide the final
Google Play store language or publishing countries; those owner decisions remain open.

## Technical release candidate update (2026-09-20)

- [x] Fresh debug APK, R8/resource-shrunk QA APK and unsigned R8/resource-shrunk release AAB build.
- [x] QA APK passes 16 KB ZIP alignment; every packaged `libgdx.so` LOAD segment is 16 KB aligned.
- [x] Bundletool 1.18.1 reports `PAGE_ALIGNMENT_16K` for the release AAB.
- [x] Forced-EEA UMP reject/accept/Privacy Options/restart/error fallback passed with transient debug-only settings.
- [x] QA/release BuildConfig proves UMP debug geography and test-device fields are empty.
- [x] Official rewarded test ad earned/dismissed correctly and returned to the one-time continue Pause flow.
- [x] Compact 720x1280 menu spacing and independent HUD touch targets pass smoke/unit checks.
- [ ] Run on API 26; no local image is installed.
- [ ] Run on an Android 15+ 16 KB kernel; both available targets report 4096-byte pages.
- [ ] Exercise a real interstitial at the policy-approved natural transition.
- [ ] Supply owner upload signing and inspect Play-generated APKs. The current AAB is unsigned.
- [ ] Complete the remaining human, tablet/foldable/multi-window and Play Console rows.

Current decision: **READY FOR INTERNAL TESTING — SIGNING REQUIRED FOR CLOSED TESTING**.
This describes a local technical candidate and does not authorize Play upload. See
[16 KB evidence](ANDROID_16KB_COMPATIBILITY.md) and [current test report](FINAL_TEST_REPORT.md).

Official documentation checked on **2026-09-19**. New mobile apps and updates must target
**Android 16 / API 36** from **2026-08-31**, according to the
[Play target API policy](https://support.google.com/googleplay/android-developer/answer/11926878).
The existing project uses `targetSdk 36`, `compileSdk 36`, `minSdk 26`, AGP 8.13.2 and libGDX
1.14.2. Play's policy is a target requirement, not a separately mandated compile SDK number;
compile 36 also exceeds the Mobile Ads guide's compile-35 minimum. No speculative SDK bump was made.

## Build and package

- [x] Existing Java/core/Android/desktop module structure retained; no Kotlin source/plugin added.
- [x] Android-only AdMob/UMP; desktop No-Op services preserved.
- [x] Central versionCode/versionName retained. Check highest Play code before each upload.
- [x] Debug uses demo ad IDs; release has explicit opt-in configuration and separate signing.
- [x] Release is non-debuggable with R8 and resource shrinking; SDK consumer rules retained.
- [x] App JNI entry-point names preserved; no blanket keep rule for all game/Google classes.
- [x] Release app logging disabled; integration does not log identifiers or SDK error payloads.
- [x] Local properties, keystores, ad/secret configuration excluded from Git.
- [ ] Supply real upload signing via the environment, then verify the signed artifact.
- [ ] Supply approved production ad/consent/audience settings, or deliberately ship with ads disabled.

Commands (JDK and SDK paths already configured):

```powershell
.\gradlew.bat :core:test :lwjgl3:build :android:lintDebug :android:verifyDebugAdConfiguration :android:assembleDebug :android:bundleRelease
.\gradlew.bat :android:verifyReleaseBundle
powershell -File tools/verify-release-inputs.ps1
powershell -File tools/verify-android-artifacts.ps1
.\gradlew.bat :lwjgl3:run --args=--smoke
.\gradlew.bat :lwjgl3:run --args=--smoke-reload
```

Artifacts: `android/build/outputs/apk/debug/android-debug.apk`,
`android/build/outputs/apk/qa/android-qa.apk` (local R8 QA only),
`android/build/outputs/bundle/release/android-release.aab`, and `lwjgl3/build/distributions/`.
The AAB produced without owner signing credentials is unsigned and is not upload-ready.
See [signing](RELEASE_SIGNING.md). A bundle build alone does not verify Play's generated APKs.

Google requires [16 KB page compatibility](https://developer.android.com/guide/practices/page-sizes)
for applicable Android 15+ Play submissions from 2025-11-01. Check both APK ZIP alignment
(`zipalign -c -P 16 4`) and 64-bit ELF LOAD-segment alignment, and test a 16 KB device/emulator.
The app packages libGDX natives for arm64-v8a, armeabi-v7a and x86_64 with non-legacy JNI packaging.
Use Play/internal testing or bundletool to inspect APKs generated from the final signed AAB.
The [libGDX deployment guide](https://libgdx.com/wiki/deployment/deploying-your-application)
documents Android packaging considerations.

## Required device and console checks

- [x] Fresh/updated debug installs reach offline gameplay on API 29 hardware and API 36 emulator.
- [ ] Complete, skip, fail, background and process-kill rewarded ads; no duplicate payment.
- [ ] Continue once, fail again, replay normally; no duplicate base salvage or progression.
- [ ] First session, insufficient completions, cooldown, failure and active gameplay show no interstitial.
- [x] Offline rewarded/interstitial load failure leaves startup and gameplay usable on both targets.
- [ ] Privacy Options updates cached ads correctly in a forced-consent region.
- [ ] Test UMP regions/choices using the [privacy matrix](PRIVACY_CHECKLIST.md).
- [x] Install and launch the debug-signed local R8/resource-shrunk QA APK on API 29 and 36.
- [ ] Install a signed minified APK generated from the final AAB; verify 16 KB runtime support.
- [ ] Play internal track/pre-launch report: crashes, ANRs, devices, permissions, accessibility.
- [ ] Verify the prepared public privacy policy live; complete Data Safety, ads, target audience,
  app access and content-rating forms.
- [ ] Confirm developer/account verification and testing requirements shown for this Play account.
- [ ] Confirm original/licensed assets and store artwork against `assets/licenses/ASSET_LICENSES.md`.

Google Play Games remains an optional adapter extension; local achievements work offline.
Do not make sign-in mandatory for this release. See [integration notes](ADS_INTEGRATION.md).

## Verification record

Device addendum on 2026-09-19 used Git-derived version
`0.1.17-g016d001ad0f4-dirty`, versionCode 17:

| Check | Result |
| --- | --- |
| Core unit suite | 254 tests, zero failures/errors; includes advertising, rewarded-continue HUD and pre-create Android lifecycle regression coverage |
| Desktop distribution build | Passed |
| Desktop OpenGL smoke | Passed: menus, all ten sectors, saves, replay, equipment, lifecycle |
| Separate desktop process reload | Passed: progression, records, equipment, achievements, settings |
| Android debug APK | Built; official demo configuration, signature and ZIP alignment verified |
| Local QA APK | R8/resource-shrunk, debug-signed, ads disabled; an API 29 early-resume crash was fixed, then locked-screen/repeated launches passed on API 29 and API 36; not Play-uploadable |
| Android lint | Zero errors; four existing compatibility/orientation/tool-version warnings |
| Release AAB | Built with R8/resource shrinking; bundletool structural validation passed |
| 64-bit native libraries in APK/AAB | arm64-v8a and x86_64 ELF LOAD segments support 16 KB alignment |
| Signing | Zero AAB signature blocks: intentionally unsigned; no release key supplied |
| Source/index identifiers and ignored files | Verification script passed; only official demo ad IDs |
| Runtime SDK/device matrix | Partial pass: Huawei SNE-LX1/API 29 and x86_64 emulator/API 36; see `MANUAL_TEST_MATRIX.md` |

The AGP-supplied bundletool emits an SDK XML-version compatibility warning on this installed
command-line SDK; validation still completes successfully. Gradle reports existing deprecated
features ahead of Gradle 9. Neither is evidence of a Play acceptance test.
Actual Android airplane/offline startup, official rewarded test-ad callback and local minified
QA launch are verified. **Forced UMP regional forms, a real interstitial placement, final
signed-AAB APKs, API 26 and a 16 KB runtime are not verified.**
No production identifiers, release keys or commits were created.
