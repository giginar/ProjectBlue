# Google Play internal testing guide

Use the `internalTest` build type for Google Play Internal Testing. It keeps the production
application ID, release optimization and Ocean Guard upload signature while enabling only
Google's exact official test App ID, rewarded unit and interstitial unit. Do not upload an
ads-disabled release bundle or substitute production identifiers in this variant.

## Build matrix

| Variant | Package | Signing | Ads |
| --- | --- | --- | --- |
| debug | `com.game.diver.oceanguard` | debug | TEST |
| qa | `com.game.diver.oceanguard.qa` | debug | DISABLED |
| internalTest | `com.game.diver.oceanguard` | Ocean Guard upload key | TEST |
| release | `com.game.diver.oceanguard` | Ocean Guard upload key | DISABLED, or external production config |

Load the external Ocean Guard signing environment without printing its values, then run one
Gradle process:

```powershell
.\gradlew.bat :android:packageInternalGooglePlayBundle --console=plain
```

The task runs the exact-ID/configuration guard, builds `bundleInternalTest`, validates the bundle
with the AGP-provided bundletool, verifies the production package and non-debuggable manifest,
and preserves the AAB plus R8 reports under `build/store/google-play/`.

## Build 27 Play entry

Release name: `Ocean Guard Internal 0.1.27 (27)`

English release notes:

> Improved ocean cleanup with visible collection progress rings. Missions now finish promptly
> after boss defeat, and the standard gameplay timer has been removed for a smoother experience.
> This build also enables Google test ads for internal QA.

Enter the separately reviewed Turkish translation in Play Console. Repository documentation stays
in English under the repository language policy.

## Tester focus

- Cleanup progress-ring feel, including leaving and re-entering collection range.
- Boss defeat and Results transition.
- English and Turkish text and layout.
- UMP consent behavior and Privacy Options availability.
- Rewarded test-ad button and reward callback.
- Three-completion and ten-minute interstitial policy.
- Results-to-menu ad transition.
- Save and stage progression.

UMP uses its real requirement status. The distributed bundle contains no forced EEA geography or
test-device identifier. With the sample App ID, a privacy message or Privacy Options entry may not
appear on every device. Form visibility, Privacy Options availability, rewarded presentation and
the policy-approved interstitial transition require manual device verification.
