# Project Blue final release candidate test report

Date: 2026-09-19. Decision: **NOT READY FOR RELEASE**.

The automated candidate checks pass after seven focused defect fixes. Publication remains
blocked by unsigned release output, missing real Android acceptance evidence, privacy/store
owner inputs and approved store exports. This report does not certify a full human campaign
playthrough or Android runtime behavior from desktop/build results.

## Candidate and scope

| Item | Inspected candidate |
|---|---|
| Repository / branch | ProjectBlue / `main`; pre-commit QA snapshot |
| Baseline HEAD | `07f65ef45ace3bd9ad52860befce5078e6af260b` |
| Audited artifact version | `0.1.16-g07f65ef45ace-dirty`, versionCode `16` |
| Initial working tree | 34 modified tracked files and 20 untracked status entries; preserved |
| Host | Windows x64; Microsoft OpenJDK 21.0.12; Java source/bytecode 17 |
| Framework/build | libGDX 1.14.2, Gradle 8.13, AGP 8.13.2 |
| Android | minSdk 26, compile/target 36, build-tools 35.0.0 |
| Android SDKs | AdMob 25.5.0, UMP 4.0.0; Android-only dependencies |
| Release configuration | `DEBUG=false`, R8 and resource shrinking enabled, ads disabled, no upload signing supplied |
| Device availability | `adb devices -l`: no connected device/emulator |

Read the repository instructions and inventoried source, configuration, tests, documentation,
assets, scripts and Android release setup before editing. Reviewed the shared simulation,
all ten authored missions and bosses, input/render/resource ownership, progression/save
paths, ads/consent boundaries, build rules and asset provenance. No architecture replacement,
Kotlin, new major feature, dependency upgrade, production ad ID or signing secret was added.
Initial status, binary diff and file hashes were preserved under ignored `build/final-qa/`.
No destructive Git command was used. The audit and artifact verification were completed
against the pre-commit snapshot above; the later source commit/push was explicitly requested
after the report was produced and does not change the recorded artifact hashes.

## Executed verification

| Check | Result | Evidence / limits |
|---|---|---|
| Initial baseline | PASS: 198 JUnit tests; desktop and Android builds | `build/final-qa/baseline-build.log`; passing baseline did not cover the defects below |
| Final full unit/headless suite | PASS: **252 tests, 32 suites, 0 failures, 0 errors, 0 skipped** | `core/build/reports/tests/test/index.html`, XML results; all tests rerun |
| Headless world integration | PASS: all 10 sectors x 4 difficulties; boss/environment/save tests | Included in the 252 count, not an additional test count; no separate headless module |
| Desktop build/distributions | PASS | `:lwjgl3:build`; Java-runtime distribution built; Windows installer packaging was not rerun |
| Desktop GL smoke | PASS | Real OpenGL process, all ten sector render paths, menus, drag, pause/lifecycle, results, replay, equipment, settings and aspect ratios |
| Texture recovery regression | PASS | Smoke deletes generated UI/font GPU textures, invalidates/reloads managed textures and checks valid handles/count; not actual Android OS context loss |
| Separate-process save reload | PASS | `--smoke-reload` restores campaign, equipment, achievements and settings from isolated smoke profile |
| Android debug APK | PASS | `:android:assembleDebug`; debug signature, ZIP alignment and 64-bit native alignment verified |
| Android release AAB | PASS as **unsigned verification artifact** | `:android:bundleRelease`, R8/resource shrinking and `:android:verifyReleaseBundle` passed |
| Android debug unit tests | NO-SOURCE | Task executed; no Android unit/instrumentation tests exist. No device test pass is claimed |
| Desktop test source set | NO-SOURCE | Integration coverage comes from the separate GL smoke run |
| Android lint debug/release | PASS with warnings: **0 errors, 4 warnings per variant** | `android/build/reports/lint-results-debug.*`, `lint-results-release.*` |
| IntelliJ static inspections | No errors in requested changed Java files; 44 warnings returned | `build/final-qa/final-ide-inspections.json`; reviewed below; not whole-program proof |
| Asset inventory/hash gate | PASS | Exactly five custom media files plus two exempt inventory documents; isolated regeneration matches all five |
| Asset-gate negative regression | PASS | Unlisted file under `assets/licenses/` rejected; temporary probe removed |
| Release-input scan | PASS | Only official demo ad identifiers; ignored sensitive paths; no literal signing credentials found |
| Packaged content inspection | PASS | Debug APK and AAB contain all ten mission JSONs, catalog and expected custom assets/inventories |
| Minimum-API regression | PASS in tests and DEX inspection | Incompatible `InputStream.readAllBytes()` invocation removed from MissionConfig debug DEX |
| Script syntax / diff whitespace | PASS | PowerShell parser checks and `git diff --check`; Git line-ending notices are not whitespace errors |
| Versioning fixture script | Not run | `tools/test-versioning.ps1` creates temporary Git commits; omitted to honor the no-commit instruction |
| Android runtime / R8 device execution | NOT RUN | No device/emulator; required before release |

The complete final Gradle command was:

```powershell
.\gradlew.bat :check :lwjgl3:build :android:testDebugUnitTest :android:lintDebug :android:lintRelease :android:verifyDebugAdConfiguration :android:assembleDebug :android:bundleRelease :android:verifyReleaseBundle --rerun-tasks --warning-mode all
```

It completed successfully in 2m 39s with 118 tasks executed. Additional commands and
publication steps are in [RELEASE_STEPS.md](RELEASE_STEPS.md). The final GL smoke and reload
logs are `build/final-qa/final-smoke.log` and `final-smoke-reload.log`; captures are under
`build/smoke/`. Build evidence is local/ignored and should be archived with the candidate
before those output directories are replaced by another run.

## Defects fixed and regression evidence

| Fix | Before / root cause | Minimal correction and validation |
|---|---|---|
| F-01 Android API compatibility | Required mission loading directly invoked `InputStream.readAllBytes()` in the generated DEX, although that method requires API 33 and minSdk is 26. Android lint did not catch this core-module use. | Shared buffered reader with a 128 KiB limit; tests simulate an unavailable API and oversized input; final DEX has no such invocation. [Android API reference](https://developer.android.com/reference/java/io/InputStream#readAllBytes()) |
| F-02 Final-escape continue | Debugger observed `finished=true`, `ESCAPE_FAILED`, full hull after continue and one update. The failed boss state survived the continue. | Restart escape warning for active/failed escape; single-use continue remains enforced. Eight parameterized final completion/continue cases pass across four difficulties. |
| F-03 Two-finger actions | Debugger observed movement pointer 0 causing pointer 1 to return before sonar handling. | Check pause/sonar hit targets before rejecting an additional movement pointer; three input regressions pass. |
| F-04 Texture recreation | Debugger reported a generated menu texture `managed=false`; the new GL smoke assertion failed. Pixmaps were disposed immediately after uploading unmanaged textures. | Retain the tiny CPU sources for managed reload and dispose them with the theme. Real GL delete/reload smoke passes. |
| F-05 Wildlife pool cleanup | Debugger observed a freed creature still active at x=600 and y=1757 after 600 frames: vortex clamping prevented the strict greater-than despawn condition. | Include the boundary and upper escape bound; regression confirms despawn and reuse of the same pool slot. |
| F-06 Laser/environment collision | Debugger observed coral health 30, enemy health 40 and coral damage 0 after a laser shot through coral. | Nearest-target laser selection includes protected coral, records habitat damage and never grants coral kill/salvage rewards; regression passes. |
| F-07 Unregistered asset bypass | The entire `licenses/` subtree was excluded from the packageable-file inventory. | Only two exact metadata files are exempt; hash keys and documented paths must match the allowlist. Negative probe test passes. |

Failing-before and passing-after logs are retained in `build/final-qa/` as
`regressions-before/after.log`, `campaign-before/after.log`, `texture-before.log`,
`texture-after-smoke.log`, and `android-api-before/after.log`. Runtime investigations used
IntelliJ debugger values/breakpoints after short-lived logpoint sessions were insufficient.
Agent-created breakpoints and sessions were removed; existing disabled user exception
breakpoints were preserved. No speculative Android runtime pass was inferred from JVM evidence.

Local source anchors for the runtime fixes: [continue state](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/logic/GameWorld.java:60),
[escape reset](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/logic/LeviathanCore.java:49),
[pointer dispatch](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/input/PointerInput.java:22),
[managed texture ownership](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/ui/MenuTheme.java:64),
[wildlife cleanup](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/logic/GameWorld.java:1343),
[laser target selection](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/logic/GameWorld.java:1429)
and [compatible mission reader](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/config/MissionConfig.java:300).

## Gameplay, progression and content assessment

Movement, relative drag, automatic fire, all five weapons, bullets/collisions, hull/shield,
cleanup beam, waste collection, rescue, environmental penalties, pauses, results, bosses,
finale and replay have source/test coverage. The second-finger, laser/coral, freed-creature
and final-escape defects above are now covered explicitly. Active sonar is available in
sonar-enabled sectors; vessel special-ability descriptions otherwise represent passive
bonuses. Physical controls and readability remain open device checks.

All ten sector records, sequential sector/difficulty locks, independent best stars/scores/
Cleanup/Rescue, salvage, six upgrade types, four pilots, three vessels, five weapons,
sixteen local achievements and final completion were reviewed against config and tests.
Schema v5 preserves earlier profile formats through explicit/additive migrations; frozen
v2 data, checksums, backup recovery, temporary/atomic writes and write-failure rollback are
covered. Corrupt primary and backup fall back visibly to defaults. In-progress dives are
not persisted across process death; completed progression is.

[MANUAL_TEST_MATRIX.md](MANUAL_TEST_MATRIX.md) contains a row for every sector's theme,
environment problem, mechanic, enemy/rescue roster, midpoint, boss/start/deadline, rewards
and NEREID/story connection, plus all 40 human-clear slots. Recovery rendering/result views
and difficulty multipliers are present for all ten. Blue Coast's midpoint is a generic
fallback, not a distinct authored encounter; Ghost Nets' NEREID link is indirect.

The 40 route tests restore hull, execute the full timeline and expect deadline failure
without bypassing boss gates. They verify finite player coordinates, bullet limits and
authored enemy counts. Boss tests/desktop smoke also use assisted combat in places.
These are **not evidence of ordinary-input clearability, fair difficulty or first-clear
economy balance**. The four-difficulty final gate/escape tests do confirm that the tested
restoration/energy/sonar sequence can reach completion with the standard loadout under
their assisted conditions.

## Mobile, advertising and technical assessment

Portrait FitViewport and menu ExtendViewport paths, back navigation, input cancellation,
pause/resume and save-on-background are implemented. Android window insets are handled in
the launcher, but physical cutouts/asymmetric padding need testing. Narrow/wide desktop
captures pass layout navigation; small sonar/pause targets, HUD text and overlapping
achievement/story banners remain findings. Large UI does not comprehensively scale the
game HUD. Reduced particles, background detail and motion are available.

Debug uses official Google test IDs. Reward settlement requires earned+dismissed callbacks
and a successful durable claim, with duplicate/stale/failure paths tested. Continue is
limited to one per run. Interstitials are restricted to successful-result navigation, skip
the first session, and use three-clear/three-minute caps. Consent relies on UMP permission;
Privacy Options invalidates stale ads. Offline/no-fill paths preserve gameplay/navigation
in shared tests/source review. Android SDK callbacks, forms, region changes and process
interruption remain unverified. No production identifiers were found in repository inputs;
the tested release disables ads and uses empty reward/interstitial unit IDs.

Entities use fixed pools; offscreen cleanup and listener/resource ownership were reviewed.
AssetManager owns audio/font assets, theme owns generated textures/source pixels, and
screen transitions release stages/listeners. The vortex pool retention and unmanaged
texture defects were corrected. This source/short-smoke review does not establish absence
of leaks: device heap/native-memory and thermal soak remain required. Some route/render/
menu allocations remain. Config field/range/ID validation exists; malformed cross-subsystem
combinations still need stronger validation before future content additions.

Release BuildConfig is non-debuggable; reset code is listed as removed in R8 `usage.txt`.
Desktop smoke code is outside Android dependencies. The release contains all mission data;
bundletool validates structure and native alignment checks pass. Runtime after obfuscation
has not been exercised on Android. Secret scans/log review found no credential or profile
payload logging in the inspected paths; that is scoped evidence, not a universal guarantee.

## Warnings reviewed

- Android lint: `UnusedAttribute` for predictive-back attribute on older APIs;
  `AndroidGradlePluginVersion` recommends a newer wrapper; `LockedOrientationActivity`
  and `DiscouragedApi` flag portrait restrictions/Android 16 large-screen behavior.
  The verified pinned build pair was retained; orientation requires device acceptance.
- Javac notes legacy inset API use in AndroidLauncher. The API 26-29 compatibility path
  needs those calls; newer versions use their guarded inset APIs.
- The final build reports SDK XML version 4 being read by tooling supporting up to 3.
  Build/lint/artifact checks still pass. Align SDK/tooling versions in a separately verified
  environment before publication; no opportunistic toolchain upgrade was made here.
- IntelliJ warnings include unused members/constants, redundant escapes/casts, nullable
  mission/subsystem paths and a stream-copy efficiency suggestion. Shipped mission paths
  pass the 40-route suite; future invalid config combinations are MP-02. The bounded UTF-8
  byte conversion was retained rather than adopting a newer Android-incompatible overload.
- Deprecated Gradle `shrinkResources` assignment syntax was corrected. Git's LF/CRLF notices
  remain host configuration notices; `git diff --check` reports no whitespace errors.

## Generated artifacts

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| `android/build/outputs/apk/debug/android-debug.apk` | 9697750 | `bc0e79716ba0fe1c5c0000f124e0897267256ddbb06e5ec2d3f10ae9134bfec8` |
| `android/build/outputs/bundle/release/android-release.aab` | 5621725 | `5f677333c5a77b878d04b04b15cdaf64e895e8d2fcbaec13e096dbaba7466ceb` |

APK is debug-signed; AAB has **zero signature blocks**. These hashes identify local QA
outputs, not publishable signed releases. Both include arm64-v8a, armeabi-v7a and x86_64;
64-bit ELF LOAD alignment is at least 16384 bytes, and debug APK ZIP alignment passes 16 KB
verification. Actual 16 KB device execution is still required. Preserve the matching
`android/build/outputs/mapping/release/mapping.txt` with a later signed artifact.

## Files changed by this audit

Pre-existing user edits remain in these and other files. The list below describes this
audit's additional changes, not ownership of the entire working-tree diff.

- Production Java: `core/.../config/MissionConfig.java`, `input/PointerInput.java`,
  `logic/GameWorld.java`, `logic/LeviathanCore.java`, `ui/MenuTheme.java`.
- Regression/integration: `core/src/test/java/com/projectblue/game/config/MissionConfigTest.java`,
  `input/PointerInputTest.java` (new), `logic/NereidCoreWorldTest.java`,
  `logic/ReleaseCandidateWorldTest.java` (new), and
  `lwjgl3/src/main/java/com/projectblue/game/lwjgl3/DesktopSmokeGame.java`.
- Build/tooling: root `build.gradle`, `android/build.gradle`, new
  `tools/test-asset-licenses.ps1`.
- Inventory/current guidance: `assets/licenses/ASSET_LICENSES.md`, `README.md`,
  `docs/SECTORS_2_3.md`, `docs/EQUIPMENT_SYSTEMS.md`, `docs/META_PROGRESSION.md`,
  `docs/STORE_ASSET_PLAN.md`.
- Requested reports: this file, [KNOWN_ISSUES.md](KNOWN_ISSUES.md),
  [RELEASE_STEPS.md](RELEASE_STEPS.md), [ASSET_AUDIT.md](ASSET_AUDIT.md),
  [MANUAL_TEST_MATRIX.md](MANUAL_TEST_MATRIX.md), [STORE_LISTING_DATA.md](STORE_LISTING_DATA.md).

## Final classification

| Classification | Outstanding findings |
|---|---|
| Release blocker | RB-01 unsigned AAB/owner package-version confirmation; RB-02 Android runtime acceptance; RB-03 public privacy/developer/Play declarations; RB-04 approved store exports |
| High priority | HP-01 human campaign/balance validation; HP-02 physical touch/HUD usability; HP-03 production ads if enabled; HP-04 Blue Coast authored midpoint gap |
| Medium priority | MP-01 allocation/soak profiling; MP-02 config cross-validation; MP-03 cutout/inset behavior; MP-04 story link; MP-05 ability expectation; MP-06 process-death limitations |
| Cosmetic | C-01 placeholders; C-02 banner/text overlap; C-03 generic recovery wording |
| Manual verification required | Device/lifecycle/R8/ads/accessibility and 40 human clears listed in the manual matrix |

Detailed closure criteria are in [KNOWN_ISSUES.md](KNOWN_ISSUES.md). Until release blockers
have evidence of closure, the decision remains **NOT READY FOR RELEASE**. No signing-key
creation, store upload or publication was performed by this audit.
